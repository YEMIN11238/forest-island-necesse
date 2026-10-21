#!/usr/bin/env python3
"""Rebuild the complete recovered kernel, compile the island adapter and package the Mac entry."""
from pathlib import Path
import subprocess, sys, shutil, os
root = Path(__file__).resolve().parents[1]
def run(*args):
    subprocess.run([sys.executable, *map(str,args)], check=True)
run(root/'rebuild/build.py', '--tag', 'project')
resources = root/'runtime/Necesse.app/Contents/Resources'
for name in ['Necesse-source-built-1.3.3.jar','Server-source-built-1.3.3.jar']:
    target = resources/name
    temporary = resources/(name+'.new')
    shutil.copy2(root/'rebuild'/name, temporary)
    os.replace(temporary, target)
run(root/'scripts/build_mod.py')
run(root/'scripts/package_app.py')
