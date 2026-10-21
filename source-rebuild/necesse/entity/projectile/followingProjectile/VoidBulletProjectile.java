package necesse.entity.projectile.followingProjectile;

import java.awt.Color;
import java.awt.Point;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.projectile.RicochetableProjectile;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.Level;

public class VoidBulletProjectile extends FollowingProjectile implements RicochetableProjectile {
   public VoidBulletProjectile() {
      this.height = 18.0F;
   }

   public VoidBulletProjectile(float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, Mob owner) {
      this();
      this.setLevel(owner.getLevel());
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.setDistance(distance);
      this.setDamage(damage);
      this.knockback = knockback;
      this.setOwner(owner);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.height);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.height = reader.getNextFloat();
   }

   @Override
   public void init() {
      super.init();
      this.turnSpeed = 0.1F;
      this.givesLight = true;
      this.trailOffset = 0.0F;
   }

   @Override
   public Trail getTrail() {
      Trail trail = new Trail(this, this.getLevel(), new Color(47, 0, 142), 22.0F, 100, this.height);
      trail.sprite = new GameSprite(GameResources.chains, 7, 0, 32);
      return trail;
   }

   @Override
   protected Color getWallHitColor() {
      return new Color(47, 0, 142);
   }

   @Override
   public void refreshParticleLight() {
      this.getLevel().lightManager.refreshParticleLightFloat(this.x, this.y, 260.0F, this.lightSaturation);
   }

   @Override
   public void updateTarget() {
      if (this.traveledDistance > 50.0F) {
         this.findTarget(m -> m.isHostile, 80.0F, 160.0F);
      }
   }

   @Override
   public float getTurnSpeed(int targetX, int targetY, float delta) {
      return this.getTurnSpeed(delta) * this.getTurnSpeedMod(targetX, targetY, 20.0F, 90.0F, 160.0F);
   }

   public float getTurnSpeedMod(int targetX, int targetY, float maxMod, float maxAngle, float maxDistance) {
      float distance = (float)new Point(targetX, targetY).distance(this.getX(), this.getY());
      if (distance < maxDistance && distance > 5.0F) {
         float deltaAngle = Math.abs(this.getAngleDifference(this.getAngleToTarget(targetX, targetY)));
         float angleMod = deltaAngle > maxAngle ? 1.0F : (deltaAngle - maxAngle) / maxAngle;
         float distMod = Math.abs(distance - maxDistance) / maxDistance;
         return 1.0F + distMod * maxMod + angleMod * maxMod;
      } else {
         return 1.0F;
      }
   }

   public float getTurnSpeedMod(int targetX, int targetY, float maxAngle, float maxAngleMod, float maxDistance, float maxDistMod) {
      float distance = (float)new Point(targetX, targetY).distance(this.getX(), this.getY());
      if (distance < maxDistance && distance > 5.0F) {
         float deltaAngle = Math.abs(this.getAngleDifference(this.getAngleToTarget(targetX, targetY)));
         float angleMod = deltaAngle > maxAngle ? 1.0F : deltaAngle / maxAngle;
         float distMod = distance / maxDistance;
         return 1.0F + distMod * maxDistMod + angleMod * maxAngleMod;
      } else {
         return 1.0F;
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
   }

   @Override
   public void playHitSound(float x, float y) {
      SoundManager.playSound(GameResources.gunhit, SoundEffect.effect(x, y));
   }
}
