package com.maher.booking_system.model.enums;

public enum BookingStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    CONFIRMED,
    CANCELLED,
    NO_SHOW;

    public BookingStatus canonical() {
        return this == CONFIRMED ? PENDING : this;
    }

    public boolean blocksAvailability() {
        BookingStatus status = canonical();
        return status == PENDING || status == ACTIVE;
    }

    public boolean canTransitionTo(BookingStatus target) {
        BookingStatus current = canonical();
        BookingStatus normalizedTarget = target.canonical();

        if (current == normalizedTarget) {
            return true;
        }

        return switch (current) {
            case PENDING -> normalizedTarget == ACTIVE
                    || normalizedTarget == CANCELLED
                    || normalizedTarget == NO_SHOW;
            case ACTIVE -> normalizedTarget == COMPLETED
                    || normalizedTarget == CANCELLED
                    || normalizedTarget == NO_SHOW;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
            case CONFIRMED -> false;
        };
    }
}
