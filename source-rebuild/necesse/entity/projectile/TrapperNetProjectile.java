package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class TrapperNetProjectile extends Projectile {
   protected int nettedDuration;

   public TrapperNetProjectile() {
   }

   public TrapperNetProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, int nettedDuration
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
      this.distance = distance;
      this.nettedDuration = nettedDuration;
   }

   public TrapperNetProjectile(
      Level level, Mob owner, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, int nettedDuration
   ) {
      this(level, owner, owner.x, owner.y, targetX, targetY, speed, distance, damage, knockback, nettedDuration);
   }

   @Override
   public void init() {
      super.init();
      this.spawnTime = this.getWorldEntity().getTime();
      this.isSolid = true;
      this.canHitMobs = true;
      this.width = 32.0F;
      this.trailOffset = 0.0F;
      this.doesImpactDamage = false;
   }

   @Override
   public float tickMovement(float delta) {
      float out = super.tickMovement(delta);
      float travelPerc = GameMath.limit(this.traveledDistance / this.distance, 0.0F, 1.0F);
      float travelPercInv = Math.abs(travelPerc - 1.0F);
      float heightF = GameMath.sin(travelPerc * 180.0F);
      int heightBasedOnDistance = this.distance / 10;
      this.height = heightF * heightBasedOnDistance + 10.0F * travelPercInv;
      return out;
   }

   @Override
   public Trail getTrail() {
      return null;
   }

   @Override
   public Color getParticleColor() {
      return new Color(69, 229, 193);
   }

   @Override
   public float getParticleChance() {
      return 0.5F;
   }

   @Override
   public boolean canHit(Mob mob) {
      return super.canHit(mob) && !mob.isCritter;
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer()) {
         if (mob != null && !mob.isBoss() && mob.getHitBox().width <= 32) {
            mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.NETTED_DEBUFF, mob, this.nettedDuration, this.getOwner()), true);
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
         GameLight light = level.getLightLevel(this.getTileX(), this.getTileY());
         int drawX = camera.getDrawX(this.x) - 32;
         int drawY = camera.getDrawY(this.y) - 16;
         TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate(this.angle - 90.0F, this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY - (int)this.getHeight());
         float shadowAlpha = Math.abs(GameMath.limit(this.height / 300.0F, 0.0F, 1.0F) - 1.0F);
         int shadowX = camera.getDrawX(this.x) - this.shadowTexture.getWidth() / 2;
         int shadowY = camera.getDrawY(this.y) - this.shadowTexture.getHeight() / 2;
         TextureDrawOptions shadowOptions = this.shadowTexture
            .initDraw()
            .light(light)
            .rotate(this.angle - 90.0F, this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .alpha(shadowAlpha)
            .pos(shadowX, shadowY);
         topList.add(tm -> {
            shadowOptions.draw();
            options.draw();
         });
      }
   }

   @Override
   protected SoundSettings getSpawnSound() {
      return new SoundSettings(GameResources.shotgun).volume(0.1F);
   }

   @Override
   protected SoundSettings getHitSound() {
      return new SoundSettings(GameResources.bloodClawShoot).fallOffDistance(1000);
   }
}
