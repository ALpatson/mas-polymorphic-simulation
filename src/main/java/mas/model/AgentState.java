package mas.model;

/** States of a mobile agent (see the state-machine diagram in the report). */
public enum AgentState {
    /** Resting in its own SafeZone, recovering EP. */
    AT_HOME("HOME"),
    /** EP >= 20 %: random moves following its movement strategy. */
    EXPLORING("EXPL"),
    /** EP < 20 % (or just revived): shortest path back to its SafeZone. */
    RETURNING("RTRN"),
    /** Transient: resolving a meeting with another agent. */
    ENCOUNTER("MEET"),
    /** EP = 0: cannot move, acts as an obstacle, has lost its messages. */
    EXHAUSTED("DEAD");

    private final String shortName;

    AgentState(String shortName) {
        this.shortName = shortName;
    }

    public String shortName() {
        return shortName;
    }
}
