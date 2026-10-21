package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
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
import necesse.level.maps.light.GameLight;

public class VoidWizardCloneProjectile extends Projectile {
   protected boolean moving;

   public VoidWizardCloneProjectile() {
   }

   public VoidWizardCloneProjectile(Level level, float x, float y, Mob owner, GameDamage damage) {
      this.setLevel(level);
      this.x = x;
      this.y = y;
      this.setDir((float)(GameRandom.globalRandom.nextGaussian() * 10.0), (float)(GameRandom.globalRandom.nextGaussian() * 10.0));
      this.speed = 0.0F;
      this.setDamage(damage);
      this.knockback = 0;
      this.setDistance(100000);
      this.setOwner(owner);
      this.setAngle(this.getAngle() + (GameRandom.globalRandom.nextFloat() - 0.5F) * 40.0F);
      this.spawnTime = this.getWorldEntity().getTime();
      this.moving = false;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextBoolean(this.moving);
      writer.putNextLong(this.spawnTime);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.moving = reader.getNextBoolean();
      this.spawnTime = reader.getNextLong();
   }

   @Override
   public void init() {
      super.init();
      this.height = 18.0F;
      this.setWidth(10.0F);
      this.bouncing = 1000;
      this.piercing = 1;
      this.givesLight = true;
      this.trailOffset = 0.0F;
      if (this.moving) {
         this.speed = 100.0F;
      } else {
         this.speed = 0.0F;
      }
   }

   @Override
   protected CollisionFilter getLevelCollisionFilter() {
      return super.getLevelCollisionFilter().addFilter(tp -> tp.object().object.isWall || tp.object().object.isRock);
   }

   @Override
   public Color getParticleColor() {
      return VoidWizard.getWizardProjectileColor(this.getOwner());
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), null, 16.0F, 300, this.getHeight()) {
         @Override
         public Color getColor() {
            return VoidWizard.getWizardProjectileColor(VoidWizardCloneProjectile.this.getOwner());
         }
      };
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (!this.moving) {
         long timeSinceSpawned = this.getWorldEntity().getTime() - this.spawnTime;
         if (timeSinceSpawned >= 2000L) {
            this.speed = 100.0F;
            this.moving = true;
         }
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (!this.moving) {
         long timeSinceSpawned = this.getWorldEntity().getTime() - this.spawnTime;
         if (timeSinceSpawned >= 2000L) {
            this.speed = 100.0F;
            this.moving = true;
         }
      }

      Mob owner = this.getOwner();
      if (owner != null && owner.removed()) {
         this.remove();
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
         float timeSinceSpawned = (float)(this.getWorldEntity().getTime() - this.spawnTime);
         float rotate = timeSinceSpawned / 10.0F;
         float height = this.getHeight();
         int dim;
         if (!this.moving) {
            double movingProgress = GameMath.limit(timeSinceSpawned / 2000.0, 0.0, 1.0);
            dim = 16 + (int)(movingProgress * 16.0);
            height = (float)(height * movingProgress);
         } else {
            float pulse = timeSinceSpawned / 5.0F % 2.0F;
            if (pulse > 1.0F) {
               pulse = Math.abs(pulse - 2.0F);
            }

            dim = (int)(32.0F * (pulse / 2.0F + 1.0F));
         }

         int drawX = camera.getDrawX(this.x) - dim / 2;
         int drawY = camera.getDrawY(this.y) - dim / 2;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light.minLevelCopy(Math.min(light.getLevel() + 100.0F, 150.0F)))
            .size(dim, dim)
            .rotate(rotate, dim / 2, dim / 2)
            .pos(drawX, drawY - (int)height);
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, rotate, dim / 2);
      }
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("voidwiz", 4);
   }
}
