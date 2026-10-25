#!/usr/bin/env python3
"""Mechanical slicing of reviewed 11x4 imagegen atlases; no painting/alpha removal."""
from pathlib import Path
import hashlib, importlib.util, json
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[2]
ART=ROOT/'mosslume/art/animation-r7'
DIRS=['up','right','down','left']

def main():
    spec=importlib.util.spec_from_file_location('biome_export',ROOT/'mosslume/scripts/export_biome_runtime.py')
    exporter=importlib.util.module_from_spec(spec);spec.loader.exec_module(exporter)
    manifest={'tool':'built-in image_gen','prompts':'prompts.json','foot':[32,52],
              'export':'Mechanical nearest sampling. One constant scale per actor. Source alpha preserved.', 'actors':{}}
    for actor in ['ashuo','luan']:
        source=ART/'sources'/f'{actor}-actions-v1.png'
        image=Image.open(source).convert('RGBA')
        components=[c for c in exporter.components(image) if c['area']>500]
        if len(components)!=44:raise ValueError(f'{actor}: expected 44 complete silhouettes, got {len(components)}')
        components.sort(key=lambda c:(c['bbox'][1]+c['bbox'][3])/2)
        rows=[]
        for row in range(4):rows.append(sorted(components[row*11:(row+1)*11],key=lambda c:c['bbox'][0]))
        # Standing establishes the actor's height for every direction and action.
        neutral=sum(row[0]['bbox'][3]-row[0]['bbox'][1] for row in rows)/4
        scale=32/neutral
        outputs={}
        for direction,frames in zip(DIRS,rows):
            exported=[]
            for index,component in enumerate(frames):
                left,top,right,bottom=component['bbox'];crop=image.crop((left,top,right,bottom))
                alpha=np.asarray(crop.getchannel('A'))>=128
                shoe_band=max(2,round(neutral*.10));feet=np.argwhere(alpha[-shoe_band:])
                foot=(float(feet[:,1].min()+feet[:,1].max())/2 if len(feet) else crop.width/2)
                w,h=round(crop.width*scale),round(crop.height*scale)
                x,y=32-round(foot*scale),53-h
                if x<0 or x+w>64 or y<0 or y+h>64:raise ValueError(f'{actor} {direction} {index}: clipped')
                frame=Image.new('RGBA',(64,64));frame.paste(crop.resize((w,h),Image.Resampling.NEAREST),(x,y))
                exported.append(frame)
            for action,indices in [('idle',[0]),('walk',[1,2,3,4]),('attack',[5,6,7,8,9,10])]:
                sheet=Image.new('RGBA',(64*len(indices),64))
                for column,index in enumerate(indices):sheet.paste(exported[index],(column*64,0))
                path=ROOT/'mod/resources/mosslume/residents'/f'{actor}-{action}-{direction}.png'
                sheet.save(path)
                outputs[f'{action}-{direction}']={'file':str(path.relative_to(ROOT)),'frames':len(indices),
                    'sourceBoxes':[frames[i]['bbox'] for i in indices],'scale':scale}
        manifest['actors'][actor]={'source':str(source.relative_to(ROOT)),
            'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'outputs':outputs}
    (ART/'resident-export.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'actors':2,'poses':88,'strips':24,'manifest':str(ART/'resident-export.json')}))

if __name__=='__main__':main()
