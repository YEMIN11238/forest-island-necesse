#!/usr/bin/env python3
"""Export reviewed, coherent 11-by-4 resident atlases without repainting art.

Rows are UP, RIGHT, DOWN, LEFT. Columns are idle, four walk poses, and six
empty-hand attack poses. Each actor has one fixed scale for all 44 poses.
Source alpha is preserved; this script never removes a background or edits a
hand. Use the bundled Python/Pillow/NumPy runtime. --stage writes reviewable
runtime strips without replacing the mod's current art.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / 'mosslume/art/characters-r8'
RUNTIME = ROOT / 'mod/resources/mosslume/residents'
ACTORS = ('farmer', 'carpenter', 'merchant')
DIRS = ('up', 'right', 'down', 'left')
STATES = (('idle', (0,)), ('walk', (1, 2, 3, 4)),
          ('attack', (5, 6, 7, 8, 9, 10)))
FRAME = 64
FOOT = (32, 52)
TARGET_HEIGHT = 32


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def component_reader():
    spec = importlib.util.spec_from_file_location(
        'biome_export', ROOT / 'mosslume/scripts/export_biome_runtime.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.components


def prepare_actor(actor: str, read_components):
    source = ART / 'sources' / f'{actor}-actions-v1.png'
    if not source.is_file():
        raise FileNotFoundError(f'Missing source atlas: {source}')
    image = Image.open(source).convert('RGBA')
    if image.getchannel('A').getextrema() == (255, 255):
        raise ValueError(f'{actor}: source has no transparency; do not auto-clear it')
    components = [c for c in read_components(image) if c['area'] > 500]
    if len(components) != 44:
        raise ValueError(f'{actor}: expected 44 complete opaque body silhouettes, '
                         f'got {len(components)}. Review source; do not guess slices.')
    components.sort(key=lambda c: (c['bbox'][1] + c['bbox'][3]) / 2)
    rows = [sorted(components[i * 11:(i + 1) * 11], key=lambda c: c['bbox'][0])
            for i in range(4)]
    # Require separated rows, and separated poses in each row, rather than silently
    # assigning a tall extra fragment to the next direction.
    for row in range(3):
        centers_a = [(c['bbox'][1] + c['bbox'][3]) / 2 for c in rows[row]]
        centers_b = [(c['bbox'][1] + c['bbox'][3]) / 2 for c in rows[row + 1]]
        if max(centers_a) >= min(centers_b):
            raise ValueError(f'{actor}: ambiguous direction rows {row}/{row + 1}')
    neutral_opaque = sum(row[0]['bbox'][3] - row[0]['bbox'][1] for row in rows) / 4
    standing_heights = []
    for row in rows:
        left, top, right, bottom = row[0]['bbox']
        standing = image.crop((max(0, left - 2), max(0, top - 2),
                               min(image.width, right + 2), min(image.height, bottom + 2)))
        box = standing.getchannel('A').getbbox()
        standing_heights.append(box[3] - box[1])
    neutral = sum(standing_heights) / 4
    scale = TARGET_HEIGHT / neutral
    files = {}
    pose_rows = []
    preview = Image.new('RGBA', (FRAME * 11, FRAME * 4))
    for row, (direction, components_in_row) in enumerate(zip(DIRS, rows)):
        frames = []
        poses = []
        for index, component in enumerate(components_in_row):
            left, top, right, bottom = component['bbox']
            # A two-pixel transparent margin retains anti-aliased edge alpha.
            # There is no thresholding or component masking in the exported crop.
            box = (max(0, left - 2), max(0, top - 2),
                   min(image.width, right + 2), min(image.height, bottom + 2))
            if index and box[0] <= components_in_row[index - 1]['bbox'][2]:
                raise ValueError(f'{actor}/{direction}/{index}: overlapping silhouettes')
            crop = image.crop(box)
            alpha = np.asarray(crop.getchannel('A'))
            opaque = alpha >= 128
            opaque_rows = np.flatnonzero(opaque.any(axis=1))
            if not len(opaque_rows):
                raise ValueError(f'{actor}/{direction}/{index}: empty source cell')
            last_row = int(opaque_rows[-1])
            shoe_band = max(2, round(neutral * .10))
            feet = np.argwhere(opaque[max(0, last_row - shoe_band + 1):last_row + 1])
            foot_source_x = float(feet[:, 1].min() + feet[:, 1].max()) / 2
            width, height = round(crop.width * scale), round(crop.height * scale)
            resized = crop.resize((width, height), Image.Resampling.NEAREST)
            resized_opaque = np.asarray(resized.getchannel('A')) >= 128
            resized_rows = np.flatnonzero(resized_opaque.any(axis=1))
            if not len(resized_rows):
                raise ValueError(f'{actor}/{direction}/{index}: empty sampled cell')
            resized_foot_y = int(resized_rows[-1])
            resized_shoe_band = max(2, round(TARGET_HEIGHT * .10))
            resized_feet = np.argwhere(resized_opaque[
                max(0, resized_foot_y - resized_shoe_band + 1):resized_foot_y + 1])
            resized_foot_x = float(resized_feet[:, 1].min() + resized_feet[:, 1].max()) / 2
            # Anchor the sampled shoe pixels, not a separately rounded estimate of
            # their pre-resize position. Nearest sampling can drop a thin last row.
            paste_x = FOOT[0] - round(resized_foot_x)
            paste_y = FOOT[1] - resized_foot_y
            if paste_x < 0 or paste_y < 0 or paste_x + width > FRAME or paste_y + height > FRAME:
                raise ValueError(f'{actor}/{direction}/{index}: would clip a pose '
                                 f'({paste_x}, {paste_y}, {width}, {height})')
            frame = Image.new('RGBA', (FRAME, FRAME))
            frame.paste(resized, (paste_x, paste_y))
            visible_box = frame.getchannel('A').getbbox()
            if visible_box is None:
                raise ValueError(f'{actor}/{direction}/{index}: empty runtime cell')
            # Opaque feet must remain fixed, even when an attack extends an arm.
            runtime_opaque = np.asarray(frame.getchannel('A')) >= 128
            actual_foot_y = int(np.flatnonzero(runtime_opaque.any(axis=1))[-1])
            if abs(actual_foot_y - FOOT[1]) > 1:
                raise ValueError(f'{actor}/{direction}/{index}: foot drift {actual_foot_y}')
            poses.append({'sourceColumn': index, 'sourceBox': list(box),
                          'sourceOpaqueBox': component['bbox'], 'scale': scale,
                          'runtimePaste': [paste_x, paste_y], 'runtimeCropSize': [width, height],
                          'runtimeVisibleBox': list(visible_box), 'opaqueFootY': actual_foot_y,
                          'sourceOpaqueFootX': foot_source_x,
                          'sourceOpaqueFootY': last_row,
                          'sampledOpaqueFoot': [resized_foot_x, resized_foot_y]})
            frames.append(frame)
            preview.paste(frame, (index * FRAME, row * FRAME))
        for action, indices in STATES:
            strip = Image.new('RGBA', (FRAME * len(indices), FRAME))
            for column, index in enumerate(indices):
                strip.paste(frames[index], (column * FRAME, 0))
            files[f'{actor}-{action}-{direction}.png'] = strip
        # Any retained fallback call must use the same new resident identity.
        files[f'{actor}-{direction}.png'] = frames[0].copy()
        pose_rows.append({'direction': direction, 'poses': poses})
    record = {'source': str(source.relative_to(ROOT)), 'sourceSha256': sha256(source),
              'sourceDimensions': list(image.size), 'neutralMeanHeight': neutral,
              'neutralOpaqueMeanHeight': neutral_opaque,
              'oneScaleForActor': scale, 'rows': pose_rows,
              'outputs': [{'file': f'mod/resources/mosslume/residents/{name}',
                           'dimensions': list(frame.size)} for name, frame in files.items()]}
    return files, preview, record


def backup_current(actor: str):
    backup_dir = ART / 'backup-before-r8/residents'
    backup_dir.mkdir(parents=True, exist_ok=True)
    index = backup_dir.parent / f'{actor}-index.json'
    if index.exists():
        return json.loads(index.read_text())
    records = []
    for source in sorted(RUNTIME.glob(f'{actor}-*.png')):
        target = backup_dir / source.name
        if not target.exists():
            shutil.copy2(source, target)
        records.append({'file': str(source.relative_to(ROOT)),
                        'backup': str(target.relative_to(ROOT)), 'sha256': sha256(target)})
    index.write_text(json.dumps(records, ensure_ascii=False, indent=2) + '\n')
    return records


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--actors', nargs='+', choices=ACTORS, default=list(ACTORS))
    parser.add_argument('--stage', action='store_true',
                        help='Write staged-residents/ for review; do not replace runtime art')
    args = parser.parse_args()
    read_components = component_reader()
    # Prepare every requested actor before changing any runtime file.
    prepared = {actor: prepare_actor(actor, read_components) for actor in args.actors}
    destination = ART / 'staged-residents' if args.stage else RUNTIME
    destination.mkdir(parents=True, exist_ok=True)
    qa = ART / 'qa'
    qa.mkdir(parents=True, exist_ok=True)
    manifest_path = ART / ('resident-export-staged.json' if args.stage else 'resident-export.json')
    manifest = json.loads(manifest_path.read_text()) if manifest_path.exists() else {
        'tool': 'built-in image_gen; mechanical Pillow nearest-neighbor export',
        'prompts': 'prompts.json', 'frameSize': [FRAME, FRAME], 'foot': list(FOOT),
        'targetMeanStandingHeight': TARGET_HEIGHT,
        'layout': 'UP RIGHT DOWN LEFT; idle1, walk4, attack6',
        'export': 'Original source alpha retained. No background removal, repainting, '
                  'hand edits, frame-specific resizing, or pose synthesis.',
        'mode': 'staged' if args.stage else 'runtime', 'actors': {}}
    for actor, (files, preview, record) in prepared.items():
        if not args.stage:
            record['backups'] = backup_current(actor)
        for name, frame in files.items():
            frame.save(destination / name)
        checkered = Image.new('RGB', preview.size, (50, 60, 67))
        checkered.paste(preview, (0, 0), preview)
        checkered.resize((preview.width * 3, preview.height * 3), Image.Resampling.NEAREST).save(
            qa / f'{actor}-actions-runtime.png')
        manifest['actors'][actor] = record
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'actors': len(prepared), 'poses': len(prepared) * 44,
                      'files': len(prepared) * 16, 'mode': manifest['mode'],
                      'manifest': str(manifest_path), 'destination': str(destination)}))


if __name__ == '__main__':
    main()
