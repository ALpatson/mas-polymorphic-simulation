package mas.model;

import mas.movement.KnightMove;

/** Goblins move like a chess Knight (walking the L tile by tile) and favour SCISSORS. */
public class Goblin extends BadAgent {

    private static int goblinCount = 0;

    public Goblin(int number, Master master) {
        super(Population.GOBLIN, "G" + number, master, new KnightMove());
        goblinCount++;
    }

    @Override
    public char getSymbol() {
        return 'G';
    }

    @Override
    protected RpsChoice chooseRps() {
        return rng.chance(0.5) ? RpsChoice.SCISSORS : super.chooseRps();
    }

    public static int getGoblinCount() {
        return goblinCount;
    }

    static void resetCount() {
        goblinCount = 0;
    }
}
