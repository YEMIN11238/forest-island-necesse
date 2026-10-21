package necesse.entity.projectile.followingProjectile;

import java.awt.Color;
import java.awt.Point;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class PhantomMissileProjectile extends FollowingProjectile {
   public PhantomMissileProjectile() {
      this.height = 18.0F;
   }

   public PhantomMissileProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback
   ) {
      this();
      this.setLevel(level);
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.setDistance(distance);
      this.setDamage(damage);
      this.knockback = knockback;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.height);
      writer.putNextFloat(this.turnSpeed);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.height = reader.getNextFloat();
      this.turnSpeed = reader.getNextFloat();
   }

   @Override
   public void init() {
      super.init();
      this.turnSpeed = 0.3F;
      this.givesLight = true;
      this.trailOffset = -8.0F;
      this.clearTargetPosWhenAligned = true;
   }

   @Override
   public Trail getTrail() {
      Trail trail = new Trail(this, this.getLevel(), new Color(108, 37, 92), 30.0F, 250, this.height);
      trail.sprite = new GameSprite(GameResources.chains, 7, 0, 32);
      return trail;
   }

   @Override
   protected Color getWallHitColor() {
      return new Color(108, 37, 92);
   }

   @Override
   public void refreshParticleLight() {
      this.getLevel().lightManager.refreshParticleLightFloat(this.x, this.y, 260.0F, this.lightSaturation);
   }

   @Override
   public void updateTarget() {
      this.findTarget(m -> m.isHostile, 100.0F, 300.0F);
      if (this.target != null) {
         this.targetPos = null;
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
         GameLight light = level.getLightLevel(this).minLevelCopy(100.0F);
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
