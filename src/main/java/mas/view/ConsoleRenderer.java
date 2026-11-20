package mas.view;

import java.util.List;

import mas.map.Map;
import mas.map.Position;
import mas.map.Tile;
import mas.model.Agent;
import mas.model.Being;
import mas.model.Elf;
import mas.model.Goblin;
import mas.model.Human;
import mas.model.Master;
import mas.model.Orc;
import mas.model.Population;
import mas.sim.EventLog;
import mas.sim.Simulation;

/**
 * ASCII display: the map, the team scores, every agent's EP / state / messages, the static
 * instance counters, the turn order and the event log of the step (message transfers,
 * fights, exhaustions, donations, meteorites). Optional ANSI colours.
 */
public class ConsoleRenderer implements SimulationView {

    private static final String RESET = "\u001B[0m";
    private static final String DIM = "\u001B[2m";
    private static final String BOLD = "\u001B[1m";
    private static final int MAX_EVENTS_SHOWN = 25;

    private final int delayMs;
    private final boolean color;
    private final boolean quiet;

    /**
     * @param delayMs pause after each step so a human can follow the run
     * @param color   use ANSI colours and clear the screen between steps
     * @param quiet   print only the final result
     */
    public ConsoleRenderer(int delayMs, boolean color, boolean quiet) {
        this.delayMs = delayMs;
        this.color = color;
        this.quiet = quiet;
    }

    /** Draws one step. */
    @Override
    public void render(Simulation sim) {
        if (quiet) {
            return;
        }
        StringBuilder out = new StringBuilder();
        if (color) {
            out.append("\u001B[H\u001B[2J"); // cursor home + clear screen
        } else {
            out.append("\n").append("=".repeat(78)).append("\n");
        }
        appendHeader(out, sim);
        appendMap(out, sim.getMap());
        appendTeams(out, sim);
        appendCounters(out, sim);
        appendEvents(out, sim);
        System.out.print(out);
        System.out.flush();
        pause();
    }

    /** Final screen: last map (in quiet mode) and the ranking. */
    @Override
    public void renderResult(Simulation sim, List<Master> winners) {
        StringBuilder out = new StringBuilder("\n");
        if (quiet) {
            appendHeader(out, sim);
            appendMap(out, sim.getMap());
            appendTeams(out, sim);
            appendCounters(out, sim);
        }
        out.append(bold("=== END OF SIMULATION after " + sim.getStep() + " steps ===")).append("\n");
        if (sim.getStep() >= sim.getMaxSteps()) {
            out.append("Reason: step limit reached.\n");
        } else if (winners.get(0).getScore() == sim.getTotalMessages()) {
            out.append("Reason: a Master collected every message.\n");
        } else {
            out.append("Reason: no agent can move any more.\n");
        }
        for (Master m : sim.getMasters()) {
            out.append(String.format("  %-8s %3d / %d messages%n",
                    m.getPopulation().getLabel(), m.getScore(), sim.getTotalMessages()));
        }
        if (winners.size() == 1) {
            Master w = winners.get(0);
            out.append(bold(paint(w.getPopulation(), "WINNER: " + w.getPopulation().getLabel()
                    + " (" + w.getAlliance() + ") with " + w.getScore() + " distinct messages"))).append("\n");
        } else {
            StringBuilder names = new StringBuilder();
            for (Master w : winners) {
                names.append(names.length() == 0 ? "" : ", ").append(w.getPopulation().getLabel());
            }
            out.append(bold("DRAW between " + names + " with " + winners.get(0).getScore() + " messages each")).append("\n");
        }
        System.out.print(out);
        System.out.flush();
    }

    // ================================================================ parts

    private void appendHeader(StringBuilder out, Simulation sim) {
        out.append(bold("MAS Polymorphic Simulation")).append(String.format(
                "   step %d/%d   seed %d   messages in play %d   obstacles %d%n",
                sim.getStep(), sim.getMaxSteps(), sim.getSeed(), sim.getTotalMessages(), sim.getObstaclesPlaced()));
    }

    private void appendMap(StringBuilder out, Map map) {
        out.append("    ");
        for (int x = 0; x < map.getWidth(); x++) {
            out.append(String.format("%3d", x));
        }
        out.append("\n");
        for (int y = 0; y < map.getHeight(); y++) {
            out.append(String.format("%3d ", y));
            for (int x = 0; x < map.getWidth(); x++) {
                out.append(cell(map.getTile(new Position(x, y))));
            }
            out.append("\n");
        }
        out.append(dim("    [E] Master   E1 agent   xe1 exhausted agent (obstacle)   ## obstacle   : SafeZone   . free"))
                .append("\n");
    }

    /** 3-character representation of a tile. */
    private String cell(Tile tile) {
        Being b = tile.getOccupant();
        if (b instanceof Master m) {
            return bold(paint(m.getPopulation(), "[" + m.getSymbol() + "]"));
        }
        if (b instanceof Agent a) {
            String number = a.getId().substring(1);
            if (a.isExhausted()) {
                return paint(a.getPopulation(), "x" + Character.toLowerCase(a.getSymbol()) + number);
            }
            return bold(paint(a.getPopulation(), " " + a.getSymbol() + number));
        }
        if (tile.isObstacle()) {
            return dim(" ##");
        }
        if (tile.getZoneOwner() != null) {
            return paint(tile.getZoneOwner(), " : ");
        }
        return dim(" . ");
    }

    private void appendTeams(StringBuilder out, Simulation sim) {
        out.append("\n");
        for (Master m : sim.getMasters()) {
            Population p = m.getPopulation();
            String moveName = "";
            StringBuilder agentsText = new StringBuilder();
            for (Agent a : sim.getAgents()) {
                if (a.getPopulation() != p) {
                    continue;
                }
                moveName = a.getMovement().getName();
                String dir = a.getLastDirection() == null ? "-" : a.getLastDirection().shortName();
                agentsText.append(String.format(" %s %s %3dep %2dm %-2s |",
                        a.getId(), a.getState().shortName(), a.getEp(), a.getMessageCount(), dir));
            }
            String head = String.format("%-7s %-4s %-6s score %2d/%d %s",
                    p.getLabel(), m.getAlliance(), moveName, m.getScore(), sim.getTotalMessages(),
                    bar(m.getScore(), sim.getTotalMessages()));
            out.append(paint(p, head)).append(" |").append(agentsText).append("\n");
        }
    }

    private void appendCounters(StringBuilder out, Simulation sim) {
        EventLog log = sim.getLog();
        out.append(String.format(
                "Instances: Being %d | Agent %d | Master %d | Elf %d | Human %d | Orc %d | Goblin %d%n",
                Being.getBeingCount(), Agent.getAgentCount(), Master.getMasterCount(),
                Elf.getElfCount(), Human.getHumanCount(), Orc.getOrcCount(), Goblin.getGoblinCount()));
        out.append(String.format(
                "Totals:    transfers %d | fights %d | exhaustions %d | donations %d | meteorites %d%n",
                log.getTotalTransfers(), log.getTotalFights(), log.getTotalExhaustions(),
                log.getTotalDonations(), sim.getMeteorCount()));
    }

    private void appendEvents(StringBuilder out, Simulation sim) {
        StringBuilder order = new StringBuilder();
        for (Agent a : sim.getTurnOrder()) {
            order.append(a.getId()).append(' ');
        }
        out.append("Turn order: ").append(order.length() == 0 ? "-" : order.toString().trim()).append("\n");
        List<String> events = sim.getLog().getStepEvents();
        out.append(bold("Events this step (" + events.size() + "):")).append("\n");
        int shown = Math.min(events.size(), MAX_EVENTS_SHOWN);
        for (int i = 0; i < shown; i++) {
            out.append("  ").append(events.get(i)).append("\n");
        }
        if (events.size() > shown) {
            out.append("  ... ").append(events.size() - shown).append(" more\n");
        }
    }

    // ============================================================== helpers

    private static String bar(int value, int total) {
        int width = 10;
        int filled = total == 0 ? 0 : Math.round(width * (float) value / total);
        return "[" + "#".repeat(filled) + "-".repeat(width - filled) + "]";
    }

    private String paint(Population p, String text) {
        if (!color) {
            return text;
        }
        String code = switch (p) {
            case ELF -> "\u001B[95m";    // pink
            case HUMAN -> "\u001B[94m";  // blue
            case ORC -> "\u001B[33m";    // orange / yellow
            case GOBLIN -> "\u001B[92m"; // green
        };
        return code + text + RESET;
    }

    private String bold(String text) {
        return color ? BOLD + text + RESET : text;
    }

    private String dim(String text) {
        return color ? DIM + text + RESET : text;
    }

    private void pause() {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
