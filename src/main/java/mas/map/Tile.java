package mas.map;

import mas.model.Being;
import mas.model.Population;

/** One box of the map: holds at most one individual OR one obstacle. */
public class Tile {

    private final Position position;
    private final Population zoneOwner;
    private boolean obstacle;
    private Being occupant;

    public Tile(Position position, Population zoneOwner) {
        this.position = position;
        this.zoneOwner = zoneOwner;
    }

    public Position getPosition() {
        return position;
    }

    /** Population owning the SafeZone this tile belongs to, or null for a neutral tile. */
    public Population getZoneOwner() {
        return zoneOwner;
    }

    public boolean isObstacle() {
        return obstacle;
    }

    void setObstacle(boolean obstacle) {
        this.obstacle = obstacle;
    }

    public Being getOccupant() {
        return occupant;
    }

    void setOccupant(Being occupant) {
        this.occupant = occupant;
    }

    public boolean isFree() {
        return !obstacle && occupant == null;
    }
}
