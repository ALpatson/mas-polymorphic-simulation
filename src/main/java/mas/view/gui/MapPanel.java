package mas.view.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import mas.map.Map;
import mas.map.Position;
import mas.map.SafeZone;
import mas.map.Tile;
import mas.model.Agent;
import mas.model.Being;
import mas.model.Master;
import mas.model.Population;
import mas.sim.EventLog;
import mas.sim.EventLog.Action;
import mas.sim.EventLog.Blocked;
import mas.sim.EventLog.Donation;
import mas.sim.EventLog.Exchange;
import mas.sim.EventLog.Exhaustion;
import mas.sim.EventLog.Fight;
import mas.sim.EventLog.Meteor;
import mas.sim.Simulation;

/**
 * The game board: grass, SafeZones, rocks, the characters, and the animation of the action
 * being replayed (walking, envelopes flying, fights, donations, meteorites). A turn-order
 * bar sits on top, a caption explaining the current action at the bottom, and title / end
 * screens on top of everything.
 */
final class MapPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int STRIP_H = 56;
    private static final int CAPTION_H = 42;

    private final transient DisplayState display;
    private final transient Playback playback;
    private transient Simulation sim;
    private transient List<Master> winners;
    private boolean showTitle = true;
    private long frame;

    private double tile;
    private double ox;
    private double oy;

    MapPanel(DisplayState display, Playback playback) {
        this.display = display;
        this.playback = playback;
        setBackground(Palette.PANEL);
        setPreferredSize(new Dimension(16 * 52 + 24, 12 * 52 + STRIP_H + CAPTION_H + 16));
        ToolTipManager.sharedInstance().registerComponent(this);
        ToolTipManager.sharedInstance().setInitialDelay(150);
    }

    void setSimulation(Simulation sim) {
        this.sim = sim;
        this.winners = null;
        this.showTitle = true;
        repaint();
    }

    void setWinners(List<Master> winners) {
        this.winners = winners;
        repaint();
    }

    void hideTitle() {
        showTitle = false;
    }

    // ================================================================ painting

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (sim == null) {
            return;
        }
        frame++;
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Map map = sim.getMap();
        int w = getWidth();
        int h = getHeight();
        tile = Math.max(14, Math.min((w - 24.0) / map.getWidth(), (h - STRIP_H - CAPTION_H - 16.0) / map.getHeight()));
        ox = (w - tile * map.getWidth()) / 2;
        oy = STRIP_H + 4 + ((h - STRIP_H - CAPTION_H - 8) - tile * map.getHeight()) / 2;

        paintBoard(g, map);
        Playback.Phase phase = playback.current();
        double t = playback.progress();
        if (phase != null && phase.action != null) {
            paintActionUnder(g, phase.action, t);
        }
        paintActorHighlight(g);
        for (Master m : sim.getMasters()) {
            paintMaster(g, m);
        }
        for (Agent a : sim.getAgents()) {
            paintAgent(g, a);
        }
        if (phase != null && phase.action != null) {
            paintActionOver(g, phase.action, t);
        }
        paintTurnStrip(g, w);
        paintCaption(g, w, h);

        if (winners != null) {
            paintEndScreen(g, w, h);
        } else if (showTitle && sim.getStep() == 0 && !playback.isActive()) {
            paintTitleScreen(g, w, h);
        }
        g.dispose();
    }

    // ---------------------------------------------------------------- board

    private void paintBoard(Graphics2D g, Map map) {
        // shadow under the board
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new RoundRectangle2D.Double(ox + 4, oy + 6, tile * map.getWidth(), tile * map.getHeight(), 14, 14));

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Tile t = map.getTile(new Position(x, y));
                double px = ox + x * tile;
                double py = oy + y * tile;
                Rectangle2D r = new Rectangle2D.Double(px, py, tile, tile);
                if (t.getZoneOwner() != null) {
                    g.setColor(Palette.zone(t.getZoneOwner()));
                } else {
                    g.setColor((x + y) % 2 == 0 ? Palette.GRASS : Palette.GRASS_ALT);
                }
                g.fill(r);
                g.setColor(Palette.GRID);
                g.setStroke(new BasicStroke(1f));
                g.draw(r);
                if (t.getZoneOwner() == null && ((x * 7 + y * 13) % 5 == 0)) {
                    paintTuft(g, px + tile * 0.3, py + tile * 0.72);
                }
                if (t.isObstacle() && !display.isHidden(t.getPosition())) {
                    paintRock(g, px, py);
                }
            }
        }
        for (Population p : Population.values()) {
            paintZoneFrame(g, map.getSafeZone(p));
        }
        g.setColor(new Color(0x5C4A2E));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Rectangle2D.Double(ox, oy, tile * map.getWidth(), tile * map.getHeight()));
    }

    private void paintTuft(Graphics2D g, double x, double y) {
        g.setColor(Palette.GRASS_TUFT);
        g.setStroke(new BasicStroke((float) Math.max(1, tile / 30), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double s = tile * 0.12;
        g.draw(new Line2D.Double(x, y, x - s * 0.5, y - s));
        g.draw(new Line2D.Double(x, y, x, y - s * 1.3));
        g.draw(new Line2D.Double(x, y, x + s * 0.5, y - s));
    }

    private void paintRock(Graphics2D g, double px, double py) {
        double m = tile * 0.12;
        double s = tile - 2 * m;
        Path2D rock = new Path2D.Double();
        rock.moveTo(px + m + s * 0.15, py + tile - m);
        rock.lineTo(px + m, py + m + s * 0.55);
        rock.lineTo(px + m + s * 0.22, py + m + s * 0.15);
        rock.lineTo(px + m + s * 0.6, py + m);
        rock.lineTo(px + tile - m, py + m + s * 0.3);
        rock.lineTo(px + tile - m, py + tile - m - s * 0.12);
        rock.lineTo(px + tile - m - s * 0.15, py + tile - m);
        rock.closePath();
        g.setColor(new Color(0, 0, 0, 50));
        g.fill(new Ellipse2D.Double(px + m, py + tile - m - s * 0.12, s, s * 0.22));
        g.setColor(Palette.ROCK);
        g.fill(rock);
        g.setColor(Palette.ROCK_LIGHT);
        g.fill(new Ellipse2D.Double(px + m + s * 0.3, py + m + s * 0.15, s * 0.3, s * 0.18));
        g.setColor(Palette.ROCK_DARK);
        g.setStroke(new BasicStroke((float) Math.max(1.2, tile / 26)));
        g.draw(rock);
    }

    private void paintZoneFrame(Graphics2D g, SafeZone zone) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Position p : zone.getTiles()) {
            minX = Math.min(minX, p.x());
            minY = Math.min(minY, p.y());
            maxX = Math.max(maxX, p.x());
            maxY = Math.max(maxY, p.y());
        }
        Color team = Palette.team(zone.getOwner());
        RoundRectangle2D frameRect = new RoundRectangle2D.Double(ox + minX * tile + 2, oy + minY * tile + 2,
                (maxX - minX + 1) * tile - 4, (maxY - minY + 1) * tile - 4, 10, 10);
        g.setColor(team.darker());
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {8f, 6f}, 0f));
        g.draw(frameRect);
        double cx = ox + (minX + maxX + 1) * tile / 2;
        double cy = oy + (minY + maxY + 1) * tile / 2;
        outlined(g, zone.getOwner().getLabel().toUpperCase(), cx, cy, (float) (tile * 0.34),
                Palette.alpha(Color.WHITE, 170), Palette.alpha(team.darker(), 130));
    }

    // ---------------------------------------------------------------- beings

    private void paintMaster(Graphics2D g, Master m) {
        DisplayState.Visual v = display.get(m);
        double cx = cxOf(v.x);
        double cy = cyOf(v.y);
        double pad = tile * 0.06;
        RoundRectangle2D base = new RoundRectangle2D.Double(cx - tile / 2 + pad, cy - tile / 2 + pad,
                tile - 2 * pad, tile - 2 * pad, tile * 0.3, tile * 0.3);
        g.setColor(Palette.team(m.getPopulation()));
        g.fill(base);
        g.setColor(Palette.GOLD);
        g.setStroke(new BasicStroke((float) Math.max(2, tile / 14)));
        g.draw(base);
        Sprites.drawMaster(g, m.getPopulation(), cx, cy, tile * 0.95);
        Sprites.drawEnvelope(g, cx + tile * 0.3, cy + tile * 0.33, tile * 0.32, String.valueOf(v.messages));
    }

    private void paintAgent(Graphics2D g, Agent a) {
        DisplayState.Visual v = display.get(a);
        double cx = cxOf(v.x);
        double cy = cyOf(v.y);
        Sprites.drawToken(g, a.getPopulation(), cx, cy - tile * 0.03, tile * 0.92, v.exhausted);

        // id tag
        outlined(g, a.getId(), cx - tile * 0.3, cy - tile * 0.36, (float) (tile * 0.22), Color.WHITE,
                Sprites.c(Palette.team(a.getPopulation()).darker().darker(), v.exhausted));
        if (v.exhausted) {
            return;
        }
        // energy bar
        double bw = tile * 0.66;
        double bh = Math.max(4, tile * 0.09);
        double bx = cx - bw / 2;
        double by = cy + tile * 0.37;
        double ratio = v.maxEp == 0 ? 0 : (double) v.ep / v.maxEp;
        g.setColor(new Color(0x2A2A2A));
        g.fill(new RoundRectangle2D.Double(bx - 1, by - 1, bw + 2, bh + 2, bh, bh));
        g.setColor(Palette.energy(ratio));
        g.fill(new RoundRectangle2D.Double(bx, by, bw * ratio, bh, bh, bh));
        // messages carried
        if (v.messages > 0) {
            Sprites.drawEnvelope(g, cx + tile * 0.3, cy - tile * 0.28, tile * 0.27, String.valueOf(v.messages));
        }
    }

    private void paintActorHighlight(Graphics2D g) {
        Set<Agent> actors = playback.actors();
        if (actors.isEmpty()) {
            return;
        }
        double pulse = 0.5 + 0.5 * Math.sin(frame * 0.25);
        for (Agent a : actors) {
            DisplayState.Visual v = display.get(a);
            if (v == null) {
                continue;
            }
            double r = tile * (0.5 + 0.04 * pulse);
            g.setColor(Palette.alpha(Palette.HIGHLIGHT, (int) (120 + 100 * pulse)));
            g.setStroke(new BasicStroke((float) Math.max(3, tile / 12)));
            g.draw(new Ellipse2D.Double(cxOf(v.x) - r, cyOf(v.y) - r, 2 * r, 2 * r));
        }
    }

    // ---------------------------------------------------------------- actions

    private void paintActionUnder(Graphics2D g, Action action, double t) {
        double fade = Math.min(1, t * 5) * Math.min(1, (1 - t) * 6);
        if (action instanceof Exchange e) {
            beam(g, center(e.from()), center(e.to()), Palette.exchange(e.kind()), fade, false);
        } else if (action instanceof Fight f) {
            beam(g, center(f.attacker()), center(f.defender()), Palette.FIGHT, fade, true);
        } else if (action instanceof Donation d) {
            beam(g, center(d.donor()), center(d.target()), Palette.DONATION, fade * 0.6, false);
        }
    }

    private void paintActionOver(Graphics2D g, Action action, double t) {
        if (action instanceof Exchange e) {
            paintExchange(g, e, t);
        } else if (action instanceof Fight f) {
            paintFight(g, f, t);
        } else if (action instanceof Donation d) {
            paintDonation(g, d, t);
        } else if (action instanceof Exhaustion x) {
            Point2D p = center(x.agent());
            double r = tile * (0.35 + 0.45 * t);
            g.setColor(Palette.alpha(new Color(0x555555), (int) (220 * (1 - t))));
            g.setStroke(new BasicStroke((float) (tile / 10)));
            g.draw(new Ellipse2D.Double(p.getX() - r, p.getY() - r, 2 * r, 2 * r));
            bubble(g, "Exhausted!", p.getX(), p.getY() - tile * 0.8, Palette.FIGHT, Color.WHITE);
        } else if (action instanceof Blocked b) {
            Point2D p = center(b.agent());
            floating(g, "-" + b.epLost() + " EP", p.getX(), p.getY() - tile * 0.5, t, Palette.FIGHT);
        } else if (action instanceof Meteor m) {
            paintMeteor(g, m, t);
        }
    }

    private void paintExchange(Graphics2D g, Exchange e, double t) {
        Point2D a = center(e.from());
        Point2D b = center(e.to());
        Color color = Palette.exchange(e.kind());
        String title = switch (e.kind()) {
            case UNION -> "UNION";
            case SHARE -> "ALLY SHARE";
            case DELIVERY -> "DELIVERY";
        };
        bubble(g, title, (a.getX() + b.getX()) / 2, Math.min(a.getY(), b.getY()) - tile * 0.75, color, Color.WHITE);
        double travel = ease(clamp((t - 0.15) / 0.6));
        if (travel > 0 && travel < 1) {
            if (e.toTarget() > 0) {
                Point2D p = lerp(a, b, travel);
                Sprites.drawEnvelope(g, p.getX(), p.getY() - tile * 0.1, tile * 0.42, String.valueOf(e.toTarget()));
            }
            if (e.toSource() > 0) {
                Point2D p = lerp(b, a, travel);
                Sprites.drawEnvelope(g, p.getX(), p.getY() + tile * 0.1, tile * 0.42, String.valueOf(e.toSource()));
            }
        }
        if (t > 0.72) {
            double k = (t - 0.72) / 0.28;
            if (e.toTarget() > 0) {
                floating(g, "+" + e.toTarget(), b.getX(), b.getY() - tile * 0.5, k, color.darker());
            }
            if (e.toSource() > 0) {
                floating(g, "+" + e.toSource(), a.getX(), a.getY() - tile * 0.5, k, color.darker());
            }
        }
        if (e.toTarget() == 0 && e.toSource() == 0 && t > 0.25) {
            bubble(g, "nothing new", (a.getX() + b.getX()) / 2, (a.getY() + b.getY()) / 2 + tile * 0.6,
                    Color.WHITE, Palette.MUTED);
        }
    }

    private void paintFight(Graphics2D g, Fight f, double t) {
        Point2D a = center(f.attacker());
        Point2D d = center(f.defender());
        Point2D mid = lerp(a, d, 0.5);
        boolean decided = t > 0.45;
        if (t < 0.6) {
            double s = tile * 0.55 * (1 + 0.15 * Math.sin(frame * 0.6));
            Sprites.drawSwords(g, mid.getX(), mid.getY(), s);
        }
        if (t > 0.08) {
            choiceBubble(g, f.attackerChoice().name(), a, decided, f.winner() == f.attacker());
            choiceBubble(g, f.defenderChoice().name(), d, decided, f.winner() == f.defender());
        }
        if (decided) {
            Point2D win = f.winner() == f.attacker() ? a : d;
            Point2D lose = f.winner() == f.attacker() ? d : a;
            double r = tile * 0.55;
            g.setColor(Palette.alpha(Palette.GOLD, 200));
            g.setStroke(new BasicStroke((float) (tile / 9)));
            g.draw(new Ellipse2D.Double(win.getX() - r, win.getY() - r, 2 * r, 2 * r));
            if (f.stolen() > 0) {
                double travel = ease(clamp((t - 0.5) / 0.35));
                if (travel < 1) {
                    Point2D p = lerp(lose, win, travel);
                    Sprites.drawEnvelope(g, p.getX(), p.getY(), tile * 0.42, String.valueOf(f.stolen()));
                }
                if (t > 0.82) {
                    double k = (t - 0.82) / 0.18;
                    floating(g, "+" + f.stolen(), win.getX(), win.getY() - tile * 0.5, k, new Color(0x1E7B34));
                    floating(g, "-" + f.stolen(), lose.getX(), lose.getY() - tile * 0.5, k, Palette.FIGHT);
                }
            } else if (t > 0.55) {
                bubble(g, "nothing to take", mid.getX(), mid.getY() + tile * 0.7, Color.WHITE, Palette.MUTED);
            }
        }
    }

    private void choiceBubble(Graphics2D g, String choice, Point2D p, boolean decided, boolean winner) {
        Color bg = !decided ? Color.WHITE : winner ? Palette.GOLD : new Color(0x9A9A9A);
        Color fg = !decided ? Palette.TEXT : winner ? Color.BLACK : Color.WHITE;
        String text = decided ? (winner ? choice + "  WINS" : choice) : choice;
        bubble(g, text, p.getX(), p.getY() - tile * 0.82, bg, fg);
    }

    private void paintDonation(Graphics2D g, Donation d, double t) {
        Point2D a = center(d.donor());
        Point2D b = center(d.target());
        for (int k = 0; k < 3; k++) {
            double tt = clamp((t - 0.1 - k * 0.12) / 0.5);
            if (tt <= 0 || tt >= 1) {
                continue;
            }
            Point2D p = lerp(a, b, ease(tt));
            double r = tile * 0.12;
            g.setColor(Palette.DONATION);
            g.fill(new Ellipse2D.Double(p.getX() - r, p.getY() - r, 2 * r, 2 * r));
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(p.getX() - r * 0.4, p.getY() - r * 0.4, r * 0.8, r * 0.8));
        }
        bubble(g, "ENERGY GIFT", (a.getX() + b.getX()) / 2, Math.min(a.getY(), b.getY()) - tile * 0.75,
                Palette.DONATION, Color.WHITE);
        if (t > 0.65) {
            double r = tile * 0.55;
            g.setColor(Palette.alpha(Palette.DONATION, 200));
            g.setStroke(new BasicStroke((float) (tile / 10)));
            g.draw(new Ellipse2D.Double(b.getX() - r, b.getY() - r, 2 * r, 2 * r));
            floating(g, "+" + d.amount() + " EP", b.getX(), b.getY() - tile * 0.5, (t - 0.65) / 0.35, Palette.DONATION);
        }
    }

    private void paintMeteor(Graphics2D g, Meteor m, double t) {
        Point2D target = new Point2D.Double(cxOf(m.where().x()), cyOf(m.where().y()));
        Point2D start = new Point2D.Double(target.getX() + tile * 3, target.getY() - tile * 5);
        if (t < 0.55) {
            double k = t / 0.55;
            Point2D p = lerp(start, target, k * k);
            Point2D tail = lerp(start, target, Math.max(0, k * k - 0.25));
            Sprites.drawFireball(g, p.getX(), p.getY(), tail.getX(), tail.getY(), tile * 0.24);
        } else {
            double k = (t - 0.55) / 0.45;
            double r = tile * (0.3 + 0.9 * k);
            g.setColor(Palette.alpha(new Color(0xFF7A1A), (int) (220 * (1 - k))));
            g.fill(new Ellipse2D.Double(target.getX() - r, target.getY() - r, 2 * r, 2 * r));
            double r2 = r * 0.55;
            g.setColor(Palette.alpha(new Color(0xFFE066), (int) (230 * (1 - k))));
            g.fill(new Ellipse2D.Double(target.getX() - r2, target.getY() - r2, 2 * r2, 2 * r2));
            bubble(g, m.hit() != null ? "HIT!" : "Meteorite!", target.getX(), target.getY() - tile * 0.9,
                    Palette.FIGHT, Color.WHITE);
        }
    }

    // ---------------------------------------------------------------- HUD

    private void paintTurnStrip(Graphics2D g, int w) {
        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new RoundRectangle2D.Double(8, 6, w - 16, STRIP_H - 10, 14, 14));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.setColor(new Color(0xE9E4C8));
        g.drawString("TURN ORDER", 20, 24);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.drawString(sim.getStep() == 0 ? "(shuffled each step)" : "step " + sim.getStep(), 20, 40);

        List<Agent> order = sim.getTurnOrder();
        if (order.isEmpty()) {
            g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 13));
            g.drawString("Press Play: every step, the agents act one after another in a new random order.", 130, 33);
            return;
        }
        double avail = w - 140 - 20;
        double icon = Math.min(STRIP_H - 16, avail / order.size());
        Set<Agent> actors = playback.actors();
        Composite normal = g.getComposite();
        for (int i = 0; i < order.size(); i++) {
            Agent a = order.get(i);
            double cx = 140 + i * icon + icon / 2;
            double cy = 6 + (STRIP_H - 10) / 2.0;
            if (actors.contains(a)) {
                g.setColor(Palette.HIGHLIGHT);
                g.fill(new RoundRectangle2D.Double(cx - icon / 2 + 1, cy - icon / 2, icon - 2, icon, 8, 8));
            }
            boolean played = playback.isActive() ? playback.hasPlayed(a) : false;
            if (played) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            }
            DisplayState.Visual v = display.get(a);
            Sprites.drawToken(g, a.getPopulation(), cx, cy - icon * 0.05, icon * 0.86, v != null && v.exhausted);
            outlined(g, a.getId(), cx, cy + icon * 0.38, (float) (icon * 0.24), Color.WHITE, Color.BLACK);
            g.setComposite(normal);
        }
    }

    private void paintCaption(Graphics2D g, int w, int h) {
        String text = playback.caption();
        if (text.isEmpty()) {
            if (winners != null) {
                text = "Game over.";
            } else if (sim.getStep() == 0) {
                text = "Ready. Press Play to watch the game, or Step to play one step at a time.";
            } else {
                text = "Step " + sim.getStep() + " done. Hover a character to see its details.";
            }
        }
        RoundRectangle2D box = new RoundRectangle2D.Double(8, h - CAPTION_H, w - 16, CAPTION_H - 6, 14, 14);
        g.setColor(new Color(0, 0, 0, 110));
        g.fill(box);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(0xFFF5D6));
        g.drawString(text, (float) (w / 2.0 - fm.stringWidth(text) / 2.0), (float) (h - CAPTION_H / 2.0 + 2));
    }

    private void paintTitleScreen(Graphics2D g, int w, int h) {
        g.setColor(new Color(10, 14, 8, 185));
        g.fill(new Rectangle2D.Double(0, STRIP_H, w, h - STRIP_H - CAPTION_H));
        double cx = w / 2.0;
        double top = STRIP_H + (h - STRIP_H - CAPTION_H) * 0.12;
        outlined(g, "MAS Polymorphic Simulation", cx, top + 20, 34f, Palette.GOLD, Color.BLACK);
        outlined(g, "Elves & Humans   vs   Orcs & Goblins", cx, top + 62, 20f, Color.WHITE, Color.BLACK);

        Population[] pops = {Population.ELF, Population.HUMAN, Population.ORC, Population.GOBLIN};
        String[] moves = {"moves like a Queen", "moves like a Bishop", "moves like a Rook", "moves like a Knight"};
        double size = Math.min(110, w / 7.0);
        double gap = size * 1.6;
        double y = top + 90 + size / 2 + 10;
        for (int i = 0; i < 4; i++) {
            double x = cx + (i - 1.5) * gap;
            Sprites.drawToken(g, pops[i], x, y, size, false);
            outlined(g, pops[i].getLabel(), x, y + size * 0.62, 18f, Palette.team(pops[i]).brighter(), Color.BLACK);
            outlined(g, moves[i], x, y + size * 0.62 + 20, 12f, new Color(0xDDDDDD), Color.BLACK);
            outlined(g, i < 2 ? "GOOD" : "BAD", x, y - size * 0.62, 12f, i < 2 ? new Color(0x9BE59B) : new Color(0xFF9A8A),
                    Color.BLACK);
        }
        double ty = y + size * 0.62 + 66;
        outlined(g, "Agents roam the map, share messages with friends and fight enemies (rock-paper-scissors).",
                cx, ty, 15f, Color.WHITE, Color.BLACK);
        outlined(g, "The team whose Master collects the most distinct messages wins.", cx, ty + 24, 15f,
                Color.WHITE, Color.BLACK);
        double pulse = 0.5 + 0.5 * Math.sin(frame * 0.12);
        outlined(g, "▶  Press Play to start", cx, ty + 70, 24f,
                Palette.alpha(Palette.HIGHLIGHT, (int) (150 + 105 * pulse)), Color.BLACK);
    }

    private void paintEndScreen(Graphics2D g, int w, int h) {
        g.setColor(new Color(10, 14, 8, 175));
        g.fill(new Rectangle2D.Double(0, STRIP_H, w, h - STRIP_H - CAPTION_H));
        double cx = w / 2.0;
        double top = STRIP_H + (h - STRIP_H - CAPTION_H) * 0.12;
        if (winners.size() == 1) {
            Master win = winners.get(0);
            Population p = win.getPopulation();
            outlined(g, p.getLabel().toUpperCase() + " WIN!", cx, top + 26, 46f, Palette.team(p).brighter(), Color.BLACK);
            double size = 130;
            Sprites.drawMaster(g, p, cx, top + 70 + size / 2, size * 1.1);
        } else {
            outlined(g, "DRAW!", cx, top + 26, 46f, Palette.GOLD, Color.BLACK);
            double size = 80;
            for (int i = 0; i < winners.size(); i++) {
                Sprites.drawMaster(g, winners.get(i).getPopulation(), cx + (i - (winners.size() - 1) / 2.0) * size * 1.3,
                        top + 70 + size / 2, size);
            }
        }
        double y = top + 240;
        for (Master m : sim.getMasters()) {
            boolean best = winners.contains(m);
            outlined(g, m.getPopulation().getLabel() + "   " + m.getScore() + " / " + sim.getTotalMessages() + " messages",
                    cx, y, best ? 20f : 17f, best ? Palette.GOLD : Color.WHITE, Color.BLACK);
            y += 28;
        }
        outlined(g, "after " + sim.getStep() + " steps  ·  Restart or New random game to play again", cx, y + 20,
                14f, new Color(0xDDDDDD), Color.BLACK);
    }

    // ---------------------------------------------------------------- helpers

    private void beam(Graphics2D g, Point2D a, Point2D b, Color c, double alpha, boolean dashed) {
        float width = (float) Math.max(4, tile / 7);
        g.setColor(Palette.alpha(c, (int) (200 * alpha)));
        if (dashed) {
            g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                    new float[] {width * 1.4f, width * 1.4f}, (float) (frame % 40)));
        } else {
            g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        g.draw(new Line2D.Double(a, b));
    }

    private void bubble(Graphics2D g, String text, double cx, double cy, Color bg, Color fg) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) Math.max(11, tile * 0.27)));
        FontMetrics fm = g.getFontMetrics();
        double tw = fm.stringWidth(text) + 14;
        double th = fm.getHeight() + 4;
        double x = Math.max(4, Math.min(getWidth() - tw - 4, cx - tw / 2));
        RoundRectangle2D box = new RoundRectangle2D.Double(x, cy - th / 2, tw, th, th, th);
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new RoundRectangle2D.Double(x + 2, cy - th / 2 + 2, tw, th, th, th));
        g.setColor(bg);
        g.fill(box);
        g.setColor(new Color(0, 0, 0, 120));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(box);
        g.setColor(fg);
        g.drawString(text, (float) (x + 7), (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    private void floating(Graphics2D g, String text, double cx, double cy, double k, Color color) {
        double y = cy - tile * 0.5 * clamp(k);
        outlined(g, text, cx, y, (float) Math.max(13, tile * 0.36), color, Color.WHITE);
    }

    private static void outlined(Graphics2D g, String text, double cx, double cy, float size, Color fill, Color outline) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, Math.round(size))));
        FontMetrics fm = g.getFontMetrics();
        float x = (float) (cx - fm.stringWidth(text) / 2.0);
        float y = (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0);
        g.setColor(outline);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    g.drawString(text, x + dx, y + dy);
                }
            }
        }
        g.setColor(fill);
        g.drawString(text, x, y);
    }

    private Point2D center(Being b) {
        DisplayState.Visual v = display.get(b);
        if (v == null) {
            Position p = b.getPosition();
            return new Point2D.Double(cxOf(p.x()), cyOf(p.y()));
        }
        return new Point2D.Double(cxOf(v.x), cyOf(v.y));
    }

    private double cxOf(double x) {
        return ox + (x + 0.5) * tile;
    }

    private double cyOf(double y) {
        return oy + (y + 0.5) * tile;
    }

    private static Point2D lerp(Point2D a, Point2D b, double t) {
        return new Point2D.Double(a.getX() + (b.getX() - a.getX()) * t, a.getY() + (b.getY() - a.getY()) * t);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static double ease(double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }

    // ================================================================ tooltips

    @Override
    public String getToolTipText(MouseEvent e) {
        if (sim == null || tile == 0 || playback.isActive()) {
            return null;
        }
        int x = (int) Math.floor((e.getX() - ox) / tile);
        int y = (int) Math.floor((e.getY() - oy) / tile);
        Position p = new Position(x, y);
        Map map = sim.getMap();
        if (!map.inBounds(p)) {
            return null;
        }
        Tile t = map.getTile(p);
        String zone = t.getZoneOwner() == null ? "" : " &middot; " + t.getZoneOwner().getLabel() + " SafeZone";
        Being b = t.getOccupant();
        if (b instanceof Master m) {
            return "<html><b>Master of the " + m.getPopulation().getLabel() + "</b> (Singleton)<br>"
                    + "Score: " + m.getScore() + " / " + sim.getTotalMessages() + " distinct messages<br>"
                    + "Tile " + p + zone + "</html>";
        }
        if (b instanceof Agent a) {
            String dir = a.getLastDirection() == null ? "-" : a.getLastDirection().shortName();
            return "<html><b>" + a.getId() + "</b> &middot; " + a.getPopulation().getLabel() + " ("
                    + a.getAlliance() + ") &middot; moves like a " + a.getMovement().getName() + "<br>"
                    + "State: " + a.getState() + "<br>"
                    + "EP: " + a.getEp() + " / " + a.getMaxEp() + "<br>"
                    + "Messages: " + a.getMessageCount() + preview(a) + "<br>"
                    + "Last direction: " + dir + "<br>Tile " + p + zone + "</html>";
        }
        if (t.isObstacle()) {
            return "<html>Obstacle at " + p + "</html>";
        }
        return "<html>Free tile " + p + zone + "</html>";
    }

    private static String preview(Agent a) {
        if (a.getMessageCount() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" (");
        int n = 0;
        for (String m : a.getMessages()) {
            if (n == 8) {
                sb.append(", ...");
                break;
            }
            sb.append(n == 0 ? "" : ", ").append(m);
            n++;
        }
        return sb.append(")").toString();
    }
}
