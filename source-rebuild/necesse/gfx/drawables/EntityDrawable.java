package necesse.gfx.drawables;

import necesse.entity.Entity;

public abstract class EntityDrawable extends LevelSortedDrawable {
   private final Entity entity;

   public EntityDrawable(Entity entity) {
      super(entity, false);
      this.entity = entity;
      this.init();
   }

   @Override
   public int getSortY() {
      return this.entity.getY();
   }
}
