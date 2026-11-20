package mas;

/**
 * Every tunable number of the simulation in one place, so the EP calibration
 * and the map parameters can be changed without touching the logic.
 */
public final class Config {

    private Config() {
        // constants only, never instantiated
    }

    // ---- Map -------------------------------------------------------------
    public static final int MAP_WIDTH = 16;
    public static final int MAP_HEIGHT = 12;
    /** Each SafeZone is ZONE_WIDTH x ZONE_HEIGHT tiles in a corner (4 x 12 = 48 of 192 tiles = 25 %). */
    public static final int ZONE_WIDTH = 4;
    public static final int ZONE_HEIGHT = 3;
    public static final int OBSTACLE_COUNT = 14;

    // ---- Populations and messages ------------------------------------------
    public static final int AGENTS_PER_POPULATION = 4;
    public static final int MESSAGES_PER_AGENT = 3;

    // ---- Energy points (EP) -------------------------------------------------
    public static final int MAX_EP = 100;
    /** EP lost per tile travelled outside the agent's own SafeZone. */
    public static final int EP_COST_PER_TILE = 2;
    /** EP regained per step spent resting in the agent's own SafeZone. */
    public static final int EP_RECOVERY_PER_STEP = 25;
    /** Below this percentage of max EP the agent heads home (brief rule: 20 %). */
    public static final int LOW_EP_PERCENT = 20;
    /** A resting agent only leaves home again once it reaches this percentage (avoids bouncing in and out). */
    public static final int LEAVE_HOME_PERCENT = 90;
    /** EP an ally donates to an exhausted agent, as a percentage of max EP. */
    public static final int DONATION_PERCENT = 30;

    // ---- Movement -------------------------------------------------------------
    /** Maximum number of tiles for Queen / Bishop / Rook moves. */
    public static final int MAX_LINE_RANGE = 4;
    /** Maximum tiles walked per step on the shortest path home. */
    public static final int HOME_TILES_PER_STEP = 3;

    // ---- Encounters -------------------------------------------------------------
    /** Same alliance, different population: each side gives 1..ALLY_SHARE_MAX messages. */
    public static final int ALLY_SHARE_MAX = 3;
    /** Enemy confrontation: the winner takes 1..STEAL_MAX messages from the loser. */
    public static final int STEAL_MAX = 3;

    // ---- Random events (bonus) ----------------------------------------------------
    /** Probability, at each step, that a meteorite falls somewhere on the map. */
    public static final double METEOR_CHANCE = 0.04;

    // ---- Run ----------------------------------------------------------------------
    public static final int DEFAULT_MAX_STEPS = 300;
    public static final int DEFAULT_DELAY_MS = 200;
    public static final long DEFAULT_SEED = 42L;
}
