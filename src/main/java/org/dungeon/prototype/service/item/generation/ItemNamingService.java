package org.dungeon.prototype.service.item.generation;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.document.item.ItemAttributes;
import org.dungeon.prototype.model.inventory.attributes.weapon.WeaponAttributes;
import org.dungeon.prototype.model.inventory.attributes.wearable.WearableAttributes;
import org.dungeon.prototype.model.inventory.items.naming.api.dto.ItemNameRequestDto;
import org.dungeon.prototype.model.inventory.items.naming.api.dto.ItemNameResponseDto;
import org.dungeon.prototype.model.inventory.items.naming.api.dto.ItemNameResponseListWrapperDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ItemNamingService {
    @Value("${huggingface.api.url}")
    private String url;
    private static final String DEFAULT_ITEM_NAME = "Mysterious unnamed item";
    @Autowired
    RestTemplate restTemplate;

    /**
     * Generates and maps names by given item's attributes
     * TODO: replace rest with kafka queue
     * TODO: and replace LLM itself
     * @param itemsAttributes set of item attributes sets
     * @return item attributes mapped to generated name
     */
    public Map<ItemAttributes, String> generateNames(Set<ItemAttributes> itemsAttributes) {
        val itemAttributesPromptMap = itemsAttributes.stream()
                .collect(Collectors.toMap(this::generatePrompt, Function.identity()));
        List<String> prompts = itemAttributesPromptMap.keySet().stream().toList();
        log.debug("Generated prompts: {}", prompts);
        ResponseEntity<ItemNameResponseListWrapperDto> response = requestNameGeneration(prompts);
        log.debug("Response generated: {}", response);
        if (Objects.isNull(response) || Objects.isNull(response.getBody())) {
            log.error("Response body is null!");
            return null;
        }
        return response.getBody().getResponses().stream()
                .collect(Collectors.toMap(responseDto -> itemAttributesPromptMap.get(responseDto.getPrompt()), this::processResponse));
    }

    private String processResponse(ItemNameResponseDto responseDto) {
        var rawResponse = responseDto.getResponse();
        var prompt = responseDto.getPrompt();
        if (rawResponse.startsWith(prompt)) {
            rawResponse = rawResponse.substring(responseDto.getPrompt().length()).trim();
        }
        log.debug("Response without initial prompt: {}", rawResponse);
        val pattern = Pattern.compile("\"([^\"]*)\"|'([^']*)'");
        var matcher = pattern.matcher(rawResponse);
        List<String> substrings = new ArrayList<>();
        String substring;
        while (matcher.find()) {
            substring = matcher.group(1);
            log.debug("Substring found: {}", substring);
            if (Objects.isNull(substring)) {
                matcher = Pattern.compile("'([^'\\\\]*(?:\\\\.[^'\\\\]*)*)'\n").matcher(rawResponse);
                while (matcher.find()) {
                    substring = matcher.group(1);
                    if (Objects.nonNull(substring)) {
                        substrings.add(substring);
                    }
                }
            } else {
                substrings.add(substring);
            }
        }
        log.debug("Substrings: {}", substrings);
        if (substrings.isEmpty()) {
            matcher = Pattern.compile("\\p{Punct}").matcher(rawResponse);
            matcher.reset();
            if (matcher.find()) {
                val index = matcher.start();
                rawResponse = rawResponse.substring(0, index);
            }
            val result = rawResponse.trim();
            log.debug("Processed result: {}", result);
            return result.isEmpty() ? DEFAULT_ITEM_NAME : result;
        } else {
            val result = substrings.getFirst().trim().isEmpty() ? DEFAULT_ITEM_NAME : substrings.getFirst().trim();
            log.debug("Processed result: {}", result);
            return result;
        }
    }

    @Nullable
    private ResponseEntity<ItemNameResponseListWrapperDto> requestNameGeneration(List<String> prompts) {
        HttpHeaders headers = new HttpHeaders();
        ItemNameRequestDto requestBody = new ItemNameRequestDto();
        requestBody.setPrompts(prompts);
        headers.set("Content-Type", "application/json");
        HttpEntity<ItemNameRequestDto> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<ItemNameResponseListWrapperDto> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, ItemNameResponseListWrapperDto.class);
        } catch (RestClientException e) {
            log.error("Exception occurred while generating name: {}", e.getMessage());
            return null;
        }
        return response;
    }

    private String generatePrompt(ItemAttributes itemAttributes) {
        if (itemAttributes instanceof WearableAttributes) {
            return generatePrompt((WearableAttributes) itemAttributes);
        } else if (itemAttributes instanceof WeaponAttributes) {
           return generatePrompt((WeaponAttributes) itemAttributes);
        }
        return "Random fantasy dungeon item name is";
    }

    private String generatePrompt(WearableAttributes wearableAttributes) {
        return wearableAttributes.toString() + " name is";
    }

    private String generatePrompt(WeaponAttributes weaponAttributes) {
        return weaponAttributes.toString() + " name is";
    }
}
