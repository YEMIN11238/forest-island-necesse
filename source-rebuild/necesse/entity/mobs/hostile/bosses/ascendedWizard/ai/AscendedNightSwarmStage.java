package necesse.entity.mobs.hostile.bosses.ascendedWizard.ai;

import java.awt.Point;
import necesse.entity.levelEvent.AscendedBatJailLevelEvent;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;

public class AscendedNightSwarmStage<T extends AscendedWizardMob> extends AINode<T> implements AttackStageInterface<T> {
   public AscendedBatJailLevelEvent event;
   public long channelEndTime;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return mob.getTime() < this.channelEndTime ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      Point baseTile = mob.getBaseTile(blackboard);
      int centerX = baseTile.x * 32 + 16;
      int centerY = baseTile.y * 32 + 16;
      int startDistance = 800;
      int edges = mob.isTransformed() ? 6 : 4;
      int edgeLength = (int)(startDistance / (edges / 8.0F) * 2.0F);
      this.event = new AscendedBatJailLevelEvent(mob, centerX, centerY, edges, 175, startDistance, edgeLength, 3000);
      mob.getLevel().entityManager.events.addHidden(this.event);
      mob.spawnedEvents.add(this.event);
      mob.playBossSoundAbility.runAndSend(AscendedWizardMob.BossSound.NIGHT_SWARM);
      int channelTime = mob.isTransformed() ? 4000 : 6000;
      mob.startChannelAnimation.runAndSend(channelTime);
      this.channelEndTime = mob.getTime() + channelTime;
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
