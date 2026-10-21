package necesse.entity.mobs.hostile.bosses.ascendedWizard.ai;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.Ray;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.projectile.AscendedGolemSpawnProjectile;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.LevelObjectHit;

public class AscendedCrystalDragonGolemsStage<T extends AscendedWizardMob> extends AINode<T> implements AttackStageInterface<T> {
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return AINodeResult.SUCCESS;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      int spawns = mob.isTransformed() ? 8 : 4;
      float anglePerPickup = 360.0F / spawns;

      for (int i = 0; i < spawns; i++) {
         float range = GameRandom.globalRandom.getFloatBetween(7.0F, 12.0F) * 32.0F;
         float currentAngle = anglePerPickup * i + GameRandom.globalRandom.nextFloat() * anglePerPickup;
         Float dir = GameMath.getAngleDir(currentAngle);
         Ray<LevelObjectHit> firstHit = GameUtils.castRayFirstHit(mob.getLevel(), mob.x, mob.y, dir.x, dir.y, range, new CollisionFilter().mobCollision());
         Point spawnPoint;
         if (firstHit == null) {
            spawnPoint = new Point((int)(mob.x + dir.x * range), (int)(mob.y + dir.y * range));
         } else {
            spawnPoint = new Point((int)firstHit.x2 - (int)(dir.x * 32.0F), (int)firstHit.y2 - (int)(dir.y * 32.0F));
         }

         int distance = (int)mob.getDistance(spawnPoint.x, spawnPoint.y);
         AscendedGolemSpawnProjectile projectile = new AscendedGolemSpawnProjectile(
            mob.getLevel(), mob, spawnPoint.x, spawnPoint.y, 30.0F, distance, new GameDamage(0.0F), 50
         );
         mob.getLevel().entityManager.projectiles.add(projectile);
         mob.spawnedProjectiles.add(projectile);
      }

      mob.playBossSoundAbility.runAndSend(AscendedWizardMob.BossSound.CRYSTAL_DRAGON);
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
