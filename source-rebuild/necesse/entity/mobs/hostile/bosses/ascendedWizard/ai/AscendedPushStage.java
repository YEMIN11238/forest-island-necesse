package necesse.entity.mobs.hostile.bosses.ascendedWizard.ai;

import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.entity.levelEvent.explosionEvent.AscendedPushExplosionEvent;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;

public class AscendedPushStage<T extends AscendedWizardMob> extends AINode<T> implements AttackStageInterface<T> {
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      int knockback = (int)((mob.isTransformed() ? 1500 : 750) * (1.0F + mob.getHealthPercent()));
      int range = mob.isTransformed() ? 300 : 200;
      if (GameUtils.streamServerClients(mob.getServer(), mob.getLevel())
         .anyMatch(c -> GameMath.diamondDistance(c.playerMob.x, c.playerMob.y, mob.x, mob.y) < range)) {
         mob.getLevel().entityManager.events.add(new AscendedPushExplosionEvent(mob.x, mob.y, range, knockback, mob));
      }
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return AINodeResult.SUCCESS;
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
