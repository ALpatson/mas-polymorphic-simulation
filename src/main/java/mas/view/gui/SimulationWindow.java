package mas.view.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import mas.model.Agent;
import mas.model.Being;
import mas.model.Elf;
import mas.model.Goblin;
import mas.model.Human;
import mas.model.Master;
import mas.model.Orc;
import mas.sim.EventLog;
import mas.sim.Simulation;
import mas.view.SimulationView;

/**
 * The game window. The engine computes a whole step instantly; the window then replays it
 * slowly (see {@link Playback}) so every move, message exchange and fight can be followed.
 * Controls: Play / Pause, Step (one step), Restart (same seed), New random game, seed,
 * and speed (Slow ... Instant).
 */
public final class SimulationWindow extends JFrame implements SimulationView {

    private static final long serialVersionUID = 1L;
    private static final int MAX_LOG_LINES = 3000;
    private static final double PAUSE_BETWEEN_STEPS_MS = 400;

    private static final String[] SPEED_NAMES = {"Slow", "Normal", "Fast", "Very fast", "Turbo", "Instant"};
    private static final double[] SPEEDS = {0.5, 1, 2, 4, 10, Double.POSITIVE_INFINITY};

    private final int maxSteps;
    private transient Simulation sim;

    private final transient DisplayState display = new DisplayState();
    private final transient Playback playback;
    private final MapPanel mapPanel;
    private final ScorePanel scorePanel;
    private final AgentTableModel tableModel;
    private final JTable agentTable;
    private final JTextArea logArea = new JTextArea();
    private final JLabel countersLabel = new JLabel();
    private final JLabel stepLabel = new JLabel();
    private final JButton playButton = new JButton("▶  Play");
    private final JButton stepButton = new JButton("Step ⏭");
    private final JTextField seedField = new JTextField(7);
    private final JComboBox<String> speedBox = new JComboBox<>(SPEED_NAMES);
    private final Timer clock;

    /** Running = the animation clock advances; continuous = start the next step automatically. */
    private boolean running;
    private boolean continuous;
    private boolean finished;
    private double waitBeforeNextStep;
    private long lastTick;

    public SimulationWindow(long seed, int maxSteps) {
        super("MAS Polymorphic Simulation — Elves & Humans vs Orcs & Goblins");
        this.maxSteps = maxSteps;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        playback = new Playback(display, this::onTurnsPlayed);
        mapPanel = new MapPanel(display, playback);
        scorePanel = new ScorePanel(display);
        tableModel = new AgentTableModel(display);
        agentTable = new JTable(tableModel);
        speedBox.setSelectedIndex(1);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildToolbar(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapPanel, buildSidePanel());
        split.setResizeWeight(0.72);
        split.setBorder(null);
        root.add(split, BorderLayout.CENTER);
        setContentPane(root);

        clock = new Timer(16, e -> tick());
        newGame(seed);
        pack();
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        clock.start();
    }

    /** Uses the Nimbus look and feel when available (looks the same on every OS). */
    public static void installLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            // keep the default look and feel
        }
    }

    // ============================================================ layout

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xD0D0D0)));

        playButton.setFont(playButton.getFont().deriveFont(Font.BOLD, 14f));
        playButton.addActionListener(e -> togglePlay());
        stepButton.setToolTipText("Play exactly one step, then stop");
        stepButton.addActionListener(e -> stepOnce());
        JButton restart = new JButton("↻ Restart");
        restart.setToolTipText("Replay the game of the seed shown (same seed = same game)");
        restart.addActionListener(e -> newGame(readSeed()));
        JButton random = new JButton("New random game");
        random.addActionListener(e -> newGame(Math.floorMod(System.nanoTime(), 1_000_000L)));
        seedField.addActionListener(e -> newGame(readSeed()));
        speedBox.setToolTipText("Slow / Normal to follow every exchange, Instant to jump ahead");

        stepLabel.setFont(stepLabel.getFont().deriveFont(Font.BOLD, 14f));

        bar.add(playButton);
        bar.add(stepButton);
        bar.add(restart);
        bar.add(random);
        bar.add(Box.createHorizontalStrut(10));
        bar.add(new JLabel("Seed:"));
        bar.add(seedField);
        bar.add(Box.createHorizontalStrut(10));
        bar.add(new JLabel("Speed:"));
        bar.add(speedBox);
        bar.add(Box.createHorizontalStrut(14));
        bar.add(stepLabel);
        return bar;
    }

    private JComponent buildSidePanel() {
        JPanel side = new JPanel(new BorderLayout(0, 6));
        side.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        side.setPreferredSize(new Dimension(430, 600));

        scorePanel.setBorder(BorderFactory.createTitledBorder("Teams"));

        agentTable.setRowHeight(20);
        agentTable.setFillsViewportHeight(true);
        agentTable.setDefaultRenderer(String.class, new AgentCellRenderer());
        agentTable.setDefaultRenderer(Integer.class, new AgentCellRenderer());
        agentTable.getColumnModel().getColumn(4).setCellRenderer(new EpBarRenderer());
        int[] widths = {45, 65, 55, 90, 95, 40};
        for (int i = 0; i < widths.length; i++) {
            agentTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JScrollPane tableScroll = new JScrollPane(agentTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Agents"));
        tableScroll.setPreferredSize(new Dimension(420, 16 * 20 + 50));

        countersLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        countersLabel.setFont(countersLabel.getFont().deriveFont(11f));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(scorePanel);
        top.add(tableScroll);
        top.add(countersLabel);
        scorePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        countersLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Event log"));

        side.add(top, BorderLayout.NORTH);
        side.add(logScroll, BorderLayout.CENTER);
        return side;
    }

    // ============================================================ game control

    private void newGame(long seed) {
        running = false;
        continuous = false;
        finished = false;
        waitBeforeNextStep = 0;
        playButton.setText("▶  Play");
        playButton.setEnabled(true);
        stepButton.setEnabled(true);
        seedField.setText(String.valueOf(seed));

        sim = new Simulation(seed, maxSteps);
        display.syncAll(sim);
        playback.load(new EventLog(), 0);
        playback.finishAll();
        mapPanel.setSimulation(sim);
        scorePanel.setSimulation(sim);
        tableModel.setSimulation(sim);
        logArea.setText("New game, seed " + seed + ": " + sim.getTotalMessages() + " messages to collect, "
                + sim.getAgents().size() + " agents, " + sim.getObstaclesPlaced() + " obstacles.\n");
        render(sim);
    }

    private void togglePlay() {
        if (finished) {
            return;
        }
        if (running && continuous) {
            running = false;
            continuous = false;
            playButton.setText("▶  Play");
        } else {
            running = true;
            continuous = true;
            mapPanel.hideTitle();
            playButton.setText("⏸  Pause");
            if (!playback.isActive() && waitBeforeNextStep <= 0) {
                startNextStep();
            }
        }
    }

    private void stepOnce() {
        if (finished) {
            return;
        }
        mapPanel.hideTitle();
        continuous = false;
        running = true;
        waitBeforeNextStep = 0;
        playButton.setText("▶  Play");
        if (!playback.isActive()) {
            startNextStep();
        }
    }

    private void startNextStep() {
        if (!sim.nextStep()) {
            showResult();
            return;
        }
        playback.load(sim.getLog(), sim.getStep());
        StringBuilder sb = new StringBuilder("\n── Step ").append(sim.getStep()).append(" ──  order: ");
        for (Agent a : sim.getTurnOrder()) {
            sb.append(a.getId()).append(' ');
        }
        appendLog(sb.append('\n').toString());
        stepLabel.setText("Step " + sim.getStep() + " / " + sim.getMaxSteps());
    }

    /** Animation clock, about 60 times per second. */
    private void tick() {
        long now = System.nanoTime();
        double dt = lastTick == 0 ? 16 : Math.min(100, (now - lastTick) / 1e6);
        lastTick = now;
        if (!running) {
            if (sim.getStep() == 0) {
                mapPanel.repaint(); // only the title screen animates while paused
            }
            return;
        }
        double speed = SPEEDS[speedBox.getSelectedIndex()];

        if (playback.isActive()) {
            if (Double.isInfinite(speed)) {
                playback.finishAll();
            } else {
                playback.advance(dt * speed);
            }
            if (!playback.isActive()) {
                onStepAnimated();
            }
        } else if (waitBeforeNextStep > 0) {
            waitBeforeNextStep -= Double.isInfinite(speed) ? Double.MAX_VALUE : dt * speed;
            if (waitBeforeNextStep <= 0) {
                startNextStep();
            }
        }
        mapPanel.repaint();
    }

    private void onStepAnimated() {
        display.syncAll(sim);
        render(sim);
        if (sim.isFinished()) {
            showResult();
        } else if (continuous) {
            waitBeforeNextStep = PAUSE_BETWEEN_STEPS_MS;
        } else {
            running = false;
        }
    }

    private void showResult() {
        if (!finished) {
            renderResult(sim, sim.computeWinners());
        }
    }

    private void onTurnsPlayed(List<EventLog.Turn> turns) {
        StringBuilder sb = new StringBuilder();
        for (EventLog.Turn t : turns) {
            for (String line : t.lines()) {
                sb.append(line).append('\n');
            }
        }
        if (sb.length() > 0) {
            appendLog(sb.toString());
        }
        tableModel.refresh();
        scorePanel.repaint();
    }

    private long readSeed() {
        try {
            return Long.parseLong(seedField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "The seed must be a whole number.", "Seed", JOptionPane.WARNING_MESSAGE);
            return sim.getSeed();
        }
    }

    private void appendLog(String text) {
        logArea.append(text);
        int extra = logArea.getLineCount() - MAX_LOG_LINES;
        if (extra > 0) {
            try {
                logArea.replaceRange("", 0, logArea.getLineEndOffset(extra - 1));
            } catch (javax.swing.text.BadLocationException e) {
                // trimming is only cosmetic
            }
        }
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // ============================================================ SimulationView

    @Override
    public void render(Simulation s) {
        stepLabel.setText("Step " + s.getStep() + " / " + s.getMaxSteps());
        scorePanel.repaint();
        tableModel.refresh();
        countersLabel.setText(countersHtml(s));
        mapPanel.repaint();
    }

    @Override
    public void renderResult(Simulation s, List<Master> winners) {
        finished = true;
        running = false;
        continuous = false;
        playButton.setText("▶  Play");
        playButton.setEnabled(false);
        stepButton.setEnabled(false);
        mapPanel.setWinners(winners);

        StringBuilder text = new StringBuilder("\n=== END after ").append(s.getStep()).append(" steps: ");
        if (winners.size() == 1) {
            Master w = winners.get(0);
            text.append(w.getPopulation().getLabel()).append(" win with ").append(w.getScore()).append(" messages\n");
        } else {
            text.append("draw with ").append(winners.get(0).getScore()).append(" messages each\n");
        }
        for (Master m : s.getMasters()) {
            text.append(String.format("%-8s %2d / %d%n", m.getPopulation().getLabel(), m.getScore(), s.getTotalMessages()));
        }
        appendLog(text.toString());
    }

    private static String countersHtml(Simulation s) {
        EventLog log = s.getLog();
        return "<html><b>Instances</b> (static counters): Being " + Being.getBeingCount()
                + " &middot; Agent " + Agent.getAgentCount() + " &middot; Master " + Master.getMasterCount()
                + " &middot; Elf " + Elf.getElfCount() + " &middot; Human " + Human.getHumanCount()
                + " &middot; Orc " + Orc.getOrcCount() + " &middot; Goblin " + Goblin.getGoblinCount()
                + "<br><b>Totals:</b> transfers " + log.getTotalTransfers() + " &middot; fights " + log.getTotalFights()
                + " &middot; exhaustions " + log.getTotalExhaustions() + " &middot; donations " + log.getTotalDonations()
                + " &middot; meteorites " + s.getMeteorCount() + "</html>";
    }

    // ============================================================ table renderers

    /** Team colour for the agent name, grey text for exhausted agents. */
    private final class AgentCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            Agent a = tableModel.getAgent(row);
            DisplayState.Visual v = tableModel.visual(row);
            boolean exhausted = v != null && v.exhausted;
            setHorizontalAlignment(column >= 4 ? CENTER : LEFT);
            setFont(getFont().deriveFont(column == 0 ? Font.BOLD : Font.PLAIN));
            if (!selected) {
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF6F6F6));
                if (exhausted) {
                    setForeground(new Color(0x9E9E9E));
                } else if (column <= 1) {
                    setForeground(Palette.team(a.getPopulation()));
                } else {
                    setForeground(Palette.TEXT);
                }
            }
            return this;
        }
    }

    /** Draws EP as a small coloured bar with the number. */
    private final class EpBarRenderer extends JComponent implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        private int ep;
        private int max = 1;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            DisplayState.Visual v = tableModel.visual(row);
            Agent a = tableModel.getAgent(row);
            ep = v == null ? a.getEp() : v.ep;
            max = a.getMaxEp();
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 8;
            int h = getHeight() - 8;
            g2.setColor(new Color(0xE4E4E4));
            g2.fillRoundRect(4, 4, w, h, 6, 6);
            double ratio = (double) ep / max;
            g2.setColor(Palette.energy(ratio));
            g2.fillRoundRect(4, 4, (int) Math.round(w * ratio), h, 6, 6);
            g2.setColor(Palette.TEXT);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            String text = String.valueOf(ep);
            int tw = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, (getWidth() - tw) / 2, getHeight() / 2 + 4);
            g2.dispose();
        }
    }
}
