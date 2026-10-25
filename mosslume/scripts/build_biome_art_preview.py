#!/usr/bin/env python3
"""Inspect generated source PNGs and build a local gallery. Never alter pixels.

Source manifests are read-only. Derived manifests record tentative *preview* regions,
not production slices. Mixed-species sheets require explicit actor metadata for
animation; otherwise they remain a static source atlas.
"""
from pathlib import Path
import argparse
import hashlib
import json
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
BIOMES = {
    'forest': '森林', 'woodland': '森林', 'meadow': '草甸', 'coast': '海岸',
    'coastal': '海岸', 'mountain': '山地', 'cave': '洞穴', 'wetland': '湿地',
    'mushroom_forest': '蘑菇林', 'mushroom': '蘑菇林', 'ruins': '遗迹',
    'ruins_overlay': '遗迹', 'snowfield': '雪地', 'snow': '雪地',
    'desert': '沙漠', 'volcano': '火山', 'shared': '通用',
}
CATEGORIES = ('地形', '建筑', '植物', '动物', '怪物', '道具', '特效')


def empty_runs(values):
    result, start = [], None
    for i, value in enumerate([*values, 1]):
        if not value and start is None:
            start = i
        if value and start is not None:
            result.append((start, i))
            start = None
    return result


def cuts(values, count):
    """Prefer transparent gutters near each grid division; report all fallbacks."""
    size = len(values)
    result, reviews = [0], []
    gaps = [(a, b) for a, b in empty_runs(values) if b - a >= 3]
    for i in range(1, count):
        target = size * i / count
        radius = size / count * .38
        candidates = [(a + b) // 2 for a, b in gaps
                      if target - radius <= (a + b) / 2 <= target + radius
                      and (a + b) // 2 > result[-1]]
        if candidates:
            result.append(min(candidates, key=lambda x: abs(x - target)))
        else:
            cut = max(result[-1] + 1, round(target))
            result.append(cut)
            reviews.append({'division': i, 'position': cut, 'reason': 'no_clear_gutter_near_grid_division'})
    return [*result, size], reviews


def category(a):
    explicit = a.get('category')
    if explicit:
        return {'植物': '植被', '植物资源': '植被', '道具特效': '道具与特效'}.get(explicit, explicit)
    key = a['id']
    if 'vfx' in key:
        return '特效'
    if any(k in key for k in ('layers', 'entrance', 'shelter', 'building')):
        return '建筑'
    if any(k in key for k in ('deer', 'chicken', 'goat')) or key.startswith('cave-bat'):
        return '动物'
    if any(k in key for k in ('beast', 'crab', 'beetle')):
        return '怪物'
    if 'props' in key:
        return '道具'
    return '地形'


def biomes(a):
    raw = a.get('biomes') or a.get('biome')
    if raw:
        return [BIOMES.get(x, x) for x in (raw if isinstance(raw, list) else [raw])]
    key = a['id']
    result = [label for token, label in (
        ('woodland', '森林'), ('forest', '森林'), ('meadow', '草甸'), ('coast', '海岸'),
        ('fishing', '海岸'), ('mountain', '山地'), ('mine-', '山地'), ('cave', '洞穴')) if token in key]
    return list(dict.fromkeys(result or ['通用']))


def inspect(a):
    path = ROOT / a['file']
    with Image.open(path) as original:
        original_mode = original.mode
        im = original.convert('RGBA')
    alpha = im.getchannel('A')
    mask = alpha.point(lambda v: 255 if v >= 32 else 0)
    _, ys = mask.getprojection()
    yc, row_reviews = cuts(ys, a['rows'])
    regions, column_reviews, crossing_cuts = [], [], []
    for y in yc[1:-1]:
        if mask.crop((0, max(0, y - 1), im.width, min(im.height, y + 1))).getbbox():
            crossing_cuts.append({'axis': 'row', 'position': y})
    for row in range(a['rows']):
        y0, y1 = yc[row:row + 2]
        xs, _ = mask.crop((0, y0, im.width, y1)).getprojection()
        xc, reviews = cuts(xs, a['columns'])
        column_reviews.extend({'row': row, **review} for review in reviews)
        for x in xc[1:-1]:
            if mask.crop((max(0, x - 1), y0, min(im.width, x + 1), y1)).getbbox():
                crossing_cuts.append({'axis': 'column', 'row': row, 'position': x})
        for col in range(a['columns']):
            x0, x1 = xc[col:col + 2]
            bbox = mask.crop((x0, y0, x1, y1)).getbbox()
            region = {'row': row, 'column': col,
                      'sourceRegion': [x0, y0, x1 - x0, y1 - y0],
                      'visibleBounds': [x0 + bbox[0], y0 + bbox[1], bbox[2] - bbox[0], bbox[3] - bbox[1]] if bbox else None}
            labels = a.get('cellLabels', [])
            if labels and isinstance(labels[0], list):
                labels = [label for row_labels in labels for label in row_labels]
            index = row * a['columns'] + col
            if index < len(labels):
                region['label'] = labels[index]
            regions.append(region)
    histogram = alpha.histogram()
    outer = mask.getbbox()
    touches_edge = bool(outer and (outer[0] == 0 or outer[1] == 0 or outer[2] == im.width or outer[3] == im.height))
    review_needed = bool(row_reviews or column_reviews or crossing_cuts or any(not r['visibleBounds'] for r in regions))
    return {**a, 'category': category(a), 'biomeLabels': biomes(a), 'width': im.width,
            'height': im.height, 'sourceMode': original_mode,
            'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
            'alpha': {'hasFullyTransparentPixels': histogram[0] > 0,
                      'fullyTransparentPixels': histogram[0],
                      'faintFringePixelsAlpha1To8': sum(histogram[1:9]),
                      'maximum': alpha.getextrema()[1]},
            'regions': regions, 'emptyRegions': sum(r['visibleBounds'] is None for r in regions),
            'regionMethod': 'Alpha projection at threshold 32; tentative preview coordinates only',
            'regionReview': {'rowFallbacks': row_reviews, 'columnFallbacks': column_reviews,
                             'foregroundCrossesCuts': crossing_cuts},
            'contentTouchesCanvasEdge': touches_edge,
            'manualRegionReviewNeeded': review_needed,
            'productionExportValidated': False, 'integratedIntoGame': False}


HTML = r'''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Mosslume · 多地形素材库</title><style>
:root{color-scheme:dark;--bg:#101b1c;--panel:#182a2a;--text:#eee8d5;--muted:#adc1b7;--line:#38504a;--gold:#e8bd76}*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:15px/1.65 -apple-system,BlinkMacSystemFont,"PingFang SC",sans-serif}main{max-width:1400px;margin:auto;padding:36px 26px 70px}h1{font-size:32px;margin:4px 0;color:var(--gold)}h2{font-size:21px;margin:0 0 12px}p{color:var(--muted);margin:8px 0}.small{font-size:12px}header{padding-bottom:22px;border-bottom:1px solid var(--line)}.tag{display:inline-block;padding:3px 9px;border:1px solid var(--line);border-radius:20px;margin:5px 6px 0 0;color:var(--muted)}.controls{display:flex;gap:9px;align-items:center;flex-wrap:wrap;margin:12px 0}button,select{font:inherit;background:#243b37;color:var(--text);border:1px solid #567061;padding:7px 10px;border-radius:7px;cursor:pointer}button[aria-pressed=true]{border-color:var(--gold);background:#4b402c}.demos{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin:26px 0}.panel,article{padding:20px;background:var(--panel);border:1px solid var(--line);border-radius:14px;overflow:hidden}.stage,.sheet{background-color:#233330;background-image:conic-gradient(#2d403a 25%,transparent 0 50%,#2d403a 0 75%,transparent 0);background-size:20px 20px;border-radius:8px}.stage{min-height:260px;display:grid;place-items:center}canvas{max-width:100%;height:auto;image-rendering:pixelated}.catalog{display:grid;grid-template-columns:1fr 1fr;gap:20px}.sheet{padding:8px;min-height:240px;display:flex;align-items:center;justify-content:center}.sheet img{width:100%;height:370px;object-fit:contain;image-rendering:pixelated}article h2{font-size:18px;margin:12px 0 4px}a{color:var(--gold)}article .links{display:flex;gap:18px;font-size:13px}details{margin-top:8px;color:var(--muted);font-size:12px}summary{cursor:pointer}nav{position:sticky;top:0;padding:10px 0;background:var(--bg);z-index:2;border-bottom:1px solid var(--line)}footer{margin-top:30px;border-top:1px solid var(--line);padding-top:15px;color:var(--muted)}.status{color:#e7c98f}.hidden{display:none!important}.scale{border-left:2px solid var(--line);padding-left:10px}.warning{color:#f1c785}.empty{padding:40px;text-align:center;color:var(--muted)}@media(max-width:800px){.demos,.catalog{grid-template-columns:1fr}main{padding:22px 14px}.sheet img{height:300px}h1{font-size:27px}}
</style><main><header><div class="small">MOSSLUME / BIOME SOURCE ART</div><h1>多地形素材库</h1><p id="intro"></p><span class="tag">内置 imagegen 生成</span><span class="tag">原始 Alpha 保留</span><span class="tag status">源图阶段 · 尚未接入游戏</span><p class="small">图集按统一视角、轮廓和目标尺度制作；展示时会适配卡片大小，不能据此判断游戏内的实际比例。地形无缝连接、建筑装配和动作衔接仍需运行时整理。</p></header>
<div class="demos"><section class="panel"><h2>动物关键姿态</h2><div class="stage"><canvas id="animalCanvas" width="512" height="300" aria-label="动物关键姿态"></canvas></div><div class="controls"><select id="animal" aria-label="动物"></select><select id="direction" aria-label="朝向"><option value="0">向下</option><option value="1">向右</option><option value="2">向上</option><option value="3">向左</option></select><button id="play" aria-pressed="false">播放动作关键帧</button></div><p class="small" id="animalNote">仅播放同一动物的已声明关键姿态。混合图集按物种分别展示；完整步幅、脚点和衔接尚未校准。</p></section>
<section class="panel"><h2>怪物造型与战斗姿态</h2><div class="stage"><canvas id="monsterCanvas" width="512" height="300" aria-label="怪物关键姿态"></canvas></div><div class="controls"><select id="monster" aria-label="怪物"></select><select id="pose" aria-label="怪物姿态"><option value="0">正面</option><option value="1">右侧</option><option value="2">背面</option><option value="3">左侧</option><option value="4">攻击预备</option><option value="5">攻击关键姿态</option><option value="6">受击</option><option value="7">倒地</option></select></div><p class="small" id="monsterNote">混合怪物图集按物种分别查看。静态关键姿态不等于完整攻击动画。</p></section></div>
<nav aria-label="素材筛选"><div class="controls" id="biomeFilters"><span class="small">地形</span></div><div class="controls" id="categoryFilters"><span class="small">种类</span></div><div class="controls"><label>检查背景 <select id="background"><option value="checker">透明棋盘</option><option value="light">浅色</option><option value="dark">深色</option></select></label><label>图集显示 <select id="atlasMode"><option value="fit">适应卡片</option><option value="native">原图比例（横向滚动）</option></select></label></div></nav><p id="count" role="status"></p><div id="catalog" class="catalog"></div><footer>所有 PNG 和提示词均已保存。分割坐标只用于检查源稿，标记需要复核的图集不参与关键帧演示。素材尚未完成自动拼接、动画帧注册、发光/投影分离、碰撞和游戏内验收。原图没有通过脚本修改像素。</footer></main>
<script>const ASSETS=__DATA__;const loaded={};window.previewReady=false;window.previewErrors=[];window.addEventListener('error',e=>window.previewErrors.push(e.message));
const $=id=>document.getElementById(id),esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));let playing=false,last=0,step=0,activeBiome='本轮新增',activeCategory='全部';const cycle=[1,2,3,2],animalActors=[],monsterActors=[];
function src(a){return '../'+a.file.replace(/^art\//,'')}
function sourceUrl(path){return '../'+path.replace(/^art\//,'')}
function scaleLabel(a){const v=a.targetScale;if(!v||typeof v==='string')return '统一目标比例；接入时校准';const parts=[];const range=x=>Array.isArray(x)?x.join(' × '):x;if(v.tilePixels||v.baseTilePixels)parts.push('1 格 = '+(v.tilePixels||v.baseTilePixels)+' px');if(v.footprint)parts.push('占地 '+range(v.footprint)+' 格');if(v.height)parts.push('高 '+v.height+' 格');if(v.treeHeightTiles)parts.push('树高 '+v.treeHeightTiles+' 格');if(v.assembledTreePixels)parts.push('整树 '+range(v.assembledTreePixels)+' px');if(v.terrainFootprintTiles)parts.push('地形组件 '+v.terrainFootprintTiles+' 格');if(v.cliffHeightTiles)parts.push('崖高 '+v.cliffHeightTiles+' 格');if(v.smallPropsTiles)parts.push('小道具 '+v.smallPropsTiles+' 格');if(v.smallFloraWidthPixels)parts.push('小植物宽 '+range(v.smallFloraWidthPixels)+' px');return parts.join(' · ')+'（目标比例，待接入校准）'}
function notes(a){return [...(a.reviewNotes||[]),...(a.limitations||[])]}
function drawPose(canvasId,actor,index){const c=$(canvasId),ctx=c.getContext('2d');ctx.clearRect(0,0,c.width,c.height);ctx.imageSmoothingEnabled=false;if(!actor){ctx.fillStyle='#adc1b7';ctx.font='16px sans-serif';ctx.textAlign='center';ctx.fillText('暂无通过分割检查的关键姿态',c.width/2,150);return}const a=actor.asset,r=a.regions[index],b=r?.visibleBounds;if(!b||!loaded[a.id])return;const peers=actor.indices.map(i=>a.regions[i]?.visibleBounds).filter(Boolean);const mw=Math.max(...peers.map(x=>x[2])),mh=Math.max(...peers.map(x=>x[3]));const scale=Math.min(330/mw,230/mh);ctx.drawImage(loaded[a.id],...b,Math.round(c.width/2-b[2]*scale/2),Math.round(270-b[3]*scale),Math.round(b[2]*scale),Math.round(b[3]*scale));c.dataset.asset=a.id;c.dataset.actor=actor.name;c.dataset.pose=String(index)}
function deriveActors(a){if(a.manualRegionReviewNeeded)return;if(Array.isArray(a.previewActors)){for(const actor of a.previewActors){if(!Array.isArray(actor.indices)||!actor.indices.length)continue;const item={...actor,asset:a,name:actor.name||a.name};(a.category==='动物'?animalActors:monsterActors).push(item)}return}const layout=typeof a.layout==='string'?a.layout:'';const kind=(a.previewKind||'')+' '+layout;const species=a.species||[];if(a.category==='怪物'&&a.columns===4&&a.rows===4){for(let s=0;s<2;s++){const spec=species[s],obj=typeof spec==='object'?spec:{};const idleRow=obj.idleRow??s*2,combatRow=obj.combatRow??s*2+1;monsterActors.push({asset:a,name:obj.name||(typeof spec==='string'?spec:a.name+' · 物种 '+(s+1)),indices:[0,1,2,3].map(i=>idleRow*4+i).concat([0,1,2,3].map(i=>combatRow*4+i))})}return}if(a.category==='怪物'&&a.rows===2&&a.columns===4){monsterActors.push({asset:a,name:a.name,indices:[0,1,2,3,4,5,6,7]});return}if(a.category==='动物'&&a.rows===4&&a.columns===4&&!/mixed|multi|双|混合/.test(kind)&&!species.length){animalActors.push({asset:a,name:a.name,indices:[...Array(16).keys()],sequence:a.previewSequence||cycle,directional:true});return}}
function animal(){const actor=animalActors[Number($('animal').value)];$('direction').disabled=!actor?.directional;$('play').disabled=!actor?.sequence;let index=actor?.indices[0]??0;if(actor){if(actor.directional){const row=Number($('direction').value),seq=actor.sequence||cycle;index=actor.indices[row*4+(playing?seq[step%seq.length]:0)]}else if(playing&&actor.sequence){index=actor.indices[actor.sequence[step%actor.sequence.length]]}else{index=actor.indices[Number($('direction').value)%actor.indices.length]}}drawPose('animalCanvas',actor,index);if(actor)$('animalNote').textContent=actor.sequence?'同一物种关键姿态演示；播放速度仅用于检查，不是最终动画。':'此混合图集只展示已声明的单物种静态姿态，未自动拼接为动作。'}
function monster(){const actor=monsterActors[Number($('monster').value)],pose=Number($('pose').value);drawPose('monsterCanvas',actor,actor?.indices[pose]??actor?.indices[0]??0)}
function animate(t){if(playing&&t-last>=160){last=t;step++;animal()}requestAnimationFrame(animate)}
function filter(){document.querySelectorAll('article').forEach(el=>{const a=ASSETS.find(a=>a.id===el.dataset.id);el.classList.toggle('hidden',(activeCategory!=='全部'&&a.category!==activeCategory)||(activeBiome==='本轮新增'?Number(a.sourceBatch)<8:(activeBiome!=='全部'&&!a.biomeLabels.includes(activeBiome))))});document.querySelectorAll('[data-biome]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.biome===activeBiome)));document.querySelectorAll('[data-filter]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.filter===activeCategory)));const visible=[...document.querySelectorAll('article')].filter(x=>!x.classList.contains('hidden')).length;$('count').textContent=`${activeBiome} / ${activeCategory} · ${visible} 张源图集`}
function option(select,value,label){const o=document.createElement('option');o.value=String(value);o.textContent=label;$(select).append(o)}
function filterButtons(id,values,attribute){for(const value of ['全部',...values]){const button=document.createElement('button');button.textContent=value;button.dataset[attribute]=value;button.onclick=()=>{if(attribute==='biome')activeBiome=value;else activeCategory=value;filter()};$(id).append(button)}}
async function init(){for(const a of ASSETS){const card=document.createElement('article');card.dataset.category=a.category;card.dataset.id=a.id;const warning=a.manualRegionReviewNeeded?'<p class="small warning">组件分割需要人工复核；此图集不参与关键帧演示。</p>':'';card.innerHTML=`<div class="sheet"><img src="${esc(src(a))}" alt="${esc(a.name)}" loading="eager"></div><h2>${esc(a.name)}</h2><div>${a.biomeLabels.map(b=>`<span class="tag small">${esc(b)}</span>`).join('')}<span class="tag small">${esc(a.category)}</span></div><p class="small">${a.columns} × ${a.rows} 源组件布局 · ${a.width} × ${a.height} px</p><p class="small scale">${esc(scaleLabel(a))}</p>${warning}<div class="links"><a href="${esc(src(a))}" target="_blank">查看原图</a><a href="${esc(sourceUrl(a.promptFile))}" target="_blank">生成提示词</a></div><details><summary>组件说明与制作状态</summary><p>${esc((a.rowLabels||[]).join(' / '))}</p><p>${esc((a.cellLabels||[]).flat().join(' · '))}</p>${notes(a).length?`<p class="warning">${esc(notes(a).join("；"))}</p>`:""}<p>所选生成源图已原样保存。分割为预览用途；地形无缝性、脚点、完整动作和运行时比例尚未验收。</p></details>`;$('catalog').append(card);deriveActors(a)}
animalActors.forEach((a,i)=>option('animal',i,a.name));monsterActors.forEach((a,i)=>option('monster',i,a.name));filterButtons('biomeFilters',['本轮新增',...new Set(ASSETS.flatMap(a=>a.biomeLabels))],'biome');filterButtons('categoryFilters',[...new Set(ASSETS.map(a=>a.category))],'filter');await Promise.all(ASSETS.map(a=>new Promise((resolve,reject)=>{const im=new Image();im.onload=()=>{loaded[a.id]=im;resolve()};im.onerror=()=>reject(new Error('图片未载入：'+a.id));im.src=src(a)})));$('intro').textContent=`本轮新增 ${ASSETS.filter(a=>Number(a.sourceBatch)>=8).length} 张，累计 ${ASSETS.length} 张源图集；湿地、蘑菇林、遗迹、雪地、沙漠与火山可按地形筛选。`;$('animal').value=String(Math.max(0,animalActors.findIndex(x=>Number(x.asset.sourceBatch)>=8)));$('monster').value=String(Math.max(0,monsterActors.findIndex(x=>Number(x.asset.sourceBatch)>=8)));animal();monster();$('animal').onchange=()=>{step=0;animal()};$('direction').onchange=animal;$('monster').onchange=monster;$('pose').onchange=monster;$('play').onclick=()=>{playing=!playing;$('play').setAttribute('aria-pressed',String(playing));$('play').textContent=playing?'暂停关键帧':'播放动作关键帧';animal()};$('background').onchange=()=>{const v=$('background').value;document.querySelectorAll('.stage,.sheet').forEach(el=>{el.style.backgroundImage=v==='checker'?'':'none';el.style.backgroundColor=v==='light'?'#dce4dc':v==='dark'?'#0a1118':''})};$('atlasMode').onchange=()=>{const native=$('atlasMode').value==='native';document.querySelectorAll('.sheet').forEach(el=>{el.style.overflow=native?'auto':'';el.style.justifyContent=native?'flex-start':''});document.querySelectorAll('.sheet img').forEach(el=>{el.style.width=native?'auto':'';el.style.height=native?'auto':'';el.style.maxWidth=native?'none':''})};filter();window.previewReady=true;window.previewSummary={atlases:ASSETS.length,biomes:[...new Set(ASSETS.flatMap(a=>a.biomeLabels))],categories:[...new Set(ASSETS.map(a=>a.category))],gameIntegrated:false,animalActors:animalActors.length,monsterActors:monsterActors.length,manualCutReview:ASSETS.filter(a=>a.manualRegionReviewNeeded).map(a=>a.id)};requestAnimationFrame(animate)}init().catch(e=>{window.previewErrors.push(e.message);$('count').textContent=e.message});</script></html>'''


def build(batches, derived=True, output='art/previews/biome-assets-v1.html'):
    assets = []
    for batch in batches:
        source = ROOT / f'art/planning/batch-{batch}-generated-sources.json'
        metadata = json.loads(source.read_text())
        inspected = [inspect({**entry, 'sourceBatch': batch}) for entry in metadata['assets']]
        manifest = {'project': 'Mosslume', 'batch': batch, 'date': metadata['date'],
                    'generator': metadata['generator'], 'integratedIntoGame': False,
                    'productionExportValidated': False, 'sourcePixelsModified': False,
                    'sourceAtlasCount': len(inspected),
                    'requestedSourceRegions': sum(x.get('plannedComponents', x['rows'] * x['columns']) for x in inspected),
                    'assets': inspected}
        if derived:
            (ROOT / f'art/planning/batch-{batch}-manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
        assets.extend(inspected)
    if len({a['id'] for a in assets}) != len(assets):
        raise ValueError('Duplicate atlas IDs across batches')
    out = ROOT / output
    out.parent.mkdir(parents=True, exist_ok=True)
    data = json.dumps(assets, ensure_ascii=False).replace('</', '<\\/')
    out.write_text(HTML.replace('__DATA__', data), encoding='utf-8')
    return assets, out


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--batches', nargs='+', default=['06', '07', '08', '09', '10'])
    parser.add_argument('--no-derived-manifests', action='store_true')
    parser.add_argument('--output', default='art/previews/biome-assets-v1.html')
    args = parser.parse_args()
    assets, out = build(args.batches, not args.no_derived_manifests, args.output)
    print(json.dumps({'preview': str(out), 'atlases': len(assets),
                      'emptyRegions': sum(a['emptyRegions'] for a in assets),
                      'manualCutReview': [a['id'] for a in assets if a['manualRegionReviewNeeded']],
                      'allHaveClearPixels': all(a['alpha']['hasFullyTransparentPixels'] for a in assets)}, ensure_ascii=False))


if __name__ == '__main__':
    main()
