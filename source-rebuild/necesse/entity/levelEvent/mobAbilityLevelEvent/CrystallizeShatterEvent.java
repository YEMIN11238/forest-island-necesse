package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.util.function.Supplier;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Mob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameTextureSection;

public class CrystallizeShatterEvent extends MobAbilityLevelEvent {
   protected CrystallizeShatterEvent.ParticleType type = CrystallizeShatterEvent.ParticleType.AMETHYST;

   public CrystallizeShatterEvent() {
   }

   public CrystallizeShatterEvent(Mob owner, CrystallizeShatterEvent.ParticleType type) {
      super(owner, new GameRandom());
      this.type = type;
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.type = reader.getNextEnum(CrystallizeShatterEvent.ParticleType.class);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextEnum(this.type);
   }

   @Override
   public void init() {
      super.init();
      if (this.isClient() && this.owner != null) {
         this.shatterCrystallizeBuff();
      }

      this.over();
   }

   public void shatterCrystallizeBuff() {
      SoundManager.playSound(GameResources.shatter2, SoundEffect.effect(this.owner).volume(2.0F).pitch(1.0F));
      this.spawnShatterParticles();
   }

   private void spawnShatterParticles() {
      int particleCount = 25;
      GameRandom random = GameRandom.globalRandom;
      ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);
      float anglePerParticle = 360.0F / particleCount;
      GameTextureSection textureSection = this.type.textureGetter.get();

      for (int i = 0; i < particleCount; i++) {
         int angle = (int)(i * anglePerParticle + random.nextFloat() * anglePerParticle);
         float dx = (float)Math.sin(Math.toRadians(angle)) * 50.0F;
         float dy = (float)Math.cos(Math.toRadians(angle)) * 50.0F;
         this.owner
            .getLevel()
            .entityManager
            .addParticle(this.owner, typeSwitcher.next())
            .sprite(textureSection.sprite(random.nextInt(4), 0, 18, 24))
            .sizeFades(22, 44)
            .movesFriction(dx * random.getFloatBetween(1.0F, 2.0F), dy * random.getFloatBetween(1.0F, 2.0F), 0.8F)
            .heightMoves(0.0F, -30.0F)
            .lifeTime(500);
      }
   }

   public enum ParticleType {
      AMETHYST(() -> GameResources.amethystShardParticles),
      SAPPHIRE(() -> GameResources.sapphireShardParticles),
      EMERALD(() -> GameResources.emeraldShardParticles),
      RUBY(() -> GameResources.rubyShardParticles),
      TOPAZ(() -> GameResources.topazShardParticles);

      public Supplier<GameTextureSection> textureGetter;

      ParticleType(Supplier<GameTextureSection> textureGetter) {
         this.textureGetter = textureGetter;
      }
   }
}
