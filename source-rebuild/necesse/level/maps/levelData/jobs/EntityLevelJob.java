package necesse.level.maps.levelData.jobs;

import necesse.entity.Entity;
import necesse.level.maps.Level;

public class EntityLevelJob<T extends Entity> extends AbstractLevelJob {
   public T target;

   public EntityLevelJob(T target) {
      this.target = target;
   }

   @Override
   public boolean isValid() {
      return !this.target.removed();
   }

   @Override
   public Level getLevel() {
      return this.target.getLevel();
   }

   @Override
   public int getTileX() {
      return this.target.getTileX();
   }

   @Override
   public int getTileY() {
      return this.target.getTileY();
   }
}
