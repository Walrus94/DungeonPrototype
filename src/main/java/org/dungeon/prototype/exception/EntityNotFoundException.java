package org.dungeon.prototype.exception;

import org.apache.commons.math3.util.Pair;
import org.dungeon.prototype.properties.CallbackType;

import java.util.Arrays;
import java.util.stream.Collectors;

public class EntityNotFoundException extends PlayerException {
    @SafeVarargs
    public EntityNotFoundException(Long chatId, String entityType, CallbackType backButton, Pair<String, String>... attributes) {
        super(String.format("Unable to find %s by %s", entityType, Arrays.stream(attributes)
                .map(pair -> String.format("%s: %s", pair.getKey(), pair.getValue()))
                .collect(Collectors.joining(", ", "", "."))), chatId, backButton);
    }
}
