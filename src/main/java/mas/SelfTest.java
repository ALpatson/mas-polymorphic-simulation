package mas;

import java.util.List;

import mas.map.Direction;
import mas.map.Map;
import mas.map.Position;
import mas.model.Agent;
import mas.model.AgentState;
import mas.model.Being;
import mas.model.Master;
import mas.sim.Simulation;
import mas.model.Elf;
import mas.model.MasterElf;
import mas.model.MasterOrc;
import mas.model.Orc;
import mas.model.Population;
import mas.model.RpsChoice;
import mas.movement.KnightMove;
import mas.sim.EventLog;
import mas.sim.RandomGen;

/**
 * Small self-checks of the main rules, runnable without any test library:
 *   java -cp out mas.SelfTest
 */
public final class SelfTest {

    private static int passed = 0;
    private static int failed = 0;

    private SelfTest() {
    }

    public static void main(String[] args) {
        RandomGen rng = RandomGen.getInstance();
        rng.setSeed(1L);
        EventLog log = new EventLog();

        // --- Map neighbours: 3 in a corner, 5 on a side, 8 inside
        Map map = new Map(16, 12, 4, 3);
        check("corner has 3 neighbours", map.getNeighbours(new Position(0, 0)).size() == 3);
        check("side has 5 neighbours", map.getNeighbours(new Position(7, 0)).size() == 5);
        check("inner tile has 8 neighbours", map.getNeighbours(new Position(7, 6)).size() == 8);

        // --- Direction to SafeZone
        check("direction from (10,8) to the Elf zone is NW",
                map.directionToSafeZone(new Position(10, 8), Population.ELF) == Direction.NORTH_WEST);
        check("direction inside own zone is null",
                map.directionToSafeZone(new Position(1, 1), Population.ELF) == null);

        // --- Singleton
        check("MasterElf is a Singleton", MasterElf.getInstance() == MasterElf.getInstance());
        check("different Masters are different objects",
                (Object) MasterElf.getInstance() != (Object) MasterOrc.getInstance());

        // --- SafeZone access
        Elf elf = new Elf(1, MasterElf.getInstance());
        Orc orc = new Orc(1, MasterOrc.getInstance());
        Position elfZoneTile = new Position(2, 1);
        check("an Elf can enter its own SafeZone", map.canEnter(elfZoneTile, elf));
        check("an Orc cannot enter the Elf SafeZone", !map.canEnter(elfZoneTile, orc));

        // --- No duplicate messages
        elf.addMessage("E01");
        elf.addMessage("E02");
        int added = elf.addMessages(List.of("E02", "E03"));
        check("adding known messages creates no duplicates", added == 1 && elf.getMessageCount() == 3);

        // --- Delivery to the Master is a copy
        int delivered = MasterElf.getInstance().receive(elf.getMessages());
        check("Master receives a copy of the messages",
                delivered == 3 && elf.getMessageCount() == 3 && MasterElf.getInstance().getScore() == 3);

        // --- Exhaustion and revival
        elf.exhaust("test", log);
        check("exhausted agent loses its messages",
                elf.isExhausted() && elf.getMessageCount() == 0 && elf.getEp() == 0);
        elf.receiveEp(30);
        check("a donation revives the agent and sends it home",
                elf.getState() == AgentState.RETURNING && elf.getEp() == 30);

        // --- BFS goes around obstacles
        Map wallMap = new Map(16, 12, 4, 3);
        for (int y = 3; y <= 8; y++) {
            wallMap.tryPlaceObstacle(new Position(6, y));
        }
        List<Position> path = wallMap.shortestPathHome(new Position(8, 5), Population.ELF);
        check("shortest path home ends inside the SafeZone",
                !path.isEmpty() && wallMap.getSafeZone(Population.ELF).contains(path.get(path.size() - 1)));
        boolean clean = true;
        Position previous = new Position(8, 5);
        for (Position p : path) {
            clean &= !wallMap.isObstacle(p) && previous.distanceTo(p) == 1;
            previous = p;
        }
        check("shortest path is made of adjacent free tiles", clean);

        // --- Knight walks an L of 3 adjacent tiles
        List<Position> knight = new KnightMove().planPath(new Position(7, 6), null, map, rng);
        Position start = new Position(7, 6);
        Position end = knight.get(2);
        int dx = Math.abs(end.x() - start.x());
        int dy = Math.abs(end.y() - start.y());
        check("knight path has 3 tiles and ends on an L square",
                knight.size() == 3 && ((dx == 2 && dy == 1) || (dx == 1 && dy == 2)));

        // --- Several games in one program (the window's Restart / New game)
        Simulation first = new Simulation(5L, 30);
        while (first.nextStep()) {
            // play the whole first game
        }
        Simulation second = new Simulation(5L, 30);
        boolean mastersEmpty = true;
        for (Master m : second.getMasters()) {
            mastersEmpty &= m.getScore() == 0;
        }
        check("a new game starts with empty Masters", mastersEmpty);
        check("a new game resets the instance counters",
                Agent.getAgentCount() == 16 && Being.getBeingCount() == 20 && Master.getMasterCount() == 4);

        // --- Rock-paper-scissors
        check("rock beats scissors", RpsChoice.ROCK.beats(RpsChoice.SCISSORS));
        check("rock does not beat paper", !RpsChoice.ROCK.beats(RpsChoice.PAPER));

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("[ OK ] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }
}
