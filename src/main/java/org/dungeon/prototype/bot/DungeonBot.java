package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.exception.CallbackException;
import org.dungeon.prototype.exception.ChatException;
import org.dungeon.prototype.exception.DeleteMessageException;
import org.dungeon.prototype.exception.SendMessageException;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.state.ChatStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.starter.SpringWebhookBot;

import java.util.List;

@Slf4j
@Component
public class DungeonBot extends SpringWebhookBot {

    private static final String AUTH_EXCEPTION_MESSAGE = "Unauthorized access. This bot is for development purposes only. Contact @arsnazarov for more info";
    @Autowired
    private PlayerService playerService;
    @Autowired
    private BotCommandHandler botCommandHandler;
    @Autowired
    private CallbackHandler callbackHandler;
    @Autowired
    private ChatStateService chatStateService;

    private final List<Long> authUsers;
    private final String botUsername;
    private final String botPath;

    public DungeonBot(String botUsername, String botToken, String botPath, SetWebhook setWebhook, List<Long> authUsers) {
        super(setWebhook, botToken);
        this.botUsername = botUsername;
        this.botPath = botPath;
        this.authUsers = authUsers;
    }

    /**
     * Handles all updates received by bot: commands, callbacks etc.
     * Main entry point of bot
     *
     * @param update Update received
     */
    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        log.debug("Authenticated userIds: {}", authUsers);
        if (update.hasMessage() && update.getMessage().hasText()) {
            //handles message text if present
            long chatId = update.getMessage().getChatId();
            if (!authUsers.isEmpty() && !authUsers.contains(chatId)) {
                log.warn("User {} failed to authenticate", chatId);
                throw new ChatException(AUTH_EXCEPTION_MESSAGE, chatId);
            }
            log.info("User {} successfully authenticated!", chatId);
            val messageText = update.getMessage().getText();
            processTextMessage(chatId, messageText);
        } else if (update.hasCallbackQuery()) {
            //or handles callback
            val callbackQuery = update.getCallbackQuery();
            val chatId = callbackQuery.getMessage().getChatId() == null ?
                    update.getMessage().getChatId() :
                    callbackQuery.getMessage().getChatId();
            callbackHandler.handleCallbackQuery(chatId, callbackQuery);
        }
        return null; //TODO: consider returning messages here instead of passing through aspect
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotPath() {
        return botPath;
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
     * Sends text message to chat
     * and deletes previous sent message if possible
     * Should be executed by annotating method with {@link org.dungeon.prototype.annotations.aspect.MessageSending}
     *
     * @param chatId  id of chat
     * @param message message to be sent
     */
    public void sendMessage(long chatId, SendMessage message) {
        try {
            val messageId = execute(message).getMessageId();
            val lastMessageId = chatStateService.updateLastMessage(chatId, messageId);
            lastMessageId.ifPresent(id -> deleteMessage(chatId, id));
        } catch (TelegramApiException e) {
            throw new SendMessageException(e.getMessage(), chatId, CallbackType.CONTINUE_GAME);
        }
    }

    /**
     * Sends photo message to chat
     * and deletes previous sent message if possible
     * Should be executed by annotating method with {@link org.dungeon.prototype.annotations.aspect.PhotoMessageSending}
     *
     * @param chatId  id of chat
     * @param message message to be sent
     */
    public void sendMessage(long chatId, SendPhoto message) {
        try {
            val messageId = execute(message).getMessageId();
            val lastMessageId = chatStateService.updateLastMessage(chatId, messageId);
            lastMessageId.ifPresent(id -> deleteMessage(chatId, id));
        } catch (TelegramApiException e) {
            throw new SendMessageException(e.getMessage(), chatId, CallbackType.CONTINUE_GAME);
        }
    }

    public void deleteMessage(long chatId, Integer messageId) {
        val deleteMessage = DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();
        try {
            if (!execute(deleteMessage)) {
                throw new DeleteMessageException(chatId, messageId, CallbackType.CONTINUE_GAME);
            }
        } catch (TelegramApiException e) {
            throw new DeleteMessageException(chatId, messageId, e.getMessage(), CallbackType.CONTINUE_GAME);
        }
    }

    private void processTextMessage(Long chatId, String messageText) {
        log.debug("Received message: {} from chatId: {}", messageText, chatId);
        if (messageText.equals("/start") && chatStateService.isStartAvailable(chatId)) {
            botCommandHandler.processStartAction(chatId);
            return;
        }
        if (messageText.equals("/map") && chatStateService.isGameMenuAvailable(chatId)) {
            botCommandHandler.processMapAction(chatId);
            return;
        }
        if (messageText.equals("/inventory") && chatStateService.isInventoryAvailable(chatId)) {
            botCommandHandler.processInventoryAction(chatId);
            return;
        }
        if (messageText.equals("/stats") && chatStateService.isGameMenuAvailable(chatId)) {
            botCommandHandler.processStatsAction(chatId);
            return;
        }
        if (messageText.equals("/stop")) {
            botCommandHandler.processStopAction(chatId);
            return;
        }
        handlePrompt(chatId, messageText);
    }

    private void handlePrompt(long chatId, String messageText) {
        if (chatStateService.isAwaitingNickname(chatId)) {
            playerService.registerPlayerAndSendStartMessage(chatId, messageText);
        }
    }
}
