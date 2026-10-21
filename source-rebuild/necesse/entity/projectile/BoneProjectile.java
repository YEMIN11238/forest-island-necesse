package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class BoneProjectile extends Projectile {
   public BoneProjectile() {
   }

   public BoneProjectile(float x, float y, float targetX, float targetY, GameDamage damage, Mob owner) {
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = 100.0F;
      this.setDamage(damage);
      this.setOwner(owner);
      this.setDistance(600);
   }

   @Override
   public void init() {
      super.init();
      this.setWidth(10.0F);
      this.height = 18.0F;
      this.spawnTime = this.getWorldEntity().getTime();
      this.trailOffset = 0.0F;
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(170, 170, 170), 10.0F, 250, 18.0F);
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
      if (!this.removed()) {
         GameLight light = level.getLightLevel(this);
         int drawX = camera.getDrawX(this.getX()) - this.texture.getWidth() / 2;
         int drawY = camera.getDrawY(this.getY()) - this.texture.getHeight() / 2;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate(this.getAngle(), this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, this.getAngle(), this.texture.getHeight() / 2);
      }
   }

   @Override
   public float getAngle() {
      return this.dx < 0.0F ? (float)(this.spawnTime - this.getWorldEntity().getTime()) : (float)(this.getWorldEntity().getTime() - this.spawnTime);
   }
}
