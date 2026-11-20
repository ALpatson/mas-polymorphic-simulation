package mas.movement;

import java.util.List;

import mas.map.Direction;

/** Bishop: the 4 diagonals. Used by Humans. */
public class BishopMove extends LineMove {

    @Override
    protected List<Direction> allowedDirections() {
        return Direction.DIAGONAL;
    }

    @Override
    public String getName() {
        return "Bishop";
    }
}
