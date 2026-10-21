package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Rectangle;
import java.awt.Shape;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobHitCooldowns;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.entity.particle.PhoenixFeatherGroundFireParticle;
import necesse.gfx.GameResources;
import necesse.level.maps.LevelObjectHit;

public class PhoenixFeatherGroundFireEvent extends GroundEffectEvent {
   private GameDamage damage = new GameDamage(0.0F);
   protected MobHitCooldowns hitCooldowns = new MobHitCooldowns(1000);
   protected int tickCounter;
   protected int hitCounter;
   private final long lifetime = 10000L;
   private PhoenixFeatherGroundFireParticle particle;

   public PhoenixFeatherGroundFireEvent() {
      this.allowNullOwner = true;
   }

   public PhoenixFeatherGroundFireEvent(Mob owner, int x, int y, GameRandom uniqueIDRandom, GameDamage damage) {
      super(owner, x, y, uniqueIDRandom);
      this.damage = damage;
      this.allowNullOwner = true;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.tickCounter);
      writer.putNextShortUnsigned(this.hitCounter);
      this.damage.writePacket(writer);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.tickCounter = reader.getNextInt();
      this.hitCounter = reader.getNextShortUnsigned();
      this.damage = GameDamage.fromReader(reader);
   }

   @Override
   public void init() {
      super.init();
      if (this.isClient()) {
         int timeAlreadyDone = 50 * this.tickCounter;
         long lifeTime = 10000L - timeAlreadyDone;
         if (lifeTime > 0L) {
            this.level
               .entityManager
               .addParticle(this.particle = new PhoenixFeatherGroundFireParticle(this.level, this.x, this.y, lifeTime), true, Particle.GType.CRITICAL);
            SoundManager.playSound(
               GameResources.fizz, SoundEffect.effect(this.x, this.y).volume(0.5F).pitch(GameRandom.globalRandom.getFloatBetween(0.5F, 1.0F))
            );
         }
      }
   }

   @Override
   public Shape getHitBox() {
      int width = 24;
      int height = 24;
      return new Rectangle(this.x - width / 2, this.y - height / 2, width, height);
   }

   @Override
   public void clientHit(Mob target) {
      this.startTargetHitCooldown(target);
      this.hitCooldowns.startCooldown(target);
      this.hitCounter++;
      if (this.hitCounter >= 3) {
         this.over();
      }
   }

   @Override
   public void serverHit(Mob target, boolean clientSubmitted) {
      if (clientSubmitted || this.hitCooldowns.canHit(target)) {
         if (this.getEventDamage(target).damage != 0.0F) {
            target.addBuff(new ActiveBuff(BuffRegistry.Debuffs.ON_FIRE, target, 5.0F, this.owner), true);
            target.isServerHit(this.getEventDamage(target), 0.0F, 0.0F, 0.0F, this.owner);
         }

         this.hitCounter++;
         if (this.hitCounter >= 3) {
            this.over();
         }

         this.hitCooldowns.startCooldown(target);
      }
   }

   public GameDamage getEventDamage(Mob target) {
      return this.damage.modDamage(target.buffManager.getModifier(BuffModifiers.FIRE_DAMAGE));
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
      if (this.tickCounter > 200.0F) {
         this.over();
      } else {
         super.clientTick();
      }
   }

   @Override
   public void serverTick() {
      this.tickCounter++;
      if (this.tickCounter > 200.0F) {
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
