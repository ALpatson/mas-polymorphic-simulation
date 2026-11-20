package mas.map;

import java.util.List;

/** The 8 compass directions an individual can take (x grows to the East, y grows to the South). */
public enum Direction {
    NORTH(0, -1, "N"),
    NORTH_EAST(1, -1, "NE"),
    EAST(1, 0, "E"),
    SOUTH_EAST(1, 1, "SE"),
    SOUTH(0, 1, "S"),
    SOUTH_WEST(-1, 1, "SW"),
    WEST(-1, 0, "W"),
    NORTH_WEST(-1, -1, "NW");

    public static final List<Direction> ORTHOGONAL = List.of(NORTH, EAST, SOUTH, WEST);
    public static final List<Direction> DIAGONAL = List.of(NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST);

    private final int dx;
    private final int dy;
    private final String shortName;

    Direction(int dx, int dy, String shortName) {
        this.dx = dx;
        this.dy = dy;
        this.shortName = shortName;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    public String shortName() {
        return shortName;
    }

    public boolean isDiagonal() {
        return dx != 0 && dy != 0;
    }

    public Direction opposite() {
        return values()[(ordinal() + 4) % 8];
    }

    /** Direction matching the sign of (dx, dy), or null when both are 0. */
    public static Direction of(int dx, int dy) {
        int sx = Integer.signum(dx);
        int sy = Integer.signum(dy);
        for (Direction d : values()) {
            if (d.dx == sx && d.dy == sy) {
                return d;
            }
        }
        return null;
    }

    /** Direction of the step from a to b (b is normally adjacent to a). */
    public static Direction between(Position a, Position b) {
        return of(b.x() - a.x(), b.y() - a.y());
    }
}
