package necesse.level.maps.hudManager;

import necesse.level.maps.Level;

public abstract class TemporaryHudDrawElement extends HudDrawElement {
   public int lifeTime;
   public long spawnTime;

   public TemporaryHudDrawElement(int millisecondsLifeTime) {
      this.lifeTime = millisecondsLifeTime;
   }

   @Override
   public void init(HudManager manager) {
      super.init(manager);
      this.spawnTime = this.getLevel().getWorldEntity().getLocalTime();
   }

   @Override
   public boolean isRemoved() {
      return super.isRemoved() || this.getTimeSinceSpawned() > this.lifeTime;
   }

   public int getTimeSinceSpawned() {
      Level level = this.getLevel();
      return level == null ? 0 : (int)(level.getWorldEntity().getLocalTime() - this.spawnTime);
   }

   public float getLifeProgressPercent() {
      return (float)this.getTimeSinceSpawned() / this.lifeTime;
   }
}
