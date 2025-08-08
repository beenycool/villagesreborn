package com.beeny.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test focused on backup serialization producing valid JSON for sample villager data.
 * We do NOT instantiate Minecraft classes. Instead, we verify the internal JSON serializer
 * produces syntactically valid JSON for representative Java values (strings, numbers, booleans, lists, null).
 *
 * This aligns with the requirement: "verify backup serialization produces valid JSON for sample villager data".
 */
public class ServerVillagerManagerTest {

    @Test
    @DisplayName("serializeObject creates valid JSON for representative villager-like map")
    void testSerializeObjectProducesValidJson() throws Exception {
        // Build a sample villager-like map using only Java types that the serializer supports.
        Map<String, Object> villager = new LinkedHashMap<>();
        villager.put("id", "123e4567-e89b-12d3-a456-426614174000");
        villager.put("name", "Test Villager \"Alice\"");
        villager.put("age", 25);
        villager.put("gender", "F");
        villager.put("personality", "KIND");
        villager.put("happiness", 87);
        villager.put("spouseId", null);
        villager.put("isAlive", true);
        villager.put("childrenIds", Arrays.asList("c1", "c2", "c3"));
        villager.put("childrenNames", List.of("Bob", "Carol"));
        villager.put("notes", "Line1\nLine2\tTabbed");
        villager.put("favoriteFood", "bread");
        villager.put("hobby", "FISHING");

        // Access the private serializer via reflection
        Method serializeObject = ServerVillagerManager.class.getDeclaredMethod("serializeObject", Map.class);
        serializeObject.setAccessible(true);
        String json = (String) serializeObject.invoke(null, villager);

        // Basic JSON structural assertions without introducing a JSON lib dependency:
        assertNotNull(json);
        assertTrue(json.startsWith("{"), "JSON should start with an object");
        assertTrue(json.endsWith("}"), "JSON should end with an object");
        assertTrue(json.contains("\"id\":\"123e4567-e89b-12d3-a456-426614174000\""));
        assertTrue(json.contains("\"name\":\"Test Villager \\\"Alice\\\"\""), "Quotes must be escaped");
        assertTrue(json.contains("\"age\":25"));
        assertTrue(json.contains("\"gender\":\"F\""));
        assertTrue(json.contains("\"personality\":\"KIND\""));
        assertTrue(json.contains("\"happiness\":87"));
        assertTrue(json.contains("\"spouseId\":null"));
        assertTrue(json.contains("\"isAlive\":true"));
        assertTrue(json.contains("\"childrenIds\":[\"c1\",\"c2\",\"c3\"]"));
        assertTrue(json.contains("\"childrenNames\":[\"Bob\",\"Carol\"]"));
        // Verify control characters escaped
        assertTrue(json.contains("\"notes\":\"Line1\\nLine2\\tTabbed\""));
        assertTrue(json.contains("\"favoriteFood\":\"bread\""));
        assertTrue(json.contains("\"hobby\":\"FISHING\""));

        // Balanced quotes check (very rough): ensure number of unescaped quotes is even.
        // Count occurrences of " that are not escaped with backslash.
        int unescapedQuotes = 0;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\"') {
                boolean escaped = (i > 0 && json.charAt(i - 1) == '\\');
                if (!escaped) unescapedQuotes++;
            }
        }
        assertEquals(0, unescapedQuotes % 2, "Unescaped quotes should be balanced in JSON");
    }

    @Test
    @DisplayName("serializeValue handles nested lists and nulls correctly")
    void testSerializeValueVariety() throws Exception {
        // Build nested data to ensure arrays within arrays are valid
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("a", List.of(1, 2, 3));
        nested.put("b", List.of(List.of("x", "y"), List.of("z")));
        nested.put("c", null);
        nested.put("d", true);
        nested.put("e", 12.34);

        Method serializeObject = ServerVillagerManager.class.getDeclaredMethod("serializeObject", Map.class);
        serializeObject.setAccessible(true);
        String json = (String) serializeObject.invoke(null, nested);

        assertNotNull(json);
        assertTrue(json.startsWith("{") && json.endsWith("}"));
        assertTrue(json.contains("\"a\":[1,2,3]"));
        assertTrue(json.contains("\"b\":[[\"x\",\"y\"],[\"z\"]]"));
        assertTrue(json.contains("\"c\":null"));
        assertTrue(json.contains("\"d\":true"));
        assertTrue(json.contains("\"e\":12.34"));
    }
}