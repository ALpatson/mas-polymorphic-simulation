package mas.model;

/** Rock-paper-scissors choices used for enemy confrontations. */
public enum RpsChoice {
    ROCK,
    PAPER,
    SCISSORS;

    /** True when this choice beats the other one. */
    public boolean beats(RpsChoice other) {
        return (this == ROCK && other == SCISSORS)
                || (this == PAPER && other == ROCK)
                || (this == SCISSORS && other == PAPER);
    }
}
