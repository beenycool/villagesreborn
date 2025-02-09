package com.villagesreborn.beeny.systems.events;

import com.villagesreborn.beeny.systems.ThreatEvent;

public class ThreatAssessmentEvent {
    private final ThreatEvent threat;
    private final long timestamp;

    public ThreatAssessmentEvent(ThreatEvent threat, long timestamp) {
        this.threat = threat;
        this.timestamp = timestamp;
    }

    public ThreatEvent getThreat() {
        return threat;
    }

    public long getTimestamp() {
        return timestamp;
    }
}