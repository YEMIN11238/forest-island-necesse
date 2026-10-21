package necesse.entity.mobs.ai.behaviourTree.leaves;

import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;

public abstract class SpawnProjectilesOnHealthLossAINode<T extends Mob> extends AINode<T> {
   public int totalProjectiles;
   public int maxPerSecond;
   public boolean isPaused;
   public int lastHealth;
   public float nextProjectile;

   public SpawnProjectilesOnHealthLossAINode(int totalProjectiles, int maxPerSecond) {
      this.totalProjectiles = totalProjectiles;
      this.maxPerSecond = maxPerSecond;
   }

   public SpawnProjectilesOnHealthLossAINode(int totalProjectiles) {
      this(totalProjectiles, 5);
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   public float getTotalProjectiles(T mob) {
      return this.totalProjectiles;
   }

   @Override
   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (!this.isPaused) {
         int nextHealth = mob.getHealth();
         int lostHealth = this.lastHealth - nextHealth;
         this.lastHealth = nextHealth;
         if (lostHealth > 0) {
            float modifier = (float)mob.getMaxHealth() / mob.getMaxHealthFlat();
            float lifePerProjectile = mob.getMaxHealthFlat() / this.getTotalProjectiles(mob) * modifier;
            float increase = lostHealth / lifePerProjectile;
            this.nextProjectile = this.nextProjectile + Math.min(this.maxPerSecond / 20.0F, increase);
         }

         if (this.nextProjectile > 1.0F) {
            this.shootProjectile(mob);
            this.nextProjectile--;
         }
      }

      return AINodeResult.SUCCESS;
   }

   public void pause() {
      this.isPaused = true;
   }

   public void resume() {
      if (this.isPaused) {
         this.lastHealth = this.mob().getHealth();
      }
   }

   public abstract void shootProjectile(T var1);
}
