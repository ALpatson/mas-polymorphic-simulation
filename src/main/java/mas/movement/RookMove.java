package mas.movement;

import java.util.List;

import mas.map.Direction;

/** Rook: the 4 orthogonal directions. Used by Orcs. */
public class RookMove extends LineMove {

    @Override
    protected List<Direction> allowedDirections() {
        return Direction.ORTHOGONAL;
    }

    @Override
    public String getName() {
        return "Rook";
    }
}
