package mas.movement;

import java.util.ArrayList;
import java.util.List;

import mas.map.Direction;
import mas.map.Map;
import mas.map.Position;
import mas.sim.RandomGen;

/**
 * Knight: an L of 2 tiles straight then 1 tile sideways. Unlike in chess the Goblin walks
 * the L tile by tile, so it can meet (or be stopped by) whoever stands on it. Used by Goblins.
 */
public class KnightMove implements MovementStrategy {

    @Override
    public List<Position> planPath(Position from, Direction avoid, Map map, RandomGen rng) {
        List<List<Position>> options = candidatePaths(from, avoid, map);
        if (options.isEmpty()) {
            options = candidatePaths(from, null, map);
        }
        return options.isEmpty() ? new ArrayList<>() : rng.pick(options);
    }

    private List<List<Position>> candidatePaths(Position from, Direction avoid, Map map) {
        List<List<Position>> result = new ArrayList<>();
        for (Direction main : Direction.ORTHOGONAL) {
            if (main == avoid) {
                continue;
            }
            for (Direction side : Direction.ORTHOGONAL) {
                if (side == main || side == main.opposite()) {
                    continue;
                }
                Position a = from.step(main);
                Position b = a.step(main);
                Position c = b.step(side);
                if (map.inBounds(a) && map.inBounds(b) && map.inBounds(c)) {
                    result.add(List.of(a, b, c));
                }
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return "Knight";
    }
}
