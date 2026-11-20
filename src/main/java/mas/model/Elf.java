package mas.model;

import mas.movement.QueenMove;

/** Elves move like a chess Queen and play rock-paper-scissors purely at random. */
public class Elf extends GoodAgent {

    private static int elfCount = 0;

    public Elf(int number, Master master) {
        super(Population.ELF, "E" + number, master, new QueenMove());
        elfCount++;
    }

    @Override
    public char getSymbol() {
        return 'E';
    }

    public static int getElfCount() {
        return elfCount;
    }

    static void resetCount() {
        elfCount = 0;
    }
}
