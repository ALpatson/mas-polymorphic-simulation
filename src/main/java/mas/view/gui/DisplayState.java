package mas.view.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import mas.map.Position;
import mas.model.Agent;
import mas.model.Being;
import mas.model.Master;
import mas.sim.EventLog;
import mas.sim.Simulation;

/**
 * What the screen currently shows. The engine plays a whole step at once; the window then
 * replays it slowly, so the displayed positions / EP / messages lag behind the real state
 * until each turn's animation is finished.
 */
final class DisplayState {

    /** Displayed values of one being. */
    static final class Visual {
        double x;
        double y;
        int ep;
        int maxEp;
        int messages;
        boolean exhausted;
    }

    private final HashMap<Being, Visual> visuals = new HashMap<>();
    private final Set<Position> hiddenObstacles = new HashSet<>();

    /** Copies the real state of every being (start of a game, end of each step). */
    void syncAll(Simulation sim) {
        visuals.clear();
        hiddenObstacles.clear();
        for (Master m : sim.getMasters()) {
            Visual v = new Visual();
            v.x = m.getPosition().x();
            v.y = m.getPosition().y();
            v.messages = m.getScore();
            visuals.put(m, v);
        }
        for (Agent a : sim.getAgents()) {
            Visual v = new Visual();
            v.x = a.getPosition().x();
            v.y = a.getPosition().y();
            v.ep = a.getEp();
            v.maxEp = a.getMaxEp();
            v.messages = a.getMessageCount();
            v.exhausted = a.isExhausted();
            visuals.put(a, v);
        }
    }

    void apply(EventLog.Snapshot s) {
        Visual v = visuals.get(s.being());
        if (v == null) {
            return;
        }
        v.x = s.position().x();
        v.y = s.position().y();
        v.ep = s.ep();
        v.messages = s.messages();
        v.exhausted = s.exhausted();
    }

    Visual get(Being b) {
        return visuals.get(b);
    }

    /** A meteorite crater exists in the model but must not be drawn before the meteorite lands. */
    void hideObstacle(Position p) {
        hiddenObstacles.add(p);
    }

    void showObstacle(Position p) {
        hiddenObstacles.remove(p);
    }

    boolean isHidden(Position p) {
        return hiddenObstacles.contains(p);
    }
}
