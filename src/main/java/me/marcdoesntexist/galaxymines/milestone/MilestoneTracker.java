package me.marcdoesntexist.galaxymines.milestone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;





public final class MilestoneTracker {

    private final List<Milestone> milestones;

    public MilestoneTracker(final Collection<Milestone> input) {
        final List<Milestone> copy = new ArrayList<>(input == null ? List.of() : input);
        copy.sort(Comparator.comparingLong(Milestone::blocks));
        this.milestones = List.copyOf(copy);
    }

    public List<Milestone> milestones() {
        return milestones;
    }

    



    public List<Milestone> crossed(final long before, final long after) {
        if (after <= before) {
            return List.of();
        }
        final List<Milestone> crossed = new ArrayList<>();
        for (final Milestone milestone : milestones) {
            if (milestone.blocks() > before && milestone.blocks() <= after) {
                crossed.add(milestone);
            }
        }
        return crossed;
    }

    public long nextThreshold(final long total) {
        for (final Milestone milestone : milestones) {
            if (milestone.blocks() > total) {
                return milestone.blocks();
            }
        }
        return 0L;
    }
}
