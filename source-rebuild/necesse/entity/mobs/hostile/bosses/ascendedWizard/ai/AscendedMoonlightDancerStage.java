package necesse.entity.mobs.hostile.bosses.ascendedWizard.ai;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.Ray;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.pickup.AscendedStarPickupEntity;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.LevelObjectHit;

public class AscendedMoonlightDancerStage<T extends AscendedWizardMob> extends AINode<T> implements AttackStageInterface<T> {
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return AINodeResult.SUCCESS;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      mob.setShieldedAbility.runAndSend(mob.getTime() + 10000L);
      GameUtils.streamServerClients(mob.getServer(), mob.getLevel())
         .forEach(
            c -> {
               PlayerMob player = c.playerMob;
               int duration = GameMath.lerp(mob.getHealthPercent(), 8000, 12000);
               ActiveBuff ab = new ActiveBuff(BuffRegistry.Debuffs.ASCENDED_DARKNESS, player, duration, mob);
               int stacks = 4;
               ab.setStacks(stacks, duration, mob);
               ab.getGndData().setInt("uniqueID", mob.getUniqueID());
               player.buffManager.addBuff(ab, true);
               float anglePerPickup = 360.0F / stacks;

               for (int i = 0; i < stacks; i++) {
                  float range = GameRandom.globalRandom.getFloatBetween(7.0F, 12.0F) * 32.0F;
                  float currentAngle = anglePerPickup * i + GameRandom.globalRandom.nextFloat() * anglePerPickup;
                  Float dir = GameMath.getAngleDir(currentAngle);
                  Ray<LevelObjectHit> firstHit = GameUtils.castRayFirstHit(
                     mob.getLevel(), player.x, player.y, dir.x, dir.y, range, new CollisionFilter().mobCollision()
                  );
                  Point spawnPoint;
                  if (firstHit == null) {
                     spawnPoint = new Point((int)(player.x + dir.x * range), (int)(player.y + dir.y * range));
                  } else {
                     spawnPoint = new Point((int)firstHit.x2 - (int)(dir.x * 32.0F), (int)firstHit.y2 - (int)(dir.y * 32.0F));
                  }

                  AscendedStarPickupEntity pickup = new AscendedStarPickupEntity(mob.getLevel(), spawnPoint.x, spawnPoint.y, 0.0F, 0.0F);
                  mob.getLevel().entityManager.pickups.add(pickup);
               }
            }
         );
      mob.playBossSoundAbility.runAndSend(AscendedWizardMob.BossSound.MOONLIGHT_DANCER);
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
