package necesse.entity.projectile;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MutantHydraSpitEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class MutantHydraSpitProjectile extends Projectile {
   public MutantHydraSpitProjectile() {
   }

   public MutantHydraSpitProjectile(
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

   public MutantHydraSpitProjectile(Level level, Mob owner, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback) {
      this(level, owner, owner.x, owner.y, targetX, targetY, speed, distance, damage, knockback);
   }

   @Override
   public void init() {
      super.init();
      this.spinningTypeSwitcher = new ParticleTypeSwitcher(Particle.GType.COSMETIC, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.CRITICAL);
      this.spawnTime = this.getTime();
      this.isSolid = false;
      this.canHitMobs = false;
      this.trailOffset = 0.0F;
      this.particleRandomOffset = 1.0F;
      this.particleDirOffset = 0.0F;
   }

   @Override
   public float tickMovement(float delta) {
      float out = super.tickMovement(delta);
      float startHeight = 0.0F;
      float heightIncrease = 100.0F;
      float travelPerc = GameMath.limit(this.traveledDistance / this.distance, 0.0F, 1.0F);
      float travelPercInv = 1.0F - travelPerc;
      this.height = GameMath.sin(travelPerc * 180.0F) * heightIncrease + startHeight * travelPercInv;
      return out;
   }

   @Override
   public Color getParticleColor() {
      return new Color(46, 213, 19);
   }

   @Override
   public Trail getTrail() {
      return null;
   }

   @Override
   protected void modifySpinningParticle(ParticleOption particle) {
      super.modifySpinningParticle(particle);
      particle.sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12));
      particle.sizeFades(20, 30);
   }

   @Override
   protected int getSpinningParticleTopDrawOrder() {
      return 100;
   }

   @Override
   protected int getExtraSpinningParticles() {
      return super.getExtraSpinningParticles() + 2;
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer()) {
         Mob owner = this.getOwner();
         if (owner != null && !owner.removed()) {
            MutantHydraSpitEvent event = new MutantHydraSpitEvent(
               owner, (int)x, (int)y, GameRandom.globalRandom, this.getDamage(), MutantHydraBossMob.spitLingerSeconds
            );
            this.getLevel().entityManager.events.add(event);
         }
      }
   }

   @Override
   protected void spawnDeathParticles() {
      Color particleColor = this.getParticleColor();
      if (particleColor != null) {
         for (int i = 0; i < 10; i++) {
            int angle = GameRandom.globalRandom.nextInt(360);
            Float dir = GameMath.getAngleDir(angle);
            this.getLevel()
               .entityManager
               .addParticle(this.x, this.y, Particle.GType.CRITICAL)
               .movesConstant(GameRandom.globalRandom.getIntBetween(20, 50) * dir.x, GameRandom.globalRandom.getIntBetween(20, 50) * dir.y)
               .color(this.getParticleColor())
               .height(this.getHeight());
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
         float shadowAlpha = Math.abs(GameMath.limit(this.height / 250.0F, 0.0F, 1.0F) - 1.0F);
         int shadowX = camera.getDrawX(this.x) - this.shadowTexture.getWidth() / 2;
         int shadowY = camera.getDrawY(this.y) - this.shadowTexture.getHeight() / 2;
         TextureDrawOptions shadowOptions = this.shadowTexture.initDraw().light(light).rotate(this.angle).alpha(shadowAlpha).pos(shadowX, shadowY);
         topList.add(tm -> shadowOptions.draw());
      }
   }
}
