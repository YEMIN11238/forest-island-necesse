#!/usr/bin/env python3
"""Build and package the 0.4 branch with its own profile, preserving the stable preview."""
from pathlib import Path
import argparse
import importlib.util
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[1]

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--skip-build',action='store_true')
    args=p.parse_args()
    if not args.skip_build:
        subprocess.run([sys.executable,str(ROOT/'scripts/build_mod.py')],check=True)
    spec=importlib.util.spec_from_file_location('mosslume_packager',ROOT/'mosslume/scripts/package_app.py')
    package=importlib.util.module_from_spec(spec);spec.loader.exec_module(package)
    package.ROOT=ROOT/'mosslume';package.BASE=ROOT
    sys.argv=[str(spec.origin),'--skip-icon','--skip-links','--no-profile-migration',
        '--app-path',str(Path.home()/'Applications/Mosslume 0.4 Preview.app'),
        '--profile-name','Mosslume-0.4','--version','0.4.0']
    package.main()

if __name__=='__main__':main()
