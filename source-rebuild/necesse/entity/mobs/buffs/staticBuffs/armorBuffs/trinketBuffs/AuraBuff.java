package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;

public abstract class AuraBuff extends Buff {
   public float particlesPerSecond = 20.0F;
   public int particleLifeTime = 2000;
   public int particleMaxHeight = 10;
   public int particleRadius = 20;

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   public abstract Color getParticleColor();

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      GNDItemMap gndData = buff.getGndData();
      float particleBuffer = gndData.getFloat("particleBuffer");
      particleBuffer += this.particlesPerSecond / 20.0F;

      while (particleBuffer >= 1.0F) {
         particleBuffer--;
         this.spawnAuraParticle(buff);
      }

      gndData.setFloat("particleBuffer", particleBuffer);
   }

   public void spawnAuraParticle(ActiveBuff buff) {
      float height = GameRandom.globalRandom.getFloatBetween(0.0F, this.particleMaxHeight);
      AtomicReference<Float> currentAngle = new AtomicReference<>(GameRandom.globalRandom.nextFloat() * 360.0F);
      ParticleOption particle = buff.owner
         .getLevel()
         .entityManager
         .addParticle(
            buff.owner.x + GameMath.sin(currentAngle.get()) * this.particleRadius,
            buff.owner.y + GameMath.cos(currentAngle.get()) * this.particleRadius * 0.75F,
            Particle.GType.CRITICAL
         )
         .color(this.getParticleColor())
         .heightMoves(height, height + 20.0F)
         .lifeTime(this.particleLifeTime)
         .sizeFades(12, 16);
      AtomicReference<Float> shouldRemoveBuffer = new AtomicReference<>(1.0F);
      particle.moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
         if (buff.owner.removed() || buff.isRemoved()) {
            float nextRemoveBuffer = shouldRemoveBuffer.get() - delta / 250.0F * 2.0F;
            shouldRemoveBuffer.set(nextRemoveBuffer);
            if (nextRemoveBuffer <= 0.0F) {
               particle.remove();
            }
         }

         float angle = currentAngle.accumulateAndGet(delta * 150.0F / 250.0F, Float::sum);
         float radius = this.particleRadius * GameMath.lerp(lifePercent, 0.5F, 1.0F);
         float distY = radius * 0.75F;
         pos.x = buff.owner.x + GameMath.sin(angle) * radius;
         pos.y = buff.owner.y + GameMath.cos(angle) * distY * 0.75F;
      });
      particle.modify((options, lifeTime, timeAlive, lifePercent) -> {
         float finalWidth = options.getWidth() * shouldRemoveBuffer.get();
         float finalHeight = options.getHeight() * shouldRemoveBuffer.get();
         options.size((int)finalWidth, (int)finalHeight);
      });
   }
}
