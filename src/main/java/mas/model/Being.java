package mas.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import mas.map.Map;
import mas.map.Position;
import mas.sim.EventLog;

/**
 * Root of the hierarchy: every living being (mobile agent or Master) has a population,
 * a position and a collection of messages.
 *
 * Messages are kept in a LinkedHashSet: an ordered array-like list of strings that can
 * never contain a duplicate (brief footnote 3).
 */
public abstract class Being {

    private static int beingCount = 0;

    protected final Population population;
    protected final String id;
    protected final Set<String> messages = new LinkedHashSet<>();
    protected Position position;

    protected Being(Population population, String id) {
        this.population = population;
        this.id = id;
        beingCount++;
    }

    // ------------------------------------------------------- polymorphic API

    /** One turn of this being. Agents move; Masters are stationary. */
    public abstract void move(Map map, EventLog log);

    /** Alliance of this being (defined by GoodAgent / BadAgent and by each Master). */
    public abstract Alliance getAlliance();

    /** Character used on the ASCII map. */
    public abstract char getSymbol();

    // ------------------------------------------------------------- messages

    public Set<String> getMessages() {
        return Collections.unmodifiableSet(messages);
    }

    public boolean hasMessage(String message) {
        return messages.contains(message);
    }

    public boolean addMessage(String message) {
        return messages.add(message);
    }

    /** Adds the messages not already known; returns how many were new. */
    public int addMessages(Collection<String> newMessages) {
        int before = messages.size();
        messages.addAll(newMessages);
        return messages.size() - before;
    }

    public int getMessageCount() {
        return messages.size();
    }

    // ------------------------------------------------------------ accessors

    public boolean isAllyOf(Being other) {
        return getAlliance() == other.getAlliance();
    }

    public Position getPosition() {
        return position;
    }

    /** Only the Map should call this (it keeps tiles and positions consistent). */
    public void setPosition(Position position) {
        this.position = position;
    }

    public Population getPopulation() {
        return population;
    }

    public String getId() {
        return id;
    }

    public static int getBeingCount() {
        return beingCount;
    }

    static void resetBeingCount(int value) {
        beingCount = value;
    }

    @Override
    public String toString() {
        return id;
    }
}
