"""Generates the project's UML diagrams as editable draw.io files.

Usage: py -I gen.py members.json out_dir
"""
import json
import zlib
import sys
from pathlib import Path
from xml.sax.saxutils import escape

members = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
OUT = Path(sys.argv[2])
OUT.mkdir(parents=True, exist_ok=True)

FONT = 11
LINE = 13.4
CHAR = 6.0


def h(s):
    """HTML-escape for draw.io html labels."""
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def attr(s):
    return escape(s, {'"': "&quot;", "\n": "&#xa;"})


class Diagram:
    def __init__(self, name):
        self.name = name
        self.cells = []
        self.n = 2

    def nid(self):
        self.n += 1
        return f"c{self.n}"

    def vertex(self, label, x, y, w, hh, style, parent="1", cid=None):
        cid = cid or self.nid()
        self.cells.append(
            f'<mxCell id="{cid}" value="{attr(label)}" style="{style}" vertex="1" parent="{parent}">'
            f'<mxGeometry x="{x:.0f}" y="{y:.0f}" width="{w:.0f}" height="{hh:.0f}" as="geometry"/></mxCell>')
        return cid

    def edge(self, src, tgt, style, label="", points=None, src_pt=None, tgt_pt=None):
        cid = self.nid()
        s = f' source="{src}"' if src else ""
        t = f' target="{tgt}"' if tgt else ""
        geo = '<mxGeometry relative="1" as="geometry">'
        if src_pt:
            geo += f'<mxPoint x="{src_pt[0]:.0f}" y="{src_pt[1]:.0f}" as="sourcePoint"/>'
        if tgt_pt:
            geo += f'<mxPoint x="{tgt_pt[0]:.0f}" y="{tgt_pt[1]:.0f}" as="targetPoint"/>'
        if points:
            geo += '<Array as="points">' + "".join(f'<mxPoint x="{px:.0f}" y="{py:.0f}"/>' for px, py in points) + "</Array>"
        geo += "</mxGeometry>"
        self.cells.append(f'<mxCell id="{cid}" value="{attr(label)}" style="{style}" edge="1" parent="1"{s}{t}>{geo}</mxCell>')
        return cid

    def xml(self):
        return (f'<diagram name="{attr(self.name)}" id="{zlib.crc32(self.name.encode())}">'
                '<mxGraphModel dx="1400" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" '
                'fold="1" page="0" pageScale="1" pageWidth="1169" pageHeight="827" math="0" shadow="0">'
                '<root><mxCell id="0"/><mxCell id="1" parent="0"/>' + "".join(self.cells) + "</root></mxGraphModel></diagram>")


def save(filename, diagrams):
    xml = '<mxfile host="drawio" type="device">' + "".join(d.xml() for d in diagrams) + "</mxfile>"
    (OUT / filename).write_text(xml, encoding="utf-8")


# ======================================================================= class diagram

FILL = {
    "mas": "#F5F5F5", "mas.sim": "#FFF2CC", "mas.model": "#DAE8FC", "mas.map": "#D5E8D4",
    "mas.movement": "#E1D5E7", "mas.view": "#FFE6CC", "mas.view.gui": "#FFE6CC",
}
STROKE = {
    "mas": "#666666", "mas.sim": "#D6B656", "mas.model": "#6C8EBF", "mas.map": "#82B366",
    "mas.movement": "#9673A6", "mas.view": "#D79B00", "mas.view.gui": "#D79B00",
}


def member_line(m, is_interface=False, is_enum=False):
    if isinstance(m, str):
        return h(m)
    t = m["t"]
    if t.startswith("~ ") and is_interface:
        t = "+ " + t[2:]
    if t.startswith("~ ") and is_enum and "(" in t and t.split("(")[0][2:].strip()[0].isupper():
        t = "- " + t[2:]  # enum constructors are private
    s = h(t)
    if m.get("static"):
        s = f"<u>{s}</u>"
    if m.get("abstract"):
        s = f"<i>{s}</i>"
    return s


def class_box(d, name, x, y):
    c = members[name]
    kind = c["kind"]
    stereo = {"interface": "«interface»", "enum": "«enumeration»", "record": "«record»"}.get(kind, "")
    if c["abstract"] and kind == "class":
        stereo = "«abstract»"
    if name.startswith("Master") and name != "Master":
        stereo = "«singleton»"
    if name == "RandomGen":
        stereo = "«singleton»"
    title = f"<i>{name}</i>" if (c["abstract"] or kind == "interface") else name
    header = (f"<font style='font-size:9px'>{stereo}</font><br>" if stereo else "") + f"<b>{title}</b>"
    attrs = [h(l) for l in c["literals"]] + [member_line(a) for a in c["attributes"]]
    meths = [member_line(m, kind == "interface", kind == "enum") for m in c["methods"]]
    if kind == "record":
        meths = [member_line(m) for m in c["methods"]]

    def plain(s):
        import re
        return re.sub(r"<[^>]+>", "", s).replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")

    def width_of(s):
        return sum(7.4 if ch.isupper() else (3.2 if ch in "il.,:;()[]| " else CHAR) for ch in s)

    longest = max([width_of(plain(s)) for s in attrs + meths] + [width_of(name) + 24, 90])
    w = max(120, longest + 20)
    head_h = 40 if stereo else 26
    ah = max(1, len(attrs)) * LINE + 6 if attrs else 8
    mh = max(1, len(meths)) * LINE + 6 if meths else 8
    total = head_h + ah + 8 + mh
    fill, stroke = FILL[c["package"]], STROKE[c["package"]]
    box = d.vertex(header, x, y, w, total,
                   f"swimlane;fontStyle=0;align=center;verticalAlign=top;childLayout=stackLayout;horizontal=1;"
                   f"startSize={head_h};horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;collapsible=0;"
                   f"marginBottom=0;html=1;fontSize={FONT};fillColor={fill};strokeColor={stroke};swimlaneFillColor=#FFFFFF;")
    text_style = (f"text;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=6;spacingRight=4;"
                  f"overflow=hidden;rotatable=0;points=[[0,0.5],[1,0.5]];portConstraint=eastwest;html=1;fontSize={FONT};"
                  f"spacingTop=-2;")
    d.vertex("<br>".join(attrs), 0, head_h, w, ah, text_style, parent=box)
    d.vertex("", 0, head_h + ah, w, 8,
             f"line;strokeWidth=1;fillColor=none;align=left;verticalAlign=middle;spacingTop=-1;spacingLeft=3;"
             f"spacingRight=3;rotatable=0;labelPosition=right;points=[];portConstraint=eastwest;strokeColor={stroke};",
             parent=box)
    d.vertex("<br>".join(meths), 0, head_h + ah + 8, w, mh, text_style, parent=box)
    return box, w, total


INHERIT = "endArrow=block;endSize=14;endFill=0;html=1;edgeStyle=orthogonalEdgeStyle;rounded=0;strokeColor=#333333;"
REALIZE = INHERIT + "dashed=1;"
ASSOC = "endArrow=open;endSize=10;html=1;edgeStyle=orthogonalEdgeStyle;rounded=0;strokeColor=#555555;fontSize=10;labelBackgroundColor=#FFFFFF;"
COMPOSE = ASSOC + "startArrow=diamondThin;startFill=1;startSize=14;"
AGGREG = ASSOC + "startArrow=diamondThin;startFill=0;startSize=14;"
DEPEND = "endArrow=open;endSize=10;dashed=1;html=1;edgeStyle=orthogonalEdgeStyle;rounded=0;strokeColor=#888888;fontSize=10;labelBackgroundColor=#FFFFFF;"


def layout_columns(d, columns, x0=20, y0=60, gap_x=46, gap_y=34):
    ids, geo = {}, {}
    x = x0
    for col in columns:
        y = y0
        col_w = 0
        for name in col:
            box, w, hh = class_box(d, name, x, y)
            ids[name] = box
            geo[name] = (x, y, w, hh)
            y += hh + gap_y
            col_w = max(col_w, w)
        x += col_w + gap_x
    return ids, geo


def title(d, text, sub):
    d.vertex(f"<b style='font-size:20px'>{h(text)}</b><br><span style='font-size:12px;color:#555555'>{h(sub)}</span>",
             20, 0, 1400, 50, "text;html=1;align=left;verticalAlign=top;fontSize=12;")


def legend(d, x, y):
    rows = [("mas (entry, config)", "mas"), ("mas.sim (engine)", "mas.sim"), ("mas.model (beings)", "mas.model"),
            ("mas.map (environment)", "mas.map"), ("mas.movement (Strategy)", "mas.movement"),
            ("mas.view (display)", "mas.view")]
    d.vertex("<b>Packages</b>", x, y, 200, 20, "text;html=1;align=left;fontSize=11;")
    for i, (label, pkg) in enumerate(rows):
        d.vertex(label, x, y + 24 + i * 24, 200, 20,
                 f"rounded=0;html=1;align=left;spacingLeft=6;fontSize=10;fillColor={FILL[pkg]};strokeColor={STROKE[pkg]};")
    d.vertex("+ public&nbsp;&nbsp;# protected&nbsp;&nbsp;- private&nbsp;&nbsp;~ package<br><u>underlined</u> = static&nbsp;&nbsp;<i>italic</i> = abstract",
             x, y + 24 + len(rows) * 24 + 4, 260, 36, "text;html=1;align=left;fontSize=10;")


core = Diagram("Class diagram - simulation core")
title(core, "MAS Polymorphic Simulation - UML class diagram (simulation core)",
      "Every attribute and method with its visibility. Generated from the Java sources (package mas.*).")


class Placer:
    """Places class boxes at explicit positions, measuring them first."""

    def __init__(self, d):
        self.d, self.ids, self.geo = d, {}, {}

    def size(self, name):
        scratch = Diagram("scratch")
        _, w, hh = class_box(scratch, name, 0, 0)
        return w, hh

    def put(self, name, x, y):
        box, w, hh = class_box(self.d, name, x, y)
        self.ids[name], self.geo[name] = box, (x, y, w, hh)
        return x, y, w, hh

    def stack(self, names, x, y, gap=30):
        right = x
        for n in names:
            _, _, w, hh = self.put(n, x, y)
            y += hh + gap
            right = max(right, x + w)
        return right, y

    def row(self, names, x, y, gap=24):
        bottom = y
        for n in names:
            _, _, w, hh = self.put(n, x, y)
            x += w + gap
            bottom = max(bottom, y + hh)
        return x, bottom


P = Placer(core)
G = 40
Y0 = 150
# ---------------- band 1: engine + beings
r1, b1 = P.stack(["Main", "Simulation", "EventManager", "Statistics"], 20, Y0)
r2, b2 = P.stack(["RandomGen", "EventLog", "SelfTest"], r1 + G, Y0)
xm = r2 + G * 1.5
P.put("Being", xm, Y0)
agent_y = Y0 + P.geo["Being"][3] + G * 1.5
P.put("Agent", xm, agent_y)
xs = xm + P.geo["Agent"][2] + G * 1.5
P.put("GoodAgent", xs, agent_y)
y = agent_y + P.geo["GoodAgent"][3] + G
_, y = P.row(["Elf", "Human"], xs, y)
P.put("BadAgent", xs, y + G)
y = y + G + P.geo["BadAgent"][3] + G
_, y = P.row(["Orc", "Goblin"], xs, y)
sub_right = max(xs + P.geo["GoodAgent"][2], P.geo["Human"][0] + P.geo["Human"][2],
                P.geo["Goblin"][0] + P.geo["Goblin"][2], xs + P.geo["BadAgent"][2])
xmas = sub_right + G * 1.5
P.put("Master", xmas, Y0)
my = Y0 + P.geo["Master"][3] + G * 1.2
_, my = P.row(["MasterElf", "MasterHuman"], xmas, my)
_, my = P.row(["MasterOrc", "MasterGoblin"], xmas, my + 24)
band1_bottom = max(b1, b2, y, agent_y + P.geo["Agent"][3], my)

# ---------------- band 2: view, movement, environment, enums
Y2 = band1_bottom + G * 2
rv, bv = P.stack(["SimulationView", "ConsoleRenderer"], 20, Y2)
P.put("InstanceCounters", rv + G, Y2)
legend(core, rv + G, Y2 + P.geo["InstanceCounters"][3] + 30)
xmv = rv + G + 290
P.put("MovementStrategy", xmv, Y2)
lm_y = Y2 + P.geo["MovementStrategy"][3] + G
P.put("LineMove", xmv, lm_y)
P.put("KnightMove", xmv + P.geo["LineMove"][2] + G, lm_y)
rmv, bmv = P.row(["QueenMove", "BishopMove", "RookMove"], xmv, lm_y + max(P.geo["LineMove"][3], P.geo["KnightMove"][3]) + G * 1.3)
mv_right = max(rmv, P.geo["KnightMove"][0] + P.geo["KnightMove"][2])
xr = mv_right + G * 1.5
r5, _ = P.stack(["Map", "Tile", "SafeZone"], xr, Y2)
r6, _ = P.stack(["Position", "Direction", "Population"], r5 + G, Y2)
r7, b7 = P.stack(["Config", "AgentState"], r6 + G, Y2)
P.stack(["RpsChoice", "Alliance"], r6 + G + P.geo["AgentState"][2] + 24, P.geo["AgentState"][1])
ids, geo = P.ids, P.geo

E = core.edge


def side(ex, ey, nx, ny):
    return f"exitX={ex};exitY={ey};exitDx=0;exitDy=0;entryX={nx};entryY={ny};entryDx=0;entryDy=0;"


def gx(name, fx):
    return geo[name][0] + geo[name][2] * fx


def gy(name, fy):
    return geo[name][1] + geo[name][3] * fy


UP = side(0.5, 0, 0.5, 1)
DOWN = side(0.5, 1, 0.5, 0)
# inheritance and realization
E(ids["Agent"], ids["Being"], INHERIT + side(0.25, 0, 0.25, 1))
E(ids["Master"], ids["Being"], INHERIT + side(0, 0.15, 1, 0.15))
E(ids["GoodAgent"], ids["Agent"], INHERIT + side(0, 0.5, 1, 0.06))
E(ids["BadAgent"], ids["Agent"], INHERIT + side(0, 0.5, 1, 0.5))
for c in ["Elf", "Human"]:
    E(ids[c], ids["GoodAgent"], INHERIT + UP)
for c in ["Orc", "Goblin"]:
    E(ids[c], ids["BadAgent"], INHERIT + UP)
for c in ["MasterElf", "MasterHuman"]:
    E(ids[c], ids["Master"], INHERIT + UP)
E(ids["MasterOrc"], ids["Master"], INHERIT + side(0, 0.5, 0, 0.85),
  points=[(gx("Master", 0) - 18, gy("MasterOrc", 0.5)), (gx("Master", 0) - 18, gy("Master", 0.85))])
E(ids["MasterGoblin"], ids["Master"], INHERIT + side(1, 0.5, 1, 0.85),
  points=[(gx("MasterGoblin", 1) + 18, gy("MasterGoblin", 0.5)), (gx("MasterGoblin", 1) + 18, gy("Master", 0.85))])
for c in ["QueenMove", "BishopMove", "RookMove"]:
    E(ids[c], ids["LineMove"], INHERIT + UP)
E(ids["LineMove"], ids["MovementStrategy"], REALIZE + side(0.5, 0, 0.3, 1))
E(ids["KnightMove"], ids["MovementStrategy"], REALIZE + side(0.5, 0, 0.8, 1))
E(ids["ConsoleRenderer"], ids["SimulationView"], REALIZE + UP)
# associations and dependencies
E(ids["Main"], ids["Simulation"], DEPEND + DOWN, "creates")
E(ids["Simulation"], ids["EventManager"], COMPOSE + DOWN, "events 1")
E(ids["Statistics"], ids["Simulation"], DEPEND + side(1, 0.5, 0.95, 1), "replays N games",
  points=[(gx("Simulation", 0.95), gy("Statistics", 0.5))])
E(ids["Simulation"], ids["RandomGen"], ASSOC + side(1, 0.06, 0, 0.5), "rng 1")
E(ids["Simulation"], ids["EventLog"], COMPOSE + side(1, 0.5, 0, 0.3), "log 1")
lane_x = gx("EventLog", 1) + 25
E(ids["Simulation"], ids["Agent"], AGGREG + side(1, 0.3, 0, 0.35), "agents 16",
  points=[(lane_x, gy("Simulation", 0.3)), (lane_x, gy("Agent", 0.35))])
E(ids["Simulation"], ids["Master"], AGGREG + side(0.6, 0, 0.5, 0), "masters 4",
  points=[(gx("Simulation", 0.6), Y0 - 35), (gx("Master", 0.5), Y0 - 35)])
E(ids["Simulation"], ids["SimulationView"], DEPEND + side(0.2, 1, 0.3, 0), "renders on",
  points=[(gx("Simulation", 0.2), Y2 - 40), (gx("SimulationView", 0.3), Y2 - 40)])
E(ids["Simulation"], ids["Map"], COMPOSE + side(0.8, 1, 0.3, 0), "map 1",
  points=[(gx("Simulation", 0.8), Y2 - 55), (gx("Map", 0.3), Y2 - 55)])
E(ids["Agent"], ids["Master"], ASSOC + side(1, 0.97, 0, 0.6), "master 1",
  points=[(xmas - 45, gy("Agent", 0.97)), (xmas - 45, gy("Master", 0.6))])
E(ids["Agent"], ids["MovementStrategy"], COMPOSE + side(0.5, 1, 0.5, 0), "movement 1",
  points=[(gx("Agent", 0.5), Y2 - 25), (gx("MovementStrategy", 0.5), Y2 - 25)])
E(ids["Map"], ids["Tile"], COMPOSE + DOWN, "tiles n*m")
E(ids["Map"], ids["SafeZone"], COMPOSE + side(1, 0.9, 1, 0.1), "safeZones 4",
  points=[(gx("Map", 1) + 20, gy("Map", 0.9)), (gx("Map", 1) + 20, gy("SafeZone", 0.1))])
# Master.safeZone and Tile.occupant are shown as typed attributes (no long connector).
save_list = [core]

# ---- page 2: graphical view
gui = Diagram("Class diagram - graphical view")
title(gui, "MAS Polymorphic Simulation - UML class diagram (graphical view, package mas.view.gui)",
      "Swing window that replays each simulation step with animations. It only reads the model.")
gcols = [["SimulationWindow", "AgentTableModel"], ["MapPanel"], ["Playback", "DisplayState", "ScorePanel"],
         ["Sprites", "Palette"]]
gids, ggeo = layout_columns(gui, gcols, y0=110)
def ggx(n, f):
    return ggeo[n][0] + ggeo[n][2] * f


def ggy(n, f):
    return ggeo[n][1] + ggeo[n][3] * f


mp_bottom = ggy("MapPanel", 1)
gui.edge(gids["SimulationWindow"], gids["MapPanel"], COMPOSE + "exitX=1;exitY=0.4;entryX=0;entryY=0.4;", "mapPanel 1")
gui.edge(gids["SimulationWindow"], gids["Playback"], COMPOSE + "exitX=0.5;exitY=0;entryX=0.3;entryY=0;", "playback 1",
         points=[(ggx("SimulationWindow", 0.5), 85), (ggx("Playback", 0.3), 85)])
gui.edge(gids["SimulationWindow"], gids["ScorePanel"], COMPOSE + "exitX=1;exitY=0.97;entryX=0;entryY=0.6;", "scorePanel 1",
         points=[(ggx("SimulationWindow", 1) + 20, ggy("SimulationWindow", 0.97)), (ggx("SimulationWindow", 1) + 20, mp_bottom + 30),
                 (ggx("ScorePanel", 0) - 25, mp_bottom + 30), (ggx("ScorePanel", 0) - 25, ggy("ScorePanel", 0.6))])
gui.edge(gids["SimulationWindow"], gids["AgentTableModel"], COMPOSE, "tableModel 1")
gui.edge(gids["Playback"], gids["DisplayState"], ASSOC, "display 1")
gui.edge(gids["MapPanel"], gids["Sprites"], DEPEND + "exitX=0.9;exitY=0;entryX=0.5;entryY=0;", "draws with",
         points=[(ggx("MapPanel", 0.9), 70), (ggx("Sprites", 0.5), 70)])
save_list.append(gui)
save("class-diagram.drawio", save_list)


# ======================================================================= state machine

sm = Diagram("Agent state machine")
STATE = "rounded=1;whiteSpace=wrap;html=1;arcSize=30;fontSize=13;fontStyle=1;fillColor=#DAE8FC;strokeColor=#6C8EBF;"
T = "endArrow=open;endSize=10;html=1;rounded=1;fontSize=11;labelBackgroundColor=#FFFFFF;strokeColor=#333333;"
sm.vertex("<b style='font-size:18px'>State-machine diagram: mobile agent (AgentState)</b>", 20, 0, 900, 30,
          "text;html=1;align=left;")
init = sm.vertex("", 455, 50, 30, 30, "ellipse;html=1;fillColor=#000000;strokeColor=#000000;")
home = sm.vertex("AT_HOME<hr style='margin:2px'><span style='font-size:11px;font-weight:normal'>do / recoverEp()<br>entry / deliverToMaster()</span>",
                 380, 120, 180, 80, STATE)
expl = sm.vertex("EXPLORING<hr style='margin:2px'><span style='font-size:11px;font-weight:normal'>do / random path<br>(MovementStrategy)</span>",
                 120, 300, 180, 80, STATE)
ret = sm.vertex("RETURNING<hr style='margin:2px'><span style='font-size:11px;font-weight:normal'>do / BFS path home<br>(max 3 tiles per step)</span>",
                640, 300, 180, 80, STATE)
enc = sm.vertex("ENCOUNTER<hr style='margin:2px'><span style='font-size:11px;font-weight:normal'>union / ally share /<br>rock-paper-scissors</span>",
                380, 480, 180, 80, STATE.replace("#DAE8FC", "#FFF2CC").replace("#6C8EBF", "#D6B656"))
exh = sm.vertex("EXHAUSTED<hr style='margin:2px'><span style='font-size:11px;font-weight:normal'>acts as an obstacle<br>messages lost</span>",
                380, 680, 180, 80, STATE.replace("#DAE8FC", "#F8CECC").replace("#6C8EBF", "#B85450"))
fin = sm.vertex("", 455, 830, 30, 30, "ellipse;html=1;shape=endState;fillColor=#000000;strokeColor=#000000;")

sm.edge(init, home, T, "created in its SafeZone<br>EP = maxEp")
sm.edge(home, home, T + "exitX=1;exitY=0.2;entryX=0.8;entryY=0;", "[EP &lt; 90%]<br>/ recoverEp()",
        points=[(600, 136), (600, 92), (524, 92)])
sm.edge(home, expl, T + "exitX=0;exitY=0.5;entryX=0.5;entryY=0;", "[EP &gt;= 90%]<br>leave home", points=[(210, 160)])
sm.edge(expl, expl, T + "exitX=0;exitY=0.25;entryX=0.15;entryY=0;",
        "move / spendEp(2 per tile)<br>enters own SafeZone<br>/ deliverToMaster()",
        points=[(60, 320), (60, 262), (147, 262)])
sm.edge(expl, ret, T + "exitX=1;exitY=0.35;entryX=0;entryY=0.35;", "[EP &lt; 20%]")
sm.edge(ret, home, T + "exitX=0.5;exitY=0;entryX=1;entryY=0.75;", "enters own SafeZone<br>/ deliverToMaster()",
        points=[(730, 180)])
sm.edge(expl, enc, T + "exitX=0.75;exitY=1;entryX=0;entryY=0.3;", "agent on the path", points=[(255, 504)])
sm.edge(enc, expl, T + "exitX=0;exitY=0.7;entryX=0.25;entryY=1;", "resolved<br>[EP &gt;= 20%]", points=[(165, 536)])
sm.edge(ret, enc, T + "exitX=0.25;exitY=1;entryX=1;entryY=0.3;", "agent on the path", points=[(685, 504)])
sm.edge(enc, ret, T + "exitX=1;exitY=0.7;entryX=0.75;entryY=1;", "resolved [EP &lt; 20%<br>or was returning]",
        points=[(775, 536)])
sm.edge(expl, exh, T + "exitX=0;exitY=0.85;entryX=0;entryY=0.5;", "[EP = 0] or meteorite<br>/ exhaust()",
        points=[(80, 368), (80, 720)])
sm.edge(ret, exh, T + "exitX=1;exitY=0.85;entryX=1;entryY=0.3;", "[EP = 0] or meteorite<br>/ exhaust()",
        points=[(860, 368), (860, 704)])
sm.edge(exh, ret, T + "exitX=1;exitY=0.7;entryX=1;entryY=0.4;dashed=1;",
        "ally bumps into it<br>/ receiveEp(30% of max)", points=[(1010, 736), (1010, 332)])
sm.edge(exh, fin, T, "end of simulation")
sm.vertex("<b>Notes</b><br>ENCOUNTER is transient: it lasts during<br>one meeting, then the agent goes back<br>"
          "to the state it came from.<br>Thresholds come from Config:<br>LOW_EP_PERCENT = 20<br>LEAVE_HOME_PERCENT = 90<br>"
          "DONATION_PERCENT = 30",
          1080, 120, 250, 160, "shape=note;whiteSpace=wrap;html=1;align=left;spacingLeft=8;fontSize=11;"
          "fillColor=#F5F5F5;strokeColor=#999999;size=14;")
save("state-machine-diagram.drawio", [sm])


# ======================================================================= activity diagram

ad = Diagram("Activity diagram - main loop")
ACT = "rounded=1;whiteSpace=wrap;html=1;arcSize=40;fontSize=12;fillColor=#DAE8FC;strokeColor=#6C8EBF;"
ACT2 = ACT.replace("#DAE8FC", "#D5E8D4").replace("#6C8EBF", "#82B366")
DEC = "rhombus;whiteSpace=wrap;html=1;fontSize=11;fillColor=#FFF2CC;strokeColor=#D6B656;"
A = "endArrow=open;endSize=9;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;fontSize=11;labelBackgroundColor=#FFFFFF;strokeColor=#333333;"
ad.vertex("<b style='font-size:18px'>Activity diagram: time-step loop and agent loop</b>", 20, 0, 900, 30,
          "text;html=1;align=left;")
# swimlane-like frames
ad.vertex("<b>Outer loop: one iteration per time step</b>", 20, 40, 1080, 1520,
          "rounded=0;html=1;dashed=1;fillColor=none;strokeColor=#999999;align=left;verticalAlign=top;spacingLeft=8;fontSize=12;")
ad.vertex("<b>Inner loop: agents in shuffled order</b>", 250, 330, 830, 1020,
          "rounded=0;html=1;dashed=1;fillColor=#FAFAFA;strokeColor=#6C8EBF;align=left;verticalAlign=top;spacingLeft=8;fontSize=12;")
ad.vertex("<b>Agent.move(map, log)</b> (polymorphic)", 500, 450, 560, 820,
          "rounded=0;html=1;fillColor=#FFFFFF;strokeColor=#82B366;align=left;verticalAlign=top;spacingLeft=8;fontSize=12;")

cx = 140
start = ad.vertex("", cx - 15, 70, 30, 30, "ellipse;html=1;fillColor=#000000;")
init_a = ad.vertex("Initialise: map, SafeZones,<br>obstacles, 4 Masters (Singletons),<br>16 agents, 48 unique messages", 30, 120, 220, 70, ACT)
fin_d = ad.vertex("Master holds all<br>messages, step = max<br>or nobody can move?", 60, 220, 160, 90, DEC)
winner = ad.vertex("computeWinners():<br>highest score wins,<br>equal = draw", 40, 1380, 200, 60, ACT)
result = ad.vertex("Display the result", 40, 1460, 200, 36, ACT)
endn = ad.vertex("", cx - 15, 1515, 30, 30, "ellipse;html=1;shape=endState;fillColor=#000000;")

stepinc = ad.vertex("step++<br>log.newStep()", 270, 240, 160, 50, ACT)
shuffle = ad.vertex("Shuffle the active agents<br>(RandomGen.shuffle)", 270, 370, 200, 50, ACT)
more = ad.vertex("More agents<br>this step?", 290, 450, 160, 80, DEC)
skip = ad.vertex("Exhausted earlier<br>this step?", 290, 580, 160, 80, DEC)

home_d = ad.vertex("In own SafeZone<br>and EP &lt; 90%?", 520, 490, 170, 80, DEC)
rest = ad.vertex("Deliver to Master,<br>recover EP, stay", 760, 500, 170, 56, ACT2)
low_d = ad.vertex("EP &lt; 20% or<br>returning?", 520, 610, 170, 80, DEC)
bfs = ad.vertex("Plan BFS path home<br>(Map.shortestPathHome)", 760, 620, 190, 56, ACT2)
rnd = ad.vertex("Plan random path<br>(movement.planPath)", 515, 720, 180, 50, ACT2)
tile_d = ad.vertex("Next tile<br>of the path?", 520, 800, 170, 80, DEC)
block_d = ad.vertex("What is on<br>the tile?", 520, 920, 170, 80, DEC)
stop = ad.vertex("Stop in front,<br>lose EP of remaining tiles", 760, 1030, 210, 56, ACT2)
meet = ad.vertex("Encounter: union (same people),<br>share 1-3 (ally), RPS fight (enemy);<br>exhausted ally: donate EP", 760, 925, 280, 70, ACT2)
step_t = ad.vertex("Step onto the tile,<br>spend EP outside SafeZone,<br>deliver if in SafeZone", 515, 1100, 180, 66, ACT2)
dead_d = ad.vertex("EP = 0?", 545, 1195, 120, 60, DEC)
exhaust = ad.vertex("Become exhausted:<br>obstacle, messages lost", 760, 1197, 210, 56, ACT2)
merge = ad.vertex("", 380, 1300, 20, 20, "rhombus;html=1;fillColor=#FFF2CC;strokeColor=#D6B656;")
meteor = ad.vertex("Random event: meteorite<br>(EventManager.maybeTrigger)", 30, 1060, 210, 56, ACT)
render = ad.vertex("Render the step:<br>map, scores, counters, events", 30, 1160, 210, 56, ACT)

ad.edge(start, init_a, A)
ad.edge(init_a, fin_d, A)
ad.edge(fin_d, stepinc, A + "exitX=1;exitY=0.5;", "[no]")
ad.edge(fin_d, winner, A + "exitX=0;exitY=0.5;entryX=0;entryY=0.5;", "[yes]", points=[(35, 265), (35, 1410)])
ad.edge(winner, result, A)
ad.edge(result, endn, A)
ad.edge(stepinc, shuffle, A)
ad.edge(shuffle, more, A)
ad.edge(more, skip, A, "[yes] next agent")
ad.edge(more, meteor, A + "exitX=0;exitY=0.5;entryX=0.5;entryY=0;", "[no]", points=[(135, 490)])
ad.edge(meteor, render, A)
ad.edge(render, fin_d, A + "exitX=0;exitY=0.5;entryX=0;entryY=0.75;", "next step", points=[(25, 1188), (25, 287)])
ad.edge(skip, more, A + "exitX=0;exitY=0.5;entryX=0;entryY=0.75;", "[yes]", points=[(270, 620), (270, 510)])
ad.edge(skip, home_d, A + "exitX=1;exitY=0.5;entryX=0;entryY=0.5;", "[no]", points=[(490, 620), (490, 530)])
ad.edge(home_d, rest, A, "[yes]")
ad.edge(home_d, low_d, A, "[no]")
ad.edge(low_d, bfs, A, "[yes]")
ad.edge(low_d, rnd, A, "[no]")
ad.edge(bfs, tile_d, A + "exitX=0.5;exitY=1;entryX=1;entryY=0.5;", points=[(855, 760), (720, 760), (720, 840)])
ad.edge(rnd, tile_d, A)
ad.edge(tile_d, block_d, A, "[yes]")
ad.edge(tile_d, merge, A + "exitX=0;exitY=0.5;entryX=0.5;entryY=0;", "[no: path done]", points=[(390, 840)])
ad.edge(block_d, stop, A + "exitX=0.75;exitY=0.75;entryX=0;entryY=0.5;", "obstacle, edge,<br>enemy SafeZone", points=[(690, 1058)])
ad.edge(block_d, meet, A + "exitX=1;exitY=0.5;entryX=0;entryY=0.5;", "another agent")
ad.edge(block_d, step_t, A, "free")
ad.edge(step_t, dead_d, A)
ad.edge(dead_d, tile_d, A + "exitX=0;exitY=0.5;entryX=0;entryY=0.5;", "[no]", points=[(505, 1225), (505, 840)])
ad.edge(dead_d, exhaust, A, "[yes]")
ad.edge(stop, merge, A + "exitX=1;exitY=0.5;entryX=1;entryY=0.5;", points=[(1050, 1058), (1050, 1310)])
ad.edge(meet, merge, A + "exitX=1;exitY=0.5;entryX=1;entryY=0.5;", points=[(1060, 960), (1060, 1310)])
ad.edge(exhaust, merge, A + "exitX=0.5;exitY=1;entryX=1;entryY=0.5;", points=[(865, 1310)])
ad.edge(rest, merge, A + "exitX=1;exitY=0.5;entryX=1;entryY=0.5;", points=[(1070, 528), (1070, 1310)])
ad.edge(merge, more, A + "exitX=0;exitY=0.5;entryX=0.25;entryY=1;", "action applied<br>immediately", points=[(330, 1310)])
save("activity-diagram.drawio", [ad])


# ======================================================================= sequence diagram

sq = Diagram("Sequence diagram - one time step")
sq.vertex("<b style='font-size:18px'>Sequence diagram: one time step (Simulation.nextStep)</b>", 20, 0, 1000, 30,
          "text;html=1;align=left;")
LIFE = ("shape=umlLifeline;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=0;collapsible=0;"
        "recursiveResize=0;outlineConnect=0;portConstraint=eastwest;size=46;fontSize=12;fillColor=#DAE8FC;strokeColor=#6C8EBF;")
names = [(":SimulationView", "#FFE6CC"), (":Simulation", "#FFF2CC"), (":RandomGen", "#FFF2CC"),
         ("a : Agent", "#DAE8FC"), (":MovementStrategy", "#E1D5E7"), (":Map", "#D5E8D4"),
         ("b : Agent", "#DAE8FC"), (":Master", "#DAE8FC"), (":EventManager", "#FFF2CC")]
X = {}
for i, (n, color) in enumerate(names):
    x = 20 + i * 150
    X[n] = x + 65
    sq.vertex(n, x, 50, 130, 1150, LIFE.replace("#DAE8FC", color))

MSG = "html=1;verticalAlign=bottom;endArrow=block;endSize=8;fontSize=11;labelBackgroundColor=none;"
RET = "html=1;verticalAlign=bottom;endArrow=open;dashed=1;endSize=8;fontSize=11;labelBackgroundColor=none;strokeColor=#666666;"
ACTB = "html=1;points=[];perimeter=orthogonalPerimeter;fillColor=#FFFFFF;strokeColor=#333333;"


def msg(a, b, y, label, ret=False):
    sq.edge(None, None, RET if ret else MSG, label, src_pt=(X[a] + (5 if X[b] > X[a] else -5), y),
            tgt_pt=(X[b] + (-5 if X[b] > X[a] else 5), y))


def self_msg(a, y, label):
    sq.edge(None, None, MSG + "align=left;spacingLeft=4;", label, src_pt=(X[a] + 5, y),
            tgt_pt=(X[a] + 5, y + 22), points=[(X[a] + 45, y), (X[a] + 45, y + 22)])


def frame(x, y, w, hh, kind, guard):
    sq.vertex(f"<b>{kind}</b>", x, y, w, hh,
              f"shape=umlFrame;whiteSpace=wrap;html=1;width=60;height=24;fontSize=11;fillColor=none;strokeColor=#555555;"
              f"labelBackgroundColor=none;")
    sq.vertex(f"[{guard}]", x + 64, y + 2, 400, 20, "text;html=1;align=left;fontSize=11;fontStyle=2;")


sq.vertex("", X[":Simulation"] - 5, 110, 10, 1060, ACTB)
self_msg(":Simulation", 110, "log.newStep()")
msg(":Simulation", ":RandomGen", 170, "shuffle(activeAgents)")
msg(":RandomGen", ":Simulation", 195, "", True)
frame(155, 220, 1190, 760, "loop", "for each agent in the shuffled order, if not exhausted")
msg(":Simulation", "a : Agent", 270, "move(map, log)")
sq.vertex("", X["a : Agent"] - 5, 270, 10, 680, ACTB)
frame(440, 295, 900, 150, "alt", "EP >= 20% (exploring)")
msg("a : Agent", ":MovementStrategy", 345, "planPath(position, blockedDirection, map, rng)")
msg(":MovementStrategy", "a : Agent", 368, "path", True)
sq.vertex("", 440, 380, 900, 1, "line;strokeWidth=1;dashed=1;html=1;strokeColor=#555555;")
sq.vertex("[EP < 20% or returning]", 445, 382, 300, 20, "text;html=1;align=left;fontSize=11;fontStyle=2;")
msg("a : Agent", ":Map", 415, "shortestPathHome(position, population)")
msg(":Map", "a : Agent", 438, "path (BFS)", True)
frame(440, 465, 900, 470, "loop", "for each tile of the path, until blocked")
msg("a : Agent", ":Map", 505, "getOccupant(next) / isObstacle(next)")
msg(":Map", "a : Agent", 528, "b or obstacle or free", True)
frame(450, 550, 880, 370, "alt", "tile is free")
msg("a : Agent", ":Map", 590, "moveBeing(this, next)")
self_msg("a : Agent", 605, "spendEp(2)")
sq.vertex("", 450, 645, 880, 1, "line;strokeWidth=1;dashed=1;html=1;strokeColor=#555555;")
sq.vertex("[another agent b on the path]", 455, 647, 300, 20, "text;html=1;align=left;fontSize=11;fontStyle=2;")
self_msg("a : Agent", 672, "encounter(b, map, log)")
msg("a : Agent", "b : Agent", 725, "unionWith(b) / shareWith(b)")
msg("a : Agent", "b : Agent", 755, "chooseRps()   (enemy: confront)")
msg("b : Agent", "a : Agent", 778, "RpsChoice", True)
sq.vertex("", 450, 800, 880, 1, "line;strokeWidth=1;dashed=1;html=1;strokeColor=#555555;")
sq.vertex("[entered its own SafeZone]", 455, 802, 300, 20, "text;html=1;align=left;fontSize=11;fontStyle=2;")
msg("a : Agent", ":Master", 845, "receive(messages)   (deliverToMaster)")
msg(":Master", "a : Agent", 870, "new messages count", True)
msg("a : Agent", ":Simulation", 960, "", True)
msg(":Simulation", ":EventManager", 1020, "maybeTrigger(map, rng, log)   (meteorite)")
msg(":EventManager", ":Simulation", 1045, "", True)
self_msg(":Simulation", 1065, "isFinished()")
msg(":Simulation", ":SimulationView", 1130, "render(this)")
sq.vertex("The view is a SimulationView: ConsoleRenderer (ASCII) or SimulationWindow (Swing).<br>"
          "Each agent's action is applied immediately, so the next agent sees the updated map.",
          20, 1210, 900, 40, "text;html=1;align=left;fontSize=11;fontStyle=2;")
save("sequence-diagram.drawio", [sq])
print("written to", OUT)
