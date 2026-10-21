package necesse.entity.mobs.ai.behaviourTree.trees;

import java.awt.Point;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.registries.ProjectileRegistry;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanChatterHandlerTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanCommandFollowMobAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanCommandMoveToAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanInteractingAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanJobSequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanMoveToAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanSleepAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanStaringAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanWanderHomeLowHealthAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WanderHomeAtConditionAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.ai.behaviourTree.util.WandererBaseOptions;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.explorerMission.SettlerMission;
import necesse.entity.mobs.gameDamageType.DamageType;
import necesse.entity.projectile.Projectile;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class HumanAI<T extends HumanMob> extends SelectorAINode<T> {
   public final HumanMoveToAINode<T> humanMoveToAINode;
   public final HumanInteractingAINode<T> humanInteractingAINode;
   public final HumanWanderHomeLowHealthAINode<T> wanderHomeLowHealthAINode;
   public final HumanTargetFinderAI<T> humanTargetFinderAI;
   public final ItemAttackerChaserAINode<T> chaserAINode;
   public final HumanJobSequenceAINode<T> humanJobsFollowAINode;
   public final HumanJobSequenceAINode<T> humanJobsAINode;
   public final HumanChatterHandlerTargetAINode<T> humanInteractAINode;
   public final WanderHomeAtConditionAINode<T> wanderHomeAtRaidAINode;
   public final WandererAINode<T> wandererAINode;

   public HumanAI(int searchDistance, boolean attackHostiles, boolean ignoreHiding, int wanderFrequency) {
      this.addChild(new AINode<T>() {
         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            SettlerMission currentMission = mob.getCurrentMission();
            if (currentMission != null && currentMission.isMobIdle(mob)) {
               if (blackboard.mover.isMoving()) {
                  blackboard.mover.stopMoving(mob);
               }

               return AINodeResult.RUNNING;
            } else {
               return AINodeResult.FAILURE;
            }
         }
      });
      this.addChild(this.humanInteractingAINode = new HumanInteractingAINode<>());
      this.addChild(this.humanMoveToAINode = new HumanMoveToAINode<>());
      this.addChild(new AINode<T>() {
         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
            blackboard.onGlobalTick(e -> {
               if (mob.isSettlerOnCurrentLevel() && mob.hideOnLowHealth.get()) {
                  float healthPercent = (float)mob.getHealth() / mob.getMaxHealth();
                  if (healthPercent <= 0.4F) {
                     if (!mob.isHiding && mob.home != null && mob.estimateCanMoveTo(mob.home.x, mob.home.y, true)) {
                        mob.clearCommandsOrders(null);
                        mob.cancelJob();
                        mob.isHiding = true;
                     }
                  } else if (healthPercent >= 0.8F) {
                     mob.isHiding = false;
                  }
               } else {
                  mob.isHiding = false;
               }
            });
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            return AINodeResult.FAILURE;
         }
      });
      this.addChild(this.wanderHomeLowHealthAINode = new HumanWanderHomeLowHealthAINode<>());
      this.addChild(new HumanStaringAINode<>());
      this.addChild(new HumanCommandMoveToAINode<>(true));
      SequenceAINode<T> chaserSequence = new SequenceAINode<>();
      chaserSequence.addChild(this.humanTargetFinderAI = new HumanTargetFinderAI<>(searchDistance, attackHostiles, ignoreHiding));
      chaserSequence.addChild(this.chaserAINode = new ItemAttackerChaserAINode<>());
      chaserSequence.addChild(new AINode<T>() {
         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            Mob target = blackboard.getObject(Mob.class, "chaserTarget");
            if (target != null) {
               LocalMessage activityDescription = new LocalMessage("activities", "attacking", "target", target.getLocalization());
               mob.setActivity("chaser", 20000, activityDescription);
            } else {
               mob.clearActivity("chaser");
            }

            return AINodeResult.SUCCESS;
         }
      });
      this.addChild(chaserSequence);
      this.addChild(this.humanJobsFollowAINode = new HumanJobSequenceAINode<>(true));
      this.addChild(new HumanCommandFollowMobAINode<>());
      this.addChild(new HumanCommandMoveToAINode<>(false));
      this.addChild(this.humanJobsAINode = new HumanJobSequenceAINode<>(false));
      this.addChild(this.wanderHomeAtRaidAINode = new WanderHomeAtConditionAINode<T>() {
         public boolean shouldGoHome(T mob) {
            if (mob.isVisitor()) {
               return false;
            }

            NetworkSettlementData settlement = mob.getSettlerSettlementNetworkData();
            return settlement != null && settlement.isRaidActive();
         }

         public Point getHomeTile(T mob) {
            return !mob.isSettlerOnCurrentLevel() ? null : mob.home;
         }

         public boolean isHomeRoom(T mob) {
            return mob.isSettlerOnCurrentLevel();
         }

         public boolean isHomeHouse(T mob) {
            return true;
         }
      });
      this.addChild(new HumanSleepAINode<>());
      this.addChild(this.humanInteractAINode = new HumanChatterHandlerTargetAINode<>());
      this.wandererAINode = new WandererAINode<T>(wanderFrequency) {
         @Override
         public WandererBaseOptions<T> getBaseOptions() {
            return new WandererBaseOptions<T>() {
               public Point getBaseTile(T mob) {
                  if (mob.isSettler() && !mob.isSettlerOnCurrentLevel()) {
                     return null;
                  }

                  if (mob.isOnStrike()) {
                     ServerSettlementData serverData = mob.getSettlerSettlementServerData();
                     if (serverData != null && serverData.networkData.isRaidActive()) {
                        return serverData.getFlagTile();
                     }
                  }

                  return mob.home;
               }

               public int getBaseRadius(T mob, WandererAINode<T> node) {
                  return mob.isOnStrike() ? 5 : HumanAI.this.wandererAINode.searchRadius;
               }

               public boolean forceFindAroundBase(T mob) {
                  return mob.isOnStrike();
               }

               public boolean isBaseHouse(T mob, Point base) {
                  return true;
               }

               public boolean isBaseRoom(T mob, Point base) {
                  return mob.isSettlerOnCurrentLevel();
               }
            };
         }
      };
      this.wandererAINode.runAwayFromAttacker = true;
      this.wandererAINode.runAwayFromAttackerToBase = true;
      this.wandererAINode.hideInside = mob -> mob.getWorldEntity().isNight() || mob.getLevel().isCave || mob.isHiding;
      this.wandererAINode.getZoneTester = mob -> mob.levelSettler != null ? mob.levelSettler.isTileInSettlementBoundsAndRestrictZoneTester() : null;
      this.addChild(this.wandererAINode);
   }

   public boolean shootSimpleProjectile(T mob, Mob target, String projectileID, int projectileDamage, int projectileSpeed, int projectileDistance) {
      return this.shootSimpleProjectile(mob, target, projectileID, DamageTypeRegistry.NORMAL, projectileDamage, projectileSpeed, projectileDistance);
   }

   public boolean shootSimpleProjectile(
      T mob, Mob target, String projectileID, DamageType damageType, int projectileDamage, int projectileSpeed, int projectileDistance
   ) {
      if (mob.canAttack()) {
         mob.attack(target.getX(), target.getY(), false);
         GameDamage damage = new GameDamage(damageType, projectileDamage);
         Projectile projectile = ProjectileRegistry.getProjectile(
            projectileID, mob.getLevel(), mob.x, mob.y, target.x, target.y, projectileSpeed, projectileDistance, damage, mob
         );
         projectile.setTargetPrediction(target, -10.0F);
         projectile.moveDist(10.0);
         mob.getLevel().entityManager.projectiles.add(projectile);
         return true;
      } else {
         return false;
      }
   }
}
