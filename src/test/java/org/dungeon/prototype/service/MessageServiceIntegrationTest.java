package org.dungeon.prototype.service;

import lombok.SneakyThrows;
import lombok.val;
import org.dungeon.prototype.aspect.MessagingAspectHandler;
import org.dungeon.prototype.bot.DungeonBot;
import org.dungeon.prototype.config.TestConfig;
import org.dungeon.prototype.model.Direction;
import org.dungeon.prototype.model.room.Room;
import org.dungeon.prototype.model.room.content.EmptyRoom;
import org.dungeon.prototype.model.room.content.RoomContent;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.properties.MessagingConstants;
import org.dungeon.prototype.service.message.KeyboardService;
import org.dungeon.prototype.service.message.MessageSender;
import org.dungeon.prototype.service.message.MessageService;
import org.dungeon.prototype.service.room.ui.RoomRenderer;
import org.dungeon.prototype.util.FileUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

import static org.dungeon.prototype.TestData.getPlayer;
import static org.dungeon.prototype.model.Direction.E;
import static org.dungeon.prototype.model.Direction.N;
import static org.dungeon.prototype.model.Direction.S;
import static org.dungeon.prototype.model.Direction.W;
import static org.dungeon.prototype.model.room.RoomType.NORMAL;
import static org.dungeon.prototype.properties.CallbackType.FORWARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        TestConfig.class,
        MessagingAspectHandler.class,
        MessageService.class,
        MessagingConstants.class,
        KeyboardService.class,
        MessageSender.class,
        DungeonBot.class
})
@EnableAspectJAutoProxy
@ActiveProfiles("aspect-test")
public class MessageServiceIntegrationTest {
    protected static final Long CHAT_ID = 123456789L;
    private static final Integer MESSAGE_ID = 123456;
    @Autowired
    private MessageService messageService;
    @MockBean
    private RoomRenderer roomRenderer;
    @MockBean
    private DungeonBot dungeonBot;

    @SneakyThrows
    @Test
    @DisplayName("Sends start message")
    public void sendsStartMessage() {
        val message = mock(Message.class);
        ArgumentCaptor<SendPhoto> messageCaptor = ArgumentCaptor.forClass(SendPhoto.class);

        doNothing().when(dungeonBot).sendMessage(anyLong(), any(SendMessage.class));
        when(message.getMessageId()).thenReturn(MESSAGE_ID);

        messageService.sendStartMessage(CHAT_ID, "nickname", false);

        verify(dungeonBot).sendMessage(eq(CHAT_ID), messageCaptor.capture());

        SendPhoto sentMessage = messageCaptor.getValue();
        assertEquals(CHAT_ID.toString(), sentMessage.getChatId());
        assertEquals("Welcome to dungeon, nickname!", sentMessage.getCaption());
    }

    @SneakyThrows
    @Test
    @DisplayName("Sends register message")
    public void sendsRegisterMessage() {
        val message = mock(Message.class);
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        doNothing().when(dungeonBot).sendMessage(anyLong(), any(SendMessage.class));
        when(message.getMessageId()).thenReturn(MESSAGE_ID);
        when(message.getChatId()).thenReturn(CHAT_ID);

        messageService.sendRegisterMessage(CHAT_ID);

        verify(dungeonBot).sendMessage(eq(CHAT_ID), messageCaptor.capture());
        SendMessage sentMessage = messageCaptor.getValue();
        assertEquals(CHAT_ID.toString(), sentMessage.getChatId());
        assertEquals("Welcome to dungeon!\nPlease, enter nickname to register", sentMessage.getText());
    }

    @SneakyThrows
    @Test
    @DisplayName("Sends welcome message")
    public void sendsContinueMessage() {
        messageService.sendStartMessage(CHAT_ID, "nickname", false);

        ArgumentCaptor<SendPhoto> messageCaptor = ArgumentCaptor.forClass(SendPhoto.class);
        verify(dungeonBot).sendMessage(eq(CHAT_ID), messageCaptor.capture());

        SendPhoto sentMessage = messageCaptor.getValue();
        assertEquals(CHAT_ID.toString(), sentMessage.getChatId());
        assertEquals("Welcome to dungeon, nickname!", sentMessage.getCaption());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sends room message")
    public void sendsRoomMessage() {
        val player = getPlayer(CHAT_ID);
        val room = new Room();
        room.setRoomContent(new EmptyRoom(NORMAL));
        room.setAdjacentRooms((new EnumMap<>(Map.of(
                N, true,
                S, false,
                E, false,
                W, false
        ))));

        try (MockedStatic<FileUtil> fileUtilMockedStatic = mockStatic(FileUtil.class)) {
            ClassPathResource imgFile = new ClassPathResource("static/empty.png");
            BufferedImage image;
            try (InputStream inputStream = imgFile.getInputStream()){
                image = ImageIO.read(inputStream);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(image, "png", outputStream);
                InputFile inputFile = new InputFile(inputStream, "rendered_room.png");

                when(FileUtil.getBackgroundLayer(CHAT_ID)).thenReturn(image);
                when(FileUtil.getDoorLayerFragment(eq(CHAT_ID), any(CallbackType.class))).thenReturn(image);
                when(FileUtil.getAdjacentRoomMap(any(), any(Direction.class))).thenReturn(new EnumMap<>(Map.of(FORWARD, true)));
                when(roomRenderer.generateRoomImage(eq(CHAT_ID), any(), any(RoomContent.class   )))
                        .thenReturn(inputFile);

                messageService.sendRoomMessage(CHAT_ID, player, room);

                ArgumentCaptor<SendPhoto> messageCaptor = ArgumentCaptor.forClass(SendPhoto.class);

                verify(dungeonBot).sendMessage(eq(CHAT_ID), messageCaptor.capture());

                SendPhoto sentMessage = messageCaptor.getValue();
                assertEquals(CHAT_ID.toString(), sentMessage.getChatId());
                assertEquals(inputFile, sentMessage.getFile());
            }
        }
    }

    @Test
    @DisplayName("Sends player stats message")
    public void sendsPlayerStatsMessage() {
        val player = getPlayer(CHAT_ID);
        messageService.sendPlayerStatsMessage(CHAT_ID, player);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(dungeonBot).sendMessage(eq(CHAT_ID), messageCaptor.capture());

        SendMessage sendMessage = messageCaptor.getValue();
        assertEquals(CHAT_ID.toString(), sendMessage.getChatId());
    }
}
