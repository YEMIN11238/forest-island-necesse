#!/usr/bin/env python3
"""Run a native visual check using a fresh, disposable copy of the 0.2 profile."""
from pathlib import Path
import argparse
import json
import os
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--case', required=True)
    parser.add_argument('--legacy', action='store_true')
    parser.add_argument('--fullscreen', action='store_true')
    parser.add_argument('--hour', type=float, default=21)
    parser.add_argument('--compare-hour', type=float)
    parser.add_argument('--lighting-legacy', action='store_true')
    parser.add_argument('--width', type=int, default=1200)
    parser.add_argument('--height', type=int, default=900)
    parser.add_argument('--perf-seconds', type=float, default=10)
    parser.add_argument('--capture-seconds', type=float, default=8)
    parser.add_argument('--capture-width', type=int, default=1000)
    parser.add_argument('--capture-height', type=int, default=800)
    args = parser.parse_args()
    if '/' in args.case or args.case in ('', '.', '..'):
        parser.error('--case must be a directory name')
    case = ROOT / 'build/evidence-s1' / args.case
    if (case / 'manifest.json').exists():
        raise SystemExit('This evidence case is complete; choose a new case name.')
    case.mkdir(parents=True, exist_ok=True)
    profile = Path.home() / 'Library/Application Support' / ('Mosslume-QA-S1-' + args.case)
    if profile.exists():
        raise SystemExit('Test profile already exists; choose a fresh case name.')
    shutil.copytree(ROOT / 'build/baseline-0.2/profile', profile)
    app = Path.home() / 'Applications/Mosslume S1 Preview.app'
    props = {
        'mosslume.visualProbe': str(case),
        'mosslume.visualProbe.hour': args.hour,
        'mosslume.visualProbe.windowWidth': args.width,
        'mosslume.visualProbe.windowHeight': args.height,
        'mosslume.visualProbe.perfSeconds': args.perf_seconds,
        'mosslume.visualProbe.captureSeconds': args.capture_seconds,
        'mosslume.visualProbe.captureFps': 30,
        'mosslume.visualProbe.captureWidth': args.capture_width,
        'mosslume.visualProbe.captureHeight': args.capture_height,
    }
    if args.legacy:
        props.update({'mosslume.visual.legacy': 'true', 'mosslume.wind.legacy': 'true',
                      'mosslume.pixel.legacy': 'true'})
    if args.fullscreen:
        props['mosslume.visualProbe.fullscreen'] = 'true'
    if args.lighting_legacy:
        props['mosslume.lighting.legacy'] = 'true'
    if args.compare_hour is not None:
        props['mosslume.visualProbe.compareHour'] = args.compare_hour
    env = os.environ.copy()
    env['MOSSLUME_PROFILE'] = str(profile)
    env['JAVA_TOOL_OPTIONS'] = ' '.join('-D' + k + '=' + str(v) for k, v in props.items())
    (case / 'launch.json').write_text(json.dumps({'profile': str(profile), 'app': str(app),
        'properties': props, 'entry': 'Same native enterIsland callback as the title button'}, indent=2))
    print('Running native probe:', case, flush=True)
    with (case / 'launcher.log').open('w') as log:
        run = subprocess.run([str(app / 'Contents/MacOS/MosslumeLauncher'), '-mosslumeqa'],
                             env=env, stdout=log, stderr=subprocess.STDOUT, timeout=240)
    manifest = case / 'manifest.json'
    if run.returncode != 0 or not manifest.exists():
        raise SystemExit('Probe did not complete; inspect ' + str(profile / 'runtime.log'))
    result = json.loads(manifest.read_text())
    if not result['captureComplete']:
        raise SystemExit('Frame capture incomplete: ' + str(manifest))
    print('Native probe completed:', case, flush=True)


if __name__ == '__main__':
    main()
