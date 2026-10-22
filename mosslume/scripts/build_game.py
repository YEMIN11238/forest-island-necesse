#!/usr/bin/env python3
"""Export the approved art, build the existing island adapter, then package Mosslume."""
from pathlib import Path
import argparse
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT.parent / 'necesse-island'
PYTHON = Path.home() / '.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3'

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--skip-export', action='store_true')
    parser.add_argument('--rebuild-kernel', action='store_true', help='Only needed after recovered engine changes.')
    parser.add_argument('--install', action='store_true', help='Replace the daily-use app. By default package a separate S1 preview.')
    args = parser.parse_args()
    if not args.skip_export:
        subprocess.run([str(PYTHON), str(ROOT/'scripts/export_runtime_art.py')], check=True)
    for folder in ('items','players','residents','portraits','world','crops'):
        shutil.copytree(ROOT/'runtime-art'/folder, BASE/'mod/resources/mosslume'/folder, dirs_exist_ok=True)
    if args.rebuild_kernel:
        subprocess.run([sys.executable, str(BASE/'rebuild/build.py'), '--tag', 'mosslume'], check=True)
        for name in ('Necesse-source-built-1.3.3.jar','Server-source-built-1.3.3.jar'):
            shutil.copy2(BASE/'rebuild'/name, BASE/'runtime/Necesse.app/Contents/Resources'/name)
    subprocess.run([sys.executable, str(BASE/'scripts/build_mod.py')], check=True)
    package=[sys.executable, str(ROOT/'scripts/package_app.py'), '--skip-icon']
    if not args.install:
        package += ['--app-path', str(Path.home()/'Applications/Mosslume S1 Preview.app'),
                    '--profile-name','Mosslume-S1','--version','0.3.0','--skip-links']
    subprocess.run(package, check=True)

if __name__ == '__main__':
    main()
