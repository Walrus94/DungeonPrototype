package org.dungeon.prototype.service.state;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.bot.state.ChatContext;
import org.dungeon.prototype.bot.state.ChatState;
import org.dungeon.prototype.exception.ChatStateUpdateException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.dungeon.prototype.bot.state.ChatState.AWAITING_NICKNAME;
import static org.dungeon.prototype.bot.state.ChatState.BATTLE;
import static org.dungeon.prototype.bot.state.ChatState.GAME;
import static org.dungeon.prototype.bot.state.ChatState.GAME_MENU;
import static org.dungeon.prototype.bot.state.ChatState.IDLE;
import static org.dungeon.prototype.bot.state.ChatState.PRE_GAME_MENU;

@Slf4j
@Service
public class ChatStateService {
    private static final long TIMEOUT_DURATION = Duration.ofMinutes(30).toMillis();
    private final Map<Long, ChatContext> chatStateByIdMap = new ConcurrentHashMap<>();

    /**
     * Initializes chat context: sets chat context state
     * with {@link org.dungeon.prototype.bot.state.ChatState#PRE_GAME_MENU} or creates new one
     * with the same state
     * Should be executed by annotating method with {@link org.dungeon.prototype.annotations.aspect.InitializeChatContext}
     *
     * @param chatId id of chat to update
     */
    public void initializeChatContext(Long chatId) {
        if (!chatStateByIdMap.containsKey(chatId)) {
            chatStateByIdMap.put(chatId, new ChatContext());
        } else {
            chatStateByIdMap.get(chatId).setChatState(PRE_GAME_MENU);
        }
    }

    /**
     * Updates initialized chat in {@param from} state with {@param to}
     * Returns true if chat state successfully updated
     * Should be executed by annotating method with {@link org.dungeon.prototype.annotations.aspect.ChatStateUpdate}
     *
     * @param chatId id of chat to update
     * @param from   initial state(s)
     * @param to     resulting state
     */
    public void updateChatState(Long chatId, ChatState to, ChatState... from) throws ChatStateUpdateException {
        if (chatStateByIdMap.containsKey(chatId) && Arrays.asList(from).contains(chatStateByIdMap.get(chatId).getChatState())) {
            log.info("Current chat state: {}", chatStateByIdMap.get(chatId).getChatState());
            chatStateByIdMap.get(chatId).setChatState(to);
            log.info("Updated chat state: {}", chatStateByIdMap.get(chatId).getChatState());
        } else {
            throw new ChatStateUpdateException(chatId, to, from);
        }
    }

    public Optional<Integer> updateLastMessage(Long chatId, int messageId) {
        if (chatStateByIdMap.containsKey(chatId)) {
            val lastMessageId = chatStateByIdMap.get(chatId).getLastMessageId();
            chatStateByIdMap.get(chatId).setLastMessageId(messageId);
            chatStateByIdMap.get(chatId).setLastActiveTime(System.currentTimeMillis());
            return Optional.of(lastMessageId);
        } else {
            val chatState = new ChatContext();
            chatState.setLastMessageId(messageId);
            chatStateByIdMap.put(chatId, new ChatContext());
            return Optional.empty();
        }
    }

    public boolean isStartAvailable(long chatId) {
        return !chatStateByIdMap.containsKey(chatId) || IDLE.equals(chatStateByIdMap.get(chatId).getChatState());
    }

    public boolean isAwaitingNickname(long chatId) {
        return chatStateByIdMap.containsKey(chatId) && AWAITING_NICKNAME.equals(chatStateByIdMap.get(chatId).getChatState());
    }

    public boolean isGameMenuAvailable(long chatId) {
        return chatStateByIdMap.containsKey(chatId) && List.of(GAME, BATTLE, GAME_MENU)
                .contains(chatStateByIdMap.get(chatId).getChatState());
    }

    public boolean isInventoryAvailable(long chatId) {
        return chatStateByIdMap.containsKey(chatId) && List.of(GAME, GAME_MENU)
                .contains(chatStateByIdMap.get(chatId).getChatState());
    }

    /**
     * Updates chat state to mark with {@link org.dungeon.prototype.bot.state.ChatState#IDLE}
     * ones that were inactive for {@link #TIMEOUT_DURATION}
     */
    @Scheduled(fixedRate = 60000)
    public void checkChatTimeouts() {
        val currentTime = System.currentTimeMillis();
        chatStateByIdMap.forEach((chatId, chatContext) -> {
            if (!IDLE.equals(chatContext.getChatState()) &&
                    currentTime - chatContext.getLastActiveTime() > TIMEOUT_DURATION) {
                clearChatContext(chatId);
            }
        });
    }

    public void clearChatContext(long chatId) {
        if (chatStateByIdMap.containsKey(chatId)) {
            var chatState = chatStateByIdMap.get(chatId);
            chatState.setChatState(IDLE);
            chatState.setLastActiveTime(System.currentTimeMillis());
        }
    }
}
