package mas.view.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import mas.model.Agent;
import mas.model.Master;
import mas.sim.Simulation;

/** Team scoreboard: a character portrait, a coloured progress bar and agent status per team. */
final class ScorePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int ROW = 46;

    private final transient DisplayState display;
    private transient Simulation sim;

    ScorePanel(DisplayState display) {
        this.display = display;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(400, 4 * ROW + 58));
    }

    void setSimulation(Simulation sim) {
        this.sim = sim;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (sim == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth() - getInsets().right;
        int total = sim.getTotalMessages();
        int y = getInsets().top + 2;
        for (Master m : sim.getMasters()) {
            DisplayState.Visual v = display.get(m);
            int score = v == null ? m.getScore() : v.messages;
            Color team = Palette.team(m.getPopulation());

            Sprites.drawToken(g2, m.getPopulation(), 30, y + ROW / 2.0, ROW - 6, false);

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g2.setColor(team.darker());
            g2.drawString(m.getPopulation().getLabel(), 58, y + 19);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            g2.setColor(Palette.MUTED);
            g2.drawString(m.getAlliance() + " · " + statusText(m), 58, y + 35);

            int barX = 205;
            int barW = w - barX - 62;
            g2.setColor(new Color(0xECECEC));
            g2.fillRoundRect(barX, y + 13, barW, 18, 9, 9);
            int filled = total == 0 ? 0 : (int) Math.round((double) barW * score / total);
            g2.setColor(team);
            g2.fillRoundRect(barX, y + 13, filled, 18, 9, 9);

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g2.setColor(Palette.TEXT);
            String text = score + "/" + total;
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, w - 10 - fm.stringWidth(text), y + 27);
            y += ROW;
        }
        g2.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        g2.setColor(Palette.MUTED);
        g2.drawString("Score = distinct messages held by the Master. First to " + total + " wins.", 10, y + 16);
        g2.dispose();
    }

    private String statusText(Master m) {
        int alive = 0;
        int down = 0;
        for (Agent a : sim.getAgents()) {
            if (a.getPopulation() != m.getPopulation()) {
                continue;
            }
            DisplayState.Visual v = display.get(a);
            if (v != null && v.exhausted) {
                down++;
            } else {
                alive++;
            }
        }
        return alive + " up · " + down + " down";
    }
}
