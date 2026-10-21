package necesse.entity.projectile.followingProjectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.explosionEvent.VoidWizardExplosionEvent;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.bosses.VoidWizard;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class VoidWizardHomingProjectile extends FollowingProjectile {
   public VoidWizardHomingProjectile() {
   }

   public VoidWizardHomingProjectile(Level level, Mob owner, Mob target, GameDamage damage) {
      this.setLevel(level);
      this.x = owner.x;
      this.y = owner.y;
      this.speed = 85.0F;
      this.setTarget(target.x, target.y);
      if (owner.getDistance(target) < 960.0F) {
         this.target = target;
      } else {
         this.target = owner;
      }

      this.setDamage(damage);
      this.knockback = 0;
      this.setDistance(100000);
      this.setOwner(owner);
      this.setAngle(this.getAngle() + (GameRandom.globalRandom.nextFloat() - 0.5F) * 40.0F);
      this.moveDist(60.0);
   }

   @Override
   public void init() {
      super.init();
      this.turnSpeed = 0.075F;
      this.givesLight = true;
      this.height = 18.0F;
      this.setWidth(8.0F);
      this.spawnTime = this.getWorldEntity().getTime();
      this.doesImpactDamage = true;
   }

   @Override
   protected CollisionFilter getLevelCollisionFilter() {
      return super.getLevelCollisionFilter().addFilter(tp -> tp.object().object.isWall || tp.object().object.isRock);
   }

   @Override
   public void serverTick() {
      super.serverTick();
      Mob owner = this.getOwner();
      if (owner != null && !owner.removed()) {
         if (!this.target.isSamePlace(this.getOwner())) {
            this.onHit(null, null, this.x, this.y, false, null);
         }
      } else {
         this.remove();
      }
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer()) {
         LevelEvent e = new VoidWizardExplosionEvent(x, y, this.getOwner());
         this.getLevel().entityManager.events.add(e);
      }
   }

   @Override
   public Color getParticleColor() {
      return VoidWizard.getWizardProjectileColor(this.getOwner());
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), null, 32.0F, 500, 18.0F) {
         @Override
         public Color getColor() {
            return VoidWizard.getWizardProjectileColor(VoidWizardHomingProjectile.this.getOwner());
         }
      };
   }

   @Override
   public void updateTarget() {
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
         float rotate = (float)(this.getWorldEntity().getTime() - this.spawnTime);
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light.minLevelCopy(Math.min(light.getLevel() + 100.0F, 150.0F)))
            .rotate(rotate, this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, rotate, this.texture.getHeight() / 2);
      }
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("voidwiz", 4);
   }
}
