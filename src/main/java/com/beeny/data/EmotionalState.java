package com.beeny.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class EmotionalState {
    public static final Codec<EmotionalState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).fieldOf("emotions").orElse(Collections.emptyMap()).forGetter(EmotionalState::getEmotions),
            Codec.FLOAT.fieldOf("moodStability").orElse(0.5f).forGetter(EmotionalState::getMoodStability),
            Codec.LONG.fieldOf("lastUpdate").orElse(System.currentTimeMillis()).forGetter(EmotionalState::getLastUpdate)
        ).apply(instance, EmotionalState::new)
    );

    private final Map<String, Float> emotions;
    private final float moodStability;
    private final long lastUpdate;

    public EmotionalState(Map<String, Float> emotions, float moodStability, long lastUpdate) {
        this.emotions = new HashMap<>(emotions);
        this.moodStability = moodStability;
        this.lastUpdate = lastUpdate;
    }

    public EmotionalState() {
        this(new HashMap<>(), 0.5f, System.currentTimeMillis());
    }

    public Map<String, Float> getEmotions() {
        return Collections.unmodifiableMap(emotions);
    }

    public float getMoodStability() {
        return moodStability;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public float getEmotion(String emotionType) {
        return emotions.getOrDefault(emotionType, 0.0f);
    }

    public void setEmotion(String emotionType, float value) {
        emotions.put(emotionType, Math.max(0.0f, Math.min(100.0f, value)));
    }

    public EmotionalState copy() {
        return new EmotionalState(new HashMap<>(emotions), moodStability, lastUpdate);
    }
}