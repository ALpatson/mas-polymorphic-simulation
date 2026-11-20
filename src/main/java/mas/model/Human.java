package mas.model;

import mas.movement.BishopMove;

/**
 * Humans move like a chess Bishop. Like Elves, they play rock-paper-scissors uniformly at
 * random, which keeps every fight fair whatever favourite the Bad populations have.
 */
public class Human extends GoodAgent {

    private static int humanCount = 0;

    public Human(int number, Master master) {
        super(Population.HUMAN, "H" + number, master, new BishopMove());
        humanCount++;
    }

    @Override
    public char getSymbol() {
        return 'H';
    }

    public static int getHumanCount() {
        return humanCount;
    }

    static void resetCount() {
        humanCount = 0;
    }
}
