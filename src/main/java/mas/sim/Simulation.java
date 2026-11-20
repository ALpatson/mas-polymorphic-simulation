package mas.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import mas.Config;
import mas.map.Map;
import mas.map.Position;
import mas.map.SafeZone;
import mas.model.Agent;
import mas.model.Elf;
import mas.model.Goblin;
import mas.model.Human;
import mas.model.InstanceCounters;
import mas.model.Master;
import mas.model.MasterElf;
import mas.model.MasterGoblin;
import mas.model.MasterHuman;
import mas.model.MasterOrc;
import mas.model.Orc;
import mas.model.Population;
import mas.view.SimulationView;

/**
 * The discrete-time engine. At each step the active agents are shuffled and each one's
 * polymorphic move() is called once; its effects apply immediately, so the next agent sees
 * the updated map. The run stops when a Master holds every message or at maxSteps.
 */
public class Simulation {

    private final RandomGen rng;
    private final Map map;
    private final List<Master> masters = new ArrayList<>();
    private final List<Agent> agents = new ArrayList<>();
    private final Set<String> allMessages = new LinkedHashSet<>();
    private final EventLog log = new EventLog();
    private final EventManager events = new EventManager();
    private final int maxSteps;
    private final int obstaclesPlaced;
    private List<Agent> turnOrder = new ArrayList<>();
    private int step;

    /** Builds a new game. Several games can be created one after another in the same program. */
    public Simulation(long seed, int maxSteps) {
        this.rng = RandomGen.getInstance();
        this.rng.setSeed(seed);
        this.maxSteps = maxSteps;
        InstanceCounters.resetForNewGame();
        this.map = new Map(Config.MAP_WIDTH, Config.MAP_HEIGHT, Config.ZONE_WIDTH, Config.ZONE_HEIGHT);
        this.obstaclesPlaced = map.placeObstacles(Config.OBSTACLE_COUNT, rng);
        createPopulations();
    }

    /** Places the 4 Masters, then the agents of each population in their SafeZone with their unique messages. */
    private void createPopulations() {
        for (Population population : Population.values()) {
            Master master = masterOf(population);
            master.resetForNewGame();
            SafeZone zone = map.getSafeZone(population);
            master.assignSafeZone(zone);
            map.place(master, zone.getMasterPosition());
            masters.add(master);

            int messageNumber = 1;
            for (int i = 1; i <= Config.AGENTS_PER_POPULATION; i++) {
                Agent agent = createAgent(population, i, master);
                Position start = map.randomFreeTileIn(zone, rng);
                map.place(agent, start);
                for (int m = 0; m < Config.MESSAGES_PER_AGENT; m++) {
                    String message = String.format("%c%02d", population.getLetter(), messageNumber++);
                    agent.addMessage(message);
                    allMessages.add(message);
                }
                agents.add(agent);
            }
        }
    }

    private static Master masterOf(Population population) {
        return switch (population) {
            case ELF -> MasterElf.getInstance();
            case HUMAN -> MasterHuman.getInstance();
            case ORC -> MasterOrc.getInstance();
            case GOBLIN -> MasterGoblin.getInstance();
        };
    }

    private static Agent createAgent(Population population, int number, Master master) {
        return switch (population) {
            case ELF -> new Elf(number, master);
            case HUMAN -> new Human(number, master);
            case ORC -> new Orc(number, master);
            case GOBLIN -> new Goblin(number, master);
        };
    }

    // ============================================================ main loop

    /** Runs the whole simulation on the given view and returns the winning Masters (several = draw). */
    public List<Master> run(SimulationView view) {
        view.render(this);
        while (nextStep()) {
            view.render(this);
        }
        List<Master> winners = computeWinners();
        view.renderResult(this, winners);
        return winners;
    }

    /**
     * Plays one time step if the game is not over (used by the graphical window, which
     * drives the game itself). Returns false when there was nothing left to play.
     */
    public boolean nextStep() {
        if (isFinished()) {
            return false;
        }
        step++;
        doStep();
        return true;
    }

    /** One time step: shuffle the active agents, then let each one act immediately. */
    private void doStep() {
        log.newStep();
        turnOrder = getActiveAgents();
        rng.shuffle(turnOrder);
        for (Agent agent : turnOrder) {
            if (!agent.isExhausted()) { // may have been exhausted earlier in this step
                log.beginTurn(agent);
                agent.move(map, log);
                log.endTurn();
            }
        }
        log.beginTurn(null); // world events
        events.maybeTrigger(map, rng, log);
        log.endTurn();
    }

    private List<Agent> getActiveAgents() {
        List<Agent> active = new ArrayList<>();
        for (Agent agent : agents) {
            if (!agent.isExhausted()) {
                active.add(agent);
            }
        }
        return active;
    }

    /** End: a Master holds every initial message, the step limit is reached, or nobody can move. */
    public boolean isFinished() {
        if (step >= maxSteps) {
            return true;
        }
        for (Master master : masters) {
            if (master.getScore() == allMessages.size()) {
                return true;
            }
        }
        return getActiveAgents().isEmpty();
    }

    /** Masters with the highest score: one = winner, several = draw. */
    public List<Master> computeWinners() {
        int best = -1;
        List<Master> winners = new ArrayList<>();
        for (Master master : masters) {
            if (master.getScore() > best) {
                best = master.getScore();
                winners.clear();
                winners.add(master);
            } else if (master.getScore() == best) {
                winners.add(master);
            }
        }
        return winners;
    }

    // ============================================================ accessors

    public Map getMap() {
        return map;
    }

    public List<Master> getMasters() {
        return Collections.unmodifiableList(masters);
    }

    public List<Agent> getAgents() {
        return Collections.unmodifiableList(agents);
    }

    public List<Agent> getTurnOrder() {
        return Collections.unmodifiableList(turnOrder);
    }

    public EventLog getLog() {
        return log;
    }

    public int getStep() {
        return step;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public int getTotalMessages() {
        return allMessages.size();
    }

    public long getSeed() {
        return rng.getSeed();
    }

    public int getMeteorCount() {
        return events.getMeteorCount();
    }

    public int getObstaclesPlaced() {
        return obstaclesPlaced;
    }
}
