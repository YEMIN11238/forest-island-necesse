package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import java.util.Comparator;
import java.util.HashSet;
import java.util.function.Predicate;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.ComputedObjectValue;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.ai.behaviourTree.util.TargetValidity;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.Level;

public class HumanAngerTargetAINode<T extends HumanMob> extends MoveTaskAINode<T> {
   public TargetValidity<T> validity;
   public TargetFinderDistance<T> distance;
   public String currentTargetKey;
   public HashSet<Mob> enemies = new HashSet<>();
   public float anger = 0.0F;
   public float angerDecreasePerSecond = 0.2F;

   public HumanAngerTargetAINode(TargetFinderDistance<T> distance, TargetValidity<T> validity, String currentTargetKey) {
      this.distance = distance;
      this.validity = validity;
      this.currentTargetKey = currentTargetKey;
   }

   public HumanAngerTargetAINode(TargetFinderDistance<T> distance, TargetValidity<T> validity) {
      this(distance, validity, "currentTarget");
   }

   public HumanAngerTargetAINode(TargetFinderDistance<T> distance) {
      this(distance, new TargetValidity<>());
   }

   public static void addNearbyHumansAnger(Mob mob, Attacker attacker, Predicate<HumanMob> isValidMob, boolean alertNearbyToo) {
      if (!mob.isClient()) {
         float percent = Math.abs((float)mob.getHealth() / mob.getMaxHealth() - 1.0F);
         float anger = percent * 2.5F;
         addNearbyHumansAnger(mob.getLevel(), mob.getTileX(), mob.getTileY(), anger, attacker, isValidMob, alertNearbyToo);
      }
   }

   public static void addNearbyHumansAnger(
      Level level, int tileX, int tileY, float anger, Attacker attacker, Predicate<HumanMob> isValidMob, boolean alertNearbyToo
   ) {
      if (!level.isClient()) {
         Mob attackOwner = attacker != null ? attacker.getAttackOwner() : null;
         if (attackOwner != null && attackOwner.isPlayer) {
            if (!attackOwner.buffManager.hasBuff(BuffRegistry.BOSS_NEARBY)) {
               level.entityManager
                  .mobs
                  .getInRegionByTileRange(tileX, tileY, 25)
                  .stream()
                  .filter(m -> m instanceof HumanMob && isValidMob.test((HumanMob)m))
                  .forEach(m -> {
                     HumanAngerTargetAINode<?> humanAngerHandler = m.ai.blackboard.getObject(HumanAngerTargetAINode.class, "humanAngerHandler");
                     humanAngerHandler.addAnger(anger, attackOwner, alertNearbyToo);
                  });
            }
         }
      }
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      blackboard.put("humanAngerHandler", this);
      blackboard.onWasHit(e -> {
         if (!mob.isClient() && !e.event.wasPrevented) {
            Mob attackOwner = e.event.attacker != null ? e.event.attacker.getAttackOwner() : null;
            if (attackOwner != null && attackOwner.isPlayer) {
               if (!attackOwner.buffManager.hasBuff(BuffRegistry.BOSS_NEARBY)) {
                  if (!mob.isFriendlyClient(((PlayerMob)attackOwner).getNetworkClient())) {
                     float percent = Math.abs((float)mob.getHealth() / mob.getMaxHealth() - 1.0F);
                     this.addAnger(percent * 2.5F, attackOwner, true);
                  }
               }
            }
         }
      });
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      this.enemies.removeIf(m -> m.removed() || m.isPlayer && mob.isFriendlyClient(((PlayerMob)m).getNetworkClient()));
      if (this.anger > 0.0F) {
         if (!mob.buffManager.hasBuff(BuffRegistry.HUMAN_ANGRY) || mob.buffManager.getBuff(BuffRegistry.HUMAN_ANGRY).getDurationLeft() < 500) {
            ActiveBuff ab = new ActiveBuff(BuffRegistry.HUMAN_ANGRY, mob, 5.0F, null);
            mob.addBuff(ab, true);
         }

         Mob lastTarget = blackboard.getObject(Mob.class, this.currentTargetKey);
         this.anger = this.anger - this.angerDecreasePerSecond / 20.0F;
         Point base;
         if (mob.home != null && !mob.hasCommandOrders()) {
            base = new Point(mob.home.x * 32 + 16, mob.home.y * 32 + 16);
         } else {
            base = new Point(mob.getX(), mob.getY());
         }

         Mob target = this.enemies
            .stream()
            .filter(m -> this.validity.isValidTarget(this, mob, m, true))
            .map(m -> new ComputedObjectValue<>(m, () -> this.distance.getDistance(base, m)))
            .filter(cv -> cv.get() < this.distance.getTargetLostDistance(mob, cv.object) * 3)
            .min(Comparator.comparingDouble(cv -> cv.get().floatValue()))
            .map(cv -> (Mob)cv.object)
            .orElse(null);
         if (target != null) {
            blackboard.put(this.currentTargetKey, target);
            return AINodeResult.SUCCESS;
         }

         if (lastTarget != null && this.enemies.contains(lastTarget)) {
            blackboard.put(this.currentTargetKey, null);
            if (mob.home != null && !mob.hasCommandOrders()) {
               return this.moveToTileTask(mob.home.x, mob.home.y, null, path -> {
                  path.move(null);
                  return AINodeResult.SUCCESS;
               });
            }

            return AINodeResult.SUCCESS;
         }
      } else if (this.anger <= 0.0F && !this.enemies.isEmpty()) {
         Mob lastTarget = blackboard.getObject(Mob.class, this.currentTargetKey);
         this.anger = 0.0F;
         if (mob.buffManager.hasBuff(BuffRegistry.HUMAN_ANGRY)) {
            mob.buffManager.removeBuff(BuffRegistry.HUMAN_ANGRY, true);
         }

         if (this.enemies.contains(lastTarget)) {
            blackboard.put(this.currentTargetKey, null);
            blackboard.mover.stopMoving(mob);
         }

         this.enemies.clear();
      }

      return AINodeResult.FAILURE;
   }

   public void addAnger(float anger, Mob attackOwner, boolean alertNearbyToo) {
      float oldAnger = this.anger;
      this.anger += anger;
      T mob = this.mob();
      if (this.anger >= 1.0F) {
         if (oldAnger < 1.0F && !mob.removed()) {
            GameMessage attackMessage = mob.getRandomAttackMessage();
            if (attackMessage != null && mob.isServer()) {
               mob.getServer().network.sendToClientsWithEntity(new PacketMobChat(mob.getUniqueID(), attackMessage), mob);
            }
         }

         if (!this.enemies.contains(attackOwner)) {
            this.getBlackboard().submitEvent("resetTarget", new AIEvent());
            this.getBlackboard().submitEvent("resetPathTime", new AIEvent());
         }

         this.addEnemy(attackOwner, this.anger);
         if (alertNearbyToo) {
            mob.getLevel()
               .entityManager
               .mobs
               .getInRegionByTileRange(mob.getTileX(), mob.getTileY(), 25)
               .stream()
               .filter(m -> m.isSameTeam(mob) && m instanceof HumanMob)
               .forEach(m -> {
                  HumanAngerTargetAINode<?> humanAngerHandler = m.ai.blackboard.getObject(HumanAngerTargetAINode.class, "humanAngerHandler");
                  if (humanAngerHandler != null) {
                     humanAngerHandler.addEnemy(attackOwner, this.anger);
                  }
               });
         }
      } else if (!mob.removed()) {
         GameMessage angryMessage = mob.getRandomAngryMessage();
         if (angryMessage != null && mob.isServer()) {
            mob.getServer().network.sendToClientsWithEntity(new PacketMobChat(mob.getUniqueID(), angryMessage), mob);
         }
      }
   }

   public void addEnemy(Mob mob, float anger) {
      this.enemies.add(mob);
      this.anger = anger;
   }
}
