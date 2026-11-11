package me.marcdoesntexist.galaxymines.quest;

import java.util.List;




public record QuestDefinition(String id, String displayName, QuestType type, long target,
                              double rewardMoney, List<String> rewardCommands) {
}
