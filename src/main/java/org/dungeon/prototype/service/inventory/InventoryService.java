package org.dungeon.prototype.service.inventory;

import lombok.val;
import org.dungeon.prototype.model.inventory.ArmorSet;
import org.dungeon.prototype.model.inventory.Inventory;
import org.dungeon.prototype.model.inventory.WeaponSet;
import org.dungeon.prototype.repository.ArmorSetRepository;
import org.dungeon.prototype.repository.InventoryRepository;
import org.dungeon.prototype.repository.WeaponSetRepository;
import org.dungeon.prototype.repository.converters.mapstruct.ArmorSetMapper;
import org.dungeon.prototype.repository.converters.mapstruct.InventoryMapper;
import org.dungeon.prototype.repository.converters.mapstruct.WeaponSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {
    @Autowired
    InventoryRepository inventoryRepository;

    @Autowired
    ArmorSetRepository armorSetRepository;

    @Autowired
    WeaponSetRepository weaponSetRepository;

    public Inventory saveOrUpdateInventory(Inventory inventory) {
        inventory.setArmorSet(saveOrUpdateArmorSet(inventory.getArmorSet()));
        inventory.setWeaponSet(saveOrUpdateWeaponSet(inventory.getWeaponSet()));
        val inventoryDocument = InventoryMapper.INSTANCE.mapToDocument(inventory);
        val savedInventoryDocument = inventoryRepository.save(inventoryDocument);
        return InventoryMapper.INSTANCE.mapToEntity(savedInventoryDocument);
    }

    private WeaponSet saveOrUpdateWeaponSet(WeaponSet weaponSet) {
        val weaponSetDocument = WeaponSetMapper.INSTANCE.mapToDocument(weaponSet);
        val savedWeaponSetDocument = weaponSetRepository.save(weaponSetDocument);
        return WeaponSetMapper.INSTANCE.mapToEntity(savedWeaponSetDocument);
    }

    private ArmorSet saveOrUpdateArmorSet(ArmorSet armorSet) {
        val armorSetDocument = ArmorSetMapper.INSTANCE.mapToDocument(armorSet);
        val savedArmorSetDocument = armorSetRepository.save(armorSetDocument);
        return ArmorSetMapper.INSTANCE.mapToEntity(savedArmorSetDocument);
    }
}
