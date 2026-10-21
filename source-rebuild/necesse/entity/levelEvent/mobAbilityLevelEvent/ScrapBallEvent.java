package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Rectangle;
import java.awt.Shape;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobHitCooldowns;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ScrapBallParticle;
import necesse.gfx.GameResources;
import necesse.level.maps.LevelObjectHit;

public class ScrapBallEvent extends GroundEffectEvent {
   private long startDelay = 2000L;
   private final int ticksActive = 10;
   private final int timeToAvoid = 500;
   private MobHitCooldowns hitCooldowns;
   private ScrapBallParticle particle;
   private GameDamage damage;
   private int tickCounter;
   private long startTime;
   private Mob target;
   protected SoundPlayer soundPlayer;

   public ScrapBallEvent() {
   }

   public ScrapBallEvent(Mob owner, Mob target, GameRandom uniqueIDRandom, GameDamage damage, long startDelay) {
      super(owner, target.getX(), target.getY(), uniqueIDRandom);
      this.target = target;
      this.damage = damage;
      this.startDelay = startDelay;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextLong(this.startDelay);
      writer.putNextInt(this.target.getUniqueID());
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.startDelay = reader.getNextLong();
      this.target = GameUtils.getLevelMob(reader.getNextInt(), this.level);
   }

   @Override
   public void init() {
      super.init();
      this.hitCooldowns = new MobHitCooldowns();
      this.startTime = this.level.getTime();
      if (this.isClient() && this.target != null) {
         this.level
            .entityManager
            .addParticle(this.particle = new ScrapBallParticle(this.level, this.target, this.startDelay, 500), true, Particle.GType.CRITICAL);
         this.soundPlayer = SoundManager.playSound(
            GameResources.haywireShredder,
            SoundEffect.effect(this.x, this.y).volume(0.75F).pitch(GameRandom.globalRandom.getFloatBetween(0.18F, 0.22F)).falloffDistance(1000)
         );
      }
   }

   @Override
   public Shape getHitBox() {
      int width = 100;
      int height = 100;
      return new Rectangle(this.x - width / 2, this.y - height / 2, width, height);
   }

   @Override
   public void clientHit(Mob target) {
      target.startHitCooldown();
      this.hitCooldowns.startCooldown(target);
   }

   @Override
   public void serverHit(Mob target, boolean clientSubmitted) {
      if (clientSubmitted || this.hitCooldowns.canHit(target)) {
         target.isServerHit(this.damage, 0.0F, 0.0F, 0.0F, this.owner);
         this.hitCooldowns.startCooldown(target);
      }
   }

   @Override
   public void clientTick() {
      this.tickCounter++;
      if (this.isEventActive()) {
         if (this.tickCounter > 10) {
            this.over();
         }

         this.tickCounter++;
      }

      super.clientTick();
      if (this.soundPlayer != null) {
         this.soundPlayer.refreshLooping(0.5F);
      }
   }

   @Override
   public void serverTick() {
      if (this.isEventActive()) {
         if (this.tickCounter > 10) {
            this.over();
         }

         this.tickCounter++;
      }

      super.serverTick();
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (this.target != null && this.level.getWorldEntity().getTime() <= this.startTime + this.startDelay - 500L) {
         this.x = (int)this.target.x;
         this.y = (int)this.target.y;
      }
   }

   @Override
   public void hitObject(LevelObjectHit hit) {
      hit.getLevelObject().attackThrough(this.damage, this.owner);
   }

   @Override
   public boolean canHit(Mob mob) {
      return !this.isEventActive() ? false : super.canHit(mob) && this.hitCooldowns.canHit(mob);
   }

   @Override
   public boolean canHit(LevelObjectHit hit) {
      return !this.isEventActive() ? false : super.canHit(hit);
   }

   public boolean isEventActive() {
      return this.level.getWorldEntity().getTime() >= this.startTime + this.startDelay;
   }

   @Override
   public void over() {
      super.over();
      if (this.particle != null) {
         this.particle.despawnNow();
      }

      if (this.soundPlayer != null) {
         this.soundPlayer.stop();
      }
   }

   @Override
   public boolean removed() {
      if (this.soundPlayer != null) {
         this.soundPlayer.stop();
      }

      return super.removed();
   }
}
