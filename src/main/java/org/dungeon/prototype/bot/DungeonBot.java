package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.service.MessageService;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.level.LevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.nonNull;
import static org.dungeon.prototype.bot.State.ACTIVE;
import static org.dungeon.prototype.bot.State.AWAITING_NICKNAME;
import static org.dungeon.prototype.bot.State.IDLE;

@Slf4j
@Component
public class DungeonBot extends TelegramLongPollingBot  {
    private static final long TIMEOUT_DURATION = Duration.ofMinutes(30).toMillis();
    private final Map<Long, ChatState> chatStateByIdMap;
    private final String botUsername;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private LevelService levelService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private CallbackRouter callbackRouter;

    public DungeonBot(String botToken, String botUsername) {
        super(botToken);
        this.botUsername = botUsername;
        chatStateByIdMap = new ConcurrentHashMap<>();
    }

    private Boolean isStartAvailable(Update update) {
        if (update.hasMessage()) {
            val chatId = update.getMessage().getChatId();
            return !chatStateByIdMap.containsKey(chatId) || IDLE.equals(chatStateByIdMap.get(chatId).getState());
        } else {
            return true;
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            val chatId = update.getMessage().getChatId();
            val messageText = update.getMessage().getText();
            if (messageText.equals("/start")) {
                processStartAction(chatId);
            }
            if (chatStateByIdMap.containsKey(chatId) && AWAITING_NICKNAME.equals(chatStateByIdMap.get(chatId).getState())) {
                val nickname = update.getMessage().getText();
                val player = playerService.addNewPlayer(chatId, nickname);
                log.debug("Player generated: {}", player);
                chatStateByIdMap.get(chatId).setState(ACTIVE);
                messageService.sendStartMessage(chatId, nickname, false);
            }
        } else if (update.hasCallbackQuery()) {
            val callbackQuery = update.getCallbackQuery();
            val chatId = callbackQuery.getMessage().getChatId() == null ?
                    update.getMessage().getChatId() :
                    callbackQuery.getMessage().getChatId();
            if (!callbackRouter.handleCallbackQuery(chatId, callbackQuery)) {
                log.warn("Unable to handle callback {}", callbackQuery.getId());
            }
        }
    }

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

    @Scheduled(fixedRate = 60000)
    public void checkChatTimeouts() {
        val currentTime = System.currentTimeMillis();
        chatStateByIdMap.forEach((chatId, chatState) -> {
            if (!IDLE.equals(chatState.getState()) &&
                    currentTime - chatState.getLastActiveTime() > TIMEOUT_DURATION) {
                chatState.setState(IDLE);
            }
        });
    }

    private void processStartAction(Long chatId) {
        if (!chatStateByIdMap.containsKey(chatId)) {
            chatStateByIdMap.put(chatId, new ChatState());
        } else {
            chatStateByIdMap.get(chatId).setState(ACTIVE);
        }
        if (!playerService.hasPlayer(chatId)) {
            sendRegisterMessage(chatId);
        } else {
            val hasSavedGame = levelService.hasLevel(chatId);
            val nickname = playerService.getNicknameByChatId(chatId);
            messageService.sendStartMessage(chatId, nickname.orElse(""), hasSavedGame);
        }
    }

    private boolean sendRegisterMessage(Long chatId) {
        chatStateByIdMap.get(chatId).setState(AWAITING_NICKNAME);
        return sendPromptMessage(chatId, "Welcome to dungeon!\nPlease, enter nickname to register");
    }

    private boolean sendPromptMessage(Long chatId, String text) {
        ForceReplyKeyboard keyboard = ForceReplyKeyboard.builder()
                .forceReply(false)
                .build();
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        return sendMessage(chatId, message);
    }

    public boolean sendMessage(Long chatId, SendMessage message) {
        try {
            val messageId = execute(message).getMessageId();
            if (messageId == -1) {
                return false;
            }
            if (chatStateByIdMap.containsKey(chatId) && nonNull(chatStateByIdMap.get(chatId).getLastMessageId())) {
                deleteMessage(chatId, chatStateByIdMap.get(chatId).getLastMessageId());
                chatStateByIdMap.get(chatId).setLastMessageId(messageId);
                chatStateByIdMap.get(chatId).setLastActiveTime(System.currentTimeMillis());
            } else {
                if (!chatStateByIdMap.containsKey(chatId)) {
                    val chatState = new ChatState();
                    chatState.setLastMessageId(messageId);
                    chatStateByIdMap.put(chatId, new ChatState());
                } else {
                    chatStateByIdMap.get(chatId).setLastMessageId(messageId);
                    chatStateByIdMap.get(chatId).setLastActiveTime(System.currentTimeMillis());
                }
            }
            return true;
        } catch (TelegramApiException e) {
            log.error("Unable to send message: ", e);
            return false;
        }
    }

    public boolean sendMessage(Long chatId, SendPhoto message) {
        try {
            val messageId = execute(message).getMessageId();
            if (messageId == -1) {
                return false;
            }
            if (chatStateByIdMap.containsKey(chatId) && nonNull(chatStateByIdMap.get(chatId).getLastMessageId())) {
                deleteMessage(chatId, chatStateByIdMap.get(chatId).getLastMessageId());
                chatStateByIdMap.get(chatId).setLastMessageId(messageId);
                chatStateByIdMap.get(chatId).setLastActiveTime(System.currentTimeMillis());
            } else {
                if (!chatStateByIdMap.containsKey(chatId)) {
                    val chatState = new ChatState();
                    chatState.setLastMessageId(messageId);
                    chatStateByIdMap.put(chatId, new ChatState());
                } else {
                    chatStateByIdMap.get(chatId).setLastMessageId(messageId);
                    chatStateByIdMap.get(chatId).setLastActiveTime(System.currentTimeMillis());
                }
            }
            return true;
        } catch (TelegramApiException e) {
            log.error("Unable to send message: ", e);
            return false;
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
}
