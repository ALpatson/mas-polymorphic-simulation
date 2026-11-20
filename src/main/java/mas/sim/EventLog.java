package mas.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import mas.map.Position;
import mas.model.Agent;
import mas.model.Being;
import mas.model.RpsChoice;

/**
 * Records what happened during the current step and keeps running totals.
 *
 * Besides text lines (for the console and the event log), every agent's turn is recorded
 * as a {@link Turn}: the tiles it walked, what it did (exchanges, fights, donations...) and
 * the state of everyone involved right after. The graphical window replays these turns one
 * by one with animations, in the same shuffled order as the engine played them.
 */
public class EventLog {

    // ================================================================ actions

    /** Kind of message exchange. */
    public enum ExchangeKind {
        /** Same population: union of all knowledge. */
        UNION,
        /** Same alliance, other population: a few messages each way. */
        SHARE,
        /** Agent to its own Master. */
        DELIVERY
    }

    /** Something that happened during a turn. */
    public sealed interface Action permits Exchange, Fight, Donation, Exhaustion, Blocked, Meteor {
    }

    /** Messages given from 'from' to 'to' (toTarget) and back (toSource). */
    public record Exchange(Being from, Being to, ExchangeKind kind, int toTarget, int toSource) implements Action {
    }

    /** Rock-paper-scissors confrontation; 'stolen' messages went from the loser to the winner. */
    public record Fight(Agent attacker, Agent defender, RpsChoice attackerChoice, RpsChoice defenderChoice,
            Agent winner, int stolen) implements Action {
    }

    /** EP given to an exhausted ally, who is revived. */
    public record Donation(Agent donor, Agent target, int amount) implements Action {
    }

    /** The agent ran out of energy (or was hit by a meteorite). */
    public record Exhaustion(Agent agent, String cause) implements Action {
    }

    /** The agent was stopped in front of something and lost EP for the tiles it did not walk. */
    public record Blocked(Agent agent, int epLost) implements Action {
    }

    /** A meteorite fell on 'where' (hit = agent struck, crater = new obstacle created). */
    public record Meteor(Position where, Agent hit, boolean crater) implements Action {
    }

    /** State of a being at a moment of the step. */
    public record Snapshot(Being being, Position position, int ep, int messages, boolean exhausted) {
    }

    /** One agent's turn (agent == null for world events such as meteorites). */
    public record Turn(Agent agent, Position start, List<Position> path, List<Action> actions,
            List<String> lines, List<Snapshot> after) {
    }

    // ================================================================ state

    private final List<String> stepEvents = new ArrayList<>();
    private final List<Turn> turns = new ArrayList<>();

    private boolean inTurn;
    private Agent turnAgent;
    private Position turnStart;
    private List<Position> turnPath;
    private List<Action> turnActions;
    private List<String> turnLines;

    private int totalTransfers;
    private int totalFights;
    private int totalExhaustions;
    private int totalDonations;

    /** Called at the start of each step. */
    public void newStep() {
        stepEvents.clear();
        turns.clear();
        inTurn = false;
    }

    /** Starts recording the turn of an agent (null = world events). */
    public void beginTurn(Agent agent) {
        inTurn = true;
        turnAgent = agent;
        turnStart = agent == null ? null : agent.getPosition();
        turnPath = new ArrayList<>();
        turnActions = new ArrayList<>();
        turnLines = new ArrayList<>();
    }

    /** Ends the current turn and stores it with a snapshot of everyone involved. */
    public void endTurn() {
        if (!inTurn) {
            return;
        }
        inTurn = false;
        if (turnAgent == null && turnActions.isEmpty() && turnLines.isEmpty()) {
            return; // nothing happened in the world
        }
        Set<Being> involved = new LinkedHashSet<>();
        if (turnAgent != null) {
            involved.add(turnAgent);
        }
        for (Action a : turnActions) {
            involved.addAll(participants(a));
        }
        List<Snapshot> after = new ArrayList<>();
        for (Being b : involved) {
            after.add(snapshot(b));
        }
        turns.add(new Turn(turnAgent, turnStart, List.copyOf(turnPath), List.copyOf(turnActions),
                List.copyOf(turnLines), after));
    }

    /** The agent of the current turn stepped onto p. */
    public void moved(Agent agent, Position p) {
        if (inTurn && agent == turnAgent) {
            turnPath.add(p);
        }
    }

    /** Records an action in the current turn. */
    public void action(Action action) {
        if (inTurn) {
            turnActions.add(action);
        }
    }

    private static List<Being> participants(Action a) {
        List<Being> list = new ArrayList<>(2);
        if (a instanceof Exchange e) {
            list.add(e.from());
            list.add(e.to());
        } else if (a instanceof Fight f) {
            list.add(f.attacker());
            list.add(f.defender());
        } else if (a instanceof Donation d) {
            list.add(d.donor());
            list.add(d.target());
        } else if (a instanceof Exhaustion x) {
            list.add(x.agent());
        } else if (a instanceof Blocked b) {
            list.add(b.agent());
        } else if (a instanceof Meteor m && m.hit() != null) {
            list.add(m.hit());
        }
        return list;
    }

    private static Snapshot snapshot(Being b) {
        if (b instanceof Agent a) {
            return new Snapshot(b, b.getPosition(), a.getEp(), b.getMessageCount(), a.isExhausted());
        }
        return new Snapshot(b, b.getPosition(), 0, b.getMessageCount(), false);
    }

    // ================================================================ text lines

    private void line(String text) {
        stepEvents.add(text);
        if (inTurn) {
            turnLines.add(text);
        }
    }

    public void info(String text) {
        line("    " + text);
    }

    public void transfer(String text) {
        totalTransfers++;
        line("MSG " + text);
    }

    public void fight(String text) {
        totalFights++;
        line("RPS " + text);
    }

    public void exhaustion(String text) {
        totalExhaustions++;
        line(" X  " + text);
    }

    public void donation(String text) {
        totalDonations++;
        line(" +  " + text);
    }

    public void event(String text) {
        line(" *  " + text);
    }

    // ================================================================ accessors

    public List<String> getStepEvents() {
        return Collections.unmodifiableList(stepEvents);
    }

    public List<Turn> getTurns() {
        return Collections.unmodifiableList(turns);
    }

    public int getTotalTransfers() {
        return totalTransfers;
    }

    public int getTotalFights() {
        return totalFights;
    }

    public int getTotalExhaustions() {
        return totalExhaustions;
    }

    public int getTotalDonations() {
        return totalDonations;
    }
}
