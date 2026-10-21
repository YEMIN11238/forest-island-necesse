package necesse.entity.mobs.ai.behaviourTree.trees;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.function.Supplier;
import necesse.engine.network.packet.PacketMobAttack;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.event.ConfuseWanderAIEvent;
import necesse.entity.mobs.ai.behaviourTree.leaves.ConfusedWandererAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.hostile.ItemAttackerRaiderMob;
import necesse.entity.mobs.hostile.SharkMob;

public class SharkAI<T extends SharkMob> extends SelectorAINode<T> {
   protected long lastCritterTargetTime = 0L;
   protected long critterAttackCooldown = 8000L;

   public SharkAI(int searchDistance) {
      this.addChild(new ConfusedWandererAINode<>());
      this.addChild(new CollisionChaserAI<T>(searchDistance, new GameDamage(24.0F), 5) {
         public void init(T mob, Blackboard<T> blackboard) {
            super.init(mob, blackboard);
            this.targetFinderAINode.canRegainSameTargetIfLostToTimer = false;
            this.targetFinderAINode.loseTargetMaxCooldown = 100;
            this.targetFinderAINode.loseTargetMinCooldown = 100;
         }

         public GameAreaStream<Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
            return distance.streamMobsAndPlayersInRange(base, mob).filter(m -> {
               if (m == mob) {
                  return false;
               } else if (!SharkAI.this.mobIsInWater(m)) {
                  return false;
               } else if (m.isPlayer) {
                  return true;
               } else if (m.isCritter) {
                  return SharkAI.this.lastCritterTargetTime + SharkAI.this.critterAttackCooldown < m.getTime();
               } else if (m instanceof ItemAttackerRaiderMob) {
                  return true;
               } else {
                  return m.isHuman ? false : false;
               }
            });
         }

         public boolean attackTarget(T mob, Mob target) {
            return SharkAI.this.sharkAttack(mob, target, () -> super.attackTarget(mob, target));
         }
      });
      this.addChild(new WandererAINode<T>(0) {
         public void init(T mob, Blackboard<T> blackboard) {
            super.init(mob, blackboard);
            this.searchRadius = 15;
         }
      });
   }

   protected boolean sharkAttack(T mob, Mob target, Supplier<Boolean> superAttackTarget) {
      boolean superAttck = superAttackTarget.get();
      if (superAttck) {
         if (target.isCritter) {
            this.lastCritterTargetTime = mob.getTime();
         } else {
            if (target.getHealth() > 0.0F) {
               target.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.SHARK_BLEED, target, 3.0F, mob), true);
            }

            mob.getServer().network.sendToAllClients(new PacketMobAttack(mob, mob.getX(), mob.getY(), true));
            Float attackDir = GameMath.normalize(target.x - mob.x, target.y - mob.y);
            float attackAngle = GameMath.getAngle(attackDir);
            float runAwayAngle = GameMath.fixAngle(attackAngle + GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F));
            Float runAwayDir = GameMath.getAngleDir(runAwayAngle);
            int confuseTime = GameRandom.globalRandom.getIntBetween(2000, 3000);
            this.getBlackboard().submitEvent("confuseWander", new ConfuseWanderAIEvent(confuseTime, runAwayDir));
         }
      }

      return superAttck;
   }

   protected boolean mobIsInWater(Mob targetMob) {
      if (targetMob == null) {
         return false;
      } else {
         return targetMob.getLevel() == null ? false : targetMob.getLevel().liquidManager.getHeight(targetMob.getTileX(), targetMob.getTileY()) < -3;
      }
   }
}
