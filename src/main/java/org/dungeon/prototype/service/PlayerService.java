package org.dungeon.prototype.service;

import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.model.player.Attribute;
import org.dungeon.prototype.model.player.Player;
import org.dungeon.prototype.repository.PlayerRepository;
import org.dungeon.prototype.repository.projections.NicknameProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.dungeon.prototype.util.PlayerUtil.*;

@Slf4j
@Component
public class PlayerService {
    @Autowired
    PlayerRepository playerRepository;

    public Player getPlayer(Long chatId) {
        return playerRepository.findByChatId(chatId).orElseGet(() -> {
            log.error("Unable to load player for chatId: {}", chatId);
            return null;
        });
    }

    public Player updatePlayer(Player player) {
        return playerRepository.save(player);
    }
    public Boolean hasPlayer(Long chatId) {
        return playerRepository.existsByChatId(chatId);
    }
    public Player addNewPlayer(Long chatId, String nickname) {
        Player player = generatePlayer(chatId, nickname);
        return playerRepository.save(player);
    }
    public Optional<String> getNicknameByChatId(Long chatId) {
        return playerRepository.getNicknameByChatId(chatId).map(NicknameProjection::getNickname);
    }

    public Player generatePlayer(Long chatId, String nickname) {
        var player = new Player();
        player.setChatId(chatId);
        player.setNickname(nickname);
        player.setGold(100);
        player.setAttributes(initializeAttributes());
        player.setArmor(getDefaultArmorSet());
        player.setWeapon(getDefaultWeaponSet());
        player.setMaxDefense(calculateMaxDefense(player.getArmor()));
        player.setDefense(player.getMaxDefense());
        player.setAttack(calculateAttack(player.getWeapon(), player.getAttributes()));
        player.setXp(0L);
        player.setMaxHp(90 + player.getAttributes().get(Attribute.STAMINA));
        player.setHp(player.getMaxHp());
        player.setMaxMana(6 + player.getAttributes().get(Attribute.MAGIC));
        player.setMana(player.getMaxMana());
        return player;
    }
}
