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
        // Store an unmodifiable copy to guarantee immutability and thread-safety
        this.emotions = Collections.unmodifiableMap(new HashMap<>(emotions));
        this.moodStability = moodStability;
        this.lastUpdate = lastUpdate;
    }

    public EmotionalState() {
        this(new HashMap<>(), 0.5f, System.currentTimeMillis());
    }

    public Map<String, Float> getEmotions() {
        // Already unmodifiable internally; return as-is to avoid extra wrappers
        return emotions;
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

    // Immutable update operations return new instances

    public EmotionalState withEmotion(String emotionType, float value) {
        Map<String, Float> updated = new HashMap<>(this.emotions);
        updated.put(emotionType, Math.max(0.0f, Math.min(100.0f, value)));
        return new EmotionalState(updated, this.moodStability, System.currentTimeMillis());
    }

    public EmotionalState withEmotions(Map<String, Float> updates) {
        Map<String, Float> updated = new HashMap<>(this.emotions);
        for (Map.Entry<String, Float> e : updates.entrySet()) {
            updated.put(e.getKey(), Math.max(0.0f, Math.min(100.0f, e.getValue())));
        }
        return new EmotionalState(updated, this.moodStability, System.currentTimeMillis());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builderFrom(EmotionalState base) {
        return new Builder().from(base);
    }

    public static final class Builder {
        private final Map<String, Float> emotions = new HashMap<>();
        private float moodStability = 0.5f;
        private long lastUpdate = System.currentTimeMillis();

        public Builder emotion(String type, float value) {
            emotions.put(type, Math.max(0.0f, Math.min(100.0f, value)));
            return this;
        }

        public Builder emotions(Map<String, Float> values) {
            for (Map.Entry<String, Float> e : values.entrySet()) {
                emotion(e.getKey(), e.getValue());
            }
            return this;
        }

        public Builder moodStability(float stability) {
            this.moodStability = stability;
            return this;
        }

        public Builder lastUpdate(long timestamp) {
            this.lastUpdate = timestamp;
            return this;
        }

        public Builder from(EmotionalState base) {
            this.emotions.clear();
            this.emotions.putAll(base.emotions);
            this.moodStability = base.moodStability;
            this.lastUpdate = base.lastUpdate;
            return this;
        }

        public EmotionalState build() {
            return new EmotionalState(emotions, moodStability, lastUpdate);
        }
    }

    public EmotionalState adjustEmotion(String emotionType, float delta) {
        float current = getEmotion(emotionType);
        return withEmotion(emotionType, current + delta);
    }

    public EmotionalState copy() {
        // Create a new instance with the same immutable data snapshot
        return new EmotionalState(new HashMap<>(emotions), moodStability, lastUpdate);
    }
    
    public String getDominantEmotion() {
        String dominantEmotion = null;
        float maxValue = 0.0f;
        
        for (Map.Entry<String, Float> entry : emotions.entrySet()) {
            if (entry.getValue() > maxValue) {
                maxValue = entry.getValue();
                dominantEmotion = entry.getKey();
            }
        }
        
        return dominantEmotion != null ? dominantEmotion : "contentment";
    }
    
    public String getEmotionalDescription() {
        String dominantEmotion = getDominantEmotion();
        float intensity = getEmotion(dominantEmotion);
        
        String intensityDesc;
        if (intensity > 80) {
            intensityDesc = "very ";
        } else if (intensity > 60) {
            intensityDesc = "quite ";
        } else if (intensity > 40) {
            intensityDesc = "somewhat ";
        } else {
            intensityDesc = "mildly ";
        }
        
        return intensityDesc + dominantEmotion.toLowerCase().replace("_", " ");
    }
}