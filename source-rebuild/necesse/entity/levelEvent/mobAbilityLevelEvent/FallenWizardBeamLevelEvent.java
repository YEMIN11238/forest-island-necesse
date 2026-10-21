package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D.Float;
import java.util.Collections;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.LinesSoundEmitter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.LineHitbox;
import necesse.engine.util.Ray;
import necesse.engine.util.RayLinkedList;
import necesse.entity.ParticleBeamHandler;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobHitCooldowns;
import necesse.entity.mobs.hostile.bosses.FallenWizardMob;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.LevelObjectHit;

public class FallenWizardBeamLevelEvent extends MobAbilityLevelEvent implements LinesSoundEmitter {
   public int seed;
   public long startTime;
   public int sweepTime;
   public float startAngle;
   public float endAngle;
   public float height = 14.0F;
   protected int bounces;
   protected float distance;
   protected GameDamage damage;
   protected int knockback;
   protected MobHitCooldowns hitCooldowns;
   private RayLinkedList<LevelObjectHit> lastRays;
   private ParticleBeamHandler beamHandler;
   private FallenWizardMob fallenWizardMob;
   private SoundPlayer sound;

   public FallenWizardBeamLevelEvent() {
   }

   public FallenWizardBeamLevelEvent(
      Mob owner,
      float startAngle,
      float endAngle,
      long startTime,
      int sweepTime,
      int seed,
      float distance,
      GameDamage damage,
      int knockback,
      int hitCooldown,
      int bounces
   ) {
      super(owner, new GameRandom(seed));
      this.startAngle = startAngle;
      this.endAngle = endAngle;
      this.startTime = startTime;
      this.sweepTime = sweepTime;
      this.seed = seed;
      this.distance = distance;
      this.damage = damage;
      this.knockback = knockback;
      this.hitCooldowns = new MobHitCooldowns(hitCooldown);
      this.bounces = Math.min(bounces, 100);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.seed = reader.getNextShortUnsigned();
      this.startAngle = reader.getNextFloat();
      this.endAngle = reader.getNextFloat();
      this.startTime = reader.getNextLong();
      this.sweepTime = reader.getNextInt();
      this.distance = reader.getNextFloat();
      this.bounces = reader.getNextByteUnsigned();
      this.hitCooldowns = new MobHitCooldowns(reader.getNextInt());
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextShortUnsigned(this.seed);
      writer.putNextFloat(this.startAngle);
      writer.putNextFloat(this.endAngle);
      writer.putNextLong(this.startTime);
      writer.putNextInt(this.sweepTime);
      writer.putNextFloat(this.distance);
      writer.putNextByteUnsigned(this.bounces);
      writer.putNextInt(this.hitCooldowns.hitCooldown);
   }

   @Override
   public boolean isNetworkImportant() {
      return true;
   }

   public float getCurrentAngle() {
      long currentTime = this.level.getWorldEntity().getTime();
      long timeDelta = currentTime - this.startTime;
      if (timeDelta >= this.sweepTime) {
         this.over();
         return this.endAngle;
      } else {
         float progress = Math.max(0.0F, (float)timeDelta / this.sweepTime);
         float angleDelta = this.endAngle - this.startAngle;
         return this.startAngle + angleDelta * progress;
      }
   }

   @Override
   public void init() {
      super.init();
      if (this.owner instanceof FallenWizardMob) {
         this.fallenWizardMob = (FallenWizardMob)this.owner;
      }

      if (this.owner != null && this.isClient()) {
         SoundManager.playSound(GameResources.magicbolt4, SoundEffect.effect(this).falloffDistance(1250).volume(0.8F).pitch(0.7F));
      }
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (this.owner.removed()) {
         this.over();
      }

      RayLinkedList<LevelObjectHit> rays = this.checkRayHitbox();
      if (rays != null && this.isClient()) {
         this.updateTrail(rays, this.level.tickManager().getDelta());
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      float currentAngle = this.getCurrentAngle();
      if (!this.isOver()) {
         if (this.sound == null || this.sound.isDone()) {
            this.sound = SoundManager.playSound(
               GameResources.laserBeam1, SoundEffect.effect(this).falloffDistance(750).pitch(1.0F).volume(0.8F), sp -> sp.fadeIn(1.0F)
            );
         }

         if (this.sound != null) {
            long currentTime = this.level.getWorldEntity().getTime();
            long timeRemaining = this.startTime + this.sweepTime - currentTime;
            if (timeRemaining >= 500L) {
               this.sound.refreshLooping(1.0F);
            }
         }

         if (this.fallenWizardMob != null) {
            Float dir = GameMath.getAngleDir(currentAngle);
            this.fallenWizardMob.showAttack((int)(this.fallenWizardMob.x + dir.x * 1000.0F), (int)(this.fallenWizardMob.y + dir.y * 1000.0F), false);
         } else {
            Float dir = GameMath.getAngleDir(currentAngle);
            this.owner.showAttack((int)(this.owner.x + dir.x * 1000.0F), (int)(this.owner.y + dir.y * 1000.0F), false);
         }
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      float currentAngle = this.getCurrentAngle();
      if (!this.isOver()) {
         if (this.fallenWizardMob != null) {
            Float dir = GameMath.getAngleDir(currentAngle);
            this.fallenWizardMob.showAttack((int)(this.fallenWizardMob.x + dir.x * 1000.0F), (int)(this.fallenWizardMob.y + dir.y * 1000.0F), false);
         } else {
            Float dir = GameMath.getAngleDir(currentAngle);
            this.owner.showAttack((int)(this.owner.x + dir.x * 1000.0F), (int)(this.owner.y + dir.y * 1000.0F), false);
         }
      }
   }

   private RayLinkedList<LevelObjectHit> checkRayHitbox() {
      float currentAngle = this.getCurrentAngle();
      if (this.isOver()) {
         return null;
      }

      Float dir = GameMath.getAngleDir(currentAngle);
      this.lastRays = GameUtils.castRay(
         this.level,
         this.owner.x + dir.x * 30.0F,
         this.owner.y + dir.y * 30.0F,
         dir.x,
         dir.y,
         this.distance,
         this.bounces,
         new CollisionFilter().projectileCollision()
      );

      for (Ray<LevelObjectHit> ray : this.lastRays) {
         this.handleHits(new LineHitbox(ray, 10.0F), this::canHit, null);
      }

      return this.lastRays;
   }

   @Override
   public int getHitCooldown(LevelObjectHit hit) {
      return this.hitCooldowns.hitCooldown;
   }

   @Override
   public void hit(LevelObjectHit hit) {
      super.hit(hit);
      hit.getLevelObject().attackThrough(this.damage, this.owner);
   }

   public boolean canHit(Mob mob) {
      return mob.canBeHit(this) && this.hitCooldowns.canHit(mob);
   }

   @Override
   public void clientHit(Mob target, Packet content) {
      super.clientHit(target, content);
      this.hitCooldowns.startCooldown(target);
      this.startTargetHitCooldown(target);
   }

   @Override
   public void serverHit(Mob target, Packet content, boolean clientSubmitted) {
      this.hitCooldowns.startCooldown(target);
      target.isServerHit(this.damage, target.x - this.owner.x, target.y - this.owner.y, this.knockback, this.owner);
   }

   private void updateTrail(RayLinkedList<LevelObjectHit> rays, float delta) {
      if (this.beamHandler == null) {
         this.beamHandler = new ParticleBeamHandler(this.level)
            .particleColor(new Color(83, 0, 165))
            .particleThicknessMod(4.0F)
            .thickness(25, 20)
            .speed(this.distance / 6.0F)
            .height(this.height)
            .sprite(new GameSprite(GameResources.chains, 8, 0, 32));
      }

      this.beamHandler.update(rays, delta);
   }

   @Override
   public void over() {
      if (this.beamHandler != null) {
         this.beamHandler.dispose();
      }

      if (this.owner != null) {
         this.owner.isAttacking = false;
      }

      super.over();
   }

   @Override
   public Iterable<Line2D> getSoundLines() {
      return this.lastRays == null ? Collections.emptyList() : GameUtils.mapIterable(this.lastRays.iterator(), ray -> ray);
   }
}
