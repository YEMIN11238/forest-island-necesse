#!/usr/bin/env python3
"""Reviewed mechanical runtime exports from original biome atlases.

Pillow + NumPy only; no image generation or changes to source PNGs. Run with
bundled Python. All cut overrides below were reviewed against source images;
connected silhouette masks prevent adjacent rows leaking into tall roofs.
"""
from pathlib import Path
from collections import defaultdict
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parent
OUT = ROOT / 'runtime-art/biomes'
RES = REPO / 'mod/resources/mosslume/biomes'
PLAN = ROOT / 'art/planning'
NEAREST = Image.Resampling.NEAREST
ASSETS = {}
IMAGES = {}
CUTS = {}
CATALOG = defaultdict(list)
FOOT = [32, 52]

# Explicitly reviewed opaque-component boxes [left,top,right,bottom], row-major.
# Boxes may overlap in Y because roof tips are taller than their neighbor's base.
# The selected connected silhouette is masked to prevent a neighbouring component
# entering the box. Original preview equal-row cuts are intentionally not used.
REVIEWED = {
 'wetland-buildings-layers': [
  [63,34,591,394],[641,114,1181,394],[1248,147,1457,387],
  [76,396,577,699],[641,446,1183,695],[1249,471,1457,695],
  [76,726,568,991],[639,737,1186,991],[1262,755,1443,991]],
 'mushroom-buildings-layers': [
  [47,28,550,348],[593,86,1102,349],[1162,81,1483,366],
  [35,356,543,653],[576,398,1098,650],[1161,404,1498,663],
  [33,656,541,972],[593,696,1126,959],[1183,694,1479,977]],
 'snowfield-flora-resources': [
  [168,75,522,481],[704,110,857,470],[1090,67,1409,489],
  [199,541,486,742],[656,551,909,754],[1099,532,1379,754],
  [199,784,487,989],[628,823,951,990],[1128,784,1352,997]],
 'snowfield-building-layers': [
  [80,17,574,400],[612,82,1134,373],[1190,114,1403,372],
  [80,388,574,688],[618,421,1116,680],[1155,451,1459,673],
  [82,682,561,997],[596,714,1142,993],[1190,738,1429,987]],
 'desert-flora-resources': [
  [213,25,498,424],[638,182,897,428],[1034,186,1370,428],
  [152,450,575,756],[711,451,831,740],[1053,485,1357,721],
  [145,774,560,991],[627,764,920,999],[1028,746,1397,992]],
 'ruins-cover-variants': [
  [139,26,436,372],[530,126,972,379],[1051,119,1405,374],
  [139,376,436,693],[520,451,974,695],[1051,436,1405,694],
  [137,694,436,1001],[522,752,974,1002],[1045,739,1402,999]],
 'volcanic-buildings-layers': [
  [53,16,518,360],[604,80,1125,366],[1221,104,1461,363],
  [49,358,521,682],[603,419,1129,683],[1222,419,1461,685],
  [47,667,526,989],[613,714,1119,985],[1223,712,1457,984]],
 'obsidian-beast-poses': [
  [55,151,312,465],[390,185,756,465],[828,152,1062,465],[1149,185,1493,465],
  [29,620,360,910],[390,654,780,910],[812,596,1117,910],[1118,729,1507,919]],
}

BIOMES = {
 'wetland': {'terrain':'wetland-terrain-connectors','flora':'wetland-flora-resources','buildings':'wetland-buildings-layers','tree':[0,1], 'floraSize':[(60,58),(30,62),(26,32),(26,18),(24,26),(22,26),(32,18),(26,28),(22,26)]},
 'mushroom': {'terrain':'mushroom-terrain-connectors','flora':'mushroom-flora-resources','buildings':'mushroom-buildings-layers','tree':[0,1], 'floraSize':[(60,36),(32,58),(26,28),(26,30),(32,22),(24,28),(26,26),(20,30),(28,30)]},
 'ruins': {'terrain':'ruins-ground-connectors','flora':'ruins-architecture-modules','buildings':'ruins-architecture-modules','floraSize':[(56,62),(28,52),(40,24),(40,32),(50,60),(50,44),(40,28),(32,24),(40,18)]},
 'snowfield': {'terrain':'snowfield-terrain-connectors','flora':'snowfield-flora-resources','buildings':'snowfield-building-layers','tree':[0,1], 'groundCell':1, 'floraSize':[(54,62),(24,60),(46,68),(28,24),(26,28),(26,28),(28,22),(32,18),(22,24)]},
 'desert': {'terrain':'desert-terrain-connectors','flora':'desert-flora-resources','buildings':'desert-building-layers','tree':[3,4], 'floraSize':[(30,48),(26,26),(26,26),(58,42),(20,62),(22,24),(32,18),(28,26),(28,24)]},
 'volcano': {'terrain':'volcanic-ground-connectors','flora':'volcanic-flora-resources','buildings':'volcanic-buildings-layers','tree':[0,1], 'floraSize':[(56,46),(24,58),(26,26),(24,26),(28,28),(28,26),(28,24),(32,28),(24,24)]},
}
ANIMALS = [
 ('wetland','frog','wetland-frog-directional-poses',22),
 ('wetland','dragonfly','wetland-dragonfly-directional-poses',24),
 ('mushroom','snail','mushroom-snail-directional-poses',22),
 ('mushroom','moth','mushroom-moth-directional-poses',26),
 ('snowfield','snow-rabbit','snow-rabbit-directional-poses',26),
 ('snowfield','arctic-fox','arctic-fox-directional-poses',28),
 ('desert','lizard','desert-lizard-directional-poses',26),
 ('desert','fennec','fennec-fox-directional-poses',28),
 ('volcano','heat-beetle','heat-beetle-directional-poses',24),
]
MONSTERS = [
 ('wetland','mud-creature','wetland-monsters-poses',0,40),
 ('wetland','poison-insect','wetland-monsters-poses',8,36),
 ('mushroom','spore-creature','mushroom-monsters-poses',0,36),
 ('mushroom','fungus-beast','mushroom-monsters-poses',8,44),
 ('snowfield','ice-beast','snowfield-monster-poses',0,44),
 ('snowfield','snow-burrower','snowfield-monster-poses',8,36),
 ('desert','sand-scorpion','desert-monster-poses',0,38),
 ('desert','sandworm','desert-monster-poses',8,42),
 ('ruins','stone-guard','ruins-statue-guard-poses',0,44),
 ('ruins','bronze-sentinel','ruins-bronze-sentinel-poses',0,42),
 ('volcano','lava-slug','lava-slug-poses',0,36),
 ('volcano','obsidian-beast','obsidian-beast-poses',0,46),
]


def components(image):
    """RLE connected components; used only to isolate reviewed manual boxes."""
    arr=np.asarray(image.getchannel('A'))>=128
    parent=[];runs=[];prev=[]
    def root(x):
        while parent[x]!=x:
            parent[x]=parent[parent[x]];x=parent[x]
        return x
    for y,row in enumerate(arr):
        edges=np.diff(np.r_[False,row,False].astype(np.int8))
        starts=np.flatnonzero(edges==1);ends=np.flatnonzero(edges==-1)
        curr=[];j=0
        for x1,x2 in zip(starts,ends):
            n=len(parent);parent.append(n);runs.append((y,int(x1),int(x2)));curr.append(n)
            while j<len(prev) and runs[prev[j]][2]<x1:j+=1
            k=j
            while k<len(prev) and runs[prev[k]][1]<=x2:
                p=root(prev[k]);c=root(n)
                if c!=p:parent[c]=p
                k+=1
        prev=curr
    result={}
    for n,(y,x1,x2) in enumerate(runs):
        r=root(n)
        if r not in result:result[r]={'area':0,'bbox':[x1,y,x2,y+1],'runs':[]}
        d=result[r];d['area']+=x2-x1
        d['bbox']=[min(d['bbox'][0],x1),min(d['bbox'][1],y),max(d['bbox'][2],x2),max(d['bbox'][3],y+1)]
        d['runs'].append((y,x1,x2))
    return sorted(result.values(),key=lambda x:-x['area'])


def load(asset_id):
    if asset_id in IMAGES:return IMAGES[asset_id]
    a=ASSETS[asset_id];im=Image.open(ROOT/a['file']).convert('RGBA')
    # Hard silhouettes in runtime pixel sprites; source file is untouched.
    im.putalpha(im.getchannel('A').point(lambda x:255 if x>=128 else 0))
    IMAGES[asset_id]=im
    if a.get('manualRegionReviewNeeded') and asset_id not in REVIEWED:
        raise ValueError('Unsafe source requires explicit reviewed boxes: '+asset_id)
    return im


def cells(asset_id):
    if asset_id in CUTS:return CUTS[asset_id]
    a=ASSETS[asset_id];im=load(asset_id);result=[]
    if asset_id in REVIEWED:
        comps=components(im)
        for i,box in enumerate(REVIEWED[asset_id]):
            candidates=[c for c in comps if c['bbox']==box]
            if len(candidates)!=1:raise ValueError(f'Reviewed component changed {asset_id} {box}')
            component=candidates[0];mask=Image.new('L',im.size);draw=ImageDraw.Draw(mask)
            for y,x1,x2 in component['runs']:draw.line((x1,y,x2-1,y),fill=255)
            isolated=im.copy();isolated.putalpha(mask)
            pic=isolated.crop(box)
            result.append((pic,{'source':a['file'],'sourceSha256':a['sha256'],'sourceCell':i,'sourceBoundsLTRB':box,'cutReview':'explicit_reviewed_connected_silhouette','sourceManualCutFlag':True}))
    else:
        for i,r in enumerate(a['regions']):
            x,y,w,h=r['sourceRegion'];region=im.crop((x,y,x+w,y+h));bbox=region.getchannel('A').getbbox()
            if not bbox:raise ValueError(f'Empty {asset_id} {i}')
            box=[x+bbox[0],y+bbox[1],x+bbox[2],y+bbox[3]]
            result.append((region.crop(bbox),{'source':a['file'],'sourceSha256':a['sha256'],'sourceCell':i,'sourceBoundsLTRB':box,'cutReview':'transparent_gutter_and_silhouette_reviewed','sourceManualCutFlag':False}))
    CUTS[asset_id]=result
    return result


def fitted(im,width,height):
    s=min(width/im.width,height/im.height)
    return im.resize((max(1,round(im.width*s)),max(1,round(im.height*s))),NEAREST)


def canvas_sprite(im,size,limits=None,foot=None):
    im=fitted(im,*(limits or (size[0]-4,size[1]-4)))
    out=Image.new('RGBA',size)
    anchor=foot or (size[0]//2,size[1]-2)
    out.alpha_composite(im,(round(anchor[0]-im.width/2),anchor[1]-im.height))
    return out


def save(biome,name,image,**meta):
    for base in [OUT,RES]:
        p=base/biome/name;p.parent.mkdir(parents=True,exist_ok=True);image.save(p)
    file=OUT/biome/name
    if not image.getchannel('A').getbbox():raise ValueError('Empty output '+str(file))
    info={'id':name.removesuffix('.png'),'file':f'{biome}/{name}','dimensions':list(image.size),'sha256':hashlib.sha256(file.read_bytes()).hexdigest(),'alphaBounds':list(image.getchannel('A').getbbox()),'resampling':'nearest','alphaThreshold':128,**meta}
    CATALOG[biome].append(info)


def export_environment(biome,config):
    terrain=cells(config['terrain']);ground,provenance=terrain[config.get('groundCell',0)]
    # Only the interior material is used: no decorative dark rim on each tile.
    w,h=ground.size;box=(round(w*.25),round(h*.25),round(w*.75),round(h*.75))
    patch=ground.crop(box)
    if patch.getchannel('A').getextrema()!=(255,255):raise ValueError('Ground interior has holes '+biome)
    patch=patch.resize((16,16),NEAREST);tile=Image.new('RGBA',(32,32))
    tile.alpha_composite(patch,(0,0));tile.alpha_composite(patch.transpose(Image.Transpose.FLIP_LEFT_RIGHT),(16,0))
    tile.alpha_composite(tile.crop((0,0,32,16)).transpose(Image.Transpose.FLIP_TOP_BOTTOM),(0,16))
    save(biome,'ground.png',tile,kind='opaque_ground',sourceCrops=[provenance],interiorCropLTRB=list(box),tiling='mirror-repeat of interior sample; matching border texels')
    # Expose original transition/cliff silhouettes as non-autotile prop modules.
    for i,(im,p) in enumerate(terrain):
        save(biome,f'terrain-{i}.png',canvas_sprite(im,(64,48),(60,44)),kind='terrain_module',sourceCrops=[p],autotileValidated=False,footAnchor=[32,46])
    flora=cells(config['flora'])
    for i,(im,p) in enumerate(flora):
        limit=config['floraSize'][i];size=(64,80) if max(limit)>32 else (32,32)
        save(biome,f'flora-{i}.png',canvas_sprite(im,size,limit),kind='flora' if biome!='ruins' else 'architecture_prop',sourceCrops=[p],targetVisibleMax=list(limit),footAnchor=[size[0]//2,size[1]-2])
    if 'tree' in config:
        crown_i,trunk_i=config['tree'];crown,cp=flora[crown_i];trunk,tp=flora[trunk_i]
        crown=fitted(crown,58,58 if biome=='snowfield' else 42)
        trunk=fitted(trunk,24 if biome!='mushroom' else 30,55)
        base=Image.new('RGBA',(64,80));top=base.copy()
        base.alpha_composite(trunk,((64-trunk.width)//2,78-trunk.height))
        # Crowns rest on a common 80px tree canvas, which keeps the trunk fixed
        # when the renderer applies crown-only wind deformation.
        crown_bottom=(78-trunk.height+12) if biome=='wetland' else (62 if biome=='snowfield' else 44)
        top.alpha_composite(crown,((64-crown.width)//2,crown_bottom-crown.height))
        tree=base.copy();tree.alpha_composite(top)
        for suffix,im in [('tree',tree),('tree-crown',top),('tree-trunk',base)]:
            save(biome,suffix+'.png',im,kind='layered_tree',sourceCrops=[cp,tp],footAnchor=[32,78],windPivot=[32,58],layer=suffix,castsBakedShadow=False)
    building=cells(config['buildings'])
    if biome=='ruins':
        for i,cell in enumerate([3,0,4]):
            im,p=building[cell]
            save(biome,f'structure-{i}.png',canvas_sprite(im,(128,144),(112,126),[64,140]),kind='structure',sourceCrops=[p],footAnchor=[64,140],castsBakedShadow=False)
    else:
        for row in range(3):
            roof,rp=building[row*3];shell,sp=building[row*3+1];door,dp=building[row*3+2]
            roof=fitted(roof,124,90);shell=fitted(shell,108,62)
            out=Image.new('RGBA',(128,144));shell_y=140-shell.height
            out.alpha_composite(shell,((128-shell.width)//2,shell_y))
            door=fitted(door,34,max(1,shell.height-8))
            # The well's last component is a bucket, placed at the base.
            dx=82 if biome=='desert' and row==2 else (128-door.width)//2
            out.alpha_composite(door,(dx,140-door.height))
            roof_y=shell_y+(32 if biome=='desert' and row==2 else 16)-roof.height
            if roof_y<2:roof_y=2
            out.alpha_composite(roof,((128-roof.width)//2,roof_y))
            save(biome,f'structure-{row}.png',out,kind='assembled_structure',sourceCrops=[rp,sp,dp],footAnchor=[64,140],layerPlacements={'roof':[int((128-roof.width)//2),roof_y,roof.width,roof.height],'shell':[int((128-shell.width)//2),shell_y,shell.width,shell.height],'door':[dx,140-door.height,door.width,door.height]},castsBakedShadow=False)


def export_creature(biome,species,asset_id,offset,max_extent,animal):
    source=cells(asset_id)[offset:offset+(16 if animal else 8)]
    if len(source)!=(16 if animal else 8):raise ValueError('Missing poses '+species)
    # One scale per creature across every pose and direction: avoid frame-by-frame
    # zoom pulsation. Only the bottom anchor is translated into fixed 64px cells.
    s=min(max_extent/max(im.width for im,_ in source),max_extent/max(im.height for im,_ in source))
    out=Image.new('RGBA',(256,256 if animal else 128));frame_meta=[]
    for i,(im,p) in enumerate(source):
        scaled=im.resize((max(1,round(im.width*s)),max(1,round(im.height*s))),NEAREST)
        x=(64-scaled.width)//2;y=52-scaled.height
        out.alpha_composite(scaled,((i%4)*64+x,(i//4)*64+y))
        frame_meta.append({'index':i,'placement':[x,y,scaled.width,scaled.height],**p})
    save(biome,('animals/' if animal else 'monsters/')+species+'.png',out,kind='animal' if animal else 'monster',species=species,frameSize=[64,64],footAnchor=FOOT,frameCount=len(source),targetVisibleMax=max_extent,scalePerAllFrames=s,frames=frame_meta,rows=['DOWN','RIGHT','UP','LEFT'] if animal else ['directional_idle_DOWN_RIGHT_UP_LEFT','right_windup_attack_hit_downed'],animationStatus='source_keyposes; no invented full combat sequence',castsBakedShadow=False)


def contact_sheet():
    # Compare exported pixels at 2x with nearest sampling (same renderer rule).
    canvas=Image.new('RGB',(1440,6*260),(52,60,59));d=ImageDraw.Draw(canvas)
    for row,(biome,entries) in enumerate(CATALOG.items()):
        y=row*260;d.text((10,y+10),biome,fill='white')
        x=8
        for name in ['ground.png','tree.png']+[f'flora-{i}.png' for i in range(9)]+[f'structure-{i}.png' for i in range(3)]:
            path=OUT/biome/name
            if not path.exists():continue
            im=Image.open(path).convert('RGBA')
            if name.startswith('structure'):im=im.resize((128,144),NEAREST)
            if x+im.width>1435:break
            canvas.paste(im,(x,y+45+144-im.height),im);d.text((x,y+198),name.split('.')[0],fill='white');x+=im.width+9
        x=8
        for e in entries:
            if e['kind'] not in ['animal','monster']:continue
            im=Image.open(OUT/e['file']).crop((0,0,64,64)).resize((64,64),NEAREST)
            canvas.paste(im,(x,y+198),im);d.text((x+65,y+225),e['species'],fill='white');x+=190
    canvas.save(OUT/'runtime-contact-sheet.png')


def validate():
    failures=[];count=0
    for biome,entries in CATALOG.items():
        for e in entries:
            p=OUT/e['file'];res=RES/e['file'];im=Image.open(p).convert('RGBA');count+=1
            if p.read_bytes()!=res.read_bytes():failures.append(e['file']+': resource copy mismatch')
            if set(np.unique(np.asarray(im.getchannel('A'))))-{0,255}:failures.append(e['file']+': soft alpha')
            if e['kind']=='opaque_ground':
                if im.size!=(32,32) or im.getchannel('A').getextrema()!=(255,255):failures.append(e['file']+': ground transparency')
                px=np.array(im)
                if not np.array_equal(px[:,0],px[:,-1]) or not np.array_equal(px[0],px[-1]):failures.append(e['file']+': tile seam')
            if 'frameCount' in e:
                for i in range(e['frameCount']):
                    a=im.crop(((i%4)*64,(i//4)*64,(i%4+1)*64,(i//4+1)*64)).getchannel('A');box=a.getbbox()
                    if not box or box[0]<1 or box[2]>63 or box[1]<1 or box[3]!=52:failures.append(e['file']+f': frame {i} anchor/crop {box}')
            for c in e.get('sourceCrops',e.get('frames',[])):
                src=ROOT/c['source']
                if hashlib.sha256(src.read_bytes()).hexdigest()!=c['sourceSha256']:failures.append(str(src)+': source changed')
    report={'runtimePngCount':count,'biomeCount':len(CATALOG),'sourcePixelsModified':False,'allPassed':not failures,'failures':failures,'validation':['dimensions','hard_alpha','nonempty_frames','fixed_foot_baseline','ground_opacity_and_matching_edges','copied_resources_hash','original_source_hash'],'limitations':['Generated walking keyposes are a first runtime set, not fully authored animation cycles.','Terrain-module atlas is not an autotile adjacency atlas.','No collider or gameplay behavior is implied by this art export.']}
    (OUT/'validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    if failures:raise ValueError(str(failures))
    return report


def main():
    for batch in ['08','09','10','11']:
        for a in json.loads((PLAN/f'batch-{batch}-manifest.json').read_text())['assets']:ASSETS[a['id']]=a
    OUT.mkdir(parents=True,exist_ok=True)
    for biome,config in BIOMES.items():export_environment(biome,config)
    for args in ANIMALS:export_creature(*args[:3],0,args[3],True)
    for args in MONSTERS:export_creature(*args,False)
    for biome,entries in CATALOG.items():
        catalog={'biome':biome,'tilePixels':32,'sourceBatches':['08','09','10','11'],'sourcePixelsModified':False,'directionOrder':['DOWN','RIGHT','UP','LEFT'],'runtimeExportVersion':1,'entries':entries}
        for base in [OUT,RES]:
            (base/biome/'catalog.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n')
    (OUT/'manifest.json').write_text(json.dumps({'biomes':list(CATALOG),'pngCount':sum(map(len,CATALOG.values())),'resourceRoot':'mod/resources/mosslume/biomes','manualCutOverrides':REVIEWED,'entries':[e for entries in CATALOG.values() for e in entries]},ensure_ascii=False,indent=2)+'\n')
    contact_sheet();print(json.dumps(validate(),ensure_ascii=False))

if __name__=='__main__':main()
