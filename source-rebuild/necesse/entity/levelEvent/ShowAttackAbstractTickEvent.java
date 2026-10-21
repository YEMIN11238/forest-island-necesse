package necesse.entity.levelEvent;

import necesse.entity.Entity;

public abstract class ShowAttackAbstractTickEvent<T extends Entity> extends LevelEvent {
   protected int totalTicksPerAttack;
   protected float lastTicksAttackProgress;
   protected T entity;

   public ShowAttackAbstractTickEvent() {
   }

   public ShowAttackAbstractTickEvent(T entity, int totalTicksPerAttack) {
      super(false);
      this.entity = entity;
      this.totalTicksPerAttack = totalTicksPerAttack;
   }

   @Override
   public void init() {
      super.init();
      if (this.entity == null) {
         this.over();
      }
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (!this.isOver()) {
         if (!this.isAttackValid(this.entity)) {
            this.over();
         } else {
            float nextProgress = this.getAttackProgress(this.entity);
            float progressPerTick = 1.0F / this.totalTicksPerAttack;

            while (true) {
               float nextProgressDelta = nextProgress - this.lastTicksAttackProgress;
               if (!(nextProgressDelta >= progressPerTick)) {
                  return;
               }

               this.tick(this.entity, this.lastTicksAttackProgress);
               this.lastTicksAttackProgress += progressPerTick;
            }
         }
      }
   }

   public abstract void tick(T var1, float var2);

   public abstract float getAttackProgress(T var1);

   public abstract boolean isAttackValid(T var1);
}
