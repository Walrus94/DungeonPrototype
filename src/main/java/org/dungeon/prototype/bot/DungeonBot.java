package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.service.MessageService;
import org.dungeon.prototype.service.PlayerService;
import org.dungeon.prototype.service.level.LevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Locality;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.objects.Privacy;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.nonNull;

@Slf4j
@Component
public class DungeonBot extends AbilityBot {
    private final Map<Long, ChatState> chatStateByIdMap;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private LevelService levelService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private CallbackRouter callbackRouter;

    @Autowired
    public DungeonBot(@Value("${bot.token}") String botToken, @Value("${bot.username}") String botUsername) {
        super(botToken, botUsername);
        chatStateByIdMap = new HashMap<>();
    }

    @Override
    public long creatorId() {
        return 151557417L;
    }

    public Ability start() {
        return Ability.builder()
                .name("start")
                .info("Starts bot")
                .locality(Locality.USER)
                .privacy(Privacy.PUBLIC)
                .action(this::processStartAction)
                .build();
    }

    private Boolean isStartAvailable(Update update) {
        if (update.hasMessage()) {
            val chatId = update.getMessage().getChatId();
            return !chatStateByIdMap.containsKey(chatId) || chatStateByIdMap.get(chatId).getState().equals(State.IDLE);
        } else {
            return true;
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            val chatId = update.getMessage().getChatId();
            if (chatStateByIdMap.containsKey(chatId) && chatStateByIdMap.get(chatId).getAwaitingNickname()) {
                val nickname = update.getMessage().getText();
                val player = playerService.addNewPlayer(chatId, nickname);
                log.debug("Player generated: {}", player);
                chatStateByIdMap.get(chatId).setAwaitingNickname(false);
                messageService.sendStartMessage(chatId, nickname, false);
            } else {
                super.onUpdateReceived(update);
            }
        } else if (update.hasCallbackQuery()) {
            val callbackQuery = update.getCallbackQuery();
            val chatId = callbackQuery.getMessage().getChatId() == null ?
                    update.getMessage().getChatId() :
                    callbackQuery.getMessage().getChatId();
            if (!callbackRouter.handleCallbackQuery(chatId, callbackQuery)) {
                super.onUpdateReceived(update);
            }
        } else {
            super.onUpdateReceived(update);
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

//    public boolean openMerchantSellItem(Long chatId, String itemId) {
//        val item = itemService.findItem(chatId, itemId);
//        SendMessage message = SendMessage.builder()
//                .chatId(chatId)
//                .text(messageService.getMerchantSellItemInfoMessageCaption(item))
//                .replyMarkup(keyboardService.getMerchantSellItemInfoReplyMarkup(item))
//                .build();
//        return sendMessage(message, chatId);
//    }

    private void processStartAction(MessageContext messageContext) {
        val chatId = messageContext.chatId();
        if (!chatStateByIdMap.containsKey(chatId)) {
            chatStateByIdMap.put(chatId, new ChatState());
        }
        val nickName = messageContext.user().getUserName() == null ?
                messageContext.user().getFirstName() :
                messageContext.user().getUserName();
        if (!playerService.hasPlayer(chatId)) {
            sendRegisterMessage(chatId, nickName);
        } else {
            val hasSavedGame = levelService.hasLevel(chatId);
            val nickname = playerService.getNicknameByChatId(chatId);
            messageService.sendStartMessage(chatId, nickname.orElse(""), hasSavedGame);
        }
    }

    private boolean sendRegisterMessage(Long chatId, String nickName) {
        chatStateByIdMap.get(chatId).setAwaitingNickname(true);
        return sendPromptMessage(chatId, "Welcome to dungeon!\nPlease, enter nickname to register", nickName);
    }

    private boolean sendPromptMessage(Long chatId, String text, String suggested) {
        ForceReplyKeyboard keyboard = ForceReplyKeyboard.builder()
                .forceReply(false)
                .inputFieldPlaceholder(suggested)
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
            } else {
                if (!chatStateByIdMap.containsKey(chatId)) {
                    val chatState = new ChatState();
                    chatState.setLastMessageId(messageId);
                    chatStateByIdMap.put(chatId, new ChatState());
                } else {
                    chatStateByIdMap.get(chatId).setLastMessageId(messageId);
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
            } else {
                if (!chatStateByIdMap.containsKey(chatId)) {
                    val chatState = new ChatState();
                    chatState.setLastMessageId(messageId);
                    chatStateByIdMap.put(chatId, new ChatState());
                } else {
                    chatStateByIdMap.get(chatId).setLastMessageId(messageId);
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
