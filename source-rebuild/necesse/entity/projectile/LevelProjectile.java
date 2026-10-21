package necesse.entity.projectile;

import java.util.function.Consumer;
import necesse.level.maps.Level;

public class LevelProjectile<T extends Projectile> {
   public int uniqueID;
   private T projectile;

   public LevelProjectile(int uniqueID) {
      this.uniqueID = uniqueID;
   }

   public LevelProjectile(T projectile) {
      this.uniqueID = projectile.getUniqueID();
      this.projectile = projectile;
   }

   public LevelProjectile() {
      this(-1);
   }

   public T get(Level level) {
      if (this.uniqueID == -1) {
         if (this.projectile != null) {
            T oldProjectile = this.projectile;
            this.projectile = null;
            this.onProjectileChanged(oldProjectile, this.projectile);
         }
      } else {
         if (this.projectile != null && this.projectile.getLevel() != level) {
            level = this.onProjectileChangedLevel(this.projectile, level);
         }

         if (this.projectile == null || this.projectile.getUniqueID() != this.uniqueID || this.projectile.getLevel() != level || this.projectile.removed()) {
            T oldProjectile = this.projectile;
            this.projectile = null;

            try {
               this.projectile = (T)level.entityManager.projectiles.get(this.uniqueID, false);
               if (this.projectile != null) {
                  this.onProjectileChanged(oldProjectile, this.projectile);
               }
            } catch (ClassCastException e) {
               this.projectile = null;
            }

            if (this.projectile == null && oldProjectile != null && oldProjectile.removed()) {
               this.onProjectileRemoved(oldProjectile);
            }
         }
      }

      return this.projectile;
   }

   public void computeIfPresent(Level level, Consumer<T> consumer) {
      T projectile = this.get(level);
      if (projectile != null) {
         consumer.accept(projectile);
      }
   }

   public void onProjectileChanged(T oldProjectile, T newProjectile) {
   }

   public Level onProjectileChangedLevel(T projectile, Level currentLevel) {
      return currentLevel;
   }

   public void onProjectileRemoved(T projectile) {
   }
}
