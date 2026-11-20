package mas.model;

/**
 * Resets the static instance counters before a new game in the same program run, so the
 * display counts only the individuals of the current game. The 4 Master Singletons are
 * never re-created, so they stay counted.
 */
public final class InstanceCounters {

    private InstanceCounters() {
    }

    public static void resetForNewGame() {
        Being.resetBeingCount(Master.getMasterCount());
        Agent.resetAgentCount();
        Elf.resetCount();
        Human.resetCount();
        Orc.resetCount();
        Goblin.resetCount();
    }
}
