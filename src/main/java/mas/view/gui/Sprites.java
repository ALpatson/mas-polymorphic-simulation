package mas.view.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import mas.model.Population;

/**
 * Cartoon characters drawn with Java2D shapes (no image files): an Elf with pointed ears
 * and golden hair, a Human knight in a plumed helmet, an Orc with tusks and an angry brow,
 * a Goblin with huge ears, a pointy nose and yellow eyes. Exhausted characters are drawn
 * in grey with X eyes. Masters wear a crown.
 */
final class Sprites {

    private Sprites() {
    }

    private static final Color SKIN = new Color(0xF6D5B5);
    private static final Color ELF_HAIR = new Color(0xF2C858);
    private static final Color ELF_EYE = new Color(0x2E8B57);
    private static final Color STEEL = new Color(0xB4BEC8);
    private static final Color STEEL_DARK = new Color(0x7D8893);
    private static final Color PLUME = new Color(0xC8303A);
    private static final Color ORC_SKIN = new Color(0x7F9C3C);
    private static final Color ORC_BROW = new Color(0x39461A);
    private static final Color TUSK = new Color(0xFFF7DE);
    private static final Color GOBLIN_SKIN = new Color(0xBACB4C);
    private static final Color GOBLIN_EAR = new Color(0xE59C9C);
    private static final Color GOBLIN_EYE = new Color(0xFFE44A);
    private static final Color DARK = new Color(0x2A2119);

    // ================================================================ tokens

    /** An agent: a disc in the team colour with the character's head on it. */
    static void drawToken(Graphics2D g, Population p, double cx, double cy, double size, boolean exhausted) {
        double r = size * 0.42;
        Ellipse2D disc = new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
        g.setColor(c(Palette.team(p), exhausted));
        g.fill(disc);
        g.setColor(c(Palette.team(p).darker().darker(), exhausted));
        g.setStroke(new BasicStroke((float) Math.max(1.5, size * 0.05)));
        g.draw(disc);
        drawHead(g, p, cx, cy + size * 0.04, size * 0.78, exhausted);
    }

    /** A Master: bigger head with a golden crown. */
    static void drawMaster(Graphics2D g, Population p, double cx, double cy, double size) {
        drawHead(g, p, cx, cy + size * 0.1, size * 0.82, false);
        drawCrown(g, cx, cy - size * 0.25, size * 0.5);
    }

    static void drawHead(Graphics2D g, Population p, double cx, double cy, double s, boolean ex) {
        switch (p) {
            case ELF -> elf(g, cx, cy, s, ex);
            case HUMAN -> human(g, cx, cy, s, ex);
            case ORC -> orc(g, cx, cy, s, ex);
            case GOBLIN -> goblin(g, cx, cy, s, ex);
        }
    }

    // ================================================================ characters

    private static void elf(Graphics2D g, double cx, double cy, double s, boolean ex) {
        // long hair behind the head
        fill(g, new Ellipse2D.Double(cx - 0.33 * s, cy - 0.37 * s, 0.66 * s, 0.8 * s), ELF_HAIR, ex, s);
        // pointed ears
        fill(g, poly(cx - 0.18 * s, cy - 0.03 * s, cx - 0.5 * s, cy - 0.27 * s, cx - 0.2 * s, cy + 0.1 * s), SKIN, ex, s);
        fill(g, poly(cx + 0.18 * s, cy - 0.03 * s, cx + 0.5 * s, cy - 0.27 * s, cx + 0.2 * s, cy + 0.1 * s), SKIN, ex, s);
        // face
        fill(g, new Ellipse2D.Double(cx - 0.21 * s, cy - 0.26 * s, 0.42 * s, 0.5 * s), SKIN, ex, s);
        // fringe
        fill(g, poly(cx - 0.22 * s, cy - 0.08 * s, cx - 0.2 * s, cy - 0.28 * s, cx, cy - 0.35 * s,
                cx + 0.2 * s, cy - 0.28 * s, cx + 0.22 * s, cy - 0.08 * s, cx + 0.1 * s, cy - 0.17 * s,
                cx - 0.01 * s, cy - 0.12 * s, cx - 0.12 * s, cy - 0.18 * s), ELF_HAIR, ex, s);
        // circlet gem
        fill(g, new Ellipse2D.Double(cx - 0.04 * s, cy - 0.27 * s, 0.08 * s, 0.08 * s), new Color(0x3CCB7F), ex, s);
        eyes(g, cx, cy + 0.02 * s, 0.085 * s, 0.075 * s, ELF_EYE, ex, s);
        smile(g, cx, cy + 0.13 * s, 0.1 * s, ex, s);
    }

    private static void human(Graphics2D g, double cx, double cy, double s, boolean ex) {
        // plume
        fill(g, poly(cx - 0.02 * s, cy - 0.3 * s, cx + 0.06 * s, cy - 0.52 * s, cx + 0.3 * s, cy - 0.5 * s,
                cx + 0.13 * s, cy - 0.28 * s), PLUME, ex, s);
        // helmet
        fill(g, new RoundRectangle2D.Double(cx - 0.28 * s, cy - 0.36 * s, 0.56 * s, 0.68 * s, 0.46 * s, 0.46 * s),
                STEEL, ex, s);
        // crest line
        line(g, cx, cy - 0.36 * s, cx, cy - 0.17 * s, STEEL_DARK, ex, s * 0.04);
        // face opening
        fill(g, new RoundRectangle2D.Double(cx - 0.17 * s, cy - 0.13 * s, 0.34 * s, 0.31 * s, 0.12 * s, 0.12 * s),
                SKIN, ex, s);
        eyes(g, cx, cy - 0.03 * s, 0.085 * s, 0.065 * s, new Color(0x4A3424), ex, s);
        // nose guard
        fill(g, new Rectangle2D.Double(cx - 0.03 * s, cy - 0.2 * s, 0.06 * s, 0.24 * s), STEEL, ex, s);
        smile(g, cx, cy + 0.1 * s, 0.07 * s, ex, s);
        // rivets
        for (int side = -1; side <= 1; side += 2) {
            g.setColor(c(STEEL_DARK, ex));
            g.fill(new Ellipse2D.Double(cx + side * 0.22 * s - 0.025 * s, cy + 0.04 * s, 0.05 * s, 0.05 * s));
        }
    }

    private static void orc(Graphics2D g, double cx, double cy, double s, boolean ex) {
        // top knot
        fill(g, new Ellipse2D.Double(cx - 0.09 * s, cy - 0.47 * s, 0.18 * s, 0.17 * s), new Color(0x2A2A2A), ex, s);
        // small ears
        fill(g, poly(cx - 0.27 * s, cy - 0.08 * s, cx - 0.43 * s, cy - 0.17 * s, cx - 0.27 * s, cy + 0.06 * s), ORC_SKIN, ex, s);
        fill(g, poly(cx + 0.27 * s, cy - 0.08 * s, cx + 0.43 * s, cy - 0.17 * s, cx + 0.27 * s, cy + 0.06 * s), ORC_SKIN, ex, s);
        // big head with a wide jaw
        Area head = new Area(new RoundRectangle2D.Double(cx - 0.28 * s, cy - 0.36 * s, 0.56 * s, 0.6 * s, 0.34 * s, 0.4 * s));
        head.add(new Area(new Ellipse2D.Double(cx - 0.32 * s, cy - 0.02 * s, 0.64 * s, 0.36 * s)));
        fill(g, head, ORC_SKIN, ex, s);
        // angry brow
        fill(g, poly(cx - 0.23 * s, cy - 0.19 * s, cx - 0.02 * s, cy - 0.09 * s, cx - 0.02 * s, cy - 0.03 * s,
                cx - 0.23 * s, cy - 0.12 * s), ORC_BROW, ex, s);
        fill(g, poly(cx + 0.23 * s, cy - 0.19 * s, cx + 0.02 * s, cy - 0.09 * s, cx + 0.02 * s, cy - 0.03 * s,
                cx + 0.23 * s, cy - 0.12 * s), ORC_BROW, ex, s);
        eyes(g, cx, cy + 0.01 * s, 0.11 * s, 0.06 * s, new Color(0xD7261E), ex, s);
        // nostrils
        g.setColor(c(ORC_BROW, ex));
        g.fill(new Ellipse2D.Double(cx - 0.06 * s, cy + 0.08 * s, 0.04 * s, 0.035 * s));
        g.fill(new Ellipse2D.Double(cx + 0.02 * s, cy + 0.08 * s, 0.04 * s, 0.035 * s));
        // mouth and tusks
        line(g, cx - 0.16 * s, cy + 0.2 * s, cx + 0.16 * s, cy + 0.2 * s, DARK, ex, s * 0.035);
        fill(g, poly(cx - 0.16 * s, cy + 0.21 * s, cx - 0.12 * s, cy + 0.05 * s, cx - 0.08 * s, cy + 0.21 * s), TUSK, ex, s);
        fill(g, poly(cx + 0.16 * s, cy + 0.21 * s, cx + 0.12 * s, cy + 0.05 * s, cx + 0.08 * s, cy + 0.21 * s), TUSK, ex, s);
    }

    private static void goblin(Graphics2D g, double cx, double cy, double s, boolean ex) {
        // huge ears
        fill(g, poly(cx - 0.14 * s, cy - 0.1 * s, cx - 0.55 * s, cy - 0.34 * s, cx - 0.17 * s, cy + 0.09 * s), GOBLIN_SKIN, ex, s);
        fill(g, poly(cx + 0.14 * s, cy - 0.1 * s, cx + 0.55 * s, cy - 0.34 * s, cx + 0.17 * s, cy + 0.09 * s), GOBLIN_SKIN, ex, s);
        g.setColor(c(GOBLIN_EAR, ex));
        g.fill(poly(cx - 0.18 * s, cy - 0.06 * s, cx - 0.44 * s, cy - 0.26 * s, cx - 0.19 * s, cy + 0.04 * s));
        g.fill(poly(cx + 0.18 * s, cy - 0.06 * s, cx + 0.44 * s, cy - 0.26 * s, cx + 0.19 * s, cy + 0.04 * s));
        // head
        fill(g, new Ellipse2D.Double(cx - 0.21 * s, cy - 0.29 * s, 0.42 * s, 0.52 * s), GOBLIN_SKIN, ex, s);
        // big yellow eyes
        for (int side = -1; side <= 1; side += 2) {
            double ex0 = cx + side * 0.095 * s;
            double ey0 = cy - 0.07 * s;
            fill(g, new Ellipse2D.Double(ex0 - 0.075 * s, ey0 - 0.065 * s, 0.15 * s, 0.13 * s), GOBLIN_EYE, ex, s);
            if (ex) {
                xMark(g, ex0, ey0, 0.045 * s, s);
            } else {
                g.setColor(DARK);
                g.fill(new Ellipse2D.Double(ex0 - 0.028 * s, ey0 - 0.04 * s, 0.056 * s, 0.08 * s));
            }
        }
        // long pointy nose
        fill(g, poly(cx - 0.035 * s, cy - 0.02 * s, cx + 0.03 * s, cy - 0.02 * s, cx + 0.1 * s, cy + 0.13 * s,
                cx - 0.005 * s, cy + 0.09 * s), GOBLIN_SKIN, ex, s);
        // wide grin with teeth
        Arc2D grin = new Arc2D.Double(cx - 0.15 * s, cy + 0.02 * s, 0.3 * s, 0.2 * s, 200, 140, Arc2D.CHORD);
        fill(g, grin, new Color(0x5A1F1F), ex, s);
        g.setColor(c(Color.WHITE, ex));
        for (int i = -1; i <= 1; i += 2) {
            g.fill(poly(cx + i * 0.07 * s - 0.025 * s, cy + 0.155 * s, cx + i * 0.07 * s + 0.025 * s, cy + 0.155 * s,
                    cx + i * 0.07 * s, cy + 0.2 * s));
        }
    }

    // ================================================================ props

    static void drawCrown(Graphics2D g, double cx, double cy, double w) {
        Path2D crown = poly(cx - 0.5 * w, cy + 0.25 * w, cx - 0.5 * w, cy - 0.2 * w, cx - 0.25 * w, cy + 0.02 * w,
                cx, cy - 0.32 * w, cx + 0.25 * w, cy + 0.02 * w, cx + 0.5 * w, cy - 0.2 * w, cx + 0.5 * w, cy + 0.25 * w);
        fill(g, crown, Palette.GOLD, false, w * 2);
        g.setColor(new Color(0xD62828));
        g.fill(new Ellipse2D.Double(cx - 0.07 * w, cy + 0.04 * w, 0.14 * w, 0.14 * w));
    }

    /** A small envelope (a message), optionally with a number on it. */
    static void drawEnvelope(Graphics2D g, double cx, double cy, double w, String label) {
        double h = w * 0.68;
        Rectangle2D body = new Rectangle2D.Double(cx - w / 2, cy - h / 2, w, h);
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Rectangle2D.Double(cx - w / 2 + 2, cy - h / 2 + 2, w, h));
        g.setColor(new Color(0xFFFDF3));
        g.fill(body);
        g.setColor(new Color(0x6B5A3A));
        g.setStroke(new BasicStroke((float) Math.max(1, w * 0.06)));
        g.draw(body);
        Path2D flap = new Path2D.Double();
        flap.moveTo(cx - w / 2, cy - h / 2);
        flap.lineTo(cx, cy + h * 0.1);
        flap.lineTo(cx + w / 2, cy - h / 2);
        g.draw(flap);
        if (label != null) {
            double r = w * 0.32;
            g.setColor(new Color(0xD62828));
            g.fill(new Ellipse2D.Double(cx + w / 2 - r, cy - h / 2 - r, 2 * r, 2 * r));
            text(g, label, cx + w / 2, cy - h / 2, Color.WHITE, (float) (r * 1.3), true);
        }
    }

    /** Two crossed swords (fight). */
    static void drawSwords(Graphics2D g, double cx, double cy, double s) {
        for (int side = -1; side <= 1; side += 2) {
            double x1 = cx - side * 0.4 * s;
            double y1 = cy + 0.4 * s;
            double x2 = cx + side * 0.4 * s;
            double y2 = cy - 0.4 * s;
            g.setStroke(new BasicStroke((float) (s * 0.16), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(DARK);
            g.draw(new Line2D.Double(x1, y1, x2, y2));
            g.setStroke(new BasicStroke((float) (s * 0.09), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0xE8EDF2));
            g.draw(new Line2D.Double(x1 + (x2 - x1) * 0.3, y1 + (y2 - y1) * 0.3, x2, y2));
            g.setColor(new Color(0x8B5A2B));
            g.draw(new Line2D.Double(x1, y1, x1 + (x2 - x1) * 0.25, y1 + (y2 - y1) * 0.25));
            g.setColor(Palette.GOLD);
            double gx = x1 + (x2 - x1) * 0.28;
            double gy = y1 + (y2 - y1) * 0.28;
            g.draw(new Line2D.Double(gx - side * 0.12 * s, gy - 0.12 * s, gx + side * 0.12 * s, gy + 0.12 * s));
        }
    }

    /** A meteorite: a fireball with a tail. */
    static void drawFireball(Graphics2D g, double x, double y, double tailX, double tailY, double r) {
        g.setStroke(new BasicStroke((float) (r * 1.2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 160, 40, 110));
        g.draw(new Line2D.Double(tailX, tailY, x, y));
        g.setColor(new Color(0xFF7A1A));
        g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
        g.setColor(new Color(0xFFE066));
        g.fill(new Ellipse2D.Double(x - r * 0.55, y - r * 0.55, r * 1.1, r * 1.1));
    }

    // ================================================================ helpers

    static void text(Graphics2D g, String text, double cx, double cy, Color color, float size, boolean bold) {
        g.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, Math.max(8, Math.round(size))));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(color);
        g.drawString(text, (float) (cx - fm.stringWidth(text) / 2.0), (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    private static void eyes(Graphics2D g, double cx, double cy, double spacing, double size, Color iris, boolean ex, double s) {
        for (int side = -1; side <= 1; side += 2) {
            double x = cx + side * spacing;
            if (ex) {
                xMark(g, x, cy, size * 0.6, s);
            } else {
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(x - size / 2, cy - size * 0.45, size, size * 0.9));
                g.setColor(iris);
                g.fill(new Ellipse2D.Double(x - size * 0.3, cy - size * 0.3, size * 0.6, size * 0.6));
                g.setColor(DARK);
                g.fill(new Ellipse2D.Double(x - size * 0.13, cy - size * 0.13, size * 0.26, size * 0.26));
            }
        }
    }

    private static void xMark(Graphics2D g, double x, double y, double r, double s) {
        g.setColor(DARK);
        g.setStroke(new BasicStroke((float) Math.max(1.2, s * 0.035), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x - r, y - r, x + r, y + r));
        g.draw(new Line2D.Double(x - r, y + r, x + r, y - r));
    }

    private static void smile(Graphics2D g, double cx, double cy, double w, boolean ex, double s) {
        g.setColor(c(new Color(0x8A3B2E), ex));
        g.setStroke(new BasicStroke((float) Math.max(1, s * 0.03), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (ex) {
            g.draw(new Line2D.Double(cx - w * 0.6, cy + w * 0.3, cx + w * 0.6, cy + w * 0.3));
        } else {
            g.draw(new Arc2D.Double(cx - w, cy - w * 0.6, 2 * w, w * 1.2, 200, 140, Arc2D.OPEN));
        }
    }

    private static void line(Graphics2D g, double x1, double y1, double x2, double y2, Color color, boolean ex, double width) {
        g.setColor(c(color, ex));
        g.setStroke(new BasicStroke((float) Math.max(1, width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void fill(Graphics2D g, Shape shape, Color color, boolean ex, double s) {
        g.setColor(c(color, ex));
        g.fill(shape);
        g.setColor(c(DARK, ex));
        g.setStroke(new BasicStroke((float) Math.max(1, s * 0.03), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shape);
    }

    private static Path2D poly(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        p.closePath();
        return p;
    }

    /** Greyed-out version of a colour for exhausted characters. */
    static Color c(Color color, boolean exhausted) {
        if (!exhausted) {
            return color;
        }
        int l = (int) (0.3 * color.getRed() + 0.59 * color.getGreen() + 0.11 * color.getBlue());
        l = (l + 150) / 2;
        return new Color(l, l, l);
    }
}
