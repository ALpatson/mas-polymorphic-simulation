"""Extracts UML members (visibility, static, abstract, types) from the project's Java sources into JSON."""
import json
import re
import sys
from pathlib import Path

src = Path(sys.argv[1])
out = Path(sys.argv[2])

VIS = {"public": "+", "protected": "#", "private": "-"}
TYPE_RE = r"([\w.<>\[\], ?]+?)"


def uml_type(t):
    return t.replace(" ", "").replace(",", ", ")


def split_params(params):
    params = params.strip()
    if not params:
        return ""
    parts, depth, cur = [], 0, ""
    for ch in params:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append(cur)
            cur = ""
        else:
            cur += ch
    parts.append(cur)
    res = []
    for p in parts:
        p = p.strip().replace("final ", "")
        bits = p.rsplit(" ", 1)
        if len(bits) == 2:
            res.append(f"{bits[1]}: {uml_type(bits[0])}")
    return ", ".join(res)


classes = {}
for f in sorted(src.rglob("*.java")):
    text = f.read_text(encoding="utf-8")
    pkg = re.search(r"^package ([\w.]+);", text, re.M).group(1)
    lines = text.splitlines()
    # top-level declaration
    decl = None
    for ln in lines:
        m = re.match(r"^(public |)(final |)(abstract |)(sealed |)(class|interface|enum|record) (\w+)(.*)", ln)
        if m:
            decl = m
            break
    if not decl:
        continue
    name = decl.group(6)
    kind = decl.group(5)
    rest = decl.group(7)
    ext = re.search(r"extends (\w+)", rest)
    impl = re.search(r"implements ([\w, ]+)", rest)
    info = {
        "package": pkg, "kind": kind, "abstract": bool(decl.group(3)), "final": bool(decl.group(2)),
        "extends": ext.group(1) if ext and kind != "interface" else None,
        "implements": [s.strip() for s in impl.group(1).split(",")] if impl else [],
        "attributes": [], "methods": [], "literals": [],
    }
    if kind == "record":
        comps = re.search(r"record \w+\((.*?)\)", "\n".join(lines))
        for p in comps.group(1).split(","):
            t, n = p.strip().rsplit(" ", 1)
            info["attributes"].append(f"- {n}: {uml_type(t)}")
            info["methods"].append(f"+ {n}(): {uml_type(t)}")
    if kind == "enum":
        body = text.split("{", 1)[1]
        lits = re.findall(r"^\s{4}([A-Z_]+)\s*(?:\(|,|;|$)", body, re.M)
        info["literals"] = lits
    for ln in lines:
        if not re.match(r"^ {4}\S", ln):  # members are indented exactly 4 spaces
            continue
        s = ln.strip()
        if s.startswith(("//", "*", "/*", "@", "return", "}")):
            continue
        vis_m = re.match(r"(public|protected|private)?\s*(.*)", s)
        vis_word, body = vis_m.group(1), vis_m.group(2)
        vis = VIS.get(vis_word, "~")
        static = "static " in body
        abstract = "abstract " in body
        body2 = re.sub(r"\b(static|final|abstract|transient|default|sealed)\s+", "", body)
        if re.match(r"(class|interface|enum|record)\b", body2):
            continue  # nested type
        mm = re.match(rf"(?:<[^>]+>\s+)?{TYPE_RE}\s+(\w+)\((.*?)\)\s*(?:throws [\w, ]+)?\s*[{{;]", body2)
        cm = re.match(rf"(\w+)\((.*?)\)\s*[{{]", body2)
        fm = re.match(rf"{TYPE_RE}\s+(\w+)\s*(=.*)?;", body2)
        if cm and cm.group(1) == name:
            info["methods"].insert(0, {"t": f"{vis} {name}({split_params(cm.group(2))})", "static": False, "abstract": False})
        elif mm and mm.group(1) not in ("new", "return"):
            rtype = uml_type(mm.group(1))
            ret = "" if rtype == "void" else f": {rtype}"
            info["methods"].append({"t": f"{vis} {mm.group(2)}({split_params(mm.group(3))}){ret}",
                                    "static": static, "abstract": abstract})
        elif fm and kind not in ("interface",):
            if fm.group(2) == "serialVersionUID":
                continue
            info["attributes"].append({"t": f"{vis} {fm.group(2)}: {uml_type(fm.group(1))}", "static": static})
    classes[name] = info

out.write_text(json.dumps(classes, indent=1), encoding="utf-8")
for n, c in classes.items():
    print(f"{c['package']}.{n} [{c['kind']}] attrs={len(c['attributes'])} methods={len(c['methods'])}")
