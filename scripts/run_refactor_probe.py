#!/usr/bin/env python3
"""Native disposable-profile check; visual capture and server API checks have separate evidence."""
from pathlib import Path
import argparse,json,os,subprocess

ROOT=Path(__file__).resolve().parents[1]

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--case',required=True)
    p.add_argument('--preset',choices=['forest_island','woodland','biomes','continuous'],default='forest_island')
    p.add_argument('--seed',default='4212026')
    p.add_argument('--reload',action='store_true')
    p.add_argument('--biome-tour',action='store_true',help='Capture six biome centers after the regular native visual probe')
    p.add_argument('--baseline',action='store_true')
    p.add_argument('--ladders',action='store_true')
    p.add_argument('--harvest',action='store_true')
    p.add_argument('--ai',action='store_true',help='Explicit opt-in: three paid DeepSeek requests using local configuration')
    p.add_argument('--cognition',action='store_true',help='Offline native cognition/social fixture and bubble capture in a fresh disposable world')
    p.add_argument('--region',action='store_true',help='Native continuous-region generation, unload/reload, negative-coordinate and disk checkpoint checks')
    p.add_argument('--gameplay',action='store_true',help='Offline server fixtures for repaired combat, conversation and population; fresh isolated case only')
    p.add_argument('--creature-animations',action='store_true',help='Observe actual client attack/hurt/death rendering for two native monster fixtures; fresh isolated case only')
    p.add_argument('--boss-encounters',action='store_true',help='Offline two-Boss native attacks, phase, save, loot and actual client rendering in a fresh continuous QA world')
    p.add_argument('--soak-seconds',type=int,default=0,help='Keep authoritative server ticks running after checks, before the saved checkpoint (0–60 seconds)')
    p.add_argument('--perf-seconds',type=int,default=10)
    p.add_argument('--jfr',action='store_true',help='Opt-in CPU/allocation sampling of this disposable process')
    p.add_argument('--app-path',type=Path,default=Path.home()/'Applications/Mosslume.app')
    p.add_argument('--mod-path',type=Path,help='Use freshly built mod files with the installed runtime, without modifying the application bundle')
    p.add_argument('--warmup-seconds',type=int,default=30)
    p.add_argument('--hour',type=float,default=21)
    p.add_argument('--performance-only',action='store_true',help='Skip framebuffer readback/PNG capture entirely')
    p.add_argument('--scene-only',action='store_true',help='Observe natural residents without inventory markers or companion command fixtures; fresh visual case only')
    p.add_argument('--immediate-wind',action='store_true',help='Diagnostic comparison with original per-vertex submission')
    p.add_argument('--batch-disabled',action='store_true',help='Diagnostic comparison with the individual sprite shader')
    p.add_argument('--fixed-wind-seconds',type=float,help='Lock only plant wind, for same-phase visual comparisons')
    p.add_argument('--attack-poses',action='store_true',help='Exercise four-direction native client attacks during visual capture, in the disposable profile only')
    p.add_argument('--weapon-matrix',action='store_true',help='Capture native sword, axe and bow attacks in four directions in a scene-only disposable case (18 seconds)')
    p.add_argument('--full-frame',action='store_true',help='Capture the entire presented framebuffer, including the coastline')
    p.add_argument('--capture-scale',type=int,choices=[1,2],default=1,help='Nearest downsampling for clip PNGs only; full-resolution stills are preserved')
    p.add_argument('--center-x',type=int)
    p.add_argument('--center-y',type=int)
    args=p.parse_args()
    if args.weapon_matrix and (not args.scene_only or args.performance_only or args.attack_poses):p.error('--weapon-matrix requires --scene-only and framebuffer capture, without legacy --attack-poses')
    if args.boss_encounters and (args.preset!='continuous' or args.reload or args.baseline or args.ladders or args.harvest or args.ai or args.cognition or args.region or args.gameplay or args.creature_animations or args.biome_tour or args.scene_only):p.error('--boss-encounters requires its own fresh --preset continuous case, without AI or other fixtures')
    if args.scene_only and (args.reload or args.baseline or args.ladders or args.harvest or args.ai or args.cognition or args.region or args.gameplay or args.creature_animations or args.boss_encounters):p.error('--scene-only requires its own fresh visual case')
    if args.creature_animations and (args.reload or args.baseline or args.ai or args.cognition or args.harvest or args.biome_tour):p.error('--creature-animations needs a fresh isolated native case')
    if args.region and (args.preset!='continuous' or args.baseline or args.ai or args.cognition or args.harvest or args.biome_tour):p.error('--region requires --preset continuous and a separate native case')
    if args.gameplay and (args.reload or args.baseline or args.ai or args.cognition or args.harvest or args.biome_tour):p.error('--gameplay needs its own fresh native case; --region may be combined')
    if args.cognition and (args.reload or args.ai or args.harvest or args.baseline or args.ladders or args.biome_tour):p.error('--cognition needs its own fresh case')
    if args.biome_tour and (args.preset!='biomes' or args.reload or args.baseline or args.ladders or args.harvest or args.ai or args.performance_only):
        p.error('--biome-tour requires a fresh --preset biomes visual case')
    if args.ai and (args.reload or args.baseline or args.ladders or args.harvest):p.error('--ai needs its own fresh case')
    if args.harvest and (args.reload or args.baseline or args.ladders):p.error('--harvest needs its own fresh daytime case')
    if args.baseline and (args.reload or args.ladders):p.error('--baseline is a fresh visual-only case')
    if not args.case.replace('-','').replace('_','').isalnum():p.error('Invalid case name')
    profile=Path.home()/'Library/Application Support'/('Mosslume-QA-Refactor-'+args.case)
    if profile.exists() and not args.reload:p.error('Choose a new case; this profile already exists')
    if args.reload and not profile.exists():p.error('Cannot reload a missing profile')
    profile.mkdir(parents=True,exist_ok=True)
    out=ROOT/'build/refactor-evidence'/args.case/('reload' if args.reload else 'initial')
    out.mkdir(parents=True,exist_ok=True)
    app=args.app_path.expanduser().resolve()
    mod_path=args.mod_path.expanduser().resolve() if args.mod_path else None
    if mod_path and not (mod_path/'localforest-island.jar').is_file():p.error('--mod-path must contain localforest-island.jar')
    props={}
    if not args.baseline:
        props.update({'mosslume.refactorProbe':str(out),'mosslume.refactorProbe.seed':args.seed,
            'mosslume.refactorProbe.preset':args.preset})
    if args.reload or args.ladders or args.harvest or args.ai or args.cognition or args.region or args.gameplay or args.creature_animations or args.boss_encounters:props['mosslume.refactorProbe.stop']='true'
    if args.ladders:props['mosslume.refactorProbe.ladders']='true'
    if args.harvest:props['mosslume.refactorProbe.harvest']='true'
    if args.ai:props['mosslume.refactorProbe.ai']='true'
    if args.cognition:props['mosslume.refactorProbe.cognition']='true'
    if args.region:props.update({'mosslume.regionProbe':'true','mosslume.regionDiagnostics':'true'})
    if args.gameplay:props['mosslume.refactorProbe.gameplay']='true'
    if args.creature_animations:props['mosslume.refactorProbe.creatureAnimations']='true'
    if args.boss_encounters:props['mosslume.refactorProbe.bossEncounters']='true'
    if args.scene_only:props['mosslume.visualProbe.sceneOnly']='true'
    if args.soak_seconds:props['mosslume.refactorProbe.soakSeconds']=str(min(60,max(0,args.soak_seconds)))
    if not args.reload and not args.ladders and not args.harvest and not args.ai and not args.cognition and not args.region and not args.gameplay and not args.creature_animations and not args.boss_encounters:
        props.update({'mosslume.visualProbe':str(out),'mosslume.visualProbe.hour':args.hour,
            'mosslume.visualProbe.compareHour':12 if args.hour>=18 else 21,
            'mosslume.visualProbe.perfSeconds':min(600,max(2,args.perf_seconds)),
            'mosslume.visualProbe.warmupSeconds':min(60,max(1,args.warmup_seconds)),
            'mosslume.visualProbe.performanceOnly':str(args.performance_only).lower(),
            'mosslume.visualProbe.captureSeconds':8,'mosslume.visualProbe.captureFps':24,
            'mosslume.visualProbe.windowWidth':1200,'mosslume.visualProbe.windowHeight':900})
        if args.center_x is not None:props['mosslume.visualProbe.centerX']=args.center_x
        if args.center_y is not None:props['mosslume.visualProbe.centerY']=args.center_y
    if args.biome_tour:props['mosslume.visualProbe.biomeTour']='true'
    if args.immediate_wind:props['mosslume.wind.immediate']='true'
    if args.batch_disabled:props['mosslume.wind.batch']='false'
    if args.fixed_wind_seconds is not None:props['mosslume.visualProbe.fixedWindSeconds']=args.fixed_wind_seconds
    if args.attack_poses:
        if 'mosslume.visualProbe' not in props or args.performance_only:p.error('--attack-poses requires a fresh visual capture case')
        props['mosslume.visualProbe.attackPoses']='true'
    if args.weapon_matrix:
        if 'mosslume.visualProbe' not in props:p.error('--weapon-matrix requires a fresh visual capture case')
        props['mosslume.visualProbe.weaponMatrix']='true'
        props['mosslume.visualProbe.captureSeconds']=18
        props['mosslume.visualProbe.hour']=12
        props.pop('mosslume.visualProbe.compareHour',None)
    if args.full_frame:props['mosslume.visualProbe.captureFullFrame']='true'
    props['mosslume.visualProbe.captureScale']=str(args.capture_scale)
    env=os.environ.copy();env['MOSSLUME_PROFILE']=str(profile)
    env['JAVA_TOOL_OPTIONS']=' '.join('-D'+k+'='+str(v) for k,v in props.items())
    if args.jfr:
        env['JAVA_TOOL_OPTIONS']='-XX:StartFlightRecording=settings=profile,dumponexit=true,filename='+str(out/'render-profile.jfr')+' '+env['JAVA_TOOL_OPTIONS']
    (out/'launch.json').write_text(json.dumps({'profile':str(profile),'app':str(app),'properties':props,
        'scope':'Native game callbacks and own framebuffer; not physical mouse evidence.'},indent=2)+'\n')
    launch=[str(app/'Contents/MacOS/MosslumeLauncher'),'-mosslumeqa']
    if mod_path:launch+=['-mod',str(mod_path)]
    (out/'mod-under-test.json').write_text(json.dumps({'directory':str(mod_path or app/'Contents/Resources/Mods'),
        'sha256':__import__('hashlib').sha256(((mod_path or app/'Contents/Resources/Mods')/'localforest-island.jar').read_bytes()).hexdigest()},indent=2)+'\n')
    with (out/'launcher.log').open('w') as log:
        run=subprocess.run(launch,env=env,
            stdout=log,stderr=subprocess.STDOUT,timeout=max(360,args.perf_seconds+args.warmup_seconds+180))
    print(json.dumps({'exit':run.returncode,'profile':str(profile),'evidence':str(out)}))
    if run.returncode:raise SystemExit(run.returncode)
    if args.weapon_matrix:
        matrix=out/'weapon-matrix.json'
        if not matrix.exists() or not json.loads(matrix.read_text()).get('passed'):
            raise RuntimeError('Weapon/portrait rendering checks did not pass: '+str(matrix))
    if not args.baseline:
        result=out/('native-checks-reload.json' if args.reload else 'native-checks.json')
        failure=out/'native-checks-failed.json'
        if failure.exists():raise RuntimeError(failure.read_text())
        if not result.exists() or not json.loads(result.read_text()).get('passed'):
            raise RuntimeError('Native checks did not report success: '+str(result))

if __name__=='__main__':main()
