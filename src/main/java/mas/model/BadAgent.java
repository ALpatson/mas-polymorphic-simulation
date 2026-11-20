package mas.model;

import mas.movement.MovementStrategy;

/** Agents of the BAD alliance (Orcs and Goblins). */
public abstract class BadAgent extends Agent {

    protected BadAgent(Population population, String id, Master master, MovementStrategy movement) {
        super(population, id, master, movement);
    }

    @Override
    public final Alliance getAlliance() {
        return Alliance.BAD;
    }
}
