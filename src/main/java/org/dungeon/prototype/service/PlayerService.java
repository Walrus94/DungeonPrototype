package org.dungeon.prototype.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.inventory.ArmorSet;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.items.Wearable;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.properties.PlayerProperties;
import org.dungeon.prototype.repository.PlayerRepository;
import org.dungeon.prototype.repository.converters.mapstruct.PlayerMapper;
import org.dungeon.prototype.repository.projections.NicknameProjection;
import org.dungeon.prototype.service.inventory.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PlayerService {
    @Autowired
    PlayerRepository playerRepository;
    @Autowired
    PlayerProperties playerProperties;
    @Autowired
    InventoryService inventoryService;

    public Player getPlayer(Long chatId) {
        val playerDocument = playerRepository.findByChatId(chatId).orElseGet(() -> {
            log.error("Unable to load player for chatId: {}", chatId);
            return null;
        });
        return PlayerMapper.INSTANCE.mapToPlayer(playerDocument);
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

    public Player generatePlayer(Long chatId, String nickname) {
        var player = new Player();
        player.setChatId(chatId);
        player.setNickname(nickname);
        player.setGold(100);
        player.setAttributes(playerProperties.getAttributes());
        return player;
    }

    public void addDefaultInventory(Player player, Long chatId) {
        Inventory inventory = inventoryService.getDefaultInventory(chatId);
        player.setInventory(inventory);
        player.addEffects(inventory
                .getItems()
                .stream()
                .flatMap(item -> item.getEffects().stream())
                .collect(Collectors.toList()));
        player.setMaxDefense(calculateMaxDefense(player.getInventory().getArmorSet()));
        player.setDefense(player.getMaxDefense());
        log.debug("Player default inventory initialized: {}", player);
        val playerDocument = PlayerMapper.INSTANCE.mapToDocument(player);
        playerRepository.save(playerDocument);
    }

    private Integer calculateMaxDefense(ArmorSet armor) {
        return armor.getArmorItems().stream().filter(Objects::nonNull).mapToInt(Wearable::getArmor).sum();
    }
}
