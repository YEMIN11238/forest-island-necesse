package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.HumanDrawBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.human.HumanDrawOptions;

public class AshenBurialBuff extends Buff implements HumanDrawBuff {
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public AshenBurialBuff() {
      this.canCancel = false;
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.GROUNDED, true);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      float particleBuffer = buff.getGndData().getFloat("particleBuffer", 0.0F);
      float particlesPerSecond = 40.0F;

      for (particleBuffer += particlesPerSecond / 20.0F; particleBuffer >= 1.0F; particleBuffer--) {
         if (buff.owner.isClient()) {
            GameRandom random = GameRandom.globalRandom;
            AtomicReference<Float> currentAngle = new AtomicReference<>(random.nextFloat() * 360.0F);
            float distance = 35.0F;
            buff.owner
               .getLevel()
               .entityManager
               .addParticle(
                  buff.owner.x + GameMath.sin(currentAngle.get()) * distance + random.getIntBetween(-5, 5),
                  buff.owner.y + GameMath.cos(currentAngle.get()) * distance + random.getIntBetween(-5, 5) * 0.85F,
                  this.particleTypeSwitcher.next()
               )
               .sprite(GameResources.puffParticles.sprite(random.getIntBetween(0, 4), 0, 12))
               .height(-16.0F)
               .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
                  float angle = currentAngle.accumulateAndGet(delta * 150.0F / 250.0F, Float::sum);
                  float distY = distance * lifePercent * 0.85F;
                  pos.x = buff.owner.x + GameMath.sin(angle) * (distance * lifePercent);
                  pos.y = buff.owner.y - 16.0F + GameMath.cos(angle) * distY * 0.85F;
               })
               .color(new Color(75, 75, 75))
               .lifeTime(1000)
               .sizeFades(50, 24);
         }
      }

      buff.getGndData().setFloat("particleBuffer", particleBuffer);
   }

   @Override
   public void addHumanDraw(ActiveBuff buff, HumanDrawOptions drawOptions) {
      MaskShaderOptions swimMask = buff.owner.getSwimMaskShaderOptions(1.0F);
      drawOptions.mask(swimMask);
   }
}
