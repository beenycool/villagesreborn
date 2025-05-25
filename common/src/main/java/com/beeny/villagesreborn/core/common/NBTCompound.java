package com.beeny.villagesreborn.core.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simple NBT-like compound for storing nested data
 * This is a minimal implementation for TDD purposes
 */
public class NBTCompound {
    private final Map<String, Object> data = new HashMap<>();

    public void putString(String key, String value) {
        data.put(key, value);
    }

    public void putInt(String key, int value) {
        data.put(key, value);
    }

    public void putFloat(String key, float value) {
        data.put(key, value);
    }

    public void putLong(String key, long value) {
        data.put(key, value);
    }

    public void putUUID(String key, UUID value) {
        data.put(key, value);
    }

    public void put(String key, NBTCompound value) {
        data.put(key, value);
    }

    public String getString(String key) {
        return (String) data.get(key);
    }

    public int getInt(String key) {
        return (Integer) data.getOrDefault(key, 0);
    }

    public float getFloat(String key) {
        return (Float) data.getOrDefault(key, 0.0f);
    }

    public long getLong(String key) {
        return (Long) data.getOrDefault(key, 0L);
    }

    public UUID getUUID(String key) {
        return (UUID) data.get(key);
    }

    public NBTCompound getCompound(String key) {
        return (NBTCompound) data.getOrDefault(key, new NBTCompound());
    }

    public NBTCompound[] getCompoundArray(String key) {
        Object value = data.get(key);
        if (value instanceof NBTCompound[]) {
            return (NBTCompound[]) value;
        }
        return new NBTCompound[0];
    }

    public boolean contains(String key) {
        return data.containsKey(key);
    }

    public byte[] serialize() {
        // Simplified serialization for testing
        return data.toString().getBytes();
    }

    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }
}