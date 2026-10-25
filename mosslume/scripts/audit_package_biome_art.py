#!/usr/bin/env python3
"""Validate source-art files/preview and optionally package them. Never edit pixels.

Use after all generation workers have finished writing source manifests.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re
import shutil
import subprocess
import tempfile
import zipfile
from build_biome_art_preview import ROOT, build


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def audit(batches, assets, preview):
    checks = []
    for asset in assets:
        path = ROOT / asset['file']
        prompt = ROOT / asset['promptFile']
        original = Path(asset.get('sourceFile', ''))
        original_exists = original.is_file()
        checks.append({'id': asset['id'], 'file': asset['file'], 'sha256': asset['sha256'],
                       'copyMatchesGeneratedSource': sha256(original) == asset['sha256'] if original_exists else None,
                       'generatedOriginalAvailable': original_exists,
                       'dimensions': [asset['width'], asset['height']],
                       'sourceMode': asset['sourceMode'],
                       'hasTransparentPixels': asset['alpha']['hasFullyTransparentPixels'],
                       'hasSavedPrompt': prompt.is_file() and bool(prompt.read_text().strip()),
                       'emptyPreviewRegions': asset['emptyRegions'],
                       'contentTouchesCanvasEdge': asset['contentTouchesCanvasEdge'],
                       'manualRegionReviewNeeded': asset['manualRegionReviewNeeded'],
                       'regionReview': asset['regionReview']})
    html = preview.read_text()
    scripts = re.findall(r'<script(?:\s[^>]*)?>(.*?)</script>', html, re.S)
    js_result = {'checked': False, 'passed': None, 'reason': 'node unavailable'}
    if shutil.which('node'):
        with tempfile.TemporaryDirectory(prefix='mosslume-art-js-') as temporary:
            for i, script in enumerate(scripts):
                target = Path(temporary) / f'script-{i}.js'
                target.write_text(script)
                result = subprocess.run(['node', '--check', str(target)], capture_output=True, text=True)
                if result.returncode:
                    raise RuntimeError('Preview JavaScript syntax check failed: ' + result.stderr[:1200])
        js_result = {'checked': True, 'passed': True, 'scripts': len(scripts)}
    local_refs = []
    for asset in assets:
        for key in ('file', 'promptFile'):
            rel_url = '../' + asset[key].removeprefix('art/')
            resolved = (preview.parent / rel_url).resolve()
            local_refs.append(resolved.is_file() and resolved == (ROOT / asset[key]).resolve())
    result = {'date': '2026-10-23', 'batches': batches,
              'sourceAtlases': len(assets),
              'requestedSourceRegions': sum(a.get('plannedComponents', a['rows'] * a['columns']) for a in assets),
              'sourcePixelsModified': False, 'gameIntegrated': False,
              'checks': checks,
              'fileIntegrityPassed': all(c['copyMatchesGeneratedSource'] is True for c in checks),
              'promptsPresent': all(c['hasSavedPrompt'] for c in checks),
              'allSourceReferencesExist': all(local_refs),
              'previewJavaScriptSyntax': js_result,
              'previewHasEmbeddedImageBase64': 'data:image/' in html,
              'manualRegionReview': [c['id'] for c in checks if c['manualRegionReviewNeeded']],
              'browserRuntimeQA': {'performed': False, 'reason': 'File/JavaScript checks only; dynamic browser inspection is separate.'},
              'notValidated': ['seamless/autotile connections', 'exact runtime scale and pivots',
                               'complete animations', 'emission and shadow maps',
                               'game collision and runtime integration']}
    return result


def package(batches, assets, preview, audit_path, zip_path):
    paths = {preview, audit_path, Path(__file__).resolve(), Path(__file__).with_name('build_biome_art_preview.py').resolve()}
    # Include the human review and coverage contract beside the source files.
    for name in ('biome-scale-style-review-v1.json', 'biome-scale-style-review-v1.txt',
                 'biome-assets-browser-check-v2.json', 'biome-assets-backlog-v1.json',
                 'biome-assets-coverage-v2.json', '本轮六类地形素材清单.txt'):
        extra = ROOT / 'art/planning' / name
        if extra.is_file():
            paths.add(extra)
    for batch in batches:
        for extra in (ROOT / 'art/planning').glob(f'batch-{batch}-*source-*'):
            if extra.suffix in ('.json', '.txt'):
                paths.add(extra)
    for batch in batches:
        paths.add(ROOT / f'art/planning/batch-{batch}-generated-sources.json')
        paths.add(ROOT / f'art/planning/batch-{batch}-manifest.json')
    for asset in assets:
        paths.update([ROOT / asset['file'], ROOT / asset['promptFile']])
        for name in ('SOURCE-NOTES.txt', 'source-audit.json'):
            extra = (ROOT / asset['file']).parent / name
            if extra.is_file():
                paths.add(extra)
    zip_path.parent.mkdir(parents=True, exist_ok=True)
    readme = ('Mosslume 多地形素材源稿\n\n'
              '解压后打开 art/previews/biome-assets-v1.html；请保留目录结构。\n'
              'PNG 为内置 imagegen 生成源稿，未由本脚本重绘或修改像素。每张附原始提示词。\n'
              '各图集已统一美术方向与目标尺度；实际游戏比例、素材切片、无缝地形、脚点、\n'
              '完整动物/怪物动作、建筑分层及发光/投影需要后续导出和游戏内验收。\n'
              '混合物种图集不应直接按整张行列循环播放。\n'
              '人工分割复核标记见 art/planning/biome-assets-source-audit-v2.json。\n'
              '本包是素材源文件，不是可运行游戏，也不表示这些地形已经加入世界。\n')
    with zipfile.ZipFile(zip_path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=5) as archive:
        archive.writestr('README-素材源稿.txt', readme)
        for path in sorted(paths):
            if not path.is_file():
                raise FileNotFoundError(path)
            archive.write(path, path.relative_to(ROOT))
    with zipfile.ZipFile(zip_path) as archive:
        bad = archive.testzip()
        if bad:
            raise RuntimeError('ZIP integrity failure: ' + bad)
        return {'archive': str(zip_path), 'archiveFiles': len(archive.namelist()), 'zipBytes': zip_path.stat().st_size}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--batches', nargs='+', default=['06', '07', '08', '09', '10'])
    parser.add_argument('--zip', dest='zip_path', default=None,
                        help='Project-relative path, e.g. art/exports/biome-source-batches-06-10-v1.zip')
    parser.add_argument('--audit', default='art/planning/biome-assets-source-audit-v2.json')
    args = parser.parse_args()
    assets, preview = build(args.batches)
    result = audit(args.batches, assets, preview)
    audit_path = ROOT / args.audit
    audit_path.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    summary = {k: result[k] for k in ('sourceAtlases', 'requestedSourceRegions', 'fileIntegrityPassed',
                                     'promptsPresent', 'allSourceReferencesExist', 'previewJavaScriptSyntax', 'manualRegionReview')}
    summary['preview'] = str(preview)
    summary['audit'] = str(audit_path)
    if args.zip_path:
        summary.update(package(args.batches, assets, preview, audit_path, ROOT / args.zip_path))
    print(json.dumps(summary, ensure_ascii=False))
    if (not result['promptsPresent'] or not result['allSourceReferencesExist']
            or not result['fileIntegrityPassed']
            or any(not c['hasTransparentPixels'] or c['emptyPreviewRegions'] for c in result['checks'])):
        raise SystemExit(1)


if __name__ == '__main__':
    main()
