package me.marcdoesntexist.galaxymines.api.exceptions;

public class ItemConversionException extends Exception {
    public ItemConversionException(final String input) {
        super("Input in string " + input + " is not supported in this version");
    }
}
