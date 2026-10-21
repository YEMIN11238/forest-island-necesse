package necesse.inventory.item;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChaserAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.ItemAttackerChaserAINode;
import necesse.entity.mobs.attackHandler.AttackHandler;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.Projectile;
import necesse.inventory.InventoryItem;
import necesse.inventory.enchants.ToolItemEnchantment;
import necesse.inventory.item.toolItem.ToolItem;
import necesse.level.maps.Level;

public interface ItemAttackerWeaponItem {
   default AINode<ItemAttackerMob> getItemAttackerWeaponChaserAI(
      final ItemAttackerChaserAINode<? extends ItemAttackerMob> node, ItemAttackerMob mob, InventoryItem item, final ItemAttackSlot slot
   ) {
      final AtomicReference<InventoryItem> lastItem = new AtomicReference<>(item);
      return new ChaserAINode<ItemAttackerMob>(50, false, false) {
         public AINodeResult tickNode(ItemAttackerMob mob, Blackboard<ItemAttackerMob> blackboard) {
            InventoryItem itemx = slot.getItem();
            if (itemx == null) {
               return AINodeResult.FAILURE;
            } else if (itemx.item == ItemAttackerWeaponItem.this) {
               lastItem.set(itemx);
               this.attackDistance = ItemAttackerWeaponItem.this.getItemAttackerAttackRange(mob, itemx);
               this.stoppingDistance = ItemAttackerWeaponItem.this.getItemAttackerStoppingDistance(mob, itemx, this.attackDistance);
               this.lostTargetStopAttackingDistance = ItemAttackerWeaponItem.this.getItemAttackerLostTargetDistance(mob, itemx, this.attackDistance);
               this.minimumAttackDistance = ItemAttackerWeaponItem.this.getItemAttackerMinimumAttackRange(mob, itemx);
               this.runAwayDistance = ItemAttackerWeaponItem.this.getItemAttackerRunAwayDistance(mob, itemx);
               return super.tickNode(mob, blackboard);
            } else {
               return AINodeResult.FAILURE;
            }
         }

         public boolean isTargetWithinAttackRange(ItemAttackerMob mob, Mob target, float distanceToTarget, boolean hasStartedAttack) {
            int lostDistance = hasStartedAttack ? Math.max(this.attackDistance, this.lostTargetStopAttackingDistance) : this.attackDistance;
            return distanceToTarget >= this.minimumAttackDistance && isTargetHitboxWithinRange(mob, mob.x, mob.y, target, Math.max(lostDistance - 20, 10));
         }

         public boolean canHitTarget(ItemAttackerMob mob, float fromX, float fromY, Mob target) {
            return ItemAttackerWeaponItem.this.canItemAttackerHitTarget(mob, fromX, fromY, target, lastItem.get());
         }

         public boolean attackTarget(ItemAttackerMob mob, Mob target) {
            if (mob.isAttacking) {
               InventoryItem invItem = lastItem.get();
               if (mob.canAnimAttackAgain(invItem) && mob.getNextAnimAttackCooldown() <= 0L) {
                  int seed = Item.getRandomAttackSeed(GameRandom.globalRandom);
                  Point attackPos = ItemAttackerWeaponItem.this.getItemAttackerAttackPosition(mob.getLevel(), mob, target, seed, invItem);
                  if (attackPos != null) {
                     GNDItemMap attackMap = mob.runItemAttack(invItem, attackPos.x, attackPos.y, seed, mob.animAttack, slot, null);
                     AttackHandler newAttackHandler = mob.getAttackHandler();
                     if (newAttackHandler != null) {
                        newAttackHandler.setItemAttackerTarget(target);
                     }

                     node.runOnAttackedEvent(mob, invItem, attackPos.x, attackPos.y, mob.animAttack, attackMap);
                     return true;
                  }
               }
            }

            if (mob.canAttack()) {
               InventoryItem invItem = lastItem.get();
               if (mob.isItemOnCooldown(invItem.item)) {
                  return false;
               }

               int seed = Item.getRandomAttackSeed(GameRandom.globalRandom);
               Point attackPos = ItemAttackerWeaponItem.this.getItemAttackerAttackPosition(mob.getLevel(), mob, target, seed, invItem);
               if (attackPos != null) {
                  AttackHandler currentAttackHandler = mob.getAttackHandler();
                  if (currentAttackHandler != null) {
                     if (!invItem.item.getConstantUse(invItem)) {
                        return false;
                     }

                     if (!currentAttackHandler.canRunAttack(mob.getLevel(), attackPos.x, attackPos.y, mob, invItem, slot)) {
                        return false;
                     }
                  }

                  String canAttack = invItem.item.canAttack(mob.getLevel(), attackPos.x, attackPos.y, mob, invItem);
                  if (canAttack == null) {
                     GNDItemMap attackMap = mob.runItemAttack(invItem, attackPos.x, attackPos.y, seed, 0, slot, null);
                     mob.showItemAttackMobAbility.runAndSend(invItem, attackPos.x, attackPos.y, 0, seed, attackMap);
                     AttackHandler newAttackHandler = mob.getAttackHandler();
                     if (newAttackHandler != null) {
                        newAttackHandler.setItemAttackerTarget(target);
                     }

                     node.runOnAttackedEvent(mob, invItem, attackPos.x, attackPos.y, 0, attackMap);
                  }
               }

               return true;
            } else {
               return false;
            }
         }
      };
   }

   default GameMessage getItemAttackerCanUseError(ItemAttackerMob mob, InventoryItem item) {
      return null;
   }

   default boolean shouldUpdateItemAttackerChaserAI(ItemAttackerMob mob, InventoryItem last, InventoryItem item) {
      return false;
   }

   default int getItemAttackerAttackRange(ItemAttackerMob mob, InventoryItem item) {
      return this instanceof ToolItem ? ((ToolItem)this).getAttackRange(item) : 50;
   }

   default int getItemAttackerStoppingDistance(ItemAttackerMob mob, InventoryItem item, int attackRange) {
      return attackRange - 20;
   }

   default int getItemAttackerLostTargetDistance(ItemAttackerMob mob, InventoryItem item, int attackRange) {
      return attackRange + 20;
   }

   default float getItemAttackerWeaponValueFlat(InventoryItem item) {
      return this instanceof ToolItem ? ((ToolItem)this).getEnchantCost(item) : 0.0F;
   }

   default float getItemAttackerWeaponValue(ItemAttackerMob mob, InventoryItem item) {
      if (this instanceof ToolItem) {
         ToolItemEnchantment enchantment = ((ToolItem)this).getEnchantment(item);
         return this.getItemAttackerWeaponValueFlat(item) * (enchantment == null ? 1.0F : enchantment.getEnchantCostMod());
      } else {
         return 0.0F;
      }
   }

   default float getRaiderTicketModifier(InventoryItem item, HashSet<String> obtainedItems) {
      return 1.0F;
   }

   default int getItemAttackerMinimumAttackRange(ItemAttackerMob attackerMob, InventoryItem item) {
      return 0;
   }

   default int getItemAttackerRunAwayDistance(ItemAttackerMob attackerMob, InventoryItem item) {
      return 0;
   }

   default boolean canItemAttackerHitTarget(ItemAttackerMob attackerMob, float fromX, float fromY, Mob target, InventoryItem item) {
      return ChaserAINode.hasLineOfSightToTarget(attackerMob, fromX, fromY, target);
   }

   default boolean itemAttackerHasLineOfSightToTarget(ItemAttackerMob attackerMob, float fromX, float fromY, Mob target, float hitBoxWidth) {
      return ChaserAINode.hasLineOfSightToTarget(attackerMob, fromX, fromY, target, hitBoxWidth);
   }

   default boolean itemAttackerHasLineOfSightToTarget(ItemAttackerMob attackerMob, Mob target, float hitBoxWidth) {
      return ChaserAINode.hasLineOfSightToTarget(attackerMob, attackerMob.x, attackerMob.y, target, hitBoxWidth);
   }

   default Point getItemAttackerAttackPosition(Level level, ItemAttackerMob attackerMob, Mob target, int seed, InventoryItem item) {
      return this.applyInaccuracy(attackerMob, item, new Point(target.getX(), target.getY()));
   }

   default Point getPredictedItemAttackerAttackPosition(ItemAttackerMob attackerMob, Mob target, float velocity, float distanceOffset) {
      Float targetPos = Projectile.getPredictedTargetPos(target, attackerMob.x, attackerMob.y, velocity, distanceOffset);
      return new Point((int)targetPos.x, (int)targetPos.y);
   }

   default Point getPredictedItemAttackerAttackPositionMillis(Mob target, int millisDelay) {
      float nextX = Projectile.getPositionAfterMillis(target.dx, millisDelay);
      float nextY = Projectile.getPositionAfterMillis(target.dy, millisDelay);
      return new Point((int)(target.x + nextX), (int)(target.y + nextY));
   }

   default Point applyInaccuracy(ItemAttackerMob mob, InventoryItem item, Point target) {
      double dist = target.distance(mob.getX(), mob.getY());
      float skillPercent;
      if (item.getGndData().hasKey("skillPercent")) {
         skillPercent = item.getGndData().getFloat("skillPercent");
      } else {
         skillPercent = mob.getWeaponSkillPercent(item);
      }

      float skillPercentInv = 1.0F - GameMath.limit(skillPercent, 0.0F, 1.0F);
      GameRandom random = new GameRandom(GameRandom.prime(mob.getInaccuracySeed(item)));
      int xInaccuracy = (int)(random.getDoubleBetween(-dist, dist) * skillPercentInv * 0.9F);
      int yInaccuracy = (int)(random.getDoubleBetween(-dist, dist) * skillPercentInv * 0.9F);
      return new Point(target.x + xInaccuracy, target.y + yInaccuracy);
   }

   default void itemAttackerTickHolding(InventoryItem item, ItemAttackerMob mob) {
      if (mob.isClient()) {
         item.item.refreshLight(mob.getLevel(), mob.x, mob.y, item, true);
      }
   }
}
