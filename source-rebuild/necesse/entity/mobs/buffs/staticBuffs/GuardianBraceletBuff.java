package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class GuardianBraceletBuff extends Buff {
   public GuardianBraceletBuff() {
      this.isVisible = true;
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.ALL_DAMAGE, 0.2F);
      buff.setModifier(BuffModifiers.SPEED, 0.2F);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);
      int particleCount = 4;
      float anglePerParticle = 360.0F / particleCount;

      for (int i = 0; i < particleCount; i++) {
         int angle = (int)(i * anglePerParticle + GameRandom.globalRandom.nextFloat() * anglePerParticle);
         float dx = (float)Math.sin(Math.toRadians(angle)) * 10.0F;
         float dy = (float)Math.cos(Math.toRadians(angle)) * 10.0F;
         buff.owner
            .getLevel()
            .entityManager
            .addParticle(buff.owner, dx, dy, typeSwitcher.next())
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 12))
            .sizeFades(6, 12)
            .movesFriction(dx, dy, 0.8F)
            .givesLight(247.0F, 0.3F)
            .heightMoves(0.0F, 30.0F)
            .lifeTime(200);
      }
   }
}
