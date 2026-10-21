package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Rectangle;
import java.awt.Shape;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobHitCooldowns;
import necesse.entity.mobs.hostile.GiantCaveSpiderMob;
import necesse.entity.particle.CaveSpiderSpitParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.level.maps.LevelObjectHit;

public class VenomStaffEvent extends GroundEffectEvent {
   protected int tickCounter;
   protected int hitCounter;
   protected MobHitCooldowns hitCooldowns;
   protected GameDamage damage;
   protected float resilienceGain;
   private CaveSpiderSpitParticle particle;

   public VenomStaffEvent() {
   }

   public VenomStaffEvent(Mob owner, int x, int y, GameRandom uniqueIDRandom, GameDamage damage, float resilienceGain) {
      super(owner, x, y, uniqueIDRandom);
      this.damage = damage;
      this.resilienceGain = resilienceGain;
   }

   @Override
   public void init() {
      super.init();
      this.tickCounter = 0;
      this.hitCooldowns = new MobHitCooldowns();
      if (this.isClient()) {
         this.level
            .entityManager
            .addParticle(
               this.particle = new CaveSpiderSpitParticle(this.level, this.x, this.y, 5000L, GiantCaveSpiderMob.Variant.NORMAL), Particle.GType.CRITICAL
            );
      }

      SoundManager.playSound(new SoundSettings(GameResources.slimeSplash3).volume(0.4F).pitchVariance(0.04F).fallOffDistance(1000), this.x, this.y);
   }

   @Override
   public Shape getHitBox() {
      int width = 40;
      int height = 30;
      return new Rectangle(this.x - width / 2, this.y - height / 2, width, height);
   }

   @Override
   public void clientHit(Mob target) {
      this.startTargetHitCooldown(target);
      this.hitCooldowns.startCooldown(target);
      this.hitCounter++;
      if (this.hitCounter >= 12) {
         this.over();
      }
   }

   @Override
   public void serverHit(Mob target, boolean clientSubmitted) {
      if (clientSubmitted || this.hitCooldowns.canHit(target)) {
         target.isServerHit(this.damage, 0.0F, 0.0F, 0.0F, this.owner);
         this.hitCooldowns.startCooldown(target);
         if (target.canGiveResilience(this.owner) && this.resilienceGain != 0.0F) {
            this.owner.addResilience(this.resilienceGain);
            this.resilienceGain = 0.0F;
         }

         this.hitCounter++;
         if (this.hitCounter >= 12) {
            this.over();
         }
      }
   }

   @Override
   public void hitObject(LevelObjectHit hit) {
      hit.getLevelObject().attackThrough(this.damage, this.owner);
   }

   @Override
   public boolean canHit(Mob mob) {
      return super.canHit(mob) && this.hitCooldowns.canHit(mob);
   }

   @Override
   public void clientTick() {
      this.tickCounter++;
      if (this.tickCounter > 100) {
         this.over();
      } else {
         super.clientTick();
      }
   }

   @Override
   public void serverTick() {
      this.tickCounter++;
      if (this.tickCounter > 100) {
         this.over();
      } else {
         super.serverTick();
      }
   }

   @Override
   public void over() {
      super.over();
      if (this.particle != null) {
         this.particle.despawnNow();
      }
   }
}
