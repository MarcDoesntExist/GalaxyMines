package me.marcdoesntexist.galaxymines.api.chat;

public class ColorDesign {
    private final char primaryColor;
    private final char secondaryColor;
    private final char neutralColor;
    private final char warningColor;

    public ColorDesign(final char primaryColor, final char secondaryColor, final char neutralColor,
                       final char warningColor) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.neutralColor = neutralColor;
        this.warningColor = warningColor;
    }

    public char getWarningColor() {
        return this.warningColor;
    }

    public char getSecondaryColor() {
        return this.secondaryColor;
    }

    public char getPrimaryColor() {
        return this.primaryColor;
    }

    public char getNeutralColor() {
        return this.neutralColor;
    }
}
