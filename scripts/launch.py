from pathlib import Path
import os,sys
root=Path(__file__).resolve().parents[1]
r=root/'runtime/Necesse.app/Contents/Resources'
profile=Path.home()/'Library/Application Support/ForestIslandNecesse'
extra=[]
args=sys.argv[1:]
if '--profile' in args:
 i=args.index('--profile');profile=Path(args[i+1]).resolve();args=args[:i]+args[i+2:]
profile.mkdir(parents=True,exist_ok=True)
params=['-datadir',str(profile),'-mod',str(root/'mod/dist'),'-islandprofile','-load','ForestIsland','-character','小岛农夫','-windowpos','100,70','-windowsize','1200,900']+args
java=r/'jre/bin/java'
kernel='Necesse-source-built-1.3.3.jar'
if not (r/kernel).is_file():
 raise SystemExit('Missing source-built kernel. Run scripts/build_project.py first.')
print('LOCALFOREST_KERNEL '+kernel,flush=True)
os.chdir(r)
os.execv('/usr/bin/arch',['/usr/bin/arch','-x86_64',str(java),'-Dlocalforest.kernel='+kernel,'-Djdk.attach.allowAttachSelf=true','-Xms512m','-Xmx3g','-jar',kernel,*params])
