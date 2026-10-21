package necesse.entity.projectile;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.explosionEvent.CursedCroneSpiritBeamsExplosionLevelEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;

public class SpiritBeamProjectile extends Projectile {
   private float nextTrailUpdatePoint = 0.0F;
   private float initialX;
   private float targetX;
   private float targetY;
   public GameDamage damage;
   public boolean generateSoulsOnHit;

   public SpiritBeamProjectile() {
   }

   public SpiritBeamProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, GameDamage damage, float speed, int distance, boolean generateSoulsOnHit
   ) {
      this.setLevel(level);
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.targetX = targetX;
      this.targetY = targetY;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.distance = distance;
      this.knockback = 0;
      this.damage = damage;
      this.setDamage(new GameDamage(0.0F));
      this.generateSoulsOnHit = generateSoulsOnHit;
   }

   @Override
   public void init() {
      super.init();
      this.height = 40.0F;
      this.piercing = 999;
      this.isSolid = false;
      this.heightBasedOnDistance = true;
      this.trailOffset = 0.0F;
      this.removeIfOutOfBounds = false;
      this.canBreakObjects = false;
      this.setWidth(6.0F, false);
      this.initialX = this.x;
      if (this.isClient()) {
         SoundManager.playSound(
            GameResources.spiritBeam, SoundEffect.effect(this.targetX, this.targetY).volume(0.6F).pitch(GameRandom.globalRandom.getFloatBetween(0.8F, 1.2F))
         );
      }
   }

   @Override
   public boolean canHit(Mob mob) {
      return false;
   }

   @Override
   public void onMoveTick(Float startPos, double movedDist) {
      super.onMoveTick(startPos, movedDist);
      GameRandom random = new GameRandom();
      if (this.traveledDistance > this.distance * 0.75F && this.x < this.initialX) {
         this.x = this.x + random.getIntBetween(1, 2);
      } else if (this.traveledDistance > this.distance * 0.75F && this.x > this.initialX) {
         this.x = this.x - random.getIntBetween(1, 2);
      } else if (this.traveledDistance > this.nextTrailUpdatePoint) {
         float randomAdditionToNextPoint = random.getFloatBetween(this.distance * 0.01F, this.distance * 0.02F);
         this.nextTrailUpdatePoint += randomAdditionToNextPoint;
         float rndX = random.getIntBetween(-4, 4);
         this.x += rndX;
      }
   }

   @Override
   public Color getParticleColor() {
      return new Color(89, 227, 145);
   }

   @Override
   public Trail getTrail() {
      GameRandom random = new GameRandom();
      int rndColor = random.getIntBetween(50, 140);
      Trail trail = new Trail(this, this.getLevel(), new Color(rndColor, 227, 145), 14.0F, 750, this.getHeight());
      trail.drawOnTop = true;
      return trail;
   }

   @Override
   public float getTrailThickness() {
      float v = (this.distance - this.traveledDistance) / 16.0F;
      return GameMath.limit(v, 3.0F, 10.0F);
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
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      CursedCroneSpiritBeamsExplosionLevelEvent explosionLevelEvent = new CursedCroneSpiritBeamsExplosionLevelEvent(
         x, y, 65, this.damage, false, 0.0F, this.getOwner(), this.generateSoulsOnHit
      );
      explosionLevelEvent.level = this.getLevel();
      explosionLevelEvent.resetUniqueID(new GameRandom(this.getUniqueID()));
      this.getLevel().entityManager.events.addHidden(explosionLevelEvent);
   }
}
