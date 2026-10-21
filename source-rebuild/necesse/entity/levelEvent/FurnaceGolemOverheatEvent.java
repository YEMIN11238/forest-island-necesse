package necesse.entity.levelEvent;

import java.awt.Color;
import java.awt.Shape;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.postProcessing.effects.AbstractHeatDistortionEffect;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.CirclePolygon;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.levelEvent.mobAbilityLevelEvent.GroundEffectEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobHitCooldowns;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.hostile.FurnaceGolemMob;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.level.maps.LevelObjectHit;

public class FurnaceGolemOverheatEvent extends GroundEffectEvent {
   public static int maxRange = 250;
   public static float maxRangeYMultiplier = 0.75F;
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );
   protected MobHitCooldowns hitCooldowns = new MobHitCooldowns();
   public GameDamage damage = new GameDamage(50.0F);
   public AbstractHeatDistortionEffect heatDistortionEffect;

   public FurnaceGolemOverheatEvent() {
   }

   public FurnaceGolemOverheatEvent(Mob owner, GameRandom uniqueIDRandom) {
      super(owner, owner.getX(), owner.getY(), uniqueIDRandom);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.x = reader.getNextInt();
      this.y = reader.getNextInt();
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.x);
      writer.putNextInt(this.y);
   }

   @Override
   public void init() {
      super.init();
      if (this.isClient()) {
         this.heatDistortionEffect = this.getClient().addHeatDistortEffect(new AbstractHeatDistortionEffect() {
            @Override
            public int getDrawX(GameCamera camera) {
               return camera.getDrawX(FurnaceGolemOverheatEvent.this.x);
            }

            @Override
            public int getDrawY(GameCamera camera) {
               return camera.getDrawY(FurnaceGolemOverheatEvent.this.y);
            }

            @Override
            public float getEllipseRadiusX() {
               return FurnaceGolemOverheatEvent.this.getOverheatRange() * 1.5F;
            }

            @Override
            public float getEllipseRadiusY() {
               return FurnaceGolemOverheatEvent.this.getOverheatRange() * 1.5F * FurnaceGolemOverheatEvent.maxRangeYMultiplier;
            }

            @Override
            public float getSpeed() {
               return 1.0F;
            }

            @Override
            public float getAlpha() {
               return 1.0F;
            }

            @Override
            public boolean shouldRemove() {
               return super.shouldRemove() || FurnaceGolemOverheatEvent.this.isOver();
            }

            @Override
            public long getCurrentTime() {
               return FurnaceGolemOverheatEvent.this.getLocalTime();
            }
         });
      }
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (this.owner != null) {
         this.x = (int)this.owner.x;
         this.y = (int)this.owner.y;
      }
   }

   public float getOverheatRange() {
      float overheatProgress = this.owner instanceof FurnaceGolemMob ? ((FurnaceGolemMob)this.owner).overheatProgress : 1.0F;
      return maxRange * overheatProgress;
   }

   public GameDamage getOverheatDamage(Mob target) {
      return this.owner instanceof FurnaceGolemMob ? ((FurnaceGolemMob)this.owner).getOverheatDamage(target) : this.damage;
   }

   @Override
   public Shape getHitBox() {
      int range = (int)(this.getOverheatRange() - 10.0F);
      return range <= 0 ? null : new CirclePolygon(this.x, this.y, range, (int)(range * maxRangeYMultiplier), 8);
   }

   @Override
   public void clientHit(Mob target) {
      target.startHitCooldown();
      this.hitCooldowns.startCooldown(target);

      for (int i = 0; i < 10; i++) {
         target.getLevel()
            .entityManager
            .addParticle(
               target.x + GameRandom.globalRandom.getIntBetween(-16, 16),
               target.y + GameRandom.globalRandom.getIntBetween(-16, 16),
               this.particleTypeSwitcher.next()
            )
            .sizeFades(20, 40)
            .heightMoves(16.0F, 32.0F)
            .color(
               (options, lifeTime, timeAlive, lifePercent) -> {
                  float smokeProgress = GameMath.min(lifePercent * 2.0F, 1.0F);
                  Color fireColor = new Color(255, 150, 50);
                  Color smokeColor = new Color(45, 35, 35);
                  options.color(
                     new Color(
                        GameMath.lerp(smokeProgress, fireColor.getRed(), smokeColor.getRed()),
                        GameMath.lerp(smokeProgress, fireColor.getGreen(), smokeColor.getGreen()),
                        GameMath.lerp(smokeProgress, fireColor.getBlue(), smokeColor.getBlue())
                     )
                  );
               }
            )
            .lifeTime(200 + GameRandom.globalRandom.getIntBetween(0, 500));
      }
   }

   @Override
   public void serverHit(Mob target, boolean clientSubmitted) {
      if (clientSubmitted || this.hitCooldowns.canHit(target)) {
         if (this.getOverheatDamage(target).damage != 0.0F) {
            target.isServerHit(this.getOverheatDamage(target), 0.0F, 0.0F, 0.0F, this.owner);
         }

         target.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.ON_FIRE.getID(), target, 5000, this.owner), true);
         this.hitCooldowns.startCooldown(target);
      }
   }

   @Override
   public void hitObject(LevelObjectHit hit) {
      hit.getLevelObject().attackThrough(new GameDamage(1.0F), this.owner);
   }

   @Override
   public boolean canHit(Mob mob) {
      return super.canHit(mob) && this.hitCooldowns.canHit(mob);
   }

   @Override
   public void clientTick() {
      if (this.owner.removed()) {
         this.over();
      } else {
         this.addTickParticles();
         super.clientTick();
      }
   }

   @Override
   public void serverTick() {
      if (this.owner.removed()) {
         this.over();
      } else {
         super.serverTick();
      }
   }

   private void addTickParticles() {
      if (!(this.getOverheatRange() <= 30.0F)) {
         GameRandom random = GameRandom.globalRandom;

         for (int i = 0; i < 5; i++) {
            AtomicReference<Float> currentAngleOuterCircle = new AtomicReference<>(random.nextFloat() * 360.0F);
            this.getLevel()
               .entityManager
               .addParticle(
                  this.x + GameMath.sin(currentAngleOuterCircle.get()) * 55.0F,
                  this.y + GameMath.cos(currentAngleOuterCircle.get()) * 55.0F,
                  Particle.GType.CRITICAL
               )
               .color(new Color(255, 150, 50))
               .ignoreLight(true)
               .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
                  float angle = currentAngleOuterCircle.accumulateAndGet(delta * 5.0F / 250.0F, Float::sum);
                  pos.x = this.x + GameMath.sin(angle) * this.getOverheatRange();
                  pos.y = this.y + GameMath.cos(angle) * this.getOverheatRange() * 0.8F;
               })
               .lifeTime(1000)
               .sizeFades(8, 16);
            this.getLevel()
               .entityManager
               .addParticle(
                  this.x + GameMath.sin(currentAngleOuterCircle.get()) * this.getOverheatRange(),
                  this.y + GameMath.cos(currentAngleOuterCircle.get()) * this.getOverheatRange() * 0.75F,
                  Particle.GType.CRITICAL
               )
               .color(
                  (options, lifeTime, timeAlive, lifePercent) -> {
                     float smokeProgress = GameMath.min(lifePercent * 2.0F, 1.0F);
                     Color fireColor = new Color(255, 150, 50);
                     Color smokeColor = new Color(45, 35, 35);
                     options.color(
                        new Color(
                           GameMath.lerp(smokeProgress, fireColor.getRed(), smokeColor.getRed()),
                           GameMath.lerp(smokeProgress, fireColor.getGreen(), smokeColor.getGreen()),
                           GameMath.lerp(smokeProgress, fireColor.getBlue(), smokeColor.getBlue())
                        )
                     );
                  }
               )
               .ignoreLight(true)
               .heightMoves(0.0F, 60.0F)
               .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
                  float angle = currentAngleOuterCircle.accumulateAndGet(delta * 5.0F / 250.0F, Float::sum);
                  pos.x = this.x + GameMath.sin(angle) * this.getOverheatRange();
                  pos.y = this.y + GameMath.cos(angle) * this.getOverheatRange() * 0.8F;
               })
               .lifeTime(500 + random.getIntBetween(0, 500))
               .sizeFades(8, 32);
         }
      }
   }

   @Override
   public void onDispose() {
      super.onDispose();
      if (this.heatDistortionEffect != null) {
         this.heatDistortionEffect.remove();
      }
   }
}
