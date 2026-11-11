package me.marcdoesntexist.galaxymines.api.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Map;


public final class Text {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Text() {
    }

    public static Component parse(final String input) {
        return parse(input, TagResolver.empty());
    }

    public static Component parse(final String input, final TagResolver resolver) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return Text.MINI_MESSAGE.deserialize(BasicMessageManager.toMiniMessage(input), resolver);
    }


    public static String escape(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("<", "\\<");
    }

    public static Component parse(final String input, final Map<String, String> placeholders) {
        final TagResolver.Builder builder = TagResolver.builder();
        for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
            builder.resolver(Placeholder.unparsed(entry.getKey(), entry.getValue()));
        }
        return parse(input, builder.build());
    }
}
