package mas.model;

import mas.movement.MovementStrategy;

/** Agents of the GOOD alliance (Elves and Humans). */
public abstract class GoodAgent extends Agent {

    protected GoodAgent(Population population, String id, Master master, MovementStrategy movement) {
        super(population, id, master, movement);
    }

    @Override
    public final Alliance getAlliance() {
        return Alliance.GOOD;
    }
}
