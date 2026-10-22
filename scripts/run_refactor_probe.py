#!/usr/bin/env python3
"""Native disposable-profile check; visual capture and server API checks have separate evidence."""
from pathlib import Path
import argparse,json,os,subprocess

ROOT=Path(__file__).resolve().parents[1]

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--case',required=True)
    p.add_argument('--preset',choices=['forest_island','woodland'],default='forest_island')
    p.add_argument('--seed',default='4212026')
    p.add_argument('--reload',action='store_true')
    p.add_argument('--baseline',action='store_true')
    p.add_argument('--ladders',action='store_true')
    p.add_argument('--harvest',action='store_true')
    p.add_argument('--perf-seconds',type=int,default=10)
    p.add_argument('--jfr',action='store_true',help='Opt-in CPU/allocation sampling of this disposable process')
    args=p.parse_args()
    if args.harvest and (args.reload or args.baseline or args.ladders):p.error('--harvest needs its own fresh daytime case')
    if args.baseline and (args.reload or args.ladders):p.error('--baseline is a fresh visual-only case')
    if not args.case.replace('-','').replace('_','').isalnum():p.error('Invalid case name')
    profile=Path.home()/'Library/Application Support'/('Mosslume-QA-Refactor-'+args.case)
    if profile.exists() and not args.reload:p.error('Choose a new case; this profile already exists')
    if args.reload and not profile.exists():p.error('Cannot reload a missing profile')
    profile.mkdir(parents=True,exist_ok=True)
    out=ROOT/'build/refactor-evidence'/args.case/('reload' if args.reload else 'initial')
    out.mkdir(parents=True,exist_ok=True)
    app=Path.home()/'Applications/Mosslume 0.4 Preview.app'
    props={}
    if not args.baseline:
        props.update({'mosslume.refactorProbe':str(out),'mosslume.refactorProbe.seed':args.seed,
            'mosslume.refactorProbe.preset':args.preset})
    if args.reload or args.ladders or args.harvest:props['mosslume.refactorProbe.stop']='true'
    if args.ladders:props['mosslume.refactorProbe.ladders']='true'
    if args.harvest:props['mosslume.refactorProbe.harvest']='true'
    if not args.reload and not args.ladders and not args.harvest:
        props.update({'mosslume.visualProbe':str(out),'mosslume.visualProbe.hour':21,
            'mosslume.visualProbe.compareHour':12,'mosslume.visualProbe.perfSeconds':min(120,max(2,args.perf_seconds)),
            'mosslume.visualProbe.captureSeconds':8,'mosslume.visualProbe.captureFps':24,
            'mosslume.visualProbe.windowWidth':1200,'mosslume.visualProbe.windowHeight':900,
            'mosslume.visualProbe.centerX':1536 if args.baseline else 3072,
            'mosslume.visualProbe.centerY':1216 if args.baseline else 2560})
    env=os.environ.copy();env['MOSSLUME_PROFILE']=str(profile)
    env['JAVA_TOOL_OPTIONS']=' '.join('-D'+k+'='+str(v) for k,v in props.items())
    if args.jfr:
        env['JAVA_TOOL_OPTIONS']='-XX:StartFlightRecording=settings=profile,dumponexit=true,filename='+str(out/'render-profile.jfr')+' '+env['JAVA_TOOL_OPTIONS']
    (out/'launch.json').write_text(json.dumps({'profile':str(profile),'app':str(app),'properties':props,
        'scope':'Native game callbacks and own framebuffer; not physical mouse evidence.'},indent=2)+'\n')
    with (out/'launcher.log').open('w') as log:
        run=subprocess.run([str(app/'Contents/MacOS/MosslumeLauncher'),'-mosslumeqa'],env=env,
            stdout=log,stderr=subprocess.STDOUT,timeout=360 if args.harvest else 240)
    print(json.dumps({'exit':run.returncode,'profile':str(profile),'evidence':str(out)}))
    if run.returncode:raise SystemExit(run.returncode)
    if not args.baseline:
        result=out/('native-checks-reload.json' if args.reload else 'native-checks.json')
        failure=out/'native-checks-failed.json'
        if failure.exists():raise RuntimeError(failure.read_text())
        if not result.exists() or not json.loads(result.read_text()).get('passed'):
            raise RuntimeError('Native checks did not report success: '+str(result))

if __name__=='__main__':main()
