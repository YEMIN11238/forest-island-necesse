package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Rectangle;
import java.awt.Shape;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobHitCooldowns;
import necesse.entity.particle.MutantAcidParticle;
import necesse.entity.particle.Particle;
import necesse.level.maps.LevelObjectHit;

public class MutantSpitEvent extends GroundEffectEvent {
   public GameDamage damage;
   protected int tickCounter;
   protected int hitsRemaining;
   protected MobHitCooldowns hitCooldowns;
   protected MutantAcidParticle particle;

   public MutantSpitEvent() {
      this.allowNullOwner = true;
   }

   public MutantSpitEvent(Mob owner, int x, int y, GameRandom uniqueIDRandom, GameDamage damage) {
      super(owner, x, y, uniqueIDRandom);
      this.damage = damage;
      this.allowNullOwner = true;
      this.hitsRemaining = 32767;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      this.damage.writePacket(writer);
      writer.putNextShortUnsigned(this.hitsRemaining);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.damage = GameDamage.fromReader(reader);
      this.hitsRemaining = reader.getNextShortUnsigned();
   }

   @Override
   public void init() {
      super.init();
      this.tickCounter = 0;
      this.hitCooldowns = new MobHitCooldowns();
      if (this.isClient()) {
         this.level.entityManager.addParticle(this.particle = new MutantAcidParticle(this.level, this.x, this.y, 5000L), true, Particle.GType.CRITICAL);
      }
   }

   @Override
   public Shape getHitBox() {
      int width = 40;
      int height = 30;
      return new Rectangle(this.x - width / 2, this.y - height / 2, width, height);
   }

   @Override
   public void clientHit(Mob target) {
      target.startHitCooldown();
      this.hitCooldowns.startCooldown(target);
      this.hitsRemaining--;
      if (this.hitsRemaining <= 0) {
         this.over();
      }
   }

   @Override
   public void serverHit(Mob target, boolean clientSubmitted) {
      if (clientSubmitted || this.hitCooldowns.canHit(target)) {
         target.isServerHit(this.damage, 0.0F, 0.0F, 0.0F, null);
         this.hitCooldowns.startCooldown(target);
         this.hitsRemaining--;
         if (this.hitsRemaining <= 0) {
            this.over();
         }
      }
   }

   @Override
   public void hitObject(LevelObjectHit hit) {
      hit.getLevelObject().attackThrough(this.damage, null);
   }

   @Override
   public boolean canHit(Mob mob) {
      return !mob.isHostile && this.hitCooldowns.canHit(mob);
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
