package mas;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import mas.sim.Simulation;
import mas.view.ConsoleRenderer;
import mas.view.gui.SimulationWindow;

/**
 * Entry point. Opens the graphical window by default; --console (or --quiet, or a machine
 * without a screen) uses the ASCII console display instead.
 *
 * Usage: java -cp out mas.Main [seed|random] [maxSteps] [delayMs] [--console] [--fast] [--quiet] [--color|--no-color]
 *   seed      same seed = same game (default 42; "random" for a new one)
 *   maxSteps  step limit (default 300)
 *   delayMs   pause between steps (default 200)
 *   --console ASCII display in the terminal instead of the window
 *   --fast    no pause (console)
 *   --quiet   console, final result only
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        long seed = Config.DEFAULT_SEED;
        int maxSteps = Config.DEFAULT_MAX_STEPS;
        int delayMs = Config.DEFAULT_DELAY_MS;
        boolean quiet = false;
        boolean console = false;
        Boolean color = null;

        int positional = 0;
        for (String arg : args) {
            switch (arg) {
                case "--console" -> console = true;
                case "--fast" -> delayMs = 0;
                case "--quiet" -> quiet = true;
                case "--color" -> color = true;
                case "--no-color" -> color = false;
                case "--help", "-h" -> {
                    System.out.println("Usage: run [seed|random] [maxSteps] [delayMs] "
                            + "[--console] [--fast] [--quiet] [--color|--no-color]");
                    return;
                }
                default -> {
                    try {
                        switch (positional++) {
                            case 0 -> seed = arg.equals("random") ? Math.floorMod(System.nanoTime(), 1_000_000L)
                                    : Long.parseLong(arg);
                            case 1 -> maxSteps = Integer.parseInt(arg);
                            case 2 -> delayMs = Integer.parseInt(arg);
                            default -> System.err.println("Ignoring extra argument: " + arg);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Not a number: " + arg);
                        return;
                    }
                }
            }
        }

        if (!console && !quiet && !GraphicsEnvironment.isHeadless()) {
            final long gameSeed = seed;
            final int steps = maxSteps;
            SwingUtilities.invokeLater(() -> {
                SimulationWindow.installLookAndFeel();
                new SimulationWindow(gameSeed, steps).setVisible(true);
            });
            return;
        }

        if (color == null) {
            color = terminalSupportsColor();
        }
        if (quiet) {
            delayMs = 0;
        }
        new Simulation(seed, maxSteps).run(new ConsoleRenderer(delayMs, color, quiet));
    }

    /** ANSI colours in real terminals (Windows Terminal, VS Code, Unix); plain text elsewhere (e.g. NetBeans). */
    private static boolean terminalSupportsColor() {
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }
        return System.getenv("WT_SESSION") != null
                || System.getenv("TERM_PROGRAM") != null
                || System.getenv("TERM") != null;
    }
}
