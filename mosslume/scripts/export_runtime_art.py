#!/usr/bin/env python3
"""Deterministic source-atlas export for Mosslume's first playable build.

This script crops/resamples approved art; it does not generate or redraw art.
Source PNGs and the art-planning acceptance records are never changed.
Run with the bundled dependency Python (Pillow required).
"""
from pathlib import Path
import hashlib
import json
from collections import Counter
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'art'
PLAN = ART / 'planning'
OUT = ROOT / 'runtime-art'
FOOT = (32, 52)
# Match a 32 px body to the 16 px logical art grid: the game draws characters
# at exactly 2x, yielding a readable two-tile (64 world px) figure. Keeping the
# source frame/foot anchor unchanged also leaves room for raised tools.
CHARACTER_BODY_HEIGHT = 32
NEAREST = Image.Resampling.NEAREST
ENTRIES = []
IMAGES = {}


def source(path):
    path = ROOT / path
    if path not in IMAGES:
        image = Image.open(path).convert('RGBA')
        image.putalpha(image.getchannel('A').point(lambda a: 0 if a <= 8 else 255 if a >= 240 else a))
        IMAGES[path] = image
    return IMAGES[path]


def save(image, relative, **metadata):
    file = OUT / relative
    file.parent.mkdir(parents=True, exist_ok=True)
    image.save(file)
    alpha = image.getchannel('A')
    bbox = alpha.getbbox()
    if not bbox:
        raise ValueError(f'Empty export: {relative}')
    ENTRIES.append({'file': relative, 'width': image.width, 'height': image.height,
                    'sha256': hashlib.sha256(file.read_bytes()).hexdigest(),
                    'alphaBounds': list(bbox), **metadata})


def crop_rect(image, rect):
    x, y, w, h = rect
    return image.crop((x, y, x + w, y + h))


def fit_icon(image, size=32, inset=2):
    box = image.getchannel('A').getbbox()
    if box:
        image = image.crop(box)
    scale = min((size - inset * 2) / image.width, (size - inset * 2) / image.height)
    fitted = image.resize((max(1, round(image.width * scale)), max(1, round(image.height * scale))), NEAREST)
    result = Image.new('RGBA', (size, size))
    result.alpha_composite(fitted, ((size - fitted.width) // 2, (size - fitted.height) // 2))
    return result


def x_silhouettes(row_image, count):
    """Find complete separated figures even when the source ignores equal columns."""
    mask = row_image.getchannel('A').point(lambda a: 255 if a > 32 else 0)
    projection, _ = mask.getprojection()
    spans = []
    for x, occupied in enumerate(projection + [0]):
        if occupied and (not spans or spans[-1][1] is not None):
            spans.append([x, None])
        if not occupied and spans and spans[-1][1] is None:
            spans[-1][1] = x
    merged = []
    for left, right in spans:
        if merged and left - merged[-1][1] < 12:
            merged[-1][1] = right
        else:
            merged.append([left, right])
    while len(merged) > count:
        index = min(range(len(merged) - 1), key=lambda i: merged[i + 1][0] - merged[i][1])
        merged[index:index + 2] = [[merged[index][0], merged[index + 1][1]]]
    if len(merged) != count:
        return [[round(i * row_image.width / count), round((i + 1) * row_image.width / count)] for i in range(count)]
    return merged


def character_geometry(cropped):
    box = cropped.getchannel('A').point(lambda a: 255 if a >= 128 else 0).getbbox()
    if not box:
        raise ValueError('Empty character source cell')
    # Tool-free frames use the head centre, not the width of a swinging
    # arm/bag or an extended boot. This prevents the body jumping sideways
    # when the silhouette changes between generated walking frames.
    alpha = cropped.getchannel('A')
    row_centres = []
    for row in range(box[1] + max(1, (box[3]-box[1])//12),
                     box[1] + max(2, (box[3]-box[1])//3)):
        occupied = [x for x in range(box[0], box[2]) if alpha.getpixel((x, row)) >= 128]
        if occupied:
            row_centres.append((occupied[0] + occupied[-1]) / 2)
    row_centres.sort()
    centre = row_centres[len(row_centres)//2] if row_centres else (box[0]+box[2])/2
    return (centre, box[3] - 1), box[3] - box[1]

def character_frame(image, rect, anchor=None, body_reference=None, normalize_sole=False):
    cropped = crop_rect(image, rect)
    if anchor is None:
        anchor, body_reference = character_geometry(cropped)
        normalize_sole = True
    scale = CHARACTER_BODY_HEIGHT / body_reference
    scaled = cropped.resize((max(1, round(cropped.width * scale)), max(1, round(cropped.height * scale))), NEAREST)
    scaled.putalpha(scaled.getchannel('A').point(lambda a: 255 if a >= 128 else 0))
    result = Image.new('RGBA', (64, 64))
    sole_y = scaled.getchannel('A').getbbox()[3] - 1
    offset = (round(FOOT[0] - anchor[0] * scale), FOOT[1] - sole_y if normalize_sole else round(FOOT[1] - anchor[1] * scale))
    result.alpha_composite(scaled, offset)
    return result


def strip(frames):
    result = Image.new('RGBA', (64 * len(frames), 64))
    for index, image in enumerate(frames):
        result.alpha_composite(image, (64 * index, 0))
    return result


def export_items():
    catalog = json.loads((PLAN / 'inventory-icon-catalog-v1.json').read_text())
    for item in catalog['items']:
        image = crop_rect(source(item['sourceAtlas']), item['previewTrimRect'])
        save(fit_icon(image), 'items/' + item['id'] + '.png', category='item', id=item['id'],
             name=item['name'], source=item['sourceAtlas'], sourceRegion=item['previewTrimRect'])


def export_compact_player():
    """Slice the authored 4x4 atlas at transparent gaps, not assumed cells."""
    source_path = 'art/characters/player/player-compact-v2.png'
    atlas = source(source_path)
    mask = atlas.getchannel('A').point(lambda a: 255 if a >= 128 else 0)
    def spans(projection):
        result, start = [], None
        for i, occupied in enumerate(list(projection) + [0]):
            if occupied and start is None:
                start = i
            elif not occupied and start is not None:
                result.append((start, i))
                start = None
        if len(result) != 4:
            raise ValueError(f'Compact player atlas needs four separated bands, got {result}')
        return result
    columns, rows = [spans(p) for p in mask.getprojection()]
    walk_order = [1, 2, 3, 2]
    walk_rows = {}
    for row, direction in enumerate(['down', 'right', 'up', 'left']):
        frames, regions = [], []
        for col in range(4):
            left, right = columns[col]
            top, bottom = rows[row]
            rect = [left - 2, top - 2, right - left + 4, bottom - top + 4]
            frames.append(character_frame(atlas, rect))
            regions.append(rect)
            save(frames[-1], f'players/frames/player-compact-{direction}-{col}.png',
                 category='player_frame', action='compact-'+direction, frame=col,
                 source=source_path, sourceRegion=rect, footAnchor=list(FOOT),
                 targetBodyHeight=CHARACTER_BODY_HEIGHT, runtimeScale=2)
        save(frames[0], f'players/player-stand-{direction}.png', category='player_standing',
             direction=direction, source=source_path, sourceRegion=regions[0],
             frameSize=[64,64], footAnchor=list(FOOT), frameCount=1)
        sheet = strip([frames[i] for i in walk_order])
        save(sheet, f'players/player-walk-{direction}.png', category='player_strip',
             action='walk-'+direction, source=source_path, frameSize=[64,64],
             frameCount=4, sourceColumns=walk_order, footAnchor=list(FOOT),
             animationPhaseValidated=False,
             note='Compact four-pose source; pose order reviewed, detailed limb identity is still art-review pending.')
        walk_rows[direction] = sheet
    combined = Image.new('RGBA', (256,256))
    for row,direction in enumerate(['up','right','down','left']):
        combined.alpha_composite(walk_rows[direction], (0,row*64))
    save(combined,'players/player-walk.png',category='player_combined_walk',source=source_path,
         frameSize=[64,64],rows=['up','right','down','left'],framesPerDirection=4,footAnchor=list(FOOT))


def export_characters():
    batch1 = json.loads((PLAN / 'batch-01-manifest.json').read_text())
    batch3 = json.loads((PLAN / 'batch-03-manifest.json').read_text())
    export_compact_player()
    for asset in batch3['assets']:
        if asset.get('kind') != 'resident_direction_source':
            continue
        image = source(asset['file'])
        role = asset['id'][4:]
        for cell in asset['cells']:
            frame = character_frame(image, cell['nominalSourceRect'])
            save(frame, f'residents/{role}-{cell["id"]}.png', category='resident_standing', role=role,
                 direction=cell['id'], source=asset['file'], footAnchor=list(FOOT), frameCount=1)
    for asset in batch1['assets']:
        if not asset['id'].endswith('_portraits'):
            continue
        role = asset['id'].split('_')[0]
        image = source(asset['file'])
        for cell in asset['atlas']['readingOrder']:
            frame = crop_rect(image, cell['nominalSourceRect']).resize((256, 256), NEAREST)
            save(frame, f'portraits/{role}-{cell["id"]}.png', category='portrait', role=role,
                 expression=cell['id'], source=asset['file'], shoulderCropReviewPending=True)


WORLD_WIDTHS = {
    'cottage-roof':90, 'cottage-walls':80, 'door-closed':30, 'door-open':30,
    'window-dark':11, 'window-lit':11,
    'bed':32, 'table':42, 'chair':20, 'cupboard':28, 'bookshelf':28, 'floor-lamp':12,
    'plank-floor':32, 'interior-wall':64, 'occlusion-trim':48,
    'woven-rug':52, 'potted-plant':13, 'wall-lamp':9,
    'stump':18, 'stump-regrowth':18, 'seedling':8, 'sapling':18,
    'young-tree':29, 'fallen-branch':20,
    'berry-empty':28, 'berry-flowering':28, 'berry-unripe':28, 'berry-ripe':28,
    'rock-intact':26, 'rock-cracked':26, 'rock-damaged':26, 'rock-depleted':26,
    'chest-closed':24, 'chest-opening':24, 'chest-open':24,
    'anvil':24, 'noticeboard':48,
}


def export_world():
    batch = json.loads((PLAN / 'batch-05-manifest.json').read_text())
    for asset in batch['assets']:
        image = source(asset['file'])
        for region in asset['regions']:
            identifier = region['id']
            cropped = crop_rect(image, region['previewTrimRect'])
            width = WORLD_WIDTHS[identifier]
            scale = width / cropped.width
            if identifier.startswith('berry-'):
                scale = 28 / 555
            elif identifier.startswith('rock-'):
                scale = 26 / 514
            size = (max(1, round(cropped.width * scale)), max(1, round(cropped.height * scale)))
            if identifier == 'plank-floor':
                size = (32, 32)
            fitted = cropped.resize(size, NEAREST)
            save(fitted, 'world/' + identifier + '.png', category='world', id=identifier,
                 source=asset['file'], sourceRegion=region['previewTrimRect'],
                 groundAnchor=[fitted.width / 2, fitted.height], anchorStatus='first_playable_provisional')


def export_crops():
    batch = json.loads((PLAN / 'batch-03-manifest.json').read_text())
    for asset in batch['assets']:
        if not asset['id'].startswith('crop_'):
            continue
        image = source(asset['file'])
        cells = asset['cells']
        mature = crop_rect(image, cells[4]['nominalSourceRect'])
        box = mature.getchannel('A').getbbox()
        scale = 24 / max(box[2] - box[0], box[3] - box[1])
        crop = asset['id'][5:]
        for cell in cells:
            part = crop_rect(image, cell['nominalSourceRect'])
            box = part.getchannel('A').getbbox()
            part = part.crop(box)
            small = part.resize((max(1, round(part.width * scale)), max(1, round(part.height * scale))), NEAREST)
            canvas = Image.new('RGBA', (32, 32))
            canvas.alpha_composite(small, ((32 - small.width) // 2, 29 - small.height))
            save(canvas, f'crops/{crop}-{cell["id"]}.png', category='crop', crop=crop, state=cell['id'],
                 groundAnchor=[16,29], source=asset['file'], sourceRegion=cell['nominalSourceRect'])


def export_assembled_cottage():
    """Reuse the visually checked source-art preview assembly in a common canvas."""
    batch = json.loads((PLAN / 'batch-05-manifest.json').read_text())
    asset = next(a for a in batch['assets'] if a['id'] == 'cottage_layers')
    image = source(asset['file'])
    regions = {r['id']:r for r in asset['regions']}
    placement = {
        'roof':[('cottage-roof',(3,7,90,71))],
        'walls':[('cottage-walls',(8,59,80,49))],
        'door-closed':[('door-closed',(33,80,30,28))],
        'door-open':[('door-open',(33,80,30,28))],
        'windows-dark':[('window-dark',(19,82,11,11)),('window-dark',(66,82,11,11))],
        'windows-lit':[('window-lit',(19,82,11,11)),('window-lit',(66,82,11,11))],
    }
    layers = {}
    for key, parts in placement.items():
        canvas = Image.new('RGBA',(96,112))
        for identifier,(x,y,w,h) in parts:
            part = crop_rect(image, regions[identifier]['previewTrimRect']).resize((w,h),NEAREST)
            canvas.alpha_composite(part,(x,y))
        layers[key] = canvas
        save(canvas,f'world/house-layers/{key}.png',category='house_layer',id=key,
             source=asset['file'],groundAnchor=[48,108],assemblyCoordinates='batch05_preview_calibration')
    for door in ['closed','open']:
        for window in ['dark','lit']:
            canvas=Image.new('RGBA',(96,112))
            for layer in ['walls','door-'+door,'windows-'+window,'roof']:
                canvas.alpha_composite(layers[layer])
            save(canvas,f'world/cottage-{door}-{window}.png',category='house_composite',
                 id=f'cottage-{door}-{window}',source=asset['file'],groundAnchor=[48,108],
                 assemblyCoordinates='batch05_preview_calibration')


def make_contact_sheet():
    chosen = [e for e in ENTRIES if e['category'] in ['item','player_standing','world','resident_standing']]
    width, cw, ch, columns = 1024, 128, 112, 8
    height = ((len(chosen) + columns - 1) // columns) * ch
    sheet = Image.new('RGB', (width, height), (19, 36, 36))
    draw = ImageDraw.Draw(sheet)
    for index, entry in enumerate(chosen):
        left, top = index % columns * cw, index // columns * ch
        draw.rectangle((left+2,top+2,left+cw-3,top+ch-3), outline=(53,83,72))
        im = Image.open(OUT / entry['file']).convert('RGBA')
        factor = min(3, 108 / im.width, 82 / im.height)
        im = im.resize((max(1, round(im.width*factor)),max(1, round(im.height*factor))),NEAREST)
        sheet.paste(im,(left+(cw-im.width)//2,top+8+(82-im.height)//2),im)
        label=entry.get('id') or entry.get('role','player')+'-'+entry.get('direction','')
        draw.text((left+7,top+94),label[:19],fill=(223,207,159))
    sheet.save(OUT/'export-contact-sheet.png')


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    export_items()
    export_characters()
    export_world()
    export_crops()
    export_assembled_cottage()
    make_contact_sheet()
    counts=dict(Counter(e['category'] for e in ENTRIES))
    manifest={'project':'Mosslume','version':1,'date':'2026-10-22',
              'generator':'Deterministic Pillow crop/nearest-neighbor runtime format export; no image generation',
              'sourceArtworkPreserved':True,'integratedIntoGame':False,
              'worldTilePixels':32,'playerFramePixels':[64,64],'playerFootAnchor':list(FOOT),
              'characterBodySourcePixels':CHARACTER_BODY_HEIGHT,'characterRenderScale':2,'characterBodyWorldPixels':64,
              'counts':counts,'totalPngAssets':len(ENTRIES),
              'notes':['Runtime-sized first-playable exports, not a production art acceptance claim.',
                       'Compact v2 player has four directions, an idle pose and four-frame walk loop; attacks use this same body with a direction-aware runtime tool overlay. Original v1 tool/body drafts are not loaded.',
                       'Characters normalize to 32 source pixels and render at integer 2x; all exported soles align to source y=52. Compact player source is sliced by transparent gaps; NPC designs remain from batch 03.',
                       'World sprites use compact widths matching existing 32px tile art; resource state height changes are preserved.',
                       'House parts require assembly/occlusion calibration; floor tile still has visible border seams.',
                       'Resident exports are static four-direction only; portraits retain source shoulder-edge cropping.',
                       'Exported item IDs require explicit native registry mapping by game integration code.'],
              'entries':ENTRIES}
    (OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'output':str(OUT),'counts':counts,'totalPngAssets':len(ENTRIES)},ensure_ascii=False))


if __name__ == '__main__':
    main()
