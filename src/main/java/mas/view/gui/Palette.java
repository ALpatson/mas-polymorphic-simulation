package mas.view.gui;

import java.awt.Color;

import mas.model.Population;
import mas.sim.EventLog;

/** Colours of the graphical view (team colours follow the brief's illustration). */
final class Palette {

    private Palette() {
    }

    static final Color GRASS = new Color(0xDCEBC0);
    static final Color GRASS_ALT = new Color(0xD3E4B4);
    static final Color GRASS_TUFT = new Color(0x9DBB6C);
    static final Color GRID = new Color(0xC2D3A0);
    static final Color ROCK = new Color(0x8D8A83);
    static final Color ROCK_LIGHT = new Color(0xB3B0A8);
    static final Color ROCK_DARK = new Color(0x55524C);
    static final Color GOLD = new Color(0xE3AE1B);
    static final Color PANEL = new Color(0x2F3B2A);
    static final Color TEXT = new Color(0x2B2B2B);
    static final Color MUTED = new Color(0x6B6B6B);
    static final Color HIGHLIGHT = new Color(0xFFE34D);

    static Color team(Population p) {
        return switch (p) {
            case ELF -> new Color(0xD6458F);    // pink
            case HUMAN -> new Color(0x2F6FD0);  // blue
            case ORC -> new Color(0xE07418);    // orange
            case GOBLIN -> new Color(0x2E9D4F); // green
        };
    }

    /** Light tint of the team colour used for its SafeZone tiles. */
    static Color zone(Population p) {
        Color c = team(p);
        return new Color(mix(c.getRed()), mix(c.getGreen()), mix(c.getBlue()));
    }

    private static int mix(int channel) {
        return (channel * 2 + 3 * 240) / 5;
    }

    static Color exchange(EventLog.ExchangeKind kind) {
        return switch (kind) {
            case UNION -> new Color(0x2E9D4F);
            case SHARE -> new Color(0x139DB5);
            case DELIVERY -> GOLD;
        };
    }

    static final Color FIGHT = new Color(0xD62828);
    static final Color DONATION = new Color(0x8E44D9);

    /** Green when full, orange in the middle, red when low. */
    static Color energy(double ratio) {
        if (ratio >= 0.5) {
            return new Color(0x3BAA4A);
        }
        if (ratio >= 0.2) {
            return new Color(0xE6A117);
        }
        return new Color(0xD62828);
    }

    static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
