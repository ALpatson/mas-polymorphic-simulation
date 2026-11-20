package mas.sim;

import mas.Config;
import mas.map.Map;
import mas.map.Position;
import mas.model.Agent;
import mas.model.Being;

/**
 * Bonus: random meteorites. A meteorite falls at a random tile with probability
 * METEOR_CHANCE per step. It leaves a crater (a new obstacle) on an empty tile and
 * exhausts an agent it hits. SafeZones are protected.
 */
public class EventManager {

    private int meteorCount;

    public void maybeTrigger(Map map, RandomGen rng, EventLog log) {
        if (!rng.chance(Config.METEOR_CHANCE)) {
            return;
        }
        meteorCount++;
        Position p = new Position(rng.nextInt(0, map.getWidth() - 1), rng.nextInt(0, map.getHeight() - 1));
        if (map.isInSafeZone(p)) {
            log.action(new EventLog.Meteor(p, null, false));
            log.event("Meteorite burns up above the " + map.getZoneOwner(p).getLabel() + " SafeZone " + p);
            return;
        }
        Being being = map.getOccupant(p);
        if (being instanceof Agent agent) {
            if (agent.isExhausted()) {
                log.event("Meteorite hits the already exhausted " + agent.getId() + " at " + p);
            } else {
                log.action(new EventLog.Meteor(p, agent, false));
                log.event("Meteorite strikes " + agent.getId() + " at " + p + "!");
                agent.exhaust("hit by a meteorite", log);
            }
            return;
        }
        boolean crater = map.tryPlaceObstacle(p);
        log.action(new EventLog.Meteor(p, null, crater));
        if (crater) {
            log.event("Meteorite crater: new obstacle at " + p);
        } else {
            log.event("Meteorite lands at " + p + " without changing the map");
        }
    }

    public int getMeteorCount() {
        return meteorCount;
    }
}
