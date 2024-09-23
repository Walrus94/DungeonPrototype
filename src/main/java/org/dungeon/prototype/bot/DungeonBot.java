package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.exception.CallbackException;
import org.dungeon.prototype.exception.ChatStateUpdateException;
import org.dungeon.prototype.exception.DeleteMessageException;
import org.dungeon.prototype.exception.SendMessageException;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.nonNull;
import static org.dungeon.prototype.bot.ChatState.ACTIVE;
import static org.dungeon.prototype.bot.ChatState.IDLE;

@Slf4j
@Component
public class DungeonBot extends TelegramLongPollingBot {
    private static final long TIMEOUT_DURATION = Duration.ofMinutes(30).toMillis();
    private final Map<Long, ChatContext> chatStateByIdMap;
    private final String botUsername;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private CallbackHandler callbackHandler;
    @Autowired
    private BotCommandHandler botCommandHandler;

    public DungeonBot(String botToken, String botUsername) {
        super(botToken);
        this.botUsername = botUsername;
        chatStateByIdMap = new ConcurrentHashMap<>();
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Handles all updates received by bot: commands, callbacks etc.
     * Main entry point of bot
     *
     * @param update Update received
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            //handles message text if present
            val chatId = update.getMessage().getChatId();
            val messageText = update.getMessage().getText();
            if (!handleBotCommand(chatId, messageText)) {
                playerService.registerPlayerAndSendStartMessage(chatId, messageText);
            }
        } else if (update.hasCallbackQuery()) {
            //or handles callback
            val callbackQuery = update.getCallbackQuery();
            val chatId = callbackQuery.getMessage().getChatId() == null ?
                    update.getMessage().getChatId() :
                    callbackQuery.getMessage().getChatId();
            callbackHandler.handleCallbackQuery(chatId, callbackQuery);
        }
    }

    /**
     * Answers processed callback query.
     * Should be executed by annotating method with
     * {@link org.dungeon.prototype.annotations.aspect.AnswerCallback}
     *
     * @param callbackQueryId id of the query
     */
    public void answerCallbackQuery(String callbackQueryId) {
        val answerCallbackQuery = AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .build();
        try {
            execute(answerCallbackQuery);
        } catch (TelegramApiException e) {
            throw new CallbackException(callbackQueryId, e.getMessage());
        }
    }

    /**
     * Updates chat state to mark with {@link org.dungeon.prototype.bot.ChatState.IDLE}
     * ones that were inactive for {@link TIMEOUT_DURATION}
     */
    @Scheduled(fixedRate = 60000)
    public void checkChatTimeouts() {
        val currentTime = System.currentTimeMillis();
        chatStateByIdMap.forEach((chatId, chatContext) -> {
            if (!IDLE.equals(chatContext.getChatState()) &&
                    currentTime - chatContext.getLastActiveTime() > TIMEOUT_DURATION) {
                chatContext.setChatState(IDLE);
            }
        });
    }

    /**
     * Updates initialized chat in {@param from} state with {@param to}
     * Returns true if chat state successfully updated
     * Should be executed by annotating method with {@link org.dungeon.prototype.annotations.aspect.ChatStateUpdate}
     * @param chatId id of chat to update
     * @param from initial state
     * @param to resulting state
     */
    public void updateChatState(Long chatId, ChatState from, ChatState to) {
        if (chatStateByIdMap.containsKey(chatId) && chatStateByIdMap.get(chatId).getChatState().equals(from)) {
            chatStateByIdMap.get(chatId).setChatState(to);
        } else {
            throw new ChatStateUpdateException(chatId, from, to);
        }
    }

    /**
     * Initializes chat context: sets chat context state
     * with {@link org.dungeon.prototype.bot.ChatState.ACTIVE} or creates new one
     * with the same state
     * Should be executed by annotating method with {@link org.dungeon.prototype.annotations.aspect.InitializeChatContext}
     * @param chatId id of chat to update
     */
    public void initializeChatContext(Long chatId) {
        if (!chatStateByIdMap.containsKey(chatId)) {
            chatStateByIdMap.put(chatId, new ChatContext());
        } else {
            chatStateByIdMap.get(chatId).setChatState(ACTIVE);
        }
    }

    /**
     * Sends text message to chat
     * and deletes previous sent message if possible
     * Should be executed by annotating method with {@link org.dungeon.prototype.annotations.aspect.MessageSending}
     * @param chatId id of chat
     * @param message message to be sent
     */
    public void sendMessage(Long chatId, SendMessage message) {
        try {
            val messageId = execute(message).getMessageId();
            if (messageId == -1) {
                throw new SendMessageException(chatId, CallbackType.DEFAULT_ERROR_RETURN);
            }
            updateLastMessage(chatId, messageId);
        } catch (TelegramApiException e) {
            throw new SendMessageException(e.getMessage(), chatId, CallbackType.DEFAULT_ERROR_RETURN);
        }
    }

    /**
     * Sends photo message to chat
     * and deletes previous sent message if possible
     * Should be executed by annotating method with {@link org.dungeon.prototype.annotations.aspect.PhotoMessageSending}
     * @param chatId id of chat
     * @param message message to be sent
     */
    public void sendMessage(Long chatId, SendPhoto message) {
        try {
            val messageId = execute(message).getMessageId();
            if (messageId == -1) {
                throw new SendMessageException(chatId, CallbackType.DEFAULT_ERROR_RETURN);
            }
            updateLastMessage(chatId, messageId);
        } catch (TelegramApiException e) {
            throw new SendMessageException(e.getMessage(), chatId, CallbackType.DEFAULT_ERROR_RETURN);
        }
    }

    private void updateLastMessage(Long chatId, Integer messageId) {
        if (chatStateByIdMap.containsKey(chatId) && nonNull(chatStateByIdMap.get(chatId).getLastMessageId())) {
            deleteMessage(chatId, chatStateByIdMap.get(chatId).getLastMessageId());
            chatStateByIdMap.get(chatId).setLastMessageId(messageId);
            chatStateByIdMap.get(chatId).setLastActiveTime(System.currentTimeMillis());
        } else {
            if (!chatStateByIdMap.containsKey(chatId)) {
                val chatState = new ChatContext();
                chatState.setLastMessageId(messageId);
                chatStateByIdMap.put(chatId, new ChatContext());
            } else {
                chatStateByIdMap.get(chatId).setLastMessageId(messageId);
                chatStateByIdMap.get(chatId).setLastActiveTime(System.currentTimeMillis());
            }
        }
    }

    private void deleteMessage(long chatId, Integer messageId) {
        val deleteMessage = DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();
        try {
            if (!execute(deleteMessage)) {
                throw new DeleteMessageException(chatId, messageId, CallbackType.DEFAULT_ERROR_RETURN);
            }
        } catch (TelegramApiException e) {
            throw new DeleteMessageException(chatId, messageId, e.getMessage(), CallbackType.DEFAULT_ERROR_RETURN);
        }
    }

    private boolean handleBotCommand(Long chatId, String messageText) {
        if (messageText.equals("/start") && isStartAvailable(chatId)) {
            botCommandHandler.processStartAction(chatId);
            return true;
        }
        return false;
    }

    private Boolean isStartAvailable(Long chatId) {
        return !chatStateByIdMap.containsKey(chatId) || IDLE.equals(chatStateByIdMap.get(chatId).getChatState());
    }
}
