package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;

public class SandKnifeWoundBuff extends Buff {
   public SandKnifeWoundBuff() {
      this.canCancel = false;
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.POISON_DAMAGE_FLAT, 100.0F);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      if (buff.owner.isVisible()) {
         Mob owner = buff.owner;
         AtomicReference<Float> currentAngle = new AtomicReference<>(GameRandom.globalRandom.nextFloat() * 360.0F);
         float distance = 20.0F;
         owner.getLevel()
            .entityManager
            .addParticle(
               owner.x + GameMath.sin(currentAngle.get()) * distance, owner.y + GameMath.cos(currentAngle.get()) * distance * 0.75F, Particle.GType.CRITICAL
            )
            .color(new Color(192, 200, 170))
            .height(0.0F)
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               float angle = currentAngle.accumulateAndGet(delta * 150.0F / 250.0F, Float::sum);
               float distY = distance * 0.75F;
               pos.x = owner.x + GameMath.sin(angle) * distance;
               pos.y = owner.y + GameMath.cos(angle) * distY * 0.75F;
            })
            .lifeTime(1000)
            .sizeFades(16, 24);
      }
   }
}
