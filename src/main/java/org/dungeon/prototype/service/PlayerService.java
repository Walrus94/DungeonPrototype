package org.dungeon.prototype.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.ChatStateUpdate;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.dungeon.prototype.properties.PlayerProperties;
import org.dungeon.prototype.repository.PlayerRepository;
import org.dungeon.prototype.repository.converters.mapstruct.PlayerMapper;
import org.dungeon.prototype.repository.projections.NicknameProjection;
import org.dungeon.prototype.service.message.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static org.dungeon.prototype.bot.ChatState.ACTIVE;
import static org.dungeon.prototype.bot.ChatState.AWAITING_NICKNAME;
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

    public Player getPlayer(Long chatId) {
        val playerDocument = playerRepository.findByChatId(chatId).orElseGet(() -> {
            log.error("Unable to load player for chatId: {}", chatId);
            return null;
        });
        return PlayerMapper.INSTANCE.mapToPlayer(playerDocument);
    }

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

    @ChatStateUpdate(from = AWAITING_NICKNAME, to = ACTIVE)
    public void registerPlayerAndSendStartMessage(Long chatId, String nickname) {
        addNewPlayer(chatId, nickname);
        messageService.sendStartMessage(chatId, nickname);
    }

    public int getDefaultMaxMana(Player player) {
        return playerProperties.getBaseMana() +
                player.getAttributes().get(PlayerAttribute.MAGIC) * playerProperties.getAttributeManaFactor();
    }

    public int getDefaultMaxHp(Player player) {
        return 100 + player.getAttributes().get(PlayerAttribute.STAMINA);
    }

    public Player updatePlayer(Player player) {
        val playerDocument = PlayerMapper.INSTANCE.mapToDocument(player);
        val savedPlayer = playerRepository.save(playerDocument);
        return PlayerMapper.INSTANCE.mapToPlayer(savedPlayer);
    }
    public Boolean hasPlayer(Long chatId) {
        return playerRepository.existsByChatId(chatId);
    }

    public Player addNewPlayer(Long chatId, String nickname) {
        val player = generatePlayer(chatId, nickname);
        val playerDocument = PlayerMapper.INSTANCE.mapToDocument(player);
        val savedPlayer = playerRepository.save(playerDocument);
        log.debug("Player generated: {}", player);
        return PlayerMapper.INSTANCE.mapToPlayer(savedPlayer);
    }
    public Optional<String> getNicknameByChatId(Long chatId) {
        return playerRepository.getNicknameByChatId(chatId).map(NicknameProjection::getNickname);
    }
    public boolean sendPlayerStatsMessage(Long chatId) {
        if (hasPlayer(chatId)) {
            val player = getPlayer(chatId);
            messageService.sendPlayerStatsMessage(chatId, player);
            return true;
        }
        return false;
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
        log.debug("Player default inventory initialized: {}", player);
        val playerDocument = PlayerMapper.INSTANCE.mapToDocument(player);
        playerRepository.save(playerDocument);
    }
}
