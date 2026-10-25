#!/usr/bin/env python3
"""Compile the mod and run deterministic, save-format and native job lifecycle regressions."""
from pathlib import Path
import json,subprocess,sys

ROOT=Path(__file__).resolve().parents[1]
JDK=Path('/Users/zhangjunhao/Documents/Codex/tools/jdk-21.0.12.1+1/Contents/Home/bin')

def main():
    subprocess.run([sys.executable,str(ROOT/'scripts/build_mod.py')],check=True)
    runtime=ROOT/'runtime/Necesse.app/Contents/Resources'
    classes=ROOT/'build/refactor-checks';classes.mkdir(parents=True,exist_ok=True)
    cp=':'.join(str(p) for p in [classes,ROOT/'mod/dist/localforest-island.jar',runtime/'Necesse-source-built-1.3.3.jar',*sorted((runtime/'lib').glob('*.jar')),*sorted((ROOT/'mod/build/agentscope-libs').glob('*.jar'))])
    sources=[*sorted((ROOT/'mod/tests').rglob('*.java')),*sorted((ROOT/'tests').glob('*.java'))]
    subprocess.run([str(JDK/'javac'),'--release','17','-encoding','UTF-8','-classpath',cp,'-d',str(classes),*map(str,sources)],check=True)
    names=['MemoryRankingCheck','localforest.world.GeneratedLayoutTest','localforest.world.BiomeLayoutTest','localforest.world.NativeFiniteTerrainTest','localforest.world.ContinuousWorldLayoutTest','localforest.world.RegionCacheInvalidationTest','localforest.scene.SceneGeometryTest','localforest.scene.SceneViewportTest','localforest.scene.SceneMetadataTest','localforest.visual.DaylightMaterialTest','localforest.visual.TileColorCacheTest',
        'localforest.tasks.CompanionTaskSaveTest','localforest.companions.CompanionWorldSaveTest','localforest.companions.NpcCognitionSaveTest','localforest.tasks.NativeWorkCompletionTest','localforest.ai.DeepSeekProviderTest','localforest.ai.NpcMindTest','localforest.ai.NpcSocialPlannerTest','localforest.ai.AiIntentAmbientTest','localforest.visual.PathCoordinateKeyTest','localforest.visual.SceneDrawOrderTest','localforest.visual.OceanGeometryCacheTest',
        'localforest.ai.AiRequestSchedulerTest','localforest.companions.NpcPopulationPolicyTest','localforest.visual.VisualMaterialWindTest','localforest.visual.VisualCatalogResourceTest','localforest.visual.CharacterAnimationTest','localforest.biomes.AttackCycleTest',
        'localforest.world.CoastalCampLayoutTest','localforest.world.CoastalHydrologyTest','localforest.scene.ResidenceMetadataTest','localforest.visual.GroundDecorationPolicyTest','localforest.companions.NativeStorageFinderRepairTest','localforest.companions.PopulationSpawnSearchTest','localforest.visual.PathAtlasCacheTest',
        'localforest.companions.NpcLifeSuppliesTest','localforest.companions.NpcActivityScheduleTest','localforest.companions.NativeInteractionApproachTest','CreatureAnimationCheck','localforest.bosses.BossAttackTimelineTest','localforest.visual.AttackHandSocketsTest','localforest.visual.ResidentPortraitsTest']
    results=[]
    for name in names:
        run=subprocess.run([str(JDK/'java'),'-classpath',cp,name],cwd=runtime if name in {'localforest.world.RegionCacheInvalidationTest','localforest.world.CoastalHydrologyTest','localforest.visual.GroundDecorationPolicyTest','localforest.companions.NativeStorageFinderRepairTest'} else ROOT,text=True,capture_output=True,timeout=60)
        results.append({'test':name,'exit':run.returncode,'output':run.stdout+run.stderr})
        print(run.stdout+run.stderr,end='')
        if run.returncode:break
    output=ROOT/'build/refactor-evidence/unit-checks.json';output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
    if len(results)!=len(names) or any(r['exit'] for r in results):raise SystemExit(1)

if __name__=='__main__':main()
