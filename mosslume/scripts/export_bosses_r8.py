#!/usr/bin/env python3
"""Mechanically export the reviewed 8-by-4 Boss atlases, retaining source RGBA.

Rows UP RIGHT DOWN LEFT; columns ready, moveA, moveB, windup, strike, recover,
hurt, death. Source cell grouping follows actual opaque gutters, including all
scattered opaque fragments in each cell. One scale per Boss fits all 32 poses
into 96-pixel frames. Corpses retain their authored short silhouette.
"""
from __future__ import annotations

from pathlib import Path
import hashlib
import json

import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / 'mosslume/art/bosses-r8'
OUT = ROOT / 'mod/resources/mosslume/bosses'
FRAME = 96
FOOT = (48, 80)
DIRS = ('up', 'right', 'down', 'left')
POSES = ('ready', 'moveA', 'moveB', 'windup', 'strike', 'recover', 'hurt', 'death')
SPECS = {
    'stone-watcher': {'source': 'stone-watcher-actions-v2.png', 'icon': 'stone-echo',
                      'iconCrop': (.26, .40, .70, .78), 'iconDescription': 'DOWN ready chest/core crop'},
    'moon-reaper': {'source': 'moon-reaper-actions-v1.png', 'icon': 'moon-seal',
                    'iconCrop': (.20, .03, .80, .55), 'iconDescription': 'DOWN ready antlers/head crop'},
}


def spans(occupied):
    edges = np.diff(np.r_[False, occupied, False].astype(np.int8))
    return list(zip(np.flatnonzero(edges == 1).tolist(), np.flatnonzero(edges == -1).tolist()))


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def source_cells(image, boss):
    alpha = np.asarray(image.getchannel('A'))
    opaque = alpha >= 128
    bands = spans(opaque.any(axis=1))
    if len(bands) != 4:
        raise ValueError(f'{boss}: expected four separated direction rows, got {bands}')
    y_cuts = [0] + [(bands[i][1] + bands[i + 1][0]) // 2 for i in range(3)] + [image.height]
    cells = []
    for row in range(4):
        y0, y1 = y_cuts[row:row + 2]
        columns = spans(opaque[y0:y1].any(axis=0))
        if len(columns) != 8:
            raise ValueError(f'{boss}/{DIRS[row]}: expected eight complete pose groups, got {columns}')
        x_cuts = [0] + [(columns[i][1] + columns[i + 1][0]) // 2 for i in range(7)] + [image.width]
        for column in range(8):
            x0, x1 = x_cuts[column:column + 2]
            positions = np.argwhere(opaque[y0:y1, x0:x1])
            if not len(positions):
                raise ValueError(f'{boss}/{row}/{column}: empty source group')
            left = int(positions[:, 1].min()) + x0
            right = int(positions[:, 1].max()) + x0 + 1
            top = int(positions[:, 0].min()) + y0
            bottom = int(positions[:, 0].max()) + y0 + 1
            # Group-level bounds retain detached rocks, antlers and blades. No
            # largest-component masking and no alpha thresholding of the crop.
            box = (max(x0, left - 2), max(y0, top - 2),
                   min(x1, right + 2), min(y1, bottom + 2))
            crop = image.crop(box)
            mask = np.asarray(crop.getchannel('A')) >= 128
            foot_y = int(np.flatnonzero(mask.any(axis=1))[-1])
            if column == 7:
                # A fallen pile has no standing shoes; keep its whole footprint
                # centered and preserve its low height at the same actor scale.
                foot_x = (crop.width - 1) / 2
            else:
                band = max(2, round((bottom - top) * .10))
                feet = np.argwhere(mask[max(0, foot_y - band + 1):foot_y + 1])
                foot_x = float(feet[:, 1].min() + feet[:, 1].max()) / 2
            cells.append({'row': row, 'column': column, 'direction': DIRS[row],
                          'pose': POSES[column], 'groupBox': [x0, y0, x1, y1],
                          'sourceBox': list(box), 'sourceOpaqueBox': [left, top, right, bottom],
                          'sourceFoot': [foot_x, foot_y], 'crop': crop})
    return cells


def sampled_frame(cell, scale):
    crop = cell['crop']
    width, height = round(crop.width * scale), round(crop.height * scale)
    sampled = crop.resize((width, height), Image.Resampling.NEAREST)
    mask = np.asarray(sampled.getchannel('A')) >= 128
    rows = np.flatnonzero(mask.any(axis=1))
    if not len(rows):
        raise ValueError('Sampling erased an entire Boss pose')
    foot_y = int(rows[-1])
    if cell['column'] == 7:
        foot_x = (sampled.width - 1) / 2
    else:
        band = max(2, round((cell['sourceOpaqueBox'][3] - cell['sourceOpaqueBox'][1]) * scale * .10))
        feet = np.argwhere(mask[max(0, foot_y - band + 1):foot_y + 1])
        foot_x = float(feet[:, 1].min() + feet[:, 1].max()) / 2
    x, y = FOOT[0] - round(foot_x), FOOT[1] - foot_y
    fits = x >= 0 and y >= 0 and x + width <= FRAME and y + height <= FRAME
    frame = Image.new('RGBA', (FRAME, FRAME))
    if fits:
        frame.paste(sampled, (x, y))
    return fits, frame, {'runtimePaste': [x, y], 'runtimeCropSize': [width, height],
                         'sampledFoot': [foot_x, foot_y]}


def prepare(boss, spec):
    source = ART / 'sources' / spec['source']
    image = Image.open(source).convert('RGBA')
    if image.getchannel('A').getextrema() == (255, 255):
        raise ValueError(f'{boss}: opaque source; do not remove its background automatically')
    cells = source_cells(image, boss)
    ready = [cell for cell in cells if cell['column'] == 0]
    ready_height = sum(cell['crop'].getchannel('A').getbbox()[3] -
                       cell['crop'].getchannel('A').getbbox()[1] for cell in ready) / 4
    requested_scale = 68 / ready_height
    scale = requested_scale
    # Some authored sweeps are wider than a 96px cell at the desired ready height.
    # Apply the same modest reduction to ALL poses, never shrink one strike only.
    for _ in range(200):
        sampled = [sampled_frame(cell, scale) for cell in cells]
        if all(result[0] for result in sampled):
            break
        scale *= .99
    else:
        raise ValueError(f'{boss}: unable to fit every pose with one scale')
    atlas = Image.new('RGBA', (FRAME * 8, FRAME * 4))
    records = []
    for cell, (_, frame, metadata) in zip(cells, sampled):
        bbox = frame.getchannel('A').getbbox()
        if bbox is None:
            raise ValueError(f'{boss}: empty exported cell')
        atlas.paste(frame, (cell['column'] * FRAME, cell['row'] * FRAME))
        records.append({key: value for key, value in cell.items() if key != 'crop'} |
                       metadata | {'runtimeVisibleBox': list(bbox)})
    down_ready = next(cell for cell in ready if cell['row'] == 2)
    original = down_ready['crop']
    x0, y0, x1, y1 = spec['iconCrop']
    icon_box = (round(original.width * x0), round(original.height * y0),
                round(original.width * x1), round(original.height * y1))
    icon_crop = original.crop(icon_box)
    if icon_crop.getchannel('A').getbbox() is None:
        raise ValueError(f'{boss}: empty icon crop')
    icon_scale = min(28 / icon_crop.width, 28 / icon_crop.height)
    icon_crop = icon_crop.resize((round(icon_crop.width * icon_scale),
                                  round(icon_crop.height * icon_scale)), Image.Resampling.NEAREST)
    icon = Image.new('RGBA', (32, 32))
    icon.paste(icon_crop, ((32 - icon_crop.width) // 2, (32 - icon_crop.height) // 2))
    heights = [r['runtimeVisibleBox'][3] - r['runtimeVisibleBox'][1]
               for r in records if r['column'] == 0]
    manifest = {'source': str(source.relative_to(ROOT)), 'sourceSha256': sha256(source),
                'sourceDimensions': list(image.size), 'frameSize': [FRAME, FRAME],
                'foot': list(FOOT), 'requestedReadyHeight': 68,
                'requestedScale': requested_scale, 'oneScaleForBoss': scale,
                'scaleLimitedByFrame': scale < requested_scale,
                'readyVisibleHeights': heights, 'readyMeanVisibleHeight': sum(heights) / 4,
                'output': f'mod/resources/mosslume/bosses/{boss}.png', 'poses': records,
                'icon': {'file': f'mod/resources/mosslume/bosses/{spec["icon"]}.png',
                         'description': spec['iconDescription'],
                         'sourcePose': 'DOWN ready', 'sourceCropWithinPose': list(icon_box),
                         'mechanicalNearestScale': icon_scale}}
    return atlas, icon, manifest


def main():
    prepared = {boss: prepare(boss, spec) for boss, spec in SPECS.items()}
    OUT.mkdir(parents=True, exist_ok=True)
    qa = ART / 'qa'
    qa.mkdir(parents=True, exist_ok=True)
    manifest = {'tool': 'built-in image_gen; mechanical nearest RGBA export',
                'layout': {'rows': list(DIRS), 'columns': list(POSES)},
                'alpha': 'Source RGBA retained in every cropped group; no repainting, '
                         'background removal, component masking, or thresholded runtime alpha.',
                'scaling': 'One constant scale per Boss; width constraints can reduce the '
                           'whole actor below target ready height. Death stays short.', 'bosses': {}}
    for boss, (atlas, icon, record) in prepared.items():
        atlas.save(OUT / f'{boss}.png')
        icon.save(OUT / f'{SPECS[boss]["icon"]}.png')
        review = Image.new('RGB', atlas.size, (50, 60, 67))
        review.paste(atlas, (0, 0), atlas)
        draw = ImageDraw.Draw(review)
        for row, direction in enumerate(DIRS):
            for column, pose in enumerate(POSES):
                draw.text((column * FRAME + 2, row * FRAME + 2),
                          f'{direction} {pose}', fill=(220, 220, 220))
                draw.line((column * FRAME, row * FRAME + FOOT[1],
                           (column + 1) * FRAME - 1, row * FRAME + FOOT[1]), fill=(140, 65, 65))
        review.resize((review.width * 2, review.height * 2), Image.Resampling.NEAREST).save(
            qa / f'{boss}-runtime-contact.png')
        manifest['bosses'][boss] = record
    path = ART / 'boss-export.json'
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'bosses': len(prepared), 'poses': len(prepared) * 32,
                      'atlasDimensions': [768, 384], 'icons': 2, 'manifest': str(path),
                      'readyMeanHeights': {boss: result[2]['readyMeanVisibleHeight']
                                           for boss, result in prepared.items()}}))


if __name__ == '__main__':
    main()
