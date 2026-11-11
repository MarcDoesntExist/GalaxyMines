package me.marcdoesntexist.galaxymines.milestone;

import java.util.List;





public record Milestone(long blocks, String message, List<String> commands) {

    public Milestone {
        blocks = Math.max(1L, blocks);
        message = message == null ? "" : message;
        commands = List.copyOf(commands == null ? List.of() : commands);
    }

    public boolean paysSomething() {
        return !message.isBlank() || !commands.isEmpty();
    }
}
