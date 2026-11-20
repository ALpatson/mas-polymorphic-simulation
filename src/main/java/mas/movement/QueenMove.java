package mas.movement;

import java.util.Arrays;
import java.util.List;

import mas.map.Direction;

/** Queen: any of the 8 directions. Used by Elves. */
public class QueenMove extends LineMove {

    private static final List<Direction> DIRECTIONS = Arrays.asList(Direction.values());

    @Override
    protected List<Direction> allowedDirections() {
        return DIRECTIONS;
    }

    @Override
    public String getName() {
        return "Queen";
    }
}
