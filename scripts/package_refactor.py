#!/usr/bin/env python3
"""Build and update the single Mosslume application, preserving its existing Life profile."""
from pathlib import Path
import argparse
import importlib.util
import subprocess
import sys
import platform

ROOT=Path(__file__).resolve().parents[1]

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--skip-build',action='store_true')
    p.add_argument('--app-path',type=Path,default=Path.home()/'Applications/Mosslume.app')
    native=ROOT/'runtime/java/temurin-17.0.19+10-macos-aarch64/Contents/Home'
    p.add_argument('--java-runtime',type=Path,default=native if platform.machine()=='arm64' and native.is_dir() else None)
    p.add_argument('--gc',choices=['g1','zgc'],default='g1')
    p.add_argument('--profile-name',default='Mosslume-Life-Preview')
    p.add_argument('--bundle-id',default='game.mosslume.lifepreview')
    p.add_argument('--version',default='0.5.2')
    args=p.parse_args()
    spec=importlib.util.spec_from_file_location('mosslume_packager',ROOT/'mosslume/scripts/package_app.py')
    package=importlib.util.module_from_spec(spec);spec.loader.exec_module(package)
    package.ROOT=ROOT/'mosslume';package.BASE=ROOT
    package.ensure_app_stopped(args.app_path)
    if not args.skip_build:
        subprocess.run([sys.executable,str(ROOT/'scripts/build_mod.py')],check=True)
    sys.argv=[str(spec.origin),'--skip-icon','--skip-links','--no-profile-migration',
        '--app-path',str(args.app_path.expanduser()),
        '--profile-name',args.profile_name,'--version',args.version]
    if args.bundle_id:sys.argv+=['--bundle-id',args.bundle_id]
    if args.java_runtime:sys.argv+=['--java-runtime',str(args.java_runtime.expanduser())]
    sys.argv+=['--gc',args.gc]
    package.main()

if __name__=='__main__':main()
