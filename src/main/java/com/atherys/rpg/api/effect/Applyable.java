package com.atherys.rpg.api.effect;

import com.atherys.rpg.api.character.RPGCharacter;
import com.atherys.rpg.api.util.SimpleIdentifiable;

/**
 * An object which can be applied to an {@link RPGCharacter} and cause various temporary effects upon their game experience.
 * Applyables are not persisted across server restarts.
 */
public interface Applyable extends SimpleIdentifiable {

    /**
     * Checks if the given Applyable can be applied to the {@link RPGCharacter}.
     *
     * @param timestamp When this Applyable is going to be applied, in the form of a UNIX timestamp.
     * @param character The RPGCharacter this Applyable is to be applied on
     * @return Whether or not the
     */
    <T extends ApplyableCarrier> boolean canApply(long timestamp, T character);

    <T extends ApplyableCarrier> boolean apply(long timestamp, T character);

    <T extends ApplyableCarrier> boolean canRemove(long timestamp, T character);

    <T extends ApplyableCarrier> boolean remove(long timestamp, T character);

}
