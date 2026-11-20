package mas.model;

import java.util.ArrayList;
import java.util.List;

import mas.Config;
import mas.map.Direction;
import mas.map.Map;
import mas.map.Position;
import mas.map.SafeZone;
import mas.movement.MovementStrategy;
import mas.sim.EventLog;
import mas.sim.RandomGen;

/**
 * A mobile individual. It explores with its population's movement strategy, spends EP
 * outside its SafeZone, heads home below 20 % EP, delivers its messages to its Master and
 * meets the other agents found on its path.
 */
public abstract class Agent extends Being {

    private static int agentCount = 0;

    protected int ep;
    protected final int maxEp;
    protected Direction lastDirection;
    /** Direction that was blocked last time, avoided by the next random move. */
    protected Direction blockedDirection;
    protected AgentState state = AgentState.AT_HOME;
    protected final Master master;
    protected final MovementStrategy movement;
    protected final RandomGen rng = RandomGen.getInstance();

    protected Agent(Population population, String id, Master master, MovementStrategy movement) {
        super(population, id);
        this.master = master;
        this.movement = movement;
        this.maxEp = Config.MAX_EP;
        this.ep = maxEp;
        agentCount++;
    }

    // ================================================================== turn

    /** One turn: rest at home, explore, or go back home, then walk the chosen path. */
    @Override
    public void move(Map map, EventLog log) {
        if (isExhausted()) {
            return;
        }
        if (isInOwnSafeZone(map)) {
            deliverToMaster(log);
            if (ep < leaveThreshold()) {
                state = AgentState.AT_HOME;
                recoverEp();
                return;
            }
            if (state == AgentState.AT_HOME || state == AgentState.RETURNING) {
                state = AgentState.EXPLORING; // rested enough: leave again
            }
        }

        List<Position> path;
        if (state == AgentState.RETURNING || isLowEp()) {
            state = AgentState.RETURNING;
            path = map.shortestPathHome(position, population);
            if (path.size() > Config.HOME_TILES_PER_STEP) {
                path = path.subList(0, Config.HOME_TILES_PER_STEP);
            }
        } else {
            state = AgentState.EXPLORING;
            path = movement.planPath(position, blockedDirection, map, rng);
        }
        walk(path, map, log);
    }

    /** Follows the planned path tile by tile and reacts to the first thing in the way. */
    private void walk(List<Position> path, Map map, EventLog log) {
        int planned = path.size();
        for (int i = 0; i < planned; i++) {
            Position next = path.get(i);
            Direction dir = Direction.between(position, next);
            int remaining = planned - i;

            if (!map.inBounds(next)) {
                stopInFront(remaining, dir, map, log, "the map edge");
                return;
            }
            if (map.isObstacle(next)) {
                stopInFront(remaining, dir, map, log, "an obstacle");
                return;
            }
            if (map.isForbiddenFor(next, population)) {
                stopInFront(remaining, dir, map, log, "the " + map.getZoneOwner(next).getLabel() + " SafeZone");
                return;
            }
            Being other = map.getOccupant(next);
            if (other != null) {
                handleBlocker(other, remaining, dir, map, log);
                return;
            }

            map.moveBeing(this, next);
            log.moved(this, next);
            lastDirection = dir;
            blockedDirection = null;
            if (!isInOwnSafeZone(map)) {
                spendEp(Config.EP_COST_PER_TILE, log);
                if (isExhausted()) {
                    return;
                }
            } else {
                deliverToMaster(log);
                if (state == AgentState.RETURNING) {
                    state = AgentState.AT_HOME;
                    log.info(id + " is back home with " + ep + " EP");
                    return;
                }
            }
        }
        refreshState();
    }

    /** Brief rule: stop in front of the blocking tile and lose the EP of the tiles not travelled. */
    private void stopInFront(int remaining, Direction dir, Map map, EventLog log, String what) {
        blockedDirection = dir;
        if (!isInOwnSafeZone(map)) {
            int cost = remaining * Config.EP_COST_PER_TILE;
            log.action(new EventLog.Blocked(this, cost));
            log.info(id + " blocked by " + what + " -> loses " + cost + " EP");
            spendEp(cost, log);
        }
        refreshState();
    }

    /** Another being stands on the path: meeting, donation or obstacle. */
    private void handleBlocker(Being other, int remaining, Direction dir, Map map, EventLog log) {
        if (other instanceof Agent otherAgent) {
            if (otherAgent.isExhausted()) {
                if (isAllyOf(otherAgent) && donateEp(otherAgent, log)) {
                    blockedDirection = dir;
                    refreshState();
                } else {
                    stopInFront(remaining, dir, map, log, "exhausted " + otherAgent.getId());
                }
                return;
            }
            encounter(otherAgent, map, log);
            refreshState();
        } else {
            // A Master (only reachable inside the agent's own SafeZone): simply stop.
            blockedDirection = dir;
            refreshState();
        }
    }

    /** After an action: keep RETURNING / AT_HOME, otherwise pick the state from the EP level. */
    private void refreshState() {
        if (isExhausted() || state == AgentState.AT_HOME) {
            return;
        }
        state = (state == AgentState.RETURNING || isLowEp()) ? AgentState.RETURNING : AgentState.EXPLORING;
    }

    // ============================================================ encounters

    /**
     * The other agent is on our path: same population = union of knowledge, same alliance =
     * random exchange, enemy = rock-paper-scissors confrontation (never inside a SafeZone).
     */
    protected void encounter(Agent other, Map map, EventLog log) {
        AgentState before = state;
        state = AgentState.ENCOUNTER;
        if (other.population == population) {
            unionWith(other, log);
        } else if (isAllyOf(other)) {
            shareWith(other, log);
        } else if (map.isInSafeZone(position) || map.isInSafeZone(other.position)) {
            log.info(id + " meets " + other.id + ": no fight inside a SafeZone");
        } else {
            confront(other, log);
        }
        state = before;
    }

    /** Same population: both end up knowing everything either of them knew. */
    protected void unionWith(Agent other, EventLog log) {
        int gained = addMessages(other.messages);
        int given = other.addMessages(messages);
        log.action(new EventLog.Exchange(this, other, EventLog.ExchangeKind.UNION, given, gained));
        log.transfer(id + " <-> " + other.id + " union: " + id + " +" + gained + ", " + other.id + " +" + given);
    }

    /** Same alliance, other population: each side gives 1..ALLY_SHARE_MAX messages the other lacks. */
    protected void shareWith(Agent other, EventLog log) {
        int given = copyRandomMessages(this, other, rng.nextInt(1, Config.ALLY_SHARE_MAX));
        int received = copyRandomMessages(other, this, rng.nextInt(1, Config.ALLY_SHARE_MAX));
        log.action(new EventLog.Exchange(this, other, EventLog.ExchangeKind.SHARE, given, received));
        log.transfer(id + " <-> " + other.id + " ally share: " + id + " gave " + given + ", got " + received);
    }

    /** Enemies: rock-paper-scissors (ties replayed); the winner takes messages from the loser. */
    protected void confront(Agent other, EventLog log) {
        RpsChoice mine;
        RpsChoice theirs;
        int rounds = 0;
        do {
            mine = chooseRps();
            theirs = other.chooseRps();
            rounds++;
        } while (mine == theirs && rounds < 10);

        Agent winner;
        if (mine == theirs) {
            winner = rng.chance(0.5) ? this : other; // endless ties: coin toss
        } else {
            winner = mine.beats(theirs) ? this : other;
        }
        Agent loser = (winner == this) ? other : this;

        List<String> candidates = missingMessages(loser, winner);
        rng.shuffle(candidates);
        int count = Math.min(candidates.size(), rng.nextInt(1, Config.STEAL_MAX));
        List<String> stolen = new ArrayList<>(candidates.subList(0, count));
        winner.messages.addAll(stolen);
        loser.messages.removeAll(stolen);

        log.action(new EventLog.Fight(this, other, mine, theirs, winner, stolen.size()));
        String outcome = stolen.isEmpty()
                ? " wins, but " + loser.id + " has nothing new to give"
                : " wins, takes " + stolen.size() + " msg" + (stolen.size() == 1 ? "" : "s") + " from " + loser.id;
        log.fight(id + "(" + mine + ") vs " + other.id + "(" + theirs + ") -> " + winner.id + outcome);
    }

    /** Rock-paper-scissors choice; populations may override it to show their temper. */
    protected RpsChoice chooseRps() {
        return rng.nextRps();
    }

    /** Copies up to n random messages that 'to' does not know from 'from' to 'to'. */
    private int copyRandomMessages(Agent from, Agent to, int n) {
        List<String> candidates = missingMessages(from, to);
        rng.shuffle(candidates);
        int count = Math.min(n, candidates.size());
        to.messages.addAll(candidates.subList(0, count));
        return count;
    }

    private static List<String> missingMessages(Being from, Being to) {
        List<String> result = new ArrayList<>();
        for (String m : from.messages) {
            if (!to.messages.contains(m)) {
                result.add(m);
            }
        }
        return result;
    }

    // ==================================================================== EP

    /** EP only decreases here. Reaching 0 exhausts the agent. */
    protected void spendEp(int amount, EventLog log) {
        ep = Math.max(0, ep - amount);
        if (ep == 0) {
            exhaust("ran out of energy", log);
        }
    }

    /** Resting in the SafeZone. */
    protected void recoverEp() {
        ep = Math.min(maxEp, ep + Config.EP_RECOVERY_PER_STEP);
    }

    /** Receives EP from an ally; an exhausted agent is revived and must go home. */
    public void receiveEp(int amount) {
        ep = Math.min(maxEp, ep + amount);
        if (state == AgentState.EXHAUSTED && ep > 0) {
            state = AgentState.RETURNING;
        }
    }

    /**
     * Gives DONATION_PERCENT of max EP to an exhausted ally, without dropping below our own
     * low-EP threshold. Returns false when we cannot spare anything.
     */
    protected boolean donateEp(Agent target, EventLog log) {
        int wanted = maxEp * Config.DONATION_PERCENT / 100;
        int spare = ep - lowThreshold();
        int amount = Math.min(wanted, spare);
        if (amount <= 0) {
            return false;
        }
        ep -= amount;
        target.receiveEp(amount);
        log.action(new EventLog.Donation(this, target, amount));
        log.donation(id + " donates " + amount + " EP to " + target.id + ", who is revived and heads home");
        return true;
    }

    /** Becomes an obstacle and loses all its messages (brief rule). Also used by meteorites. */
    public void exhaust(String cause, EventLog log) {
        int lost = messages.size();
        ep = 0;
        state = AgentState.EXHAUSTED;
        messages.clear();
        log.action(new EventLog.Exhaustion(this, cause));
        log.exhaustion(id + " " + cause + ": now an obstacle, " + lost + " msg" + (lost == 1 ? "" : "s") + " lost");
    }

    /** Copies all messages to the Master (the agent keeps them to keep sharing). */
    protected void deliverToMaster(EventLog log) {
        int added = master.receive(messages);
        if (added > 0) {
            log.action(new EventLog.Exchange(this, master, EventLog.ExchangeKind.DELIVERY, added, 0));
            log.transfer(id + " -> " + master.getId() + " delivers " + added + " new msg"
                    + (added == 1 ? "" : "s") + " (score " + master.getScore() + ")");
        }
    }

    // ============================================================= accessors

    private boolean isInOwnSafeZone(Map map) {
        SafeZone home = map.getSafeZone(population);
        return home.contains(position);
    }

    protected int lowThreshold() {
        return maxEp * Config.LOW_EP_PERCENT / 100;
    }

    protected int leaveThreshold() {
        return maxEp * Config.LEAVE_HOME_PERCENT / 100;
    }

    public boolean isLowEp() {
        return ep < lowThreshold();
    }

    public boolean isExhausted() {
        return state == AgentState.EXHAUSTED;
    }

    public int getEp() {
        return ep;
    }

    public int getMaxEp() {
        return maxEp;
    }

    public AgentState getState() {
        return state;
    }

    public Direction getLastDirection() {
        return lastDirection;
    }

    public Master getMaster() {
        return master;
    }

    public MovementStrategy getMovement() {
        return movement;
    }

    public static int getAgentCount() {
        return agentCount;
    }

    static void resetAgentCount() {
        agentCount = 0;
    }
}
