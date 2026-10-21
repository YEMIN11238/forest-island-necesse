package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.PhoenixFeatherGroundFireEvent;
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

public class PhoenixFeatherProjectile extends Projectile {
   protected long spawnTime;

   public PhoenixFeatherProjectile() {
   }

   public PhoenixFeatherProjectile(Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage) {
      this.setLevel(level);
      this.x = x;
      this.y = y;
      this.speed = speed;
      this.setTarget(targetX, targetY);
      this.setDamage(damage);
      this.setDistance(distance);
      this.setOwner(owner);
   }

   @Override
   public void init() {
      super.init();
      this.spawnTime = this.getWorldEntity().getTime();
      this.isSolid = false;
      this.canHitMobs = false;
      this.trailOffset = 0.0F;
   }

   @Override
   public float tickMovement(float delta) {
      float out = super.tickMovement(delta);
      float travelPercInv = Math.abs(this.getTravelPerc() - 1.0F);
      float heightF = GameMath.sin(this.getTravelPerc() * 180.0F);
      int heightBasedOnDistance = this.distance / 5;
      this.height = heightF * heightBasedOnDistance + 50.0F * travelPercInv;
      return out;
   }

   private float getTravelPerc() {
      return GameMath.limit(this.traveledDistance / this.distance, 0.0F, 1.0F);
   }

   @Override
   public Color getParticleColor() {
      return null;
   }

   @Override
   public Trail getTrail() {
      return null;
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer()) {
         PhoenixFeatherGroundFireEvent event = new PhoenixFeatherGroundFireEvent(
            this.getOwner(), this.getX(), this.getY(), GameRandom.globalRandom, this.getDamage()
         );
         this.getLevel().entityManager.events.add(event);
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
         GameLight light = level.getLightLevel(this.getTileX(), this.getTileY()).minLevelCopy(150.0F);
         int drawX = camera.getDrawX(this.x) - 16;
         int drawY = camera.getDrawY(this.y) - 16;
         float angle = (float)Math.sin((this.getWorldEntity().getTime() - this.spawnTime) / 500.0) * 180.0F;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .sprite(0, 0, 32, 32)
            .light(light)
            .rotate(angle, 16, 0)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, angle, 18);
      }
   }

   @Override
   protected void addShadowDrawables(OrderableDrawables list, int drawX, int drawY, GameLight light, float angle, int centerY) {
      this.addShadowDrawables(list, drawX, drawY, light, o -> o.rotate(angle, 16, 0).alpha(this.getTravelPerc()));
   }
}
