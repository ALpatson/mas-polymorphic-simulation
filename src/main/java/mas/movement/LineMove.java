package mas.movement;

import java.util.ArrayList;
import java.util.List;

import mas.Config;
import mas.map.Direction;
import mas.map.Map;
import mas.map.Position;
import mas.sim.RandomGen;

/**
 * Straight-line chess moves (Queen, Bishop, Rook): a random allowed direction and a random
 * number of tiles (1..MAX_LINE_RANGE). Subclasses only say which directions are allowed.
 */
public abstract class LineMove implements MovementStrategy {

    protected abstract List<Direction> allowedDirections();

    @Override
    public List<Position> planPath(Position from, Direction avoid, Map map, RandomGen rng) {
        List<Direction> options = new ArrayList<>();
        for (Direction d : allowedDirections()) {
            if (d != avoid && map.inBounds(from.step(d))) {
                options.add(d);
            }
        }
        if (options.isEmpty()) {
            for (Direction d : allowedDirections()) {
                if (map.inBounds(from.step(d))) {
                    options.add(d);
                }
            }
        }
        List<Position> path = new ArrayList<>();
        if (options.isEmpty()) {
            return path;
        }
        Direction d = rng.pick(options);
        int length = rng.nextInt(1, Config.MAX_LINE_RANGE);
        Position p = from;
        for (int i = 0; i < length; i++) {
            p = p.step(d);
            if (!map.inBounds(p)) {
                break;
            }
            path.add(p);
        }
        return path;
    }
}
