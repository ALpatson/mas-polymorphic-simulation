package mas.view.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import mas.model.Agent;
import mas.sim.Simulation;

/** Rows of the agent table: one per mobile agent, showing the displayed (animated) values. */
final class AgentTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Agent", "Team", "Moves", "State", "EP", "Msgs"};

    private final transient DisplayState display;
    private transient List<Agent> agents = new ArrayList<>();

    AgentTableModel(DisplayState display) {
        this.display = display;
    }

    void setSimulation(Simulation sim) {
        agents = sim.getAgents();
        fireTableDataChanged();
    }

    void refresh() {
        if (!agents.isEmpty()) {
            fireTableRowsUpdated(0, agents.size() - 1);
        }
    }

    Agent getAgent(int row) {
        return agents.get(row);
    }

    DisplayState.Visual visual(int row) {
        return display.get(agents.get(row));
    }

    @Override
    public int getRowCount() {
        return agents.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return column == 4 || column == 5 ? Integer.class : String.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Agent a = agents.get(row);
        DisplayState.Visual v = display.get(a);
        return switch (column) {
            case 0 -> a.getId();
            case 1 -> a.getPopulation().getLabel();
            case 2 -> a.getMovement().getName();
            case 3 -> v != null && v.exhausted ? "EXHAUSTED" : a.getState().name().replace('_', ' ');
            case 4 -> v == null ? a.getEp() : v.ep;
            default -> v == null ? a.getMessageCount() : v.messages;
        };
    }
}
