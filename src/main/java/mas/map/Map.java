package mas.map;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

import mas.model.Agent;
import mas.model.Being;
import mas.model.Population;
import mas.sim.RandomGen;

/**
 * The n x m environment. Four SafeZones sit in the corners (Elves top-left, Orcs top-right,
 * Goblins bottom-left, Humans bottom-right, so every Good zone borders a Bad one).
 */
public class Map {

    private final int width;
    private final int height;
    private final Tile[][] tiles;
    private final EnumMap<Population, SafeZone> safeZones = new EnumMap<>(Population.class);

    public Map(int width, int height, int zoneWidth, int zoneHeight) {
        this.width = width;
        this.height = height;
        int right = width - zoneWidth;
        int bottom = height - zoneHeight;
        addZone(new SafeZone(Population.ELF, 0, 0, zoneWidth - 1, zoneHeight - 1,
                new Position(0, 0)));
        addZone(new SafeZone(Population.ORC, right, 0, width - 1, zoneHeight - 1,
                new Position(width - 1, 0)));
        addZone(new SafeZone(Population.GOBLIN, 0, bottom, zoneWidth - 1, height - 1,
                new Position(0, height - 1)));
        addZone(new SafeZone(Population.HUMAN, right, bottom, width - 1, height - 1,
                new Position(width - 1, height - 1)));

        tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Position p = new Position(x, y);
                tiles[y][x] = new Tile(p, findZoneOwner(p));
            }
        }
    }

    private void addZone(SafeZone zone) {
        safeZones.put(zone.getOwner(), zone);
    }

    private Population findZoneOwner(Position p) {
        for (SafeZone zone : safeZones.values()) {
            if (zone.contains(p)) {
                return zone.getOwner();
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- queries

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean inBounds(Position p) {
        return p.x() >= 0 && p.x() < width && p.y() >= 0 && p.y() < height;
    }

    public Tile getTile(Position p) {
        return tiles[p.y()][p.x()];
    }

    public boolean isObstacle(Position p) {
        return getTile(p).isObstacle();
    }

    public Being getOccupant(Position p) {
        return getTile(p).getOccupant();
    }

    public SafeZone getSafeZone(Population population) {
        return safeZones.get(population);
    }

    public Population getZoneOwner(Position p) {
        return getTile(p).getZoneOwner();
    }

    public boolean isInSafeZone(Position p) {
        return getZoneOwner(p) != null;
    }

    /** True when p is inside the SafeZone of another population: for that population it is a wall. */
    public boolean isForbiddenFor(Position p, Population population) {
        Population owner = getZoneOwner(p);
        return owner != null && owner != population;
    }

    /**
     * The tiles around p that exist on the map: 3 in a corner, 5 on a side, 8 elsewhere
     * (brief requirement).
     */
    public List<Position> getNeighbours(Position p) {
        List<Position> result = new ArrayList<>(8);
        for (Direction d : Direction.values()) {
            Position n = p.step(d);
            if (inBounds(n)) {
                result.add(n);
            }
        }
        return result;
    }

    /**
     * Direction to take from p to reach the SafeZone of the given population
     * (straight-line heading, brief requirement). Null when p is already inside.
     */
    public Direction directionToSafeZone(Position p, Population population) {
        Position target = getSafeZone(population).nearestTileTo(p);
        return Direction.of(target.x() - p.x(), target.y() - p.y());
    }

    // ---------------------------------------------------------------- updates

    /** Puts a being on an empty tile. */
    public void place(Being being, Position p) {
        getTile(p).setOccupant(being);
        being.setPosition(p);
    }

    /** Moves a being from its current tile to the given (free) tile. */
    public void moveBeing(Being being, Position to) {
        getTile(being.getPosition()).setOccupant(null);
        getTile(to).setOccupant(being);
        being.setPosition(to);
    }

    /**
     * Turns p into a fixed obstacle if that does not cut the map in two.
     * Returns false (and changes nothing) otherwise.
     */
    public boolean tryPlaceObstacle(Position p) {
        Tile tile = getTile(p);
        if (!tile.isFree() || tile.getZoneOwner() != null) {
            return false;
        }
        tile.setObstacle(true);
        if (!isConnected()) {
            tile.setObstacle(false);
            return false;
        }
        return true;
    }

    /** Places up to count random obstacles outside the SafeZones, keeping the map connected. */
    public int placeObstacles(int count, RandomGen rng) {
        int placed = 0;
        int attempts = 0;
        while (placed < count && attempts < count * 50) {
            attempts++;
            Position p = new Position(rng.nextInt(0, width - 1), rng.nextInt(0, height - 1));
            if (tryPlaceObstacle(p)) {
                placed++;
            }
        }
        return placed;
    }

    /**
     * True when every neutral free tile can reach every other one and every SafeZone
     * touches that neutral area (so all agents can get out and back home).
     */
    private boolean isConnected() {
        Position start = null;
        int neutralFree = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile t = tiles[y][x];
                if (!t.isObstacle() && t.getZoneOwner() == null) {
                    neutralFree++;
                    if (start == null) {
                        start = t.getPosition();
                    }
                }
            }
        }
        if (start == null) {
            return false;
        }
        boolean[][] seen = new boolean[height][width];
        EnumMap<Population, Boolean> zoneTouched = new EnumMap<>(Population.class);
        Deque<Position> queue = new ArrayDeque<>();
        queue.add(start);
        seen[start.y()][start.x()] = true;
        int reached = 0;
        while (!queue.isEmpty()) {
            Position p = queue.poll();
            reached++;
            for (Position n : getNeighbours(p)) {
                Tile t = getTile(n);
                if (t.isObstacle() || seen[n.y()][n.x()]) {
                    continue;
                }
                if (t.getZoneOwner() != null) {
                    zoneTouched.put(t.getZoneOwner(), true);
                    continue; // zones are destinations, not corridors
                }
                seen[n.y()][n.x()] = true;
                queue.add(n);
            }
        }
        return reached == neutralFree && zoneTouched.size() == safeZones.size();
    }

    /**
     * Shortest path (BFS over the 8 neighbours) from 'from' to the closest tile of the
     * population's SafeZone, avoiding fixed obstacles, exhausted agents, Masters and enemy
     * SafeZones. It first tries to go around the other agents too (so tired agents do not
     * jam into each other); if that is impossible it ignores active agents, since they move.
     * The returned list excludes 'from'. If no path exists at all, falls back to one step in
     * directionToSafeZone().
     */
    public List<Position> shortestPathHome(Position from, Population population) {
        List<Position> path = searchPathHome(from, population, true);
        if (path == null) {
            path = searchPathHome(from, population, false);
        }
        if (path != null) {
            return path;
        }
        List<Position> fallback = new ArrayList<>();
        Direction d = directionToSafeZone(from, population);
        if (d != null) {
            fallback.add(from.step(d));
        }
        return fallback;
    }

    private List<Position> searchPathHome(Position from, Population population, boolean avoidAgents) {
        SafeZone home = getSafeZone(population);
        HashMap<Position, Position> parent = new HashMap<>();
        Deque<Position> queue = new ArrayDeque<>();
        queue.add(from);
        parent.put(from, from);
        while (!queue.isEmpty()) {
            Position p = queue.poll();
            if (!p.equals(from) && home.contains(p)) {
                return rebuildPath(parent, from, p);
            }
            for (Position n : getNeighbours(p)) {
                if (!parent.containsKey(n) && isPassableForPathfinding(n, population, avoidAgents)) {
                    parent.put(n, p);
                    queue.add(n);
                }
            }
        }
        return null;
    }

    private boolean isPassableForPathfinding(Position p, Population population, boolean avoidAgents) {
        Tile t = getTile(p);
        if (t.isObstacle() || isForbiddenFor(p, population)) {
            return false;
        }
        Being b = t.getOccupant();
        if (b == null) {
            return true;
        }
        return !avoidAgents && b instanceof Agent a && !a.isExhausted();
    }

    private static List<Position> rebuildPath(HashMap<Position, Position> parent, Position from, Position to) {
        List<Position> path = new ArrayList<>();
        Position p = to;
        while (!p.equals(from)) {
            path.add(p);
            p = parent.get(p);
        }
        Collections.reverse(path);
        return path;
    }

    /** A random free tile inside the given zone (used for initial placement). */
    public Position randomFreeTileIn(SafeZone zone, RandomGen rng) {
        List<Position> free = new ArrayList<>();
        for (Position p : zone.getTiles()) {
            if (getTile(p).isFree()) {
                free.add(p);
            }
        }
        return free.isEmpty() ? null : rng.pick(free);
    }
}
