#!/usr/bin/env python3
"""Mechanical, alpha-preserving export of reviewed r7 creature animation sources.

No generative drawing occurs here. The source PNGs remain byte-for-byte unchanged.
Each creature uses one scale across all forty poses, including the collapsed corpse.
"""
from pathlib import Path
import hashlib
import json
import re

import numpy as np
from PIL import Image, ImageDraw
from export_biome_runtime import MONSTERS

MOSS = Path(__file__).resolve().parents[1]
REPO = MOSS.parent
ART = MOSS / "art/animation-r7"
OUT = REPO / "mod/resources/mosslume/biomes"
QA = ART / "qa"
ROWS = ["DOWN", "RIGHT", "UP", "LEFT"]
ACTIONS = {"attack": range(0, 4), "hurt": range(4, 6), "death": range(6, 10)}
FRAME = 64
FOOT = (32, 52)
SEGMENT_ALPHA = 64
PAD = 4
MIN_SOURCE_VERSION = {"sandworm": 2}  # v1 UP row faced the wrong way; never export it.
# These recovery poses still lean sideways in their DOWN/UP rows. Their ready
# poses preserve the true direction, so use those without synthesizing a view.
IDLE_SOURCE_COLUMNS = {"spore-creature": 0, "sandworm": 0, "obsidian-beast": 0}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def choose_source(species):
    candidates = []
    for source in (ART / "sources").glob(f"{species}-actions-v*.png"):
        match = re.search(r"-v(\d+)\.png$", source.name)
        if match and int(match[1]) >= MIN_SOURCE_VERSION.get(species, 1):
            candidates.append((int(match[1]), source))
    return max(candidates)[1] if candidates else None


def intervals(occupied):
    out, start = [], None
    for i, value in enumerate(occupied):
        if value and start is None:
            start = i
        elif not value and start is not None:
            if i-start >= 20:
                out.append((start, i))
            start = None
    if start is not None and len(occupied)-start >= 20:
        out.append((start, len(occupied)))
    return out


def source_regions(image):
    alpha = np.asarray(image.getchannel("A"))
    regions = []
    row_bands = intervals((alpha >= SEGMENT_ALPHA).sum(axis=1) > 3)
    if len(row_bands) != 4:
        raise ValueError(f"Expected four separated direction rows, got {row_bands}")
    for row in range(4):
        top = 0 if row == 0 else (row_bands[row-1][1]+row_bands[row][0])//2
        bottom = image.height if row == 3 else (row_bands[row][1]+row_bands[row+1][0])//2
        silhouettes = intervals((alpha[top:bottom] >= SEGMENT_ALPHA).sum(axis=0) > 2)
        if len(silhouettes) != 10:
            raise ValueError(f"Expected 10 separated silhouettes in row {row}, got {silhouettes}")
        for col, (left, right) in enumerate(silhouettes):
            ys, xs = np.where(alpha[top:bottom, left:right] >= SEGMENT_ALPHA)
            strong = (left+int(xs.min()), top+int(ys.min()), left+int(xs.max())+1, top+int(ys.max())+1)
            # Use actual transparent gutters: the mud creature's right attack arm
            # extends across its nominal equal-width cell, and must not be clipped.
            lower_x = 0 if col == 0 else (silhouettes[col-1][1]+left)//2
            upper_x = image.width if col == 9 else (right+silhouettes[col+1][0])//2
            crop = (max(lower_x, strong[0]-PAD), max(top, strong[1]-PAD),
                    min(upper_x, strong[2]+PAD), min(bottom, strong[3]+PAD))
            if crop[0] >= strong[0] or crop[1] >= strong[1] or crop[2] <= strong[2] or crop[3] <= strong[3]:
                raise ValueError(f"No clear padding around silhouette {row},{col}: {crop}/{strong}")
            regions.append({"row": row, "column": col, "strongBounds": strong, "cropBounds": crop})
    return regions


def export_species(biome, species, target):
    source = choose_source(species)
    if source is None:
        raise ValueError(f"No approved source revision for {species}")
    original_sha = sha(source)
    with Image.open(source) as opened:
        image = opened.convert("RGBA")
    regions = source_regions(image)
    standing = [r for r in regions if r["column"] in (0, 3)]
    longest = max(max(r["strongBounds"][2]-r["strongBounds"][0], r["strongBounds"][3]-r["strongBounds"][1]) for r in standing)
    scale = target/longest
    frames, records = [], []
    for region in regions:
        box, strong = region["cropBounds"], region["strongBounds"]
        part = image.crop(box)
        size = (max(1, round(part.width*scale)), max(1, round(part.height*scale)))
        part = part.resize(size, Image.Resampling.NEAREST)
        x = FOOT[0] - round(part.width/2)
        y = FOOT[1] - round((strong[3]-box[1])*scale)
        if x < 1 or y < 1 or x+part.width > 63 or y+part.height > 63:
            raise ValueError(f"64px frame would clip {species}/{region}: {size} at {x},{y}")
        frame = Image.new("RGBA", (FRAME, FRAME))
        # An unmasked paste copies all RGBA channels, including semi-transparent edges.
        frame.paste(part, (x, y))
        alpha = np.asarray(frame.getchannel("A"))
        if not (alpha >= 128).any() or (alpha[[0, -1], :] >= 32).any() or (alpha[:, [0, -1]] >= 32).any():
            raise ValueError(f"Empty/clipped runtime frame: {species}/{region}")
        ys, xs = np.where(alpha >= SEGMENT_ALPHA)
        bounds = [int(xs.min()), int(ys.min()), int(xs.max())+1, int(ys.max())+1]
        frames.append(frame)
        records.append({**region, "direction": ROWS[region["row"]], "runtimePaste": [x, y],
                        "resizedDimensions": size, "runtimeStrongBounds": bounds})
    files = []
    for action, columns in ACTIONS.items():
        columns = list(columns)
        sheet = Image.new("RGBA", (len(columns)*FRAME, 4*FRAME))
        for row in range(4):
            for target_col, source_col in enumerate(columns):
                sheet.paste(frames[row*10+source_col], (target_col*FRAME, row*FRAME))
        destination = OUT / biome / "monsters" / f"{species}-{action}.png"
        destination.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(destination)
        files.append({"action": action, "file": str(destination.relative_to(REPO)),
                      "dimensions": list(sheet.size), "sha256": sha(destination),
                      "sourceColumns": columns, "frameCount": len(columns)*4})
    idle = Image.new("RGBA", (4*FRAME, FRAME))
    idle_source_col = IDLE_SOURCE_COLUMNS.get(species, 3)
    for row in range(4):
        idle.paste(frames[row*10+idle_source_col], (row*FRAME, 0))
    idle_path = OUT / biome / "monsters" / f"{species}-idle.png"
    idle.save(idle_path)
    files.append({"action": "idle", "file": str(idle_path.relative_to(REPO)),
                  "dimensions": list(idle.size), "sha256": sha(idle_path), "frameCount": 4,
                  "sourceColumns": [idle_source_col], "sourceRows": [0, 1, 2, 3],
                  "animationStatus": "Four directional " + ("ready" if idle_source_col == 0 else "neutral recovery") + " poses reused for idle/walking body; not a new authored walking sequence. Original base sprite remains untouched."})
    # A directly rendered contact sheet makes first/last poses and baseline drift reviewable.
    contact = Image.new("RGB", (10*FRAME, 4*(FRAME+20)+26), "#26302b")
    draw = ImageDraw.Draw(contact)
    draw.text((6, 6), f"{species} | fixed scale {scale:.6f} | 64px / foot32,52", fill="white")
    for row in range(4):
        base_y = 26+row*(FRAME+20)
        draw.text((4, base_y), ROWS[row], fill="#aebdad")
        for col in range(10):
            x, y = col*FRAME, base_y+20
            draw.rectangle((x, y, x+63, y+63), outline="#45534a")
            draw.line((x+4, y+52, x+59, y+52), fill="#5a695a")
            contact.paste(frames[row*10+col], (x, y), frames[row*10+col])
    contact = contact.resize((contact.width*3, contact.height*3), Image.Resampling.NEAREST)
    contact_path = QA / f"{species}-runtime-contact.png"
    contact.save(contact_path)
    death_shorter = []
    for row in range(4):
        a, b = records[row*10]["runtimeStrongBounds"], records[row*10+9]["runtimeStrongBounds"]
        death_shorter.append((b[3]-b[1]) < (a[3]-a[1]))
    if not all(death_shorter):
        raise ValueError(f"A corpse is not lower than standing pose: {species}/{death_shorter}")
    if sha(source) != original_sha:
        raise ValueError("Source PNG was modified")
    return {"biome": biome, "species": species, "source": str(source.relative_to(REPO)), "sourceSha256": original_sha,
            "sourceDimensions": list(image.size), "fixedScaleAllFrames": scale,
            "standingMaxLogicalPixels": target, "nominalLayout": [10, 4], "frames": records,
            "files": files, "qaContact": str(contact_path.relative_to(REPO)),
            "validation": {"frameCount": len(frames), "allFramesNonempty": True, "no64pxBoundaryClipping": True,
                           "corpseLowerThanStandingAllDirections": all(death_shorter), "sourceUnchanged": True}}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    QA.mkdir(parents=True, exist_ok=True)
    available = [(biome, name, target) for biome, name, _, _, target in MONSTERS
                 if choose_source(name) is not None]
    missing = [name for _, name, _, _, _ in MONSTERS
               if choose_source(name) is None]
    result = {"version": 1, "frameSize": [64, 64], "footAnchor": list(FOOT), "directionRows": ROWS,
              "alphaHandling": "Original source alpha retained in padded silhouette crops; no alpha threshold applied to output. Alpha64 is used only to locate silhouettes; faint exterior background speckles are excluded by cropping.",
              "resampling": "nearest, one constant scale per species across all40poses",
              "scope": "Mechanical export of existing generated artwork; not a runtime visual capture or combat-playtest result.",
              "missingSources": missing,
              "species": [export_species(biome, name, size) for biome, name, size in available]}
    manifest = ART / "monster-export.json"
    result["counts"] = {"species": len(available), "runtimeSheets": len(available)*4,
                        "authoredActionFrames": len(available)*40, "reusedIdleFrames": len(available)*4}
    manifest.write_text(json.dumps(result, ensure_ascii=False, indent=2)+"\n")
    idle_contact = Image.new("RGB", (660, 32+len(available)*150), "#26302b")
    draw = ImageDraw.Draw(idle_contact)
    draw.text((144, 8), "DOWN              RIGHT             UP                LEFT", fill="white")
    for row, species in enumerate(result["species"]):
        y = 32+row*150
        draw.text((4, y+4), species["species"], fill="white")
        entry = next(entry for entry in species["files"] if entry["action"] == "idle")
        with Image.open(REPO / entry["file"]) as source:
            strip = source.convert("RGBA").resize((512, 128), Image.Resampling.NEAREST)
            idle_contact.paste(strip, (144, y+20), strip)
    idle_contact.save(QA / "all-idle-directions.png")
    print(json.dumps({"exportedSheets": len(available)*4, "runtimeActionFrames": len(available)*40,
                      "runtimeIdleFramesReused": len(available)*4,
                      "remainingSpecies": len(missing), "manifest": str(manifest), "qaDirectory": str(QA)}))


if __name__ == "__main__":
    main()
