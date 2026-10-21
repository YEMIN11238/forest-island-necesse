package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.level.maps.Level;

public class DryadPossessedBuff extends Buff {
   public DryadPossessedBuff() {
      this.shouldSave = false;
      this.isVisible = true;
      this.isImportant = true;
      this.canCancel = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.GROUNDED, true);
      buff.setModifier(BuffModifiers.POISON_DAMAGE_FLAT, 5.0F);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      GameRandom random = GameRandom.globalRandom;
      AtomicReference<Float> currentAngle = new AtomicReference<>(random.nextFloat() * 360.0F);
      float distance = 25.0F;
      Level level = buff.owner.getLevel();
      float x = buff.owner.x;
      float y = buff.owner.y;
      level.entityManager
         .addTopParticle(x + GameMath.sin(currentAngle.get()) * distance, y + GameMath.cos(currentAngle.get()) * distance, Particle.GType.CRITICAL)
         .sprite(GameResources.puffParticles.sprite(random.nextInt(5), 0, 12))
         .color(new Color(30, 177, 143))
         .givesLight(247.0F, 0.3F)
         .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
            float angle = currentAngle.accumulateAndGet(delta * 15.0F / 250.0F, Float::sum);
            pos.x = x + GameMath.sin(angle) * distance;
            pos.y = y + (x - pos.x) - angle / 10.0F + GameMath.cos(angle) * distance;
         })
         .lifeTime(1000)
         .sizeFades(12, 24);
      level.entityManager
         .addTopParticle(x + GameMath.sin(currentAngle.get()) * distance, y + GameMath.cos(currentAngle.get()) * distance, Particle.GType.CRITICAL)
         .sprite(GameResources.puffParticles.sprite(random.nextInt(5), 0, 12))
         .color(new Color(30, 177, 143))
         .givesLight(247.0F, 0.3F)
         .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
            float angle = currentAngle.accumulateAndGet(delta * 15.0F / 250.0F, Float::sum);
            pos.x = x + GameMath.sin(angle) * distance;
            pos.y = y - (x - pos.x) - angle / 10.0F + GameMath.cos(angle) * distance;
         })
         .lifeTime(1000)
         .sizeFades(12, 24);
   }
}
