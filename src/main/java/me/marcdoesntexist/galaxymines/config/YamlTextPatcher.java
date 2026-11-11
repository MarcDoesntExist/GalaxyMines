package me.marcdoesntexist.galaxymines.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public final class YamlTextPatcher {
    private static final String INDENT = "  ";

    private YamlTextPatcher() {
    }


    public static String insertMissingLeaf(final String text, final String path,
                                           final String valueLiteral) {
        if (text == null || path == null || path.isBlank()) {
            return text;
        }
        final String[] segments = path.split("\\.");
        final List<String> lines = toLines(text);
        final String lineSeparator = text.contains("\r\n") ? "\r\n" : "\n";

        final int[] root = findSection(lines, 0, lines.size(), 0, segments[0]);
        if (root == null) {
            final List<String> block = buildBlock(0, segments, valueLiteral);
            final int insertion = endOfFile(lines);
            lines.addAll(insertion, block);
            return join(lines, lineSeparator);
        }
        if (segments.length == 1) {
            return text;
        }
        final boolean changed = insertBelow(lines, root, segments, 1, valueLiteral);
        return changed ? join(lines, lineSeparator) : text;
    }


    public static String setTopLevelValue(final String text, final String key,
                                          final String valueLiteral) {
        if (text == null || key == null || key.isBlank()) {
            return text;
        }
        final List<String> lines = toLines(text);
        final String lineSeparator = text.contains("\r\n") ? "\r\n" : "\n";
        final String rendered = key + ": " + valueLiteral;
        for (int i = 0; i < lines.size(); ++i) {
            final String line = lines.get(i);
            if (line.startsWith(key + ":")) {
                lines.set(i, rendered);
                return join(lines, lineSeparator);
            }
        }
        lines.add(0, rendered);
        return join(lines, lineSeparator);
    }


    private static boolean insertBelow(final List<String> lines, final int[] section,
                                       final String[] segments, final int from,
                                       final String valueLiteral) {
        final int keyLine = section[0];
        final int endLine = section[1];
        final int childIndent = indentOf(lines.get(keyLine)) + INDENT.length();
        final String key = segments[from];
        if (from == segments.length - 1) {
            if (findSection(lines, keyLine + 1, endLine, childIndent, key) != null) {
                return false;
            }
            final int insertion = insertionPoint(lines, keyLine + 1, endLine);
            lines.add(insertion, spaces(childIndent) + key + ": " + valueLiteral);
            return true;
        }
        final int[] child = findSection(lines, keyLine + 1, endLine, childIndent, key);
        if (child != null) {
            return insertBelow(lines, child, segments, from + 1, valueLiteral);
        }
        final int insertion = insertionPoint(lines, keyLine + 1, endLine);
        lines.addAll(insertion, buildBlock(childIndent, Arrays.copyOfRange(segments, from,
                segments.length), valueLiteral));
        return true;
    }


    private static List<String> buildBlock(final int baseIndent, final String[] segments,
                                           final String valueLiteral) {
        final List<String> block = new ArrayList<>(segments.length);
        int indent = baseIndent;
        for (int i = 0; i < segments.length; ++i) {
            if (i == segments.length - 1) {
                block.add(spaces(indent) + segments[i] + ": " + valueLiteral);
            } else {
                block.add(spaces(indent) + segments[i] + ":");
            }
            indent += INDENT.length();
        }
        return block;
    }


    private static int[] findSection(final List<String> lines, final int from, final int to,
                                     final int indent, final String key) {
        final String prefix = spaces(indent) + key + ":";
        for (int i = Math.max(0, from); i < Math.min(to, lines.size()); ++i) {
            if (lines.get(i).startsWith(prefix)) {
                return new int[]{i, sectionEnd(lines, i, to, indent)};
            }
        }
        return null;
    }


    private static int sectionEnd(final List<String> lines, final int keyLine, final int to,
                                  final int indent) {
        for (int i = keyLine + 1; i < Math.min(to, lines.size()); ++i) {
            final String line = lines.get(i);
            if (isBlankOrComment(line)) {
                continue;
            }
            if (indentOf(line) <= indent) {
                return i;
            }
        }
        return Math.min(to, lines.size());
    }


    private static int insertionPoint(final List<String> lines, final int from, final int to) {
        int insertion = Math.max(from, 0);
        for (int i = Math.max(from, 0); i < Math.min(to, lines.size()); ++i) {
            if (!lines.get(i).isBlank()) {
                insertion = i + 1;
            }
        }
        return insertion;
    }

    private static int endOfFile(final List<String> lines) {
        int end = lines.size();
        while (end > 0 && lines.get(end - 1).isBlank()) {
            --end;
        }
        return end;
    }

    private static boolean isBlankOrComment(final String line) {
        final String trimmed = line.trim();
        return trimmed.isEmpty() || trimmed.startsWith("#");
    }

    private static int indentOf(final String line) {
        int indent = 0;
        while (indent < line.length() && line.charAt(indent) == ' ') {
            ++indent;
        }
        return indent;
    }

    private static String spaces(final int count) {
        return " ".repeat(Math.max(0, count));
    }

    private static List<String> toLines(final String text) {
        final List<String> lines = new ArrayList<>(Arrays.asList(text.split("\n", -1)));
        for (int i = 0; i < lines.size(); ++i) {
            final String line = lines.get(i);
            if (line.endsWith("\r")) {
                lines.set(i, line.substring(0, line.length() - 1));
            }
        }
        return lines;
    }

    private static String join(final List<String> lines, final String lineSeparator) {
        return String.join(lineSeparator, lines);
    }
}
