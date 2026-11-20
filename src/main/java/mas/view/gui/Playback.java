package mas.view.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mas.map.Position;
import mas.model.Agent;
import mas.sim.EventLog;
import mas.sim.EventLog.Action;
import mas.sim.EventLog.Blocked;
import mas.sim.EventLog.Donation;
import mas.sim.EventLog.Exchange;
import mas.sim.EventLog.Exhaustion;
import mas.sim.EventLog.Fight;
import mas.sim.EventLog.Meteor;
import mas.sim.EventLog.Turn;

/**
 * Replays one simulation step as a sequence of timed phases, in the engine's shuffled order:
 * agents that only walk are animated together (to keep the pace), while every interaction
 * (exchange, fight, delivery, donation, exhaustion, meteorite) gets its own phase so the
 * player can see it. Durations are in milliseconds at normal speed.
 */
final class Playback {

    static final double TILE_MS = 170;
    static final double SHUFFLE_MS = 700;

    enum Kind {
        SHUFFLE, MOVE, ACTION
    }

    /** One timed piece of the replay. */
    static final class Phase {
        final Kind kind;
        final double duration;
        final List<Turn> moving;
        final Action action;
        final Agent actor;
        final List<Turn> applyAfter = new ArrayList<>();

        Phase(Kind kind, double duration, List<Turn> moving, Action action, Agent actor) {
            this.kind = kind;
            this.duration = duration;
            this.moving = moving;
            this.action = action;
            this.actor = actor;
        }
    }

    /** Told whenever turns have finished playing (to update the log and the tables). */
    interface Listener {
        void turnsPlayed(List<Turn> turns);
    }

    private final DisplayState display;
    private final Listener listener;
    private final List<Phase> phases = new ArrayList<>();
    private final Set<Agent> done = new HashSet<>();
    private int index;
    private double elapsed;
    private int step;

    Playback(DisplayState display, Listener listener) {
        this.display = display;
        this.listener = listener;
    }

    // ================================================================ building

    void load(EventLog log, int stepNumber) {
        phases.clear();
        done.clear();
        index = 0;
        elapsed = 0;
        step = stepNumber;
        phases.add(new Phase(Kind.SHUFFLE, SHUFFLE_MS, List.of(), null, null));

        List<Turn> quiet = new ArrayList<>();
        for (Turn t : log.getTurns()) {
            if (t.agent() != null && t.actions().isEmpty()) {
                quiet.add(t);
                continue;
            }
            flushQuiet(quiet);
            Phase last = null;
            if (t.agent() != null && !t.path().isEmpty()) {
                last = new Phase(Kind.MOVE, t.path().size() * TILE_MS, List.of(t), null, t.agent());
                phases.add(last);
            }
            for (Action a : t.actions()) {
                last = new Phase(Kind.ACTION, duration(a), List.of(), a, t.agent());
                phases.add(last);
                if (a instanceof Meteor m && m.crater()) {
                    display.hideObstacle(m.where());
                }
            }
            if (last == null) {
                last = new Phase(Kind.MOVE, 0, List.of(), null, t.agent());
                phases.add(last);
            }
            last.applyAfter.add(t);
        }
        flushQuiet(quiet);
    }

    /** Agents that only walked this turn move together in one phase. */
    private void flushQuiet(List<Turn> quiet) {
        if (quiet.isEmpty()) {
            return;
        }
        int longest = 0;
        for (Turn t : quiet) {
            longest = Math.max(longest, t.path().size());
        }
        Phase p = new Phase(Kind.MOVE, longest * TILE_MS, List.copyOf(quiet), null, null);
        p.applyAfter.addAll(quiet);
        phases.add(p);
        quiet.clear();
    }

    private static double duration(Action a) {
        if (a instanceof Exchange e) {
            return switch (e.kind()) {
                case UNION -> 1500;
                case SHARE -> 1600;
                case DELIVERY -> 850;
            };
        }
        if (a instanceof Fight) {
            return 2300;
        }
        if (a instanceof Donation) {
            return 1500;
        }
        if (a instanceof Exhaustion) {
            return 1300;
        }
        if (a instanceof Blocked) {
            return 650;
        }
        return 1700; // meteorite
    }

    // ================================================================ playing

    boolean isActive() {
        return index < phases.size();
    }

    /** Advances the replay by dt milliseconds (already multiplied by the speed). */
    void advance(double dt) {
        double left = dt;
        while (isActive()) {
            Phase p = phases.get(index);
            double remaining = p.duration - elapsed;
            if (left < remaining) {
                elapsed += left;
                updateMoving(p);
                return;
            }
            left -= remaining;
            complete(p);
        }
    }

    /** Jumps to the end of the step. */
    void finishAll() {
        while (isActive()) {
            complete(phases.get(index));
        }
    }

    private void complete(Phase p) {
        elapsed = p.duration;
        updateMoving(p);
        for (Turn t : p.applyAfter) {
            for (EventLog.Snapshot s : t.after()) {
                display.apply(s);
            }
            for (Action a : t.actions()) {
                if (a instanceof Meteor m) {
                    display.showObstacle(m.where());
                }
            }
            if (t.agent() != null) {
                done.add(t.agent());
            }
        }
        if (!p.applyAfter.isEmpty()) {
            listener.turnsPlayed(p.applyAfter);
        }
        index++;
        elapsed = 0;
    }

    /** Places walking agents along their path according to the elapsed time. */
    private void updateMoving(Phase p) {
        if (p.kind != Kind.MOVE) {
            return;
        }
        for (Turn t : p.moving) {
            DisplayState.Visual v = display.get(t.agent());
            if (v == null || t.path().isEmpty()) {
                continue;
            }
            double tiles = Math.min(t.path().size(), elapsed / TILE_MS);
            int i = (int) Math.floor(tiles);
            double f = tiles - i;
            Position from = i == 0 ? t.start() : t.path().get(i - 1);
            Position to = i < t.path().size() ? t.path().get(i) : from;
            double e = f * f * (3 - 2 * f); // smooth step
            v.x = from.x() + (to.x() - from.x()) * e;
            v.y = from.y() + (to.y() - from.y()) * e;
        }
    }

    // ================================================================ queries for drawing

    Phase current() {
        return isActive() ? phases.get(index) : null;
    }

    /** 0..1 progress of the current phase. */
    double progress() {
        Phase p = current();
        return p == null || p.duration <= 0 ? 1 : Math.min(1, elapsed / p.duration);
    }

    boolean hasPlayed(Agent a) {
        return done.contains(a);
    }

    /** Agents acting right now (highlighted). */
    Set<Agent> actors() {
        Set<Agent> set = new HashSet<>();
        Phase p = current();
        if (p == null) {
            return set;
        }
        if (p.actor != null) {
            set.add(p.actor);
        }
        for (Turn t : p.moving) {
            set.add(t.agent());
        }
        return set;
    }

    /** Plain-language description of what is on screen. */
    String caption() {
        Phase p = current();
        if (p == null) {
            return "";
        }
        if (p.kind == Kind.SHUFFLE) {
            return "Step " + step + ": the agents are shuffled into a new random turn order";
        }
        if (p.kind == Kind.MOVE) {
            if (p.moving.size() == 1 && p.actor != null) {
                return p.actor.getId() + "'s turn: walks " + p.moving.get(0).path().size() + " tile(s)";
            }
            StringBuilder sb = new StringBuilder("Moving: ");
            for (int i = 0; i < p.moving.size(); i++) {
                sb.append(i == 0 ? "" : ", ").append(p.moving.get(i).agent().getId());
            }
            return sb.toString();
        }
        return describe(p.action);
    }

    static String describe(Action action) {
        if (action instanceof Exchange e) {
            String a = e.from().getId();
            String b = e.to().getId();
            return switch (e.kind()) {
                case UNION -> a + " meets " + b + " (same people): they pool all their messages  ("
                        + a + " +" + e.toSource() + ", " + b + " +" + e.toTarget() + ")";
                case SHARE -> a + " meets ally " + b + ": they swap a few messages  ("
                        + a + " +" + e.toSource() + ", " + b + " +" + e.toTarget() + ")";
                case DELIVERY -> a + " reaches home and gives " + e.toTarget() + " new message"
                        + (e.toTarget() == 1 ? "" : "s") + " to its Master";
            };
        }
        if (action instanceof Fight f) {
            String loser = f.winner() == f.attacker() ? f.defender().getId() : f.attacker().getId();
            return f.attacker().getId() + " runs into enemy " + f.defender().getId() + ": " + f.attackerChoice()
                    + " vs " + f.defenderChoice() + "  ->  " + f.winner().getId() + " wins"
                    + (f.stolen() > 0 ? " and takes " + f.stolen() + " message" + (f.stolen() == 1 ? "" : "s")
                            + " from " + loser : " (nothing new to take)");
        }
        if (action instanceof Donation d) {
            return d.donor().getId() + " gives " + d.amount() + " EP to the exhausted " + d.target().getId()
                    + ", who gets up and heads home";
        }
        if (action instanceof Exhaustion x) {
            return x.agent().getId() + " " + x.cause() + ": it becomes an obstacle and loses its messages";
        }
        if (action instanceof Blocked b) {
            return b.agent().getId() + " is blocked and loses " + b.epLost() + " EP";
        }
        if (action instanceof Meteor m) {
            return m.hit() != null ? "A meteorite strikes " + m.hit().getId() + "!" : "A meteorite falls at " + m.where();
        }
        return "";
    }
}
