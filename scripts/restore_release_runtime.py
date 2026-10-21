from pathlib import Path
import argparse, shutil
p=argparse.ArgumentParser();p.add_argument('--app',type=Path,required=True);a=p.parse_args()
root=Path(__file__).resolve().parents[1]
source=a.app.expanduser().resolve()/'Contents/Resources/Game'
if not (source/'Necesse-source-built-1.3.3.jar').is_file() or not (source/'lib').is_dir():
 raise SystemExit('Not the self-contained Necesse island app')
dest=root/'runtime/Necesse.app/Contents/Resources'
shutil.copytree(source,dest,dirs_exist_ok=True)
print('Restored runtime:',dest)
