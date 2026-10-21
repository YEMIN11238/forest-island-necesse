package necesse.entity.projectile.followingProjectile;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameUtils;
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
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class MageSlimeBoltProjectile extends FollowingProjectile {
   public MageSlimeBoltProjectile() {
   }

   public MageSlimeBoltProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback
   ) {
      this.setLevel(level);
      this.x = x;
      this.y = y;
      this.speed = speed;
      this.setTarget(targetX, targetY);
      this.setDamage(damage);
      this.knockback = knockback;
      this.setDistance(distance);
      this.setOwner(owner);
   }

   @Override
   public void init() {
      super.init();
      this.turnSpeed = 0.13F;
      this.givesLight = true;
      this.height = 10.0F;
      this.piercing = 0;
      this.isSolid = true;
      this.particleDirOffset = -30.0F;
      this.particleRandomOffset = 3.0F;
      this.setWidth(5.0F);
   }

   @Override
   public float getTurnSpeed(int targetX, int targetY, float delta) {
      return this.getTurnSpeed(delta) * this.invDynamicTurnSpeedMod(targetX, targetY, this.getTurnRadius());
   }

   @Override
   public Color getParticleColor() {
      return new Color(43, 161, 214);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(32, 131, 176), 16.0F, 500, 10.0F);
   }

   @Override
   public void updateTarget() {
      if (!this.isClient()) {
         if (this.target == null || this.target == this.getOwner()) {
            ServerClient target = GameUtils.streamServerClients(this.getLevel())
               .min(Comparator.comparing(m -> m.playerMob.getDistance(this.getOwner())))
               .orElse(null);
            if (target != null && target.playerMob != this.getOwner() && target.playerMob.getDistance(this.getOwner()) < 960.0F) {
               this.target = target.playerMob;
               this.sendServerTargetUpdate(false);
            }
         } else if (!this.isSamePlace(this.target) || ((Mob)this.target).getDistance(this.getOwner()) > 960.0F) {
            this.target = this.getOwner();
            this.sendServerTargetUpdate(false);
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
   protected SoundSettings getSpawnSound() {
      return new SoundSettings(GameResources.zap2);
   }

   @Override
   protected SoundSettings getHitSound() {
      return new SoundSettings(GameResources.slimeSplash4).volume(0.3F);
   }

   @Override
   protected SoundSettings getDisappearSound() {
      return this.getHitSound();
   }
}
