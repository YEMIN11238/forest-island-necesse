package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobTexture;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class SlimeEggProjectile extends Projectile {
   protected int sprite = GameRandom.globalRandom.nextInt(4);

   public SlimeEggProjectile() {
   }

   public SlimeEggProjectile(
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

   public SlimeEggProjectile(Level level, Mob owner, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback) {
      this(level, owner, owner.x, owner.y, targetX, targetY, speed, distance, damage, knockback);
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
      float travelPerc = GameMath.limit(this.traveledDistance / this.distance, 0.0F, 1.0F);
      float travelPercInv = Math.abs(travelPerc - 1.0F);
      float heightF = GameMath.sin(travelPerc * 180.0F);
      this.height = (int)(heightF * 200.0F + 70.0F * travelPercInv);
      return out;
   }

   @Override
   public Color getParticleColor() {
      return new Color(177, 121, 31);
   }

   @Override
   public Trail getTrail() {
      return null;
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer()) {
         Mob owner = this.getOwner();
         if (owner != null && !owner.removed()) {
            Mob slimeSpawn = MobRegistry.getMob("warriorslime", this.getLevel());
            slimeSpawn.isSummoned = true;
            slimeSpawn.canDespawn = false;
            if (!slimeSpawn.collidesWith(this.getLevel(), (int)x, (int)y)) {
               this.getLevel().entityManager.addMob(slimeSpawn, (int)x, (int)y);
            }
         }
      }
   }

   @Override
   protected void spawnDeathParticles() {
      int particles = 20;
      float anglePerParticle = 360.0F / particles;

      for (int i = 0; i < particles; i++) {
         int angle = (int)(i * anglePerParticle + GameRandom.globalRandom.nextFloat() * anglePerParticle);
         int startRange = GameRandom.globalRandom.getIntBetween(0, 10);
         float startX = this.x + (float)Math.sin(Math.toRadians(angle)) * startRange;
         float startY = this.y + (float)Math.cos(Math.toRadians(angle)) * startRange * 0.6F;
         float dx = (float)Math.sin(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(40, 60);
         float dy = (float)Math.cos(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(40, 60) * 0.6F;
         this.getLevel()
            .entityManager
            .addParticle(startX, startY, i % 4 == 0 ? Particle.GType.IMPORTANT_COSMETIC : Particle.GType.COSMETIC)
            .movesFriction(dx, dy, 0.8F)
            .colorRandom(36.0F, 0.7F, 0.6F, 10.0F, 0.1F, 0.1F)
            .heightMoves(0.0F, 50.0F)
            .lifeTime(1500);
      }

      SoundManager.playSound(GameResources.slimeSplash2, SoundEffect.effect(this).pitch(0.9F).volume(0.5F));
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
         int drawY = camera.getDrawY(this.y) - 44;
         float angle = (float)(this.getWorldEntity().getTime() - this.spawnTime) / 1.5F;
         if (this.dx < 0.0F) {
            angle = -angle;
         }

         MobTexture texture = MobRegistry.Textures.warriorSlime;
         TextureDrawOptions options = texture.body
            .initDraw()
            .sprite(1, this.sprite, 64)
            .light(light)
            .rotate(angle, 32, 44)
            .pos(drawX, drawY - (int)this.getHeight());
         float shadowAlpha = Math.abs(GameMath.limit(this.height / 400.0F, 0.0F, 1.0F) - 1.0F);
         int shadowX = camera.getDrawX(this.x) - 32;
         int shadowY = camera.getDrawY(this.y) - 54;
         TextureDrawOptions shadowOptions = texture.shadow
            .initDraw()
            .sprite(1, this.sprite, 64)
            .light(light)
            .rotate(angle, 32, 54)
            .alpha(shadowAlpha)
            .pos(shadowX, shadowY);
         topList.add(tm -> {
            shadowOptions.draw();
            options.draw();
         });
      }
   }
}
