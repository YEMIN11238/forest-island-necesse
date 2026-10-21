package necesse.entity.mobs.hostile.bosses.ascendedWizard.ai;

import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedGauntletMob;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;

public class AscendedSunlightChampionStage<T extends AscendedWizardMob> extends AINode<T> implements AttackStageInterface<T> {
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return AINodeResult.SUCCESS;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
      ArrayList<AscendedGauntletMob> gauntlets = spawnGauntletPair(mob, currentTarget);
      mob.spawnedMobs.addAll(gauntlets);
      mob.playBossSoundAbility.runAndSend(AscendedWizardMob.BossSound.SUNLIGHT_CHAMPION);
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }

   public static ArrayList<AscendedGauntletMob> spawnGauntletPair(Mob master, Mob target) {
      ArrayList<AscendedGauntletMob> gauntletMobs = new ArrayList<>(2);
      float spawnAngle = GameRandom.globalRandom.nextInt(360);
      if (target != null) {
         float angleToPlayer = GameMath.getAngle(GameMath.normalize(target.x - master.x, target.y - master.y));
         spawnAngle = GameMath.fixAngle(angleToPlayer + 90.0F);
      }

      for (int i = 0; i < 2; i++) {
         AscendedGauntletMob gauntlet = new AscendedGauntletMob();
         gauntlet.leftHanded = i == 0;
         gauntlet.setLevel(master.getLevel());
         gauntlet.onSpawned(master.getX() + GameRandom.globalRandom.getIntBetween(-20, 20), master.getY() + GameRandom.globalRandom.getIntBetween(-20, 20));
         gauntlet.master.uniqueID = master.getUniqueID();
         if (i % 2 == 1) {
            spawnAngle += 180.0F;
         }

         Float dir = GameMath.getAngleDir(spawnAngle);
         gauntlet.dx = dir.x * 100.0F;
         gauntlet.dy = dir.y * 100.0F;
         master.getLevel().entityManager.mobs.add(gauntlet);
         gauntletMobs.add(gauntlet);
         if (target != null) {
            gauntlet.ai.blackboard.put("currentTarget", target);
         }
      }

      return gauntletMobs;
   }
}
