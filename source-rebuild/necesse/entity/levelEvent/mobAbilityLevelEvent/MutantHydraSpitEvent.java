package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Rectangle;
import java.awt.Shape;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobHitCooldowns;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.level.maps.LevelObjectHit;

public class MutantHydraSpitEvent extends GroundEffectEvent {
   public GameDamage damage;
   public float lingerTimeSeconds;
   protected int tickCounter;
   protected MobHitCooldowns hitCooldowns;
   protected float particleBuffer = 5.0F;
   protected ParticleTypeSwitcher gooSprayParticleSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );
   public static Rectangle hitBox = new Rectangle(-25, -20, 50, 40);

   public MutantHydraSpitEvent() {
   }

   public MutantHydraSpitEvent(Mob owner, int x, int y, GameRandom uniqueIDRandom, GameDamage damage, float lingerTimeSeconds) {
      super(owner, x, y, uniqueIDRandom);
      this.damage = damage;
      this.lingerTimeSeconds = lingerTimeSeconds;
   }

   @Override
   public boolean isNetworkImportant() {
      return true;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      this.damage.writePacket(writer);
      writer.putNextFloat(this.lingerTimeSeconds);
      writer.putNextInt(this.tickCounter);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.damage = GameDamage.fromReader(reader);
      this.lingerTimeSeconds = reader.getNextFloat();
      this.tickCounter = reader.getNextInt();
   }

   @Override
   public void init() {
      super.init();
      this.hitCooldowns = new MobHitCooldowns();
      if (this.isClient()) {
         SoundManager.playSound(GameResources.blunthit, SoundEffect.effect(this.x, this.y).volume(0.4F));
         SoundManager.playSound(GameResources.slimeSplash3, SoundEffect.effect(this.x, this.y).falloffDistance(600).volume(0.6F));
      }
   }

   @Override
   public Shape getHitBox() {
      return new Rectangle(this.x + hitBox.x, this.y + hitBox.y, hitBox.width, hitBox.height);
   }

   @Override
   public void clientHit(Mob target) {
      this.startTargetHitCooldown(target);
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
      if (this.tickCounter > 20.0F * this.lingerTimeSeconds) {
         this.over();
      } else {
         super.clientTick();
      }

      if (!this.isOver()) {
         int msLeft = (int)(this.lingerTimeSeconds * 1000.0F) - this.tickCounter * 50;
         if (msLeft >= 2000) {
            int hitBoxSize = hitBox.width * hitBox.height;
            float particlesPerSecond = hitBoxSize / 400.0F;
            int groundIdleTime = 4000;
            this.particleBuffer += 0.05F * particlesPerSecond;

            while (this.particleBuffer >= 1.0F) {
               this.particleBuffer--;
               int hitboxPadding = 5;
               float endX = this.x + GameRandom.globalRandom.getFloatBetween(hitBox.x + hitboxPadding, hitBox.x + hitBox.width - hitboxPadding);
               float endY = this.y + GameRandom.globalRandom.getFloatBetween(hitBox.y + hitboxPadding, hitBox.y + hitBox.height - hitboxPadding);
               this.getLevel()
                  .entityManager
                  .addTileParticle(endX, endY, this.gooSprayParticleSwitcher.next())
                  .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12))
                  .sizeFades(50, 60)
                  .fadesAlphaTime(100, 0)
                  .colorRandom(130.0F, 0.9F, 0.8F, 20.0F, 0.1F, 0.1F)
                  .givesLight(130.0F, 0.5F)
                  .lifeTime(groundIdleTime);
            }
         }
      }
   }

   @Override
   public void serverTick() {
      this.tickCounter++;
      if (this.tickCounter > 20.0F * this.lingerTimeSeconds) {
         this.over();
      } else {
         super.serverTick();
      }
   }
}
