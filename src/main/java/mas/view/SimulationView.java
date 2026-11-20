package mas.view;

import java.util.List;

import mas.model.Master;
import mas.sim.Simulation;

/**
 * A way of showing the simulation. The console renderer and the Swing window both
 * implement it, so the engine does not depend on how it is displayed.
 */
public interface SimulationView {

    /** Shows the current state (called once before the first step and after every step). */
    void render(Simulation sim);

    /** Shows the final result. Several winners = draw. */
    void renderResult(Simulation sim, List<Master> winners);
}
