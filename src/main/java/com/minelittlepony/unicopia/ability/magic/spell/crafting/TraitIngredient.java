package com.minelittlepony.unicopia.ability.magic.spell.crafting;

import java.util.Optional;
import java.util.function.Predicate;

import com.minelittlepony.unicopia.ability.magic.spell.trait.SpellTraits;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.dynamic.Codecs;

public record TraitIngredient (
        Optional<SpellTraits> min,
        Optional<SpellTraits> max
    ) implements Predicate<SpellTraits> {
    public static final TraitIngredient EMPTY = new TraitIngredient(Optional.empty(), Optional.empty());
    private static final Codec<TraitIngredient> INLINE_CODEC = SpellTraits.CODEC.xmap(
            traits -> new TraitIngredient(Optional.ofNullable(traits), Optional.empty()),
            ingredient -> ingredient.min().orElse(SpellTraits.EMPTY));
    private static final Codec<TraitIngredient> STRUCTURED_CODEC = RecordCodecBuilder.<TraitIngredient>create(instance -> instance.group(
            SpellTraits.CODEC.optionalFieldOf("min").forGetter(TraitIngredient::min),
            SpellTraits.CODEC.optionalFieldOf("max").forGetter(TraitIngredient::max)
    ).apply(instance, TraitIngredient::new)).flatXmap(
            ingredient -> !ingredient.isEmpty() ? DataResult.success(ingredient) : DataResult.error(() -> "No min or max supplied for ingredient"),
            DataResult::success
    );
    public static final Codec<TraitIngredient> CODEC = Codecs.xor(INLINE_CODEC, STRUCTURED_CODEC).flatXmap(
            either -> either.left().or(either::right).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Invalid traits")),
            ingredient -> DataResult.success(ingredient.max.isEmpty() ? Either.left(ingredient) : Either.right(ingredient))
    );

    public static TraitIngredient of(SpellTraits minTraits) {
        if (minTraits.isEmpty()) {
            return EMPTY;
        }
        return new TraitIngredient(Optional.of(minTraits), Optional.empty());
    }

    public static TraitIngredient of(SpellTraits minTraits, SpellTraits maxTraits) {
        if (minTraits.isEmpty() && maxTraits.isEmpty()) {
            return EMPTY;
        }
        return new TraitIngredient(
                Optional.of(minTraits).filter(s -> !s.isEmpty()),
                Optional.of(maxTraits).filter(s -> !s.isEmpty())
        );
    }

    public boolean isEmpty() {
        return min.filter(SpellTraits::isPresent).isEmpty()
            && max.filter(SpellTraits::isPresent).isEmpty();
    }

    @Override
    public boolean test(SpellTraits t) {
        boolean minMatch = min.map(m -> t.includes(m)).orElse(true);
        boolean maxMatch = max.map(m -> m.includes(t)).orElse(true);
        return minMatch && maxMatch;
    }

    public void write(PacketByteBuf buf) {
        buf.writeOptional(min, (b, m) -> m.write(b));
        buf.writeOptional(max, (b, m) -> m.write(b));
    }

    public static TraitIngredient fromPacket(PacketByteBuf buf) {
        return new TraitIngredient(SpellTraits.fromPacketOrEmpty(buf), SpellTraits.fromPacketOrEmpty(buf));
    }
}
