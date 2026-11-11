package me.marcdoesntexist.galaxymines.api.utils;

public enum MessageColor {
    PRIMARY(1), SECONDARY(2), NEUTRAL(3), ERROR(4);

    private final Integer id;

    MessageColor(final Integer id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "$" + this.id;
    }
}
