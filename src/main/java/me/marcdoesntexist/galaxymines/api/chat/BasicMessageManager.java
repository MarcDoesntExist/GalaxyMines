package me.marcdoesntexist.galaxymines.api.chat;

import me.marcdoesntexist.galaxymines.api.CustomPlugin;
import me.marcdoesntexist.galaxymines.api.Manager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;


public abstract class BasicMessageManager implements MessageManager {
    public static String pluginPrefix;
    public static String header;
    public static String messageSeparator;
    public static String primaryColor = "a";
    public static String secondaryColor = "e";
    public static String neutralColor = "7";
    public static String warningColor = "c";

    protected static boolean isValidColor(final char c) {
        return c == '1' || c == '2' || c == '3' || c == '4';
    }

    public static String getColor(final int i) {
        switch (i) {
            case 1:
                return BasicMessageManager.primaryColor;
            case 2:
                return BasicMessageManager.secondaryColor;
            case 3:
                return BasicMessageManager.neutralColor;
            case 4:
                return BasicMessageManager.warningColor;
            default:
                return null;
        }
    }


    public static String toMiniMessage(final String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        if (!hasLegacyFormatting(input)) {
            return input;
        }
        final StringBuilder out = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < input.length()
                    && isLegacyCode(input.charAt(i + 1))) {
                out.append(tagFor(input.charAt(++i)));
            } else if (c == '$' && i + 1 < input.length() && isValidColor(input.charAt(i + 1))) {
                final String color = getColor(Character.getNumericValue(input.charAt(i + 1)));
                if (color != null && !color.isEmpty()) {
                    out.append(tagFor(color.charAt(0)));
                    i++;
                } else {
                    out.append(c);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean hasLegacyFormatting(final String input) {
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < input.length()
                    && isLegacyCode(input.charAt(i + 1))) {
                return true;
            }
            if (c == '$' && i + 1 < input.length() && isValidColor(input.charAt(i + 1))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLegacyCode(final char c) {
        return "0123456789abcdefklmnor".indexOf(Character.toLowerCase(c)) >= 0;
    }

    public static String tagFor(final char code) {
        switch (Character.toLowerCase(code)) {
            case '0':
                return "<black>";
            case '1':
                return "<dark_blue>";
            case '2':
                return "<dark_green>";
            case '3':
                return "<dark_aqua>";
            case '4':
                return "<dark_red>";
            case '5':
                return "<dark_purple>";
            case '6':
                return "<gold>";
            case '7':
                return "<gray>";
            case '8':
                return "<dark_gray>";
            case '9':
                return "<blue>";
            case 'a':
                return "<green>";
            case 'b':
                return "<aqua>";
            case 'c':
                return "<red>";
            case 'd':
                return "<light_purple>";
            case 'e':
                return "<yellow>";
            case 'f':
                return "<white>";
            case 'k':
                return "<obfuscated>";
            case 'l':
                return "<bold>";
            case 'm':
                return "<strikethrough>";
            case 'n':
                return "<underlined>";
            case 'o':
                return "<italic>";
            case 'r':
                return "<reset>";
            default:
                return "";
        }
    }

    @Override
    public Manager setup() {
        CustomPlugin.instance.debug("Loading colors...", 3);
        final FileConfiguration messagesConfig = CustomPlugin.instance.getMessagesConfig();
        BasicMessageManager.primaryColor = this.readColor(messagesConfig, "colors.primaryColor");
        BasicMessageManager.secondaryColor =
                this.readColor(messagesConfig, "colors.secondaryColor");
        BasicMessageManager.neutralColor = this.readColor(messagesConfig, "colors.neutralColor");
        BasicMessageManager.warningColor = this.readColor(messagesConfig, "colors.warningColor");
        CustomPlugin.instance.debug("Colors loaded!", 3);
        CustomPlugin.instance.debug("Loading basic messages...", 3);
        BasicMessageManager.messageSeparator =
                this.translateColors(messagesConfig.getString("messages.messageSeparator", ""));
        BasicMessageManager.pluginPrefix =
                this.translateColors(messagesConfig.getString("messages.messagePrefix", ""));
        BasicMessageManager.header =
                this.translateAll(messagesConfig.getString("messages.header", ""));
        CustomPlugin.instance.debug("Messages loaded!", 3);
        this.postSetup();
        return this;
    }

    private String readColor(final FileConfiguration config, final String path) {
        String value = config.getString(path);
        if (value == null || value.isEmpty()) {
            value = this.defaultColor(path);
            config.set(path, value);
        }
        return value;
    }

    private String defaultColor(final String path) {
        if (path.endsWith("secondaryColor")) {
            return "e";
        }
        if (path.endsWith("neutralColor")) {
            return "7";
        }
        if (path.endsWith("warningColor")) {
            return "c";
        }
        return "a";
    }

    public abstract void postSetup();


    @Override
    public String translateColors(final String input) {
        return BasicMessageManager.toMiniMessage(input);
    }

    @Override
    public String translateAll(final String input) {
        return this.translateColors(this.translateTags(input));
    }

    @Override
    public List<String> translateTags(final Iterable<String> input) {
        final ArrayList<String> results = new ArrayList<>();
        for (final String value : input) {
            results.add(this.translateTags(value));
        }
        return results;
    }

    @Override
    public List<String> translateColors(final Iterable<String> input) {
        final ArrayList<String> results = new ArrayList<>();
        for (final String value : input) {
            results.add(this.translateColors(value));
        }
        return results;
    }

    @Override
    public List<String> translateAll(final Iterable<String> input) {
        final ArrayList<String> results = new ArrayList<>();
        for (final String value : input) {
            results.add(this.translateAll(value));
        }
        return results;
    }

    @Override
    public String getPluginPrefix() {
        return BasicMessageManager.pluginPrefix;
    }

    @Override
    public String getMessageSeparator() {
        return BasicMessageManager.messageSeparator;
    }
}
