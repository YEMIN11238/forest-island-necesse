package necesse.entity.projectile.pathProjectile;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ElectricOrbEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;

public class StaticJellyfishProjectile extends PathProjectile {
   protected float startX;
   protected float startY;
   protected float endX;
   protected float endY;
   protected float frequency;
   protected float amplitude;
   protected SoundPlayer playingSound;

   public StaticJellyfishProjectile() {
   }

   public StaticJellyfishProjectile(
      float startX, float startY, float endX, float endY, float maxFrequency, float amplitude, float speed, GameDamage damage, Mob owner
   ) {
      this.x = startX;
      this.y = startY;
      this.startX = startX;
      this.startY = startY;
      this.endX = endX;
      this.endY = endY;
      float distance = GameMath.getExactDistance(startX, startY, endX, endY);
      int periods = Math.max(1, (int)(distance / (maxFrequency * 2.0F)));
      this.frequency = distance / periods;
      this.amplitude = amplitude;
      this.speed = speed;
      this.setDistance((int)distance);
      this.setDamage(damage);
      this.setOwner(owner);
   }

   @Override
   public void init() {
      super.init();
      this.height = 0.0F;
      this.piercing = 1000;
      this.isSolid = false;
      this.canHitMobs = false;
      this.spinningTypeSwitcher = new ParticleTypeSwitcher(Particle.GType.IMPORTANT_COSMETIC, Particle.GType.CRITICAL);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.startX);
      writer.putNextFloat(this.startY);
      writer.putNextFloat(this.endX);
      writer.putNextFloat(this.endY);
      writer.putNextFloat(this.frequency);
      writer.putNextFloat(this.amplitude);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.startX = reader.getNextFloat();
      this.startY = reader.getNextFloat();
      this.endX = reader.getNextFloat();
      this.endY = reader.getNextFloat();
      this.frequency = reader.getNextFloat();
      this.amplitude = reader.getNextFloat();
   }

   @Override
   public Float getPosition(double dist) {
      double currentDist = this.traveledDistance + dist;
      double period = currentDist / this.frequency;
      Float dir = GameMath.normalize(this.endX - this.startX, this.endY - this.startY);
      Float perpDir = GameMath.getPerpendicularDir(dir.x, dir.y);
      float currentCenterX = (float)(this.startX + dir.x * currentDist);
      float currentCenterY = (float)(this.startY + dir.y * currentDist);
      float periodProgress = (float)(period % 1.0);
      float currentAmplitude;
      if (periodProgress < 0.25F) {
         float progress = GameMath.clamp(periodProgress, 0.0F, 0.25F);
         currentAmplitude = progress * this.amplitude;
      } else if (periodProgress < 0.75F) {
         float progress = GameMath.clamp(periodProgress, 0.25F, 0.5F) - 1.0F;
         currentAmplitude = progress * -this.amplitude;
      } else {
         float progress = GameMath.clamp(periodProgress, 1.0F, 0.75F);
         currentAmplitude = progress * -this.amplitude;
      }

      return new Float(currentCenterX + perpDir.x * currentAmplitude, currentCenterY + perpDir.y * currentAmplitude);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.playingSound == null || this.playingSound.isDone()) {
         this.playingSound = SoundManager.playSound(GameResources.electricLoop, SoundEffect.effect(this).pitch(1.3F));
      }

      if (this.playingSound != null) {
         this.playingSound.refreshLooping(1.0F);
      }
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      ElectricOrbEvent event = new ElectricOrbEvent(this.getOwner(), (int)x, (int)y, GameRandom.globalRandom, this.getDamage());
      this.getLevel().entityManager.events.add(event);
   }

   @Override
   protected void modifySpinningParticle(ParticleOption particle) {
      super.modifySpinningParticle(particle);
      particle.sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 22)).sizeFades(15, 20);
   }

   @Override
   protected int getExtraSpinningParticles() {
      return 2;
   }

   @Override
   public Color getParticleColor() {
      return new Color(95, 205, 228);
   }

   @Override
   public Trail getTrail() {
      return null;
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
