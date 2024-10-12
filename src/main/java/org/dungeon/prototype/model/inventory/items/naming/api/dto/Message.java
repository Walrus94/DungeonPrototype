package org.dungeon.prototype.model.inventory.items.naming.api.dto;

import lombok.Data;

@Data
public class Message {
    String role;
    String content;

    public Message (String role, String content) {
        this.role = role;
        this.content = content;
    }
}
