package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
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

public class DruidsGreatBowPetalProjectile extends Projectile {
   public int splitsRemaining = 3;
   public int remainingDistance;

   public DruidsGreatBowPetalProjectile() {
   }

   public DruidsGreatBowPetalProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback
   ) {
      this.setLevel(level);
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.distance = Math.min(distance, 100);
      this.remainingDistance = distance;
      this.setDamage(damage);
      this.knockback = knockback;
   }

   public DruidsGreatBowPetalProjectile(Level level, Mob owner, float x, float y, float angle, float speed, int distance, GameDamage damage, int knockback) {
      this.setLevel(level);
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setAngle(angle);
      this.speed = speed;
      this.distance = Math.min(distance, 100);
      this.remainingDistance = distance;
      this.setDamage(damage);
      this.knockback = knockback;
   }

   @Override
   public void init() {
      super.init();
      this.height = 18.0F;
      this.piercing = 0;
      this.setWidth(6.0F, false);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.remainingDistance);
      writer.putNextByteUnsigned(this.splitsRemaining);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.remainingDistance = reader.getNextInt();
      this.splitsRemaining = reader.getNextByteUnsigned();
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer()) {
         if (this.splitsRemaining > 0) {
            this.splitsRemaining--;
            if (this.traveledDistance >= this.distance || mob != null) {
               int remainingDistance = this.remainingDistance - this.distance;
               float angle = this.getAngle();
               DruidsGreatBowPetalProjectile projectile1 = new DruidsGreatBowPetalProjectile(
                  this.getLevel(), this.getOwner(), x, y, angle - 10.0F, this.speed, this.distance, this.getDamage().modFinalMultiplier(0.66F), this.knockback
               );
               if (this.modifier != null) {
                  this.modifier.initChildProjectile(projectile1, 0.5F, 2);
               }

               projectile1.distance = this.splitsRemaining > 0 ? Math.min(remainingDistance, 200) : remainingDistance;
               projectile1.remainingDistance = remainingDistance;
               projectile1.splitsRemaining = this.splitsRemaining;
               if (mob != null) {
                  projectile1.startHitCooldown(mob);
               }

               this.getLevel().entityManager.projectiles.add(projectile1);
               DruidsGreatBowPetalProjectile projectile2 = new DruidsGreatBowPetalProjectile(
                  this.getLevel(), this.getOwner(), x, y, angle + 10.0F, this.speed, this.distance, this.getDamage().modFinalMultiplier(0.66F), this.knockback
               );
               if (this.modifier != null) {
                  this.modifier.initChildProjectile(projectile2, 0.5F, 2);
               }

               projectile2.distance = this.splitsRemaining > 0 ? Math.min(remainingDistance, 200) : remainingDistance;
               projectile2.remainingDistance = remainingDistance;
               projectile2.splitsRemaining = this.splitsRemaining;
               if (mob != null) {
                  projectile2.startHitCooldown(mob);
               }

               this.getLevel().entityManager.projectiles.add(projectile2);
            }
         }
      }
   }

   @Override
   public Color getParticleColor() {
      return new Color(64, 101, 70);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(64, 101, 70), 12.0F, 200, this.getHeight());
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
         int drawY = camera.getDrawY(this.y);
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate(this.getAngle(), this.texture.getWidth() / 2, 0)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, this.getAngle(), 0);
      }
   }
}
