package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import java.awt.Point;
import java.util.function.Function;
import necesse.engine.util.GameRandom;
import necesse.entity.Entity;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;
import necesse.entity.projectile.MutantHydraGooSprayProjectile;
import necesse.entity.projectile.Projectile;
import necesse.inventory.item.toolItem.projectileToolItem.ProjectileToolItem;

public class HeadGooSprayStage<T extends MutantHydraBossMob> extends AINode<T> implements AttackStageInterface<T> {
   public MutantHydraHeadType head;
   public Function<T, Integer> totalTimeGetter;
   public MutantHydraBossMob.HeadData currentHead;
   public Projectile currentProjectile;

   public HeadGooSprayStage(MutantHydraHeadType head, Function<T, Integer> totalTimeGetter) {
      this.head = head;
      this.totalTimeGetter = totalTimeGetter;
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
      this.currentHead = this.head.getter.apply(mob);
      MutantHydraHeadMob headMob = this.currentHead.getHeadMob();
      if (currentTarget != null && headMob != null) {
         Point startPosition = ProjectileToolItem.controlledRangePosition(
            GameRandom.globalRandom, mob.getX(), mob.getY(), currentTarget.getX(), currentTarget.getY(), 100, 50, 50
         );
         int totalTime = this.totalTimeGetter.apply(mob);
         int distance = (int)Entity.getPositionAfterMillis(30.0F, totalTime);
         this.currentProjectile = new MutantHydraGooSprayProjectile(
            startPosition.x, startPosition.y, currentTarget, 30.0F, distance, MutantHydraBossMob.gooSprayDamage, 50, mob
         );
         mob.getLevel().entityManager.projectiles.add(this.currentProjectile);
         headMob.startGooSprayAbility.runAndSend(this.currentProjectile);
      }
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return this.currentProjectile != null && !this.currentProjectile.removed() ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
      if (this.currentHead != null) {
         MutantHydraHeadMob headMob = this.currentHead.getHeadMob();
         if (headMob != null) {
            headMob.stopGooSprayAbility.runAndSend();
         }
      }
   }
}
