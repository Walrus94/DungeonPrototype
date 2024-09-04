package org.dungeon.prototype.service.room;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.monster.Monster;
import org.dungeon.prototype.repository.MonsterRepository;
import org.dungeon.prototype.repository.converters.mapstruct.MonsterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MonsterService {

    @Autowired
    MonsterRepository monsterRepository;

    public Monster saveOrUpdateMonster(Monster monster) {
        val monsterDocument = MonsterMapper.INSTANCE.mapToDocument(monster);
        return MonsterMapper.INSTANCE.mapToMonster(monsterRepository.save(monsterDocument));
    }
}
