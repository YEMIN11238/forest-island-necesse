package necesse.entity.projectile.followingProjectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;

public class ChromaticBoltProjectile extends FollowingProjectile {
   public ChromaticBoltProjectile() {
   }

   public ChromaticBoltProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback
   ) {
      this.setLevel(level);
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.distance = distance;
      this.setDamage(damage);
      this.knockback = knockback;
   }

   @Override
   public void init() {
      super.init();
      this.turnSpeed = 2.0F;
      this.givesLight = true;
      this.height = 18.0F;
      this.trailOffset = -4.0F;
      this.setWidth(12.0F, true);
      this.piercing = 1;
      this.bouncing = 0;
   }

   @Override
   public Color getParticleColor() {
      float hueMod = (float)this.getLevel().getWorldEntity().getLocalTime() / 49.0F % 360.0F;
      return Color.getHSBColor(hueMod, 1.0F, 1.0F);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), this.getParticleColor(), 18.0F, 250, this.getHeight());
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.trail != null) {
         this.trail.setColor(this.getParticleColor());
      }
   }

   @Override
   public void updateTarget() {
      if (this.traveledDistance > 20.0F && this.target == null) {
         this.findTarget(m -> m.isHostile, 200.0F, 200.0F);
      }
   }

   @Override
   public void onHit(Mob mob, LevelObjectHit object, float x, float y, boolean fromPacket, ServerClient packetSubmitter) {
      super.onHit(mob, object, x, y, fromPacket, packetSubmitter);
      if (this.target != null) {
         this.isSolid = false;
      }
   }

   @Override
   protected void spawnDeathParticles() {
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
