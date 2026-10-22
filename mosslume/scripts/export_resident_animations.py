#!/usr/bin/env python3
"""Crop generated NPC atlases at real transparent gutters; export fixed native frames.

This performs only deterministic crop/nearest-neighbor format conversion, never drawing.
Old resident standing textures remain untouched.
"""
from pathlib import Path
import hashlib
import json
import shutil
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/characters/residents/animations'
OUT = ROOT / 'runtime-art/residents'
MOD = ROOT.parent / 'mod/resources/mosslume/residents'
FOOT = (32, 52)
BODY = 32
ROLES = ('farmer', 'carpenter')
DIRECTIONS = ('down', 'right', 'up', 'left')
ENTRIES = []


def spans(projection):
    raw, start = [], None
    for index, occupied in enumerate(list(projection) + [0]):
        if occupied and start is None:
            start = index
        elif not occupied and start is not None:
            raw.append([start, index]); start = None
    merged = []
    for left, right in raw:
        if merged and left - merged[-1][1] < 12:
            merged[-1][1] = right
        else:
            merged.append([left, right])
    return merged


def atlas(path, count):
    image = Image.open(path).convert('RGBA')
    alpha = image.getchannel('A')
    assert alpha.getextrema()[0] == 0, f'No transparent background: {path}'
    mask = alpha.point(lambda value: 255 if value >= 128 else 0)
    rows = spans(mask.getprojection()[1])
    assert len(rows) == count, (path, 'rows', rows)
    result = []
    for top, bottom in rows:
        columns = spans(mask.crop((0, top, image.width, bottom)).getprojection()[0])
        assert len(columns) == count, (path, 'columns', columns)
        row = []
        for left, right in columns:
            cut = image.crop((left, top, right, bottom))
            box = cut.getchannel('A').point(lambda value: 255 if value >= 128 else 0).getbbox()
            rect = (left + box[0], top + box[1], left + box[2], top + box[3])
            row.append((image.crop(rect), rect))
        result.append(row)
    return image.size, result


def head_center(image):
    mask = image.getchannel('A')
    centers = []
    for y in range(max(1, image.height // 12), max(2, image.height // 3)):
        xs = [x for x in range(image.width) if mask.getpixel((x, y)) >= 128]
        if xs:
            centers.append((xs[0] + xs[-1]) / 2)
    return sorted(centers)[len(centers) // 2] if centers else image.width / 2


def frame(image, scale, horizontal=False):
    small = image.resize((max(1, round(image.width * scale)), max(1, round(image.height * scale))), Image.Resampling.NEAREST)
    small.putalpha(small.getchannel('A').point(lambda value: 255 if value >= 128 else 0))
    box = small.getchannel('A').getbbox()
    assert box
    if horizontal:
        # The right-facing feet stay at the world anchor; the lying body extends left.
        x = FOOT[0] - (box[2] - 1)
    else:
        x = round(FOOT[0] - head_center(image) * scale)
    y = FOOT[1] - (box[3] - 1)
    assert x + box[0] >= 0 and x + box[2] <= 64 and y + box[1] >= 0 and y + box[3] <= 64, (image.size, scale, (x, y), box)
    result = Image.new('RGBA', (64, 64))
    result.alpha_composite(small, (x, y))
    return result


def write(image, filename, **metadata):
    path = OUT / filename
    path.parent.mkdir(parents=True, exist_ok=True)
    target = MOD / filename
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)
    shutil.copy2(path, target)
    alpha = image.getchannel('A')
    assert alpha.getextrema() == (0, 255), filename
    ENTRIES.append(dict(file=filename, dimensions=list(image.size), alphaBounds=list(alpha.getbbox()),
                        sha256=hashlib.sha256(path.read_bytes()).hexdigest(), **metadata))


def export():
    source_records = []
    for role in ROLES:
        path = SOURCE / f'{role}-walk-v1.png'
        size, rows = atlas(path, 4)
        source_records.append(dict(file=str(path.relative_to(ROOT)), dimensions=list(size), rows=4, columns=4,
                                   sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
        for direction, row in zip(DIRECTIONS, rows):
            # One scale per direction prevents head/body breathing between gait poses.
            scale = BODY / row[0][0].height
            frames = []
            for index, (cut, rect) in enumerate(row):
                result = frame(cut, scale)
                frames.append(result)
                write(result, f'frames/{role}-{direction}-{index}.png', role=role, direction=direction,
                      source=str(path.relative_to(ROOT)), sourceRect=list(rect), sourceColumn=index,
                      foot=list(FOOT), scale=scale)
            write(frames[0], f'{role}-idle-{direction}.png', role=role, action='idle', direction=direction, foot=list(FOOT))
            strip = Image.new('RGBA', (256, 64))
            for index, original in enumerate((1, 2, 3, 2)):
                strip.alpha_composite(frames[original], (index * 64, 0))
            assert len({frame.tobytes() for frame in frames[1:]}) == 3, f'Duplicate gait poses: {role}/{direction}'
            write(strip, f'{role}-walk-{direction}.png', role=role, action='walk', direction=direction,
                  frameSize=[64, 64], frameCount=4, sourceColumns=[1, 2, 3, 2], foot=list(FOOT))
        path = SOURCE / f'{role}-states-v1.png'
        size, states = atlas(path, 2)
        source_records.append(dict(file=str(path.relative_to(ROOT)), dimensions=list(size), rows=2, columns=2,
                                   sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
        standing_height = states[0][1][0].height
        for index, action in enumerate(('work', 'carry', 'downed', 'sleep')):
            cut, rect = states[index // 2][index % 2]
            # Lying art uses body length; scaling its height to 32 would make a giant NPC.
            scale = BODY / cut.width if action == 'downed' else BODY / standing_height
            result = frame(cut, scale, horizontal=action == 'downed')
            write(result, f'{role}-{action}.png', role=role, action=action, frameSize=[64, 64],
                  source=str(path.relative_to(ROOT)), sourceRect=list(rect), foot=list(FOOT), scale=scale)
    manifest = dict(version=1, tool='built-in image_gen', method='alpha-gap crop and nearest-neighbor export; no painted/generated code',
                    bodySourceHeight=32, framePixels=[64, 64], foot=list(FOOT), runtimeScale=2,
                    existingStaticAssetsPreserved=True, sourceAtlases=source_records,
                    limitations=['Work and carry are single front-right action poses, not four-way animated action cycles.',
                                 'Downed is a left-head/right-foot resting pose; sleep is supine head-up for native bed rotation.',
                                 'Walk poses are generated, with 1,2,3,2 loop; pose identity reviewed visually, not motion-capture ground truth.'],
                    entries=ENTRIES)
    (OUT / 'animations-v1-manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(dict(passed=True, sourceAtlases=len(source_records), exported=len(ENTRIES), manifest=str(OUT / 'animations-v1-manifest.json'))))


if __name__ == '__main__':
    export()
