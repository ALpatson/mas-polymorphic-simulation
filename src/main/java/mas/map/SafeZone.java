package mas.map;

import java.util.ArrayList;
import java.util.List;

import mas.model.Population;

/**
 * A rectangular corner area owned by one population. Only that population can enter it,
 * its Master lives there, and no confrontation can happen inside it.
 */
public class SafeZone {

    private final Population owner;
    private final int minX;
    private final int minY;
    private final int maxX;
    private final int maxY;
    private final Position masterPosition;

    public SafeZone(Population owner, int minX, int minY, int maxX, int maxY, Position masterPosition) {
        this.owner = owner;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.masterPosition = masterPosition;
    }

    public boolean contains(Position p) {
        return p.x() >= minX && p.x() <= maxX && p.y() >= minY && p.y() <= maxY;
    }

    /** The zone tile closest to p (p itself when p is inside). */
    public Position nearestTileTo(Position p) {
        int x = Math.max(minX, Math.min(maxX, p.x()));
        int y = Math.max(minY, Math.min(maxY, p.y()));
        return new Position(x, y);
    }

    public List<Position> getTiles() {
        List<Position> tiles = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                tiles.add(new Position(x, y));
            }
        }
        return tiles;
    }

    public int getArea() {
        return (maxX - minX + 1) * (maxY - minY + 1);
    }

    public Population getOwner() {
        return owner;
    }

    public Position getMasterPosition() {
        return masterPosition;
    }
}
