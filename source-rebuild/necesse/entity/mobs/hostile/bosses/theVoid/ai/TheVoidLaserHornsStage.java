package necesse.entity.mobs.hostile.bosses.theVoid.ai;

import java.util.ArrayList;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.TheVoidHornBeamLevelEvent;
import necesse.entity.mobs.LevelMob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidHornMob;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;

public class TheVoidLaserHornsStage<T extends TheVoidMob> extends AINode<T> implements AttackStageInterface<T> {
   private final ArrayList<TheVoidHornBeamLevelEvent> events = new ArrayList<>();

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return !this.events.isEmpty() && !this.events.stream().allMatch(LevelEvent::isOver) ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.events.clear();

      for (LevelMob<TheVoidHornMob> spawnedHorn : mob.spawnedHorns) {
         TheVoidHornMob horn = spawnedHorn.get(mob.getLevel());
         if (horn != null) {
            TheVoidHornBeamLevelEvent event = new TheVoidHornBeamLevelEvent(
               horn,
               horn.isLeftHorn,
               90.0F + (horn.isLeftHorn ? 30.0F : -30.0F),
               90.0F + (horn.isLeftHorn ? -25.0F : 25.0F),
               mob.getTime(),
               1700,
               GameRandom.globalRandom.nextInt(),
               1200.0F,
               TheVoidMob.laserDamage,
               100,
               1000,
               0
            );
            this.events.add(event);
            mob.getLevel().entityManager.events.add(event);
            mob.spawnedEvents.add(event);
         }
      }
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
