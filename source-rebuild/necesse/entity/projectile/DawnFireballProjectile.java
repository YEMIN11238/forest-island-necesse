package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.BuffRegistry;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.projectile.followingProjectile.FollowingProjectile;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class DawnFireballProjectile extends FollowingProjectile {
   boolean empowered = false;

   public DawnFireballProjectile() {
   }

   public DawnFireballProjectile(
      Level level, Mob owner, float x, float y, float angle, float speed, int distance, GameDamage damage, int knockback, boolean empowered
   ) {
      this.setLevel(level);
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setAngle(angle);
      this.speed = speed;
      this.distance = distance;
      this.setDamage(damage);
      this.knockback = knockback;
      this.empowered = empowered;
   }

   @Override
   public void init() {
      super.init();
      this.setWidth(16.0F);
      this.turnSpeed = this.empowered ? 0.25F : 0.15F;
      this.height = 18.0F;
      this.spawnTime = this.getWorldEntity().getTime();
      this.trailOffset = 0.0F;
      this.isSolid = false;
      this.givesLight = true;
   }

   @Override
   public Color getParticleColor() {
      return new Color(249, 155, 78);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(255, 233, 73), 10.0F, 400, this.getHeight());
   }

   @Override
   public void updateTarget() {
      if (this.traveledDistance > 50.0F) {
         this.findTarget(m -> m.isHostile, 0.0F, 250.0F);
      }
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer()) {
         if (mob != null) {
            ActiveBuff ab = new ActiveBuff(BuffRegistry.Debuffs.SPIDER_VENOM, mob, 10.0F, this.getOwner());
            mob.addBuff(ab, true);
            if (this.modifier != null) {
               this.modifier.doHitLogic(mob, object, x, y);
            }
         }
      }
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
         int drawX = camera.getDrawX(this.x) - this.texture.getWidth() / 2;
         int drawY = camera.getDrawY(this.y) - this.texture.getHeight() / 2;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate((float)(-(this.getWorldEntity().getTime() - this.spawnTime)), this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
      }
   }
}
