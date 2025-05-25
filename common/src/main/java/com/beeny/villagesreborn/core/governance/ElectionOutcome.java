package com.beeny.villagesreborn.core.governance;

/**
 * Outcome type of an election
 */
public enum ElectionOutcome {
    WINNER_DETERMINED,
    TIE_BROKEN_BY_REPUTATION,
    TIE_BROKEN_BY_RANDOM,
    NO_CANDIDATES,
    NO_VOTES
}