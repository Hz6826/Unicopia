package com.minelittlepony.unicopia;

import java.util.Locale;

import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;

public enum Affinity implements StringIdentifiable {
    GOOD(Formatting.BLUE, -1, 0),
    NEUTRAL(Formatting.LIGHT_PURPLE, 0, 0.5F),
    BAD(Formatting.RED, 1, 1);

    @SuppressWarnings("deprecation")
    public static final EnumCodec<Affinity> CODEC = StringIdentifiable.createCodec(Affinity::values);

    private final Formatting color;

    private final int corruption;
    private final float alignment;

    public static final Affinity[] VALUES = values();

    Affinity(Formatting color, int corruption, float alignment) {
        this.color = color;
        this.corruption = corruption;
        this.alignment = alignment;
    }

    @Override
    public String asString() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Formatting getColor() {
        return color;
    }

    public String getTranslationKey() {
        return this == BAD ? "curse" : "spell";
    }

    public int getCorruption() {
        return corruption;
    }

    public float getAlignment() {
        return alignment;
    }

    public boolean isNeutral() {
        return this == NEUTRAL;
    }

    public boolean alignsWith(Affinity other) {
        return isNeutral() || other.isNeutral() || this == other;
    }

    public static Affinity of(int ordinal, Affinity fallback) {
        return ordinal < 0 || ordinal >= VALUES.length ? fallback : VALUES[ordinal];
    }
}
