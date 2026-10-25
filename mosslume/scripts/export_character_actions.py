#!/usr/bin/env python3
"""Mechanical sprite slicing of imagegen output; source art is retained unchanged.

Uses reviewed connected components (not equal-row cuts which clip the first row),
one scale per direction, and a shoe anchor. Does not synthesize or repaint art.
Run with the bundled Python/Pillow/NumPy runtime.
"""
from pathlib import Path
import hashlib, importlib.util, json, shutil
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[2]
ART=ROOT/'mosslume/art/characters-r6'
GENERATED=Path('/Users/zhangjunhao/.codex/generated_images/01a1336f-4018-7dc3-a548-a66dff7389cc')
SOURCES={
 'player':'exec-f283e3e4-c89f-4c98-ac39-c2bb2ddc31f0.png',
 'farmer':'exec-5dbe80b2-0ee3-4098-81b8-97e2c1560533.png',
 'carpenter':'exec-23279825-4483-4d84-a6ae-653224f3abda.png',
 'merchant':'exec-7f69603d-41ec-4503-b1d2-b519d03576f6.png',
}
DIRS=['up','right','down','left']

def main():
 spec=importlib.util.spec_from_file_location('biome_export',ROOT/'mosslume/scripts/export_biome_runtime.py')
 exporter=importlib.util.module_from_spec(spec);spec.loader.exec_module(exporter)
 (ART/'sources').mkdir(parents=True,exist_ok=True)
 result={'source':'built-in imagegen','grid':'6 columns, 4 directions UP RIGHT DOWN LEFT',
  'export':'reviewed alpha components; constant per-direction scale; foot 32,52; original alpha preserved',
  'prompt':'Exact reference character identity, compact pixel RPG, empty hands; ready/windup/raised/strike/followthrough/ready; transparent; no tools/shadows/text. Player follow-up removes surrounding haze.',
  'actors':{}}
 for actor,filename in SOURCES.items():
  source=ART/'sources'/f'{actor}-attack-v1.png'
  if not source.exists():shutil.copy2(GENERATED/filename,source)
  image=Image.open(source).convert('RGBA')
  components=[c for c in exporter.components(image) if c['area']>1500]
  components.sort(key=lambda c:((c['bbox'][1]+c['bbox'][3])//2//256,c['bbox'][0]))
  if len(components)!=24:raise ValueError(f'{actor}: expected 24 reviewed silhouettes, got {len(components)}')
  folder=ROOT/'mod/resources/mosslume'/('players' if actor=='player' else 'residents')
  actions={}
  for row,direction in enumerate(DIRS):
   frames=components[row*6:(row+1)*6]
   neutral=sum(frames[i]['bbox'][3]-frames[i]['bbox'][1] for i in [0,5])/2
   scale=32/neutral
   sheet=Image.new('RGBA',(384,64));metadata=[]
   for col,component in enumerate(frames):
    left,top,right,bottom=component['bbox']
    crop=image.crop((left,top,right,bottom))
    alpha=np.array(crop.getchannel('A'))>=128
    feet=np.argwhere(alpha[max(0,alpha.shape[0]-max(3,round(neutral*.12))):])
    # Shoe midpoint, independent of an extended arm widening the frame.
    foot=(float(feet[:,1].min()+feet[:,1].max())/2 if len(feet) else (right-left)/2)
    width,height=round(crop.width*scale),round(crop.height*scale)
    crop=crop.resize((width,height),Image.Resampling.NEAREST)
    x,y=32-round(foot*scale),53-height
    if x<0 or x+width>64 or y<0:raise ValueError(f'{actor} {direction} frame{col}: overflow')
    sheet.paste(crop,(col*64+x,y))
    metadata.append({'frame':col,'sourceBox':component['bbox'],'foot':[32,52],'scale':scale,'toolInFrame':False})
   output=folder/f'{actor}-attack-{direction}.png';sheet.save(output)
   actions[direction]={'file':str(output.relative_to(ROOT)),'frames':metadata}
  result['actors'][actor]={'source':str(source.relative_to(ROOT)),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'actions':actions}
 (ART/'manifest.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({'actors':len(result['actors']),'directionStrips':4*len(SOURCES),'frames':24*len(SOURCES),'manifest':str(ART/'manifest.json')}))

if __name__=='__main__':main()
