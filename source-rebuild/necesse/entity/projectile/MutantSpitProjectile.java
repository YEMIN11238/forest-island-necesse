package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MutantSpitEvent;
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
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class MutantSpitProjectile extends Projectile {
   public MutantSpitProjectile() {
   }

   public MutantSpitProjectile(float x, float y, float targetX, float targetY, GameDamage damage, Mob owner, int distance) {
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = 160.0F;
      this.setDamage(damage);
      this.setOwner(owner);
      this.setDistance(distance);
   }

   @Override
   public void init() {
      super.init();
      this.height = 16.0F;
   }

   @Override
   public Color getParticleColor() {
      return new Color(54, 243, 30);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), this.getParticleColor(), 16.0F, 200, this.getHeight());
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
         int drawX = camera.getDrawX(this.x) - 16;
         int drawY = camera.getDrawY(this.y);
         int anim = GameUtils.getAnim(this.getWorldEntity().getTime(), 6, 400);
         final TextureDrawOptions options = this.texture
            .initDraw()
            .sprite(anim, 0, 32, 64)
            .light(light)
            .rotate(this.getAngle(), 16, 0)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         TextureDrawOptions shadowOptions = this.shadowTexture.initDraw().sprite(anim, 0, 32, 64).light(light).rotate(this.getAngle(), 16, 0).pos(drawX, drawY);
         tileList.add(tm -> shadowOptions.draw());
      }
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer()) {
         Mob owner = this.getOwner();
         if (owner != null && !owner.removed()) {
            MutantSpitEvent event = new MutantSpitEvent(owner, (int)x, (int)y, GameRandom.globalRandom, this.getDamage());
            this.getLevel().entityManager.events.add(event);
         }
      }
   }
}
