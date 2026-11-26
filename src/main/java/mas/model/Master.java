package mas.model;

import java.util.Collection;

import mas.map.Map;
import mas.sim.EventLog;

/**
 * The stationary leader of a population, living in its SafeZone. The team score is the
 * number of distinct messages its Master holds. Each concrete Master is a Singleton.
 */
public abstract class Master extends Being {

    private static int masterCount = 0;

    protected Master(Population population) {
        super(population, "M" + population.getLetter());
        masterCount++;
    }

    /** Masters never move: their turn does nothing (polymorphic counterpart of Agent.move). */
    @Override
    public void move(Map map, EventLog log) {
        // stationary in its SafeZone
    }

    /** Stores the delivered messages; returns how many were new. */
    public int receive(Collection<String> delivered) {
        return addMessages(delivered);
    }

    public int getScore() {
        return messages.size();
    }

    /**
     * A Singleton lives as long as the program, so before a new game its knowledge from
     * the previous game must be wiped.
     */
    public void resetForNewGame() {
        messages.clear();
        position = null;
    }

    @Override
    public char getSymbol() {
        return population.getLetter();
    }

    public static int getMasterCount() {
        return masterCount;
    }
}
