package necesse.entity.mobs.ai.behaviourTree.trees;

import java.awt.Point;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.decorators.FailerAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.CallHelpAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.CurrentTargetTalkerAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.PirateEscapeAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.ai.behaviourTree.util.WandererBaseOptions;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.hostile.pirates.PirateMob;

public class PirateAITree<T extends PirateMob> extends SelectorAINode<T> {
   protected int startPassiveCounter;

   public PirateAITree(int shootDistance, int shootCooldown, int meleeDistance, int searchDistance, int wanderFrequency) {
      final String baseKey = "mobBase";
      this.addChild(new AINode<T>() {
         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            if (mob.baseTile != null) {
               blackboard.put(baseKey, new Point(mob.baseTile.x * 32 + 16, mob.baseTile.y * 32 + 16));
            }

            return AINodeResult.FAILURE;
         }
      });
      final String targetKey = "chaserTarget";
      this.addChild(new AINode<T>() {
         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
            blackboard.onEvent("refreshBossDespawn", event -> PirateAITree.this.startPassiveCounter = 0);
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            Mob target = blackboard.getObject(Mob.class, targetKey);
            boolean shouldHavePassive = target == null;
            if (mob.buffManager.hasBuff(BuffRegistry.PIRATE_PASSIVE) != shouldHavePassive) {
               if (shouldHavePassive) {
                  PirateAITree.this.startPassiveCounter++;
                  if (PirateAITree.this.startPassiveCounter > 60) {
                     mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.PIRATE_PASSIVE, mob, 0, null), true);
                  }
               } else {
                  mob.buffManager.removeBuff(BuffRegistry.PIRATE_PASSIVE, true);
                  PirateAITree.this.startPassiveCounter = 0;
               }
            }

            if (!shouldHavePassive) {
               PirateAITree.this.startPassiveCounter = 0;
            }

            if (target == null && mob.buffManager.hasBuff(BuffRegistry.PIRATE_ESCAPE)) {
               mob.buffManager.removeBuff(BuffRegistry.PIRATE_ESCAPE, true);
            }

            return AINodeResult.FAILURE;
         }
      });
      this.addChild(new FailerAINode<>(new CallHelpAINode<>("piratehelp", 5, 8000)));
      this.addChild(new FailerAINode<>(new CurrentTargetTalkerAINode<T>(true, 8000) {
         public void talk(T mob, Mob target) {
            PirateAITree.this.sendRandomAttackMessage(mob);
         }
      }));
      this.addChild(this.getChaserNode(shootDistance, shootCooldown, meleeDistance, searchDistance));
      this.addChild(new PirateEscapeAINode<T>(384) {
         @Override
         public void onEscape(T mob) {
            super.onEscape(mob);
            PirateAITree.this.sendRandomEscapeMessage(mob);
         }
      });
      WandererAINode<T> wanderer = new WandererAINode<T>(wanderFrequency) {
         @Override
         public WandererBaseOptions<T> getBaseOptions() {
            return new WandererBaseOptions<T>() {
               public Point getBaseTile(T mob) {
                  return mob.baseTile;
               }
            };
         }
      };
      wanderer.searchRadius = 5;
      this.addChild(wanderer);
   }

   protected AINode<T> getChaserNode(int shootDistance, int shootCooldown, int meleeDistance, int searchDistance) {
      return new PirateChaserAI<>(shootDistance, shootCooldown, meleeDistance, searchDistance);
   }

   private void sendRandomAttackMessage(T mob) {
      if (mob.isServer()) {
         mob.getLevel().getServer().network.sendToClientsWithEntity(new PacketMobChat(mob.getUniqueID(), "mobmsg", this.getRandomAttackKey()), mob);
      }
   }

   private String getRandomAttackKey() {
      int nextInt = GameRandom.globalRandom.nextInt(7) + 1;
      return "pirateattack" + nextInt;
   }

   private void sendRandomEscapeMessage(T mob) {
      if (mob.isServer()) {
         mob.getLevel().getServer().network.sendToClientsWithEntity(new PacketMobChat(mob.getUniqueID(), "mobmsg", this.getRandomEscapeKey()), mob);
      }
   }

   private String getRandomEscapeKey() {
      int nextInt = GameRandom.globalRandom.nextInt(3) + 1;
      return "pirateescape" + nextInt;
   }
}
