package mas.map;

/** Immutable tile coordinates: x = horizontal index (column), y = vertical index (row). */
public record Position(int x, int y) {

    /** The adjacent position in the given direction (may be outside the map). */
    public Position step(Direction d) {
        return new Position(x + d.dx(), y + d.dy());
    }

    /** Number of king moves between two positions (diagonals count as 1). */
    public int distanceTo(Position other) {
        return Math.max(Math.abs(x - other.x), Math.abs(y - other.y));
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
