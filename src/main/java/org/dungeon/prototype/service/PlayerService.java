package org.dungeon.prototype.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.ChatStateUpdate;
import org.dungeon.prototype.exception.EntityNotFoundException;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.dungeon.prototype.properties.CallbackType;
import org.dungeon.prototype.properties.PlayerProperties;
import org.dungeon.prototype.repository.mongo.PlayerRepository;
import org.dungeon.prototype.repository.mongo.converters.mapstruct.PlayerMapper;
import org.dungeon.prototype.repository.mongo.projections.NicknameProjection;
import org.dungeon.prototype.service.message.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static org.dungeon.prototype.bot.state.ChatState.AWAITING_NICKNAME;
import static org.dungeon.prototype.bot.state.ChatState.PRE_GAME_MENU;
import static org.dungeon.prototype.util.PlayerUtil.getPrimaryAttack;
import static org.dungeon.prototype.util.PlayerUtil.getSecondaryAttack;

@Slf4j
@Service
public class PlayerService {
    @Autowired
    PlayerRepository playerRepository;
    @Autowired
    PlayerProperties playerProperties;
    @Autowired
    MessageService messageService;

    /**
     * Finds player for given chat
     * throws {@link EntityNotFoundException} if none found
     * @param chatId current chat id
     * @return found player
     */
    public Player getPlayer(Long chatId) {
        val playerDocument = playerRepository.findByChatId(chatId)
                .orElseThrow(() ->
                        new EntityNotFoundException(chatId, "player", CallbackType.BOT_START));
        return PlayerMapper.INSTANCE.mapToPlayer(playerDocument);
    }

    /**
     * Prepares player for new game
     * @param chatId current chat id
     * @param defaultInventory player's default inventory
     * @return prepared player with default inventory
     */
    public Player getPlayerPreparedForNewGame(Long chatId, Inventory defaultInventory) {
        Player player = getPlayer(chatId);
        player.setMaxHp(getDefaultMaxHp(player));
        player.setHp(player.getMaxHp());
        player.setXp(0L);
        player.setPlayerLevel(PlayerLevelService.getLevel(player.getXp()));
        player.setNextLevelXp(PlayerLevelService.calculateXPForLevel(player.getPlayerLevel() + 1));
        player.setMaxMana(getDefaultMaxMana(player));
        player.setMana(player.getMaxMana());
        addDefaultInventory(player, defaultInventory);
        return updatePlayer(player);
    }

    /**
     * Registers and generates new player
     * @param chatId current chat id
     * @param nickname new player's nickname
     */
    @ChatStateUpdate(from = AWAITING_NICKNAME, to = PRE_GAME_MENU)
    public void registerPlayerAndSendStartMessage(Long chatId, String nickname) {
        val player = addNewPlayer(chatId, nickname);
        messageService.sendStartMessage(chatId, player.getNickname(), false);
    }

    /**
     * Returns player's maximum amount of mana
     * without applied effects
     * @param player current player
     * @return default max mana
     */
    public int getDefaultMaxMana(Player player) {
        return playerProperties.getBaseMana() +
                player.getAttributes().get(PlayerAttribute.MAGIC) * playerProperties.getAttributeManaFactor();
    }

    /**
     * Returns player's maximum amount of health points
     * without applied effects
     * @param player current player
     * @return default max health points
     */
    public int getDefaultMaxHp(Player player) {
        return 100 + player.getAttributes().get(PlayerAttribute.STAMINA);
    }

    /**
     * Updates given player
     * @param player given player
     * @return updated player
     */
    public Player updatePlayer(Player player) {
        val playerDocument = PlayerMapper.INSTANCE.mapToDocument(player);
        val savedPlayer = playerRepository.save(playerDocument);
        return PlayerMapper.INSTANCE.mapToPlayer(savedPlayer);
    }

    /**
     * Checks if player exist for given chat id
     * @param chatId current chat id
     * @return true if player exists
     */
    public Boolean hasPlayer(Long chatId) {
        return playerRepository.existsByChatId(chatId);
    }

    /**
     * Looks for player's nickname by chat id
     * throws {@link EntityNotFoundException} if none found
     * @param chatId current chat id
     * @return found nickname
     */
    public String getNicknameByChatId(Long chatId) {
        return playerRepository.getNicknameByChatId(chatId).map(NicknameProjection::getNickname).orElseThrow(() ->
                new EntityNotFoundException(chatId, "player", CallbackType.CONTINUE_GAME));
        }

    /**
     * Sends player's stats message to current chat
     * @param chatId current chat id
     */
    public void sendPlayerStatsMessage(Long chatId) {
        if (hasPlayer(chatId)) {
            val player = getPlayer(chatId);
            messageService.sendPlayerStatsMessage(chatId, player);
        }
    }
    private Player addNewPlayer(Long chatId, String nickname) {
        val player = generatePlayer(chatId, nickname);
        val playerDocument = PlayerMapper.INSTANCE.mapToDocument(player);
        val savedPlayer = playerRepository.save(playerDocument);
        log.info("Player generated: {}", player);
        return PlayerMapper.INSTANCE.mapToPlayer(savedPlayer);
    }

    private Player generatePlayer(Long chatId, String nickname) {
        var player = new Player();
        player.setChatId(chatId);
        player.setNickname(nickname);
        player.setGold(100);
        player.setAttributes(playerProperties.getAttributes());
        return player;
    }

    private void addDefaultInventory(Player player, Inventory inventory) {
        player.setInventory(inventory);
        player.addEffects(Stream.concat(inventory.getWeapons().stream(), inventory.getArmorItems().stream())
                .flatMap(item -> item.getEffects().stream())
                .collect(Collectors.toCollection(ArrayList::new)));
        player.setPrimaryAttack(getPrimaryAttack(player, inventory.getPrimaryWeapon()));
        player.setSecondaryAttack(getSecondaryAttack(player, inventory.getSecondaryWeapon()));
        if (nonNull(inventory.getBoots())) {
            player.setChanceToDodge(inventory.getBoots().getChanceToDodge());
        } else {
            player.setChanceToDodge(0.0);
        }
        log.info("Player default inventory initialized: {}", player);
        val playerDocument = PlayerMapper.INSTANCE.mapToDocument(player);
        playerRepository.save(playerDocument);
    }
}
