package com.haqms.enums;

public enum AppointmentPriority {
    EMERGENCY(1),
    URGENT(2),
    REGULAR(3);

    private final int weight;

    AppointmentPriority(int weight) { this.weight = weight; }

    public int getWeight() { return weight; }
}
