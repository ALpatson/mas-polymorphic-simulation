package mas.movement;

import java.util.List;

import mas.map.Direction;
import mas.map.Map;
import mas.map.Position;
import mas.sim.RandomGen;

/**
 * Strategy pattern: how a population explores the map. Each population gets one
 * implementation (Queen, Bishop, Rook, Knight) and the agent code never needs to know which.
 */
public interface MovementStrategy {

    /**
     * Plans a random path from 'from'. The path lists consecutive adjacent tiles (without
     * 'from') and stays inside the map; obstacles and other beings are discovered while
     * walking it. The 'avoid' direction (last blocked one) is skipped when possible.
     */
    List<Position> planPath(Position from, Direction avoid, Map map, RandomGen rng);

    /** Name shown in the display and the report. */
    String getName();
}
