package org.dungeon.prototype.bot;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.Room;
import org.dungeon.prototype.service.LevelService;
import org.dungeon.prototype.util.LevelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Locality;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.objects.Privacy;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.dungeon.prototype.util.LevelUtil.buildLevelMap;
import static org.dungeon.prototype.util.LevelUtil.printMap;

@Slf4j
@Component
public class DungeonBot extends AbilityBot {

    @Autowired
    public DungeonBot(@Value("${bot.token}") String botToken, @Value("${bot.username}") String botUsername) {
        super(botToken, botUsername);
    }
    @Override
    public long creatorId() {
        return 151557417L;
    }

    @Autowired
    private LevelService levelService;

    public Ability start() {
        return Ability.builder()
                .name("start")
                .info("Starts bot")
                .locality(Locality.USER)
                .privacy(Privacy.PUBLIC)
                .action(this::processStartAction)
                .build();
    }

    public Ability showMap() {
        return Ability.builder()
                    .name("map")
                .info("Shows map")
                .locality(Locality.USER)
                .privacy(Privacy.PUBLIC)
                .action(this::displayMap)
                .build();
    }

    private void displayMap(MessageContext messageContext) {
        var level = new Level().generateLevel(1);
        val levelMap = printMap(level.getGrid());
        sendMapMessage(messageContext.chatId(), levelMap);
    }

    private void sendMapMessage(Long chatId, String levelMap) {
        val message = SendMessage.builder()
                .chatId(chatId)
                .text(levelMap)
                .build();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Unable to send message: ", e);
        }
    }

    private void processStartAction(MessageContext messageContext) {
        val chatId = messageContext.chatId();
        if (messageContext.update().hasMessage()) {
            sendStartMessage(chatId);
        }
    }

    private void sendStartMessage(Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Welcome to dungeon! " + getEmojis())
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(List.of(List.of(
                                InlineKeyboardButton.builder()
                                        .text("Start Game!")
                                        .build()
                        ))).build())
                .build();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Unable to send message: ", e);
        }
    }

    private SendMessage createCustomKeyboardMessage(long chat) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(new InlineKeyboardButton("Left"));
        row1.add(new InlineKeyboardButton("Middle"));
        row1.add(new InlineKeyboardButton("Right"));
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(new InlineKeyboardButton("Map"));
        row2.add(new InlineKeyboardButton("Action"));
        row2.add(new InlineKeyboardButton("Turn Back"));
        inlineKeyboard.setKeyboard(List.of(row1, row2));
        return SendMessage.builder()
                .chatId(chat)
                .text("Welcome to dungeon! " + getEmojis())
                .replyMarkup(inlineKeyboard)
                .build();
    }

    private String getEmojis() {
        val result = new StringBuilder();
        for (int i = 0; i < Room.Type.values().length; i ++) {
            result.append(LevelUtil.getIcon(Optional.ofNullable(Room.Type.values()[i])));
        }
        return result.toString();
    }
}
