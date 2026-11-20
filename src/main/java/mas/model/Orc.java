package mas.model;

import mas.movement.RookMove;

/** Orcs move like a chess Rook and favour ROCK in confrontations. */
public class Orc extends BadAgent {

    private static int orcCount = 0;

    public Orc(int number, Master master) {
        super(Population.ORC, "O" + number, master, new RookMove());
        orcCount++;
    }

    @Override
    public char getSymbol() {
        return 'O';
    }

    @Override
    protected RpsChoice chooseRps() {
        return rng.chance(0.5) ? RpsChoice.ROCK : super.chooseRps();
    }

    public static int getOrcCount() {
        return orcCount;
    }

    static void resetCount() {
        orcCount = 0;
    }
}
