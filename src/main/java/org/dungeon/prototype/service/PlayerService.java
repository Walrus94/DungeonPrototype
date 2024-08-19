package org.dungeon.prototype.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.annotations.aspect.SendPlayerStatsMessage;
import org.dungeon.prototype.annotations.aspect.SendRoomMessage;
import org.dungeon.prototype.model.inventory.ArmorSet;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.model.player.PlayerAttribute;
import org.dungeon.prototype.properties.PlayerProperties;
import org.dungeon.prototype.repository.PlayerRepository;
import org.dungeon.prototype.repository.converters.mapstruct.PlayerMapper;
import org.dungeon.prototype.repository.projections.NicknameProjection;
import org.dungeon.prototype.service.effect.EffectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

@Slf4j
@Component
public class PlayerService {
    @Autowired
    EffectService effectService;
    @Autowired
    PlayerRepository playerRepository;
    @Autowired
    PlayerProperties playerProperties;

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
        return updatePlayer(effectService.updatePlayerEffects(player));
    }

    //TODO: extract magic numbers to properties (consider different formula)
    public static int getDefaultMaxMana(Player player) {
        return 6 + player.getAttributes().get(PlayerAttribute.MAGIC);
    }

    public static int getDefaultMaxHp(Player player) {
        return 90 + player.getAttributes().get(PlayerAttribute.STAMINA);
    }

    @SendRoomMessage
    public boolean restoreArmor(Long chatId) {
        val player = getPlayer(chatId);
        player.restoreArmor();
        updatePlayer(player);
        return true;
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
        return PlayerMapper.INSTANCE.mapToPlayer(savedPlayer);
    }
    public Optional<String> getNicknameByChatId(Long chatId) {
        return playerRepository.getNicknameByChatId(chatId).map(NicknameProjection::getNickname);
    }

    //TODO: refactor using repository directly
    @SendRoomMessage
    public boolean upgradePlayerAttribute(Long chatId, PlayerAttribute playerAttribute) {
        val player = getPlayer(chatId);
        player.getAttributes().put(playerAttribute, player.getAttributes().get(playerAttribute) + 1);
        return nonNull(updatePlayer(player));
    }
    @SendPlayerStatsMessage
    public boolean sendPlayerStatsMessage(Long chatId) {
        return hasPlayer(chatId);
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
        player.addEffects(Stream.concat(inventory.getWeaponSet().getWeapons().stream(), inventory.getArmorSet().getArmorItems().stream())
                .flatMap(item -> item.getEffects().stream())
                .collect(Collectors.toCollection(ArrayList::new)));
        //TODO: consider moving to effect calculation
        player.setMaxDefense(calculateMaxDefense(player.getInventory().getArmorSet()));
        player.setDefense(player.getMaxDefense());
        player.setPrimaryAttack(player.getAttributes().get(PlayerAttribute.POWER) + (nonNull(inventory.getWeaponSet().getPrimaryWeapon()) ? inventory.getWeaponSet().getPrimaryWeapon().getAttack() : 0));
        player.setSecondaryAttack(nonNull(inventory.getWeaponSet().getSecondaryWeapon()) ? player.getAttributes().get(PlayerAttribute.POWER) + inventory.getWeaponSet().getSecondaryWeapon().getAttack() : 0);
        log.debug("Player default inventory initialized: {}", player);
        val playerDocument = PlayerMapper.INSTANCE.mapToDocument(player);
        playerRepository.save(playerDocument);
    }

    private Integer calculateMaxDefense(ArmorSet armor) {
        return armor.getArmorItems().stream().filter(Objects::nonNull).mapToInt(Wearable::getArmor).sum();
    }
}
