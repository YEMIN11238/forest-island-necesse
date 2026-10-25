#!/usr/bin/env python3
"""Use the user's ORIGINAL GIF/WebP sprites, without generative redrawing.

GIF backgrounds are exact palette-color keys. WebP backgrounds use a border-
connected tolerance mask only (lossy WebP has many near-gray background values).
Retained foreground RGB values are never repainted or recolored. Documented
palette/sample strips below the characters are removed from alpha. Extracted
native-size frames and WebP cells remain beside the source provenance manifest.
"""
from pathlib import Path
from collections import deque
import hashlib
import importlib.util
import json
import shutil

import numpy as np
from PIL import Image, ImageDraw, ImageOps

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / 'mosslume/art/bosses-r8'
REF = ART / 'references'
OUT = ROOT / 'mod/resources/mosslume/bosses'
EXTRACTED = ART / 'direct-extracted'
DIRS = ('up', 'right', 'down', 'left')
FILES = {
 'stone-pillar': ('f57c0b3885181.560c0dcbe4424.gif', (244,358,336,380)),
 'stone-blue': ('cba5c03885181.560c0e374e22d.gif', (244,358,336,380)),
 'stone-red': ('6c15a63885181.560c0db5917a5.gif', (146,358,238,380)),
 'moon-melee': ('c3e3b03885181.560c0d477b5ac.gif', (118,221,162,243)),
 'moon-idle-hurt': ('1bd3053885181.560c0e379a1b9.gif', (118,217,162,239)),
 'moon-ambush': ('f8e18c3885181.560c0db5364de.gif', (118,225,162,247)),
 'moon-beam': ('bd62af3885181.560c0d17d84c7.gif', None),
}
STONE_WEBP = '7420453885181.560c0e368a667.webp'
MOON_WEBP = '57fc2a3885181.560c0de9a97bd.webp'
STONE_BODIES = [(37,18,129,272),(199,17,287,270),(37,309,129,563),
                (199,309,289,561),(329,306,475,561),(28,769,154,1023),
                (181,769,305,1021),(315,768,453,1023)]
MOON_BODIES = [(12,13,147,178),(156,13,286,176),(304,13,434,174),(450,16,582,178),
 (18,195,150,361),(20,378,147,550),(208,380,336,550),(379,400,548,548),
 (24,576,192,722),(30,741,164,906),(202,745,358,929),(407,745,560,928),
 (18,963,173,1083),(202,961,352,1081),(18,1108,176,1292),(211,1108,369,1294),
 (407,1104,565,1292),(22,1313,180,1500),(287,1384,353,1498)]
FX = {
 'blue-glyph': [(373,11,462,67),(371,83,428,141),(371,153,419,209),(373,225,413,275)],
 'red-glyph': [(492,11,581,67),(491,83,547,141),(491,153,537,209),(492,225,532,275)],
 'crystal-impact': [(20,592,190,733),(213,598,349,719),(373,617,477,707),(500,616,586,707)],
 'blue-spark': [(510,302,578,360),(520,360,566,391),(534,395,556,417)],
 'red-spark': [(510,432,578,489),(520,493,566,525),(534,530,556,550)],
 'ground-rings': [(38,1085,93,1106),(123,1082,199,1106),(230,1076,329,1106),
                  (353,1078,453,1104),(496,1078,554,1101)],
 'earth-pillars': [(28,1216,90,1280),(117,1138,222,1280),(256,1168,366,1280)],
 'falling-earth': [(492,768,570,847),(495,872,575,912)],
 'glyph-squares': [(482,944,526,998),(541,943,587,999)],
}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def components(image):
    spec = importlib.util.spec_from_file_location('biome_export', ROOT / 'mosslume/scripts/export_biome_runtime.py')
    module = importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    return module.components(image)


def read_gif(name, filename, hud):
    source = REF / filename;gif = Image.open(source);frames=[];records=[]
    folder=EXTRACTED/'gif'/name;folder.mkdir(parents=True,exist_ok=True)
    for index in range(gif.n_frames):
        gif.seek(index);source_frame=gif.convert('RGBA');array=np.array(source_frame)
        background=array[0,0,:3].copy();mask=np.all(array[:,:,:3]==background,axis=2)
        array[:,:,3]=np.where(mask,0,255)
        if hud is not None:
            x0,y0,x1,y1=hud;array[y0:y1,x0:x1,3]=0
        image=Image.fromarray(array)
        # This is the important color-retention assertion: only alpha changes.
        assert np.array_equal(np.array(image)[:,:,:3],np.array(source_frame)[:,:,:3])
        path=folder/f'{index:02d}.png';image.save(path);frames.append(image)
        records.append({'frame':index,'durationMillis':gif.info.get('duration',90),
                        'sourceSize':list(gif.size),'backgroundRGB':background.tolist(),
                        'paletteStripRemoved':list(hud) if hud else None,
                        'visibleBox':image.getchannel('A').getbbox(),
                        'file':str(path.relative_to(ROOT))})
    return frames,{'source':str(source.relative_to(ROOT)),'sourceSha256':digest(source),
                   'sourceHadAlpha':False,'foregroundRGBUnchanged':True,'frames':records}


def read_webp(filename):
    source=REF/filename;image=Image.open(source).convert('RGBA');array=np.array(image)
    rgb=array[:,:,:3].astype(np.int16);candidate=np.max(np.abs(rgb-rgb[0,0]),axis=2)<=12
    height,width=candidate.shape;seen=np.zeros_like(candidate);queue=deque()
    for x in range(width):
        for y in (0,height-1):
            if candidate[y,x] and not seen[y,x]:seen[y,x]=True;queue.append((y,x))
    for y in range(height):
        for x in (0,width-1):
            if candidate[y,x] and not seen[y,x]:seen[y,x]=True;queue.append((y,x))
    while queue:
        y,x=queue.popleft()
        for yy,xx in ((y-1,x),(y+1,x),(y,x-1),(y,x+1)):
            if 0<=yy<height and 0<=xx<width and candidate[yy,xx] and not seen[yy,xx]:
                seen[yy,xx]=True;queue.append((yy,xx))
    array[:,:,3]=np.where(seen,0,255);result=Image.fromarray(array)
    assert np.array_equal(np.array(result)[:,:,:3],np.array(image)[:,:,:3])
    result.save(EXTRACTED/(Path(filename).stem+'-keyed.png'))
    return result,{'source':str(source.relative_to(ROOT)),'sourceSha256':digest(source),
                   'sourceHadAlpha':False,'backgroundMethod':'border-connected max RGB delta <=12',
                   'foregroundRGBUnchanged':True,'note':'Lossy WebP edge RGB is kept, not repainted.'}


def extract_cells(image, label, boxes):
    folder=EXTRACTED/'webp'/label;folder.mkdir(parents=True,exist_ok=True);cells=[];records=[]
    for index,box in enumerate(boxes):
        left,top,right,bottom=box
        expanded=(max(0,left-2),max(0,top-2),min(image.width,right+2),min(image.height,bottom+2))
        crop=image.crop(expanded)
        if crop.getchannel('A').getbbox() is None:raise ValueError(f'Empty original WebP cell {label}/{index}')
        path=folder/f'{index:02d}.png';crop.save(path);cells.append(crop)
        records.append({'index':index,'sourceBox':list(expanded),'file':str(path.relative_to(ROOT))})
    return cells,records


def mirror_at_foot(image, foot_x):
    # Reflect around the actual native anchor, retaining identical colors.
    mirrored=ImageOps.mirror(image);shift=2*foot_x-(image.width-1)
    result=Image.new('RGBA',image.size);result.paste(mirrored,(shift,0))
    if np.count_nonzero(np.array(result)[:,:,3])!=np.count_nonzero(np.array(image)[:,:,3]):
        raise ValueError('Mirroring around foot clipped original foreground')
    return result


def frame_from_source(image, anchor, scale, size, foot, source_facing, body_only=False):
    if body_only:
        pieces=components(image)
        if pieces:
            left,top,right,bottom=pieces[0]['bbox']
            box=(max(0,left-2),max(0,top-2),min(image.width,right+2),min(image.height,bottom+2))
        else:box=None
    else:box=image.getchannel('A').getbbox()
    result=Image.new('RGBA',(size,size))
    if box is None:return result,{'empty':True,'sourceBox':None}
    crop=image.crop(box);width,height=round(crop.width*scale),round(crop.height*scale)
    sampled=crop.resize((width,height),Image.Resampling.NEAREST)
    x=foot[0]+round((box[0]-anchor[0])*scale)
    y=foot[1]+round((box[1]-anchor[1])*scale)
    if x<0 or y<0 or x+width>size or y+height>size:
        raise ValueError(f'Original pose clips: box={box} anchor={anchor} size={size} paste={x,y,width,height}')
    result.paste(sampled,(x,y))
    if source_facing=='left':result=mirror_at_foot(result,foot[0])
    return result,{'empty':False,'sourceBox':list(box),'sourceAnchor':list(anchor),
                   'sourceFacing':source_facing,'nearestScale':scale,'runtimePaste':[x,y],
                   'sampledSize':[width,height]}


def directional_strip(frames, filename, size, foot):
    texture=Image.new('RGBA',(size*len(frames),size*4))
    for row,direction in enumerate(DIRS):
        for column,frame in enumerate(frames):
            displayed=mirror_at_foot(frame,foot[0]) if direction=='left' else frame
            texture.paste(displayed,(column*size,row*size))
    texture.save(OUT/(filename+'.png'))
    return texture


def contact(texture, filename, size, labels=None):
    review=Image.new('RGB',texture.size,(40,60,70));review.paste(texture,(0,0),texture)
    draw=ImageDraw.Draw(review)
    for column in range(texture.width//size):
        draw.text((column*size+2,2),str(column) if labels is None else labels[column],fill='white')
    review.save(ART/'qa/direct-source'/filename)


def small_strip(cells, filename, size, foot, scale, center_vertical=False):
    strip=Image.new('RGBA',(len(cells)*size,size))
    for index,crop in enumerate(cells):
        bbox=crop.getchannel('A').getbbox();crop=crop.crop(bbox)
        width,height=round(crop.width*scale),round(crop.height*scale)
        x=foot[0]-width//2
        y=foot[1]-height//2 if center_vertical else foot[1]-height
        if min(x,y)<0 or x+width>size or y+height>size:raise ValueError('FX clip '+filename)
        strip.paste(crop.resize((width,height),Image.Resampling.NEAREST),(index*size+x,y))
    strip.save(OUT/(filename+'.png'));return strip


def main():
    EXTRACTED.mkdir(parents=True,exist_ok=True);OUT.mkdir(parents=True,exist_ok=True)
    (ART/'qa/direct-source').mkdir(parents=True,exist_ok=True)
    backup=ART/'generated-runtime-backup';backup.mkdir(exist_ok=True)
    for name in ('stone-watcher','moon-reaper','stone-echo','moon-seal'):
        source=OUT/(name+'.png');target=backup/source.name
        if source.exists() and not target.exists():shutil.copy2(source,target)
    gifs={};manifest={'source':'User-supplied original GIF and WebP files; no generated Boss pixels used.',
       'directions':'Original side-view art. RIGHT is native/mirrored right-facing art; LEFT mirrored. '
                    'UP and DOWN intentionally reuse the side view; no invented top-down directions.',
       'death':'No authored death or corpse found. Native death uses original idle with fade.',
       'sources':{},'webpCells':{},'bosses':{}}
    for name,(filename,hud) in FILES.items():
        gifs[name],manifest['sources'][name]=read_gif(name,filename,hud)
    stone,manifest['sources']['stone-webp']=read_webp(STONE_WEBP)
    moon,manifest['sources']['moon-webp']=read_webp(MOON_WEBP)
    _,manifest['webpCells']['stone-bodies']=extract_cells(stone,'stone-bodies',STONE_BODIES)
    _,manifest['webpCells']['moon-bodies']=extract_cells(moon,'moon-bodies',MOON_BODIES)
    fx={}
    for name,boxes in FX.items():fx[name],manifest['webpCells'][name]=extract_cells(stone,name,boxes)
    # Constant per-character scales preserve full original character proportions.
    stone_scale=68/254;moon_scale=64/164
    props=['# Direct original side-view sprites. Foreground RGB unchanged; no generated Boss art.',
           '# UP/DOWN reuse a side view. death=idle-fade; no source corpse exists.']
    clips={
      'stone-watcher': [('stone-pillar',192,(96,168),(297.5,315),'left',range(3),[3],range(4,8)),
                        ('stone-blue',192,(96,168),(297.5,315),'left',range(5),range(5,8),range(8,10))],
      'moon-reaper': [('moon-melee',192,(96,168),(136.5,204),'right',range(4),range(4,6),range(6,8)),
                      ('moon-ambush',192,(96,168),(228.5,208),'left',range(8),range(8,12),range(12,14))]}
    for boss,specs in clips.items():
        scale=stone_scale if boss=='stone-watcher' else moon_scale;records=[]
        for index,(name,size,foot,anchor,facing,windup,strike,recovery) in enumerate(specs):
            frames=[];poses=[];empty=[]
            for frame_index,image in enumerate(gifs[name]):
                actual_anchor=anchor;actual_facing=facing
                if name=='moon-ambush' and frame_index>=8:actual_anchor=(112.5,208);actual_facing='right'
                try:
                    frame,record=frame_from_source(image,actual_anchor,scale,size,foot,actual_facing)
                except ValueError as error:
                    raise ValueError(f'{name} frame {frame_index}: {error}') from error
                if record['empty']:empty.append(frame_index)
                frames.append(frame);poses.append(record)
            filename=f'{boss}-attack{index}';texture=directional_strip(frames,filename,size,foot)
            prefix=f'{boss}.attack{index}.'
            values={'file':filename,'columns':len(frames),'frameMillis':90,'frameSize':size,
                    'footX':foot[0],'footY':foot[1],
                    'windup':','.join(map(str,windup)),'strike':','.join(map(str,strike)),
                    'recovery':','.join(map(str,recovery)),'emptyFrames':','.join(map(str,empty)),
                    'damageFrame': 10 if name=='moon-ambush' else list(strike)[0]}
            props.extend(prefix+key+'='+str(value) for key,value in values.items())
            records.append({'clip':name,'runtime':filename+'.png','properties':values,'poses':poses})
            contact(texture,f'{filename}-contact.png',size)
        manifest['bosses'][boss]={'constantSourceScale':scale,'clips':records,'death':'idle-fade'}
    # Compatibility base atlases contain original bodies only; real attacks load
    # the full variable-cell clips above. No original wide slash is cropped down.
    base_specs={
      'stone-watcher': [('stone-blue',0,(297.5,315),'left'),('stone-blue',0,(297.5,315),'left'),
          ('stone-blue',1,(297.5,315),'left'),('stone-pillar',2,(297.5,315),'left'),
          ('stone-pillar',3,(297.5,315),'left'),('stone-blue',9,(297.5,315),'left'),
          ('stone-blue',0,(297.5,315),'left'),('stone-blue',0,(297.5,315),'left')],
      'moon-reaper': [('moon-melee',0,(136.5,204),'right'),('moon-melee',0,(136.5,204),'right'),
          ('moon-melee',1,(136.5,204),'right'),('moon-melee',2,(136.5,204),'right'),
          ('moon-melee',7,(136.5,204),'right'),('moon-melee',7,(136.5,204),'right'),
          ('moon-idle-hurt',9,(136.5,200),'right'),('moon-melee',0,(136.5,204),'right')]}
    for boss,rows in base_specs.items():
        frames=[];records=[];scale=stone_scale if boss=='stone-watcher' else moon_scale
        for name,index,anchor,facing in rows:
            frame,record=frame_from_source(gifs[name][index],anchor,scale,96,(48,80),facing,body_only=True)
            if record['empty']:raise ValueError('Empty compatibility Boss body')
            frames.append(frame);records.append({'sourceClip':name,'sourceFrame':index}|record)
        texture=directional_strip(frames,boss,96,(48,80))
        contact(texture,f'{boss}-direct-base-contact.png',96)
        manifest['bosses'][boss]['baseAtlas']={'poses':records,
            'note':'Compatibility body only. Locomotion reuses original idle; attack reads full clips. '
                   'Column7 repeats idle for native fade; it is not an authored corpse.'}
    glyph_scale=min(28/c.width for c in fx['blue-glyph'])
    glyph=small_strip(fx['blue-glyph'],'stone-glyph',32,(16,16),glyph_scale,center_vertical=True)
    impact_scale=min(88/max(c.width,c.height) for c in fx['crystal-impact'])
    impact=small_strip(fx['crystal-impact'],'stone-impact',96,(48,80),min(impact_scale,.50))
    contact(glyph,'stone-glyph-direct.png',32);contact(impact,'stone-impact-direct.png',96)
    props.extend(['fx.stone-glyph.columns=4','fx.stone-glyph.frameMillis=90','fx.stone-glyph.frameSize=32',
                  'fx.stone-glyph.footX=16','fx.stone-glyph.footY=16',
                  'fx.stone-impact.columns=4','fx.stone-impact.frameMillis=90','fx.stone-impact.frameSize=96',
                  'fx.stone-impact.footX=48','fx.stone-impact.footY=80'])
    # Item icons are cropped directly from original gray stone and crescent head.
    for name,image,box in [('stone-echo',stone,(44,44,125,145)),
                            ('moon-seal',moon,(18,13,145,106))]:
        crop=image.crop(box);s=min(28/crop.width,28/crop.height);crop=crop.resize((round(crop.width*s),round(crop.height*s)),Image.Resampling.NEAREST)
        icon=Image.new('RGBA',(32,32));icon.paste(crop,((32-crop.width)//2,(32-crop.height)//2));icon.save(OUT/(name+'.png'))
    (OUT/'source-clips.properties').write_text('\n'.join(props)+'\n')
    (ART/'direct-source-export.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'source':'original user GIF/WebP','gifFramesExtracted':sum(len(frames) for frames in gifs.values()),
                      'activeAttackClips':4,'activeAttackSourceFrames':40,'generatedPixelsUsed':False,
                      'runtimeProperties':str(OUT/'source-clips.properties'),
                      'manifest':str(ART/'direct-source-export.json')}))


if __name__=='__main__':main()
