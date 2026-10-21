package necesse.entity.projectile.followingProjectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameUtils;
import necesse.entity.levelEvent.explosionEvent.MiniRocketExplosionEvent;
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

public class MiniRocketProjectile extends FollowingProjectile {
   public MiniRocketProjectile() {
   }

   public MiniRocketProjectile(
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
      this.turnSpeed = 0.2F;
      this.givesLight = true;
      this.height = 18.0F;
      this.trailOffset = -4.0F;
      this.setWidth(12.0F, true);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), this.getParticleColor(), 8.0F, 250, this.getHeight());
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.trail != null) {
         this.trail.setColor(this.getParticleColor());
      }
   }

   @Override
   public Color getParticleColor() {
      return new Color(255, 150, 0);
   }

   @Override
   public void updateTarget() {
      this.findTarget(m -> m.isHostile, 160.0F, 350.0F);
   }

   @Override
   public void onHit(Mob mob, LevelObjectHit object, float x, float y, boolean fromPacket, ServerClient packetSubmitter) {
      super.onHit(mob, object, x, y, fromPacket, packetSubmitter);
      MiniRocketExplosionEvent event = new MiniRocketExplosionEvent(x, y, 50, this.getDamage(), this.getOwner());
      this.getLevel().entityManager.events.add(event);
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
      if (!this.removed()) {
         GameLight light = level.getLightLevel(this);
         int drawX = camera.getDrawX(this.x) - this.texture.getWidth() / 4;
         int drawY = camera.getDrawY(this.y) - 22;
         int anim = GameUtils.getAnim(this.getWorldEntity().getTime(), 2, 100);
         final TextureDrawOptions options = this.texture
            .initDraw()
            .sprite(anim, 0, 22, 44)
            .light(light)
            .rotate(this.getAngle(), this.texture.getWidth() / 4, this.texture.getWidth() / 2)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         TextureDrawOptions shadowOptions = this.shadowTexture
            .initDraw()
            .sprite(anim, 0, 22, 44)
            .light(light)
            .rotate(this.getAngle(), this.texture.getWidth() / 4, 4)
            .pos(drawX, drawY);
         tileList.add(tm -> shadowOptions.draw());
         this.addShadowDrawables(tileList, drawX, drawY, light, this.getAngle(), 0);
      }
   }
}
