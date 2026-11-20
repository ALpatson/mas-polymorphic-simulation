package mas.model;

/** The four populations living on the map. */
public enum Population {
    ELF("Elves", 'E'),
    HUMAN("Humans", 'H'),
    ORC("Orcs", 'O'),
    GOBLIN("Goblins", 'G');

    private final String label;
    private final char letter;

    Population(String label, char letter) {
        this.label = label;
        this.letter = letter;
    }

    public String getLabel() {
        return label;
    }

    public char getLetter() {
        return letter;
    }
}
