package com.beeny.villagesreborn.core.expansion;

/**
 * Strategy for placing buildings during village expansion
 */
public enum PlacementStrategy {
    COMPACT,      // Buildings placed close together
    DISTRIBUTED,  // Buildings spread out evenly
    LINEAR,       // Buildings placed in lines/rows
    DEFENSIVE     // Buildings placed with defensive considerations
}