package mas.sim;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import mas.model.RpsChoice;

/**
 * The single pseudo-random number generator used by the whole program (brief requirement).
 * It is a Singleton so that every class shares one seeded stream: the same seed replays
 * exactly the same simulation.
 */
public final class RandomGen {

    private static RandomGen instance;

    private Random random;
    private long seed;

    private RandomGen() {
        setSeed(System.nanoTime());
    }

    public static RandomGen getInstance() {
        if (instance == null) {
            instance = new RandomGen();
        }
        return instance;
    }

    /** Restarts the stream from the given seed. */
    public void setSeed(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    public long getSeed() {
        return seed;
    }

    /** Uniform integer in [min, max] (both inclusive). */
    public int nextInt(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    /** True with the given probability (0..1). */
    public boolean chance(double probability) {
        return random.nextDouble() < probability;
    }

    public <T> void shuffle(List<T> list) {
        Collections.shuffle(list, random);
    }

    public <T> T pick(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    public RpsChoice nextRps() {
        RpsChoice[] all = RpsChoice.values();
        return all[random.nextInt(all.length)];
    }
}
