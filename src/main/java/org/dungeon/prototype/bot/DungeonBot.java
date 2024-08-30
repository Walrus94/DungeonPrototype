package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
            if (!handleBotCommand(update, chatId, messageText)) {
                playerService.registerPlayerAndSendStartMessage(chatId, messageText);
            }
        } else if (update.hasCallbackQuery()) {
            //or handles callback
            val callbackQuery = update.getCallbackQuery();
            val chatId = callbackQuery.getMessage().getChatId() == null ?
                    update.getMessage().getChatId() :
                    callbackQuery.getMessage().getChatId();
            if (!callbackHandler.handleCallbackQuery(chatId, callbackQuery)) {
                log.warn("Unable to handle callback {}", callbackQuery.getId());
            }
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
            log.error("Unable to answer callback: {}", callbackQueryId);
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
     * @return false, in case chat not initialized or has
     * different initial state
     */
    public boolean updateChatState(Long chatId, ChatState from, ChatState to) {
        if (chatStateByIdMap.containsKey(chatId) && chatStateByIdMap.get(chatId).getChatState().equals(from)) {
            chatStateByIdMap.get(chatId).setChatState(to);
            return true;
        }
        return false;
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
                //TODO: handle exception
            }
            updateLastMessage(chatId, messageId);
        } catch (TelegramApiException e) {
            log.error("Unable to send message: ", e);
            //TODO: handle exception
        }
    }

    /**
     * Sends photo message to chat
     * and deletes previous sent message if possible
     * Should be executed by annotating method with {@link org.dungeon.prototype.annotations.aspect.PhotoMessageSending}
     * @param chatId id of chat
     * @param message message to be sent
     * @return true if message successfully sent
     */
    public boolean sendMessage(Long chatId, SendPhoto message) {
        try {
            val messageId = execute(message).getMessageId();
            if (messageId == -1) {
                return false;
            }
            updateLastMessage(chatId, messageId);
            return true;
        } catch (TelegramApiException e) {
            log.error("Unable to send message: ", e);
            return false;
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

    private boolean deleteMessage(long chatId, Integer messageId) {
        val deleteMessage = DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();
        try {
            return execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Unable to edit message id:{}. {}", messageId, e);
            return false;
        }
    }

    private boolean handleBotCommand(Update update, Long chatId, String messageText) {
        if (messageText.equals("/start") && isStartAvailable(chatId)) {
            val nickname = update.getMessage().getFrom().getUserName() == null ?
                    update.getMessage().getFrom().getFirstName() :
                    update.getMessage().getFrom().getUserName();
            botCommandHandler.processStartAction(chatId, nickname);
            return true;
        }
        return false;
    }

    private Boolean isStartAvailable(Long chatId) {
        return !chatStateByIdMap.containsKey(chatId) || IDLE.equals(chatStateByIdMap.get(chatId).getChatState());
    }
}
