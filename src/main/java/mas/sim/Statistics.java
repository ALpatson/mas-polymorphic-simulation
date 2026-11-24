package mas.sim;

import java.util.EnumMap;
import java.util.List;

import mas.model.Agent;
import mas.model.Master;
import mas.model.Population;

/**
 * Bonus: replays the simulation many times and prints the victory percentage of each
 * population. Every run uses its own seed (firstSeed, firstSeed + 1, ...), so the
 * generator is never reset to the same value twice and the whole table can be reproduced.
 */
public final class Statistics {

    private Statistics() {
    }

    public static void run(int runs, long firstSeed, int maxSteps) {
        EnumMap<Population, Integer> wins = new EnumMap<>(Population.class);
        EnumMap<Population, Long> scores = new EnumMap<>(Population.class);
        for (Population p : Population.values()) {
            wins.put(p, 0);
            scores.put(p, 0L);
        }
        int draws = 0;
        int fullCollections = 0;
        long steps = 0;
        long exhausted = 0;
        long fights = 0;
        long transfers = 0;

        for (int i = 0; i < runs; i++) {
            Simulation sim = new Simulation(firstSeed + i, maxSteps);
            while (sim.nextStep()) {
                // play the whole game without display
            }
            List<Master> winners = sim.computeWinners();
            if (winners.size() == 1) {
                Population p = winners.get(0).getPopulation();
                wins.put(p, wins.get(p) + 1);
            } else {
                draws++;
            }
            for (Master m : sim.getMasters()) {
                scores.put(m.getPopulation(), scores.get(m.getPopulation()) + m.getScore());
                if (m.getScore() == sim.getTotalMessages()) {
                    fullCollections++;
                }
            }
            for (Agent a : sim.getAgents()) {
                if (a.isExhausted()) {
                    exhausted++;
                }
            }
            steps += sim.getStep();
            fights += sim.getLog().getTotalFights();
            transfers += sim.getLog().getTotalTransfers();
        }

        System.out.printf("Statistics over %d runs (seeds %d to %d, max %d steps)%n", runs, firstSeed,
                firstSeed + runs - 1, maxSteps);
        System.out.println("Population  Wins   Win %   Avg final score");
        for (Population p : Population.values()) {
            System.out.printf("%-10s %5d  %5.1f%%   %6.1f%n", p.getLabel(), wins.get(p), 100.0 * wins.get(p) / runs,
                    (double) scores.get(p) / runs);
        }
        System.out.printf("%-10s %5d  %5.1f%%%n", "Draws", draws, 100.0 * draws / runs);
        System.out.printf("Games won by a full collection: %.1f%%%n", 100.0 * fullCollections / runs);
        System.out.printf("Average steps: %.1f | exhausted agents at the end: %.2f | fights: %.1f | transfers: %.1f%n",
                (double) steps / runs, (double) exhausted / runs, (double) fights / runs, (double) transfers / runs);
    }
}
