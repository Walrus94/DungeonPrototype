package org.dungeon.prototype.service.message;

import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.annotations.aspect.MessageSending;
import org.dungeon.prototype.annotations.aspect.PhotoMessageSending;
import org.dungeon.prototype.aspect.MessagingAspectHandler;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

@Slf4j
@Service
public class MessageSender {

    /**
     * Sends text message using {@link org.dungeon.prototype.bot.DungeonBot}
     * via {@link MessageSending} aspect
     * @param chatId id of chat to send message
     * @param text text content of message
     * @param keyboardMarkup inline keyboard content of message
     * @return message data processed by {@link MessagingAspectHandler}
     */
    @MessageSending
    public SendMessage sendMessage(Long chatId, String text, InlineKeyboardMarkup keyboardMarkup) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(keyboardMarkup)
                .build();
    }

    /**
     * Sends message that requires prompt from user
     * Does the same as {@link MessageSender::sendMessage} at the moment
     * @param chatId id of chat to send message
     * @param text text content of message
     * @param keyboard inline keyboard content of message
     * @return message data processed by {@link MessagingAspectHandler}
     */
    @MessageSending
    public SendMessage sendPromptMessage(Long chatId, ReplyKeyboardMarkup keyboard, String text) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
    }

    /**
     * Sends image message using {@link org.dungeon.prototype.bot.DungeonBot}
     * via {@link PhotoMessageSending} aspect
     * @param chatId id of chat to send message
     * @param caption text caption
     * @param keyboardMarkup inline keyboard content of message
     * @param inputFile file with image
     * @return
     */
    @PhotoMessageSending
    public SendPhoto sendPhotoMessage(Long chatId, String caption, InlineKeyboardMarkup keyboardMarkup, InputFile inputFile) {
        return SendPhoto.builder()
                .chatId(chatId)
                .caption(caption)
                .photo(inputFile)
                .parseMode("Markdown")
                .replyMarkup(keyboardMarkup)
                .build();
    }
}
