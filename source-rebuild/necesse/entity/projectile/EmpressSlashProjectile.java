package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;

public class EmpressSlashProjectile extends Projectile {
   protected final int startThickness = 40;

   public EmpressSlashProjectile() {
   }

   public EmpressSlashProjectile(float x, float y, float angle, GameDamage damage, Mob owner) {
      this.x = x;
      this.y = y;
      this.setAngle(angle);
      this.setDamage(damage);
      this.setOwner(owner);
      this.setDistance(500);
      this.speed = 500.0F;
   }

   @Override
   public void init() {
      super.init();
      this.piercing = Integer.MAX_VALUE;
      this.isSolid = false;
   }

   @Override
   public float getTrailThickness() {
      return 40.0F * (1.0F - this.traveledDistance / this.distance);
   }

   @Override
   public Trail getTrail() {
      Trail trail = new Trail(this, this.getLevel(), new Color(250, 50, 50), 40.0F, 250, 18.0F);
      trail.drawOnTop = true;
      return trail;
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
   }
}
