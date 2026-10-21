package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicBoolean;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;

public class RunicEmpowermentActiveBuff extends Buff {
   final float startManaCost = 10.0F;
   final float manaIncreasePerTick = 0.25F;

   public RunicEmpowermentActiveBuff() {
      this.isVisible = true;
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setMaxModifier(BuffModifiers.COMBAT_MANA_REGEN, 0.0F, Integer.MAX_VALUE);
      buff.setMaxModifier(BuffModifiers.MANA_REGEN, 0.0F, Integer.MAX_VALUE);
      buff.setModifier(BuffModifiers.ALL_DAMAGE, 0.01F);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      this.tickEmpowermentBuff(buff);
      float particlesPerSecond = 30.0F;
      float particlesPerTick = particlesPerSecond / 20.0F;
      float particleBuffer = buff.getGndData().getFloat("particleBuffer", 0.0F) + particlesPerTick;
      if (particleBuffer >= 1.0F) {
         float percent = GameMath.limit(buff.getStacks() / 100.0F, 0.0F, 1.0F);
         Color color = Color.getHSBColor(GameMath.lerp(percent, 0.5F, 0.6666667F), 0.8F, buff.getGndData().getBoolean("charging") ? 0.8F : 0.5F);
         buff.owner
            .getLevel()
            .entityManager
            .addParticle(
               buff.owner.x + GameRandom.globalRandom.getFloatBetween(-15.0F, 15.0F),
               buff.owner.y + GameRandom.globalRandom.getFloatBetween(-15.0F, 15.0F),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .movesConstant(0.0F, -10.0F)
            .ignoreLight(true)
            .height(10.0F)
            .givesLight(200.0F, 0.5F)
            .color(color)
            .sizeFades(16, 20)
            .size((options, lifeTime, timeAlive, lifePercent) -> options.size((int)(20.0F * (1.0F - lifePercent)), (int)(20.0F * (1.0F - lifePercent))))
            .lifeTime(500);
         particleBuffer--;
      }

      buff.getGndData().setFloat("particleBuffer", particleBuffer);
   }

   @Override
   public void serverTick(ActiveBuff buff) {
      super.serverTick(buff);
      this.tickEmpowermentBuff(buff);
   }

   public void tickEmpowermentBuff(ActiveBuff buff) {
      if (buff.getGndData().getBoolean("charging")) {
         float stacksIncreasePerSecond = 10.0F;
         float stacksIncreasePerTick = stacksIncreasePerSecond / 20.0F;
         float manaCost = 10.0F + buff.getGndData().getFloat("additionalManaCost");
         float manaUsagePerTick = manaCost / 20.0F;
         if (buff.getStacks() == buff.getMaxStacks()) {
            buff.getGndData().setFloat("additionalManaCost", manaCost + 0.25F - 10.0F);
         }

         float remainingMana = buff.owner.getMana() - manaUsagePerTick;
         buff.owner.setManaHidden(remainingMana);
         if (remainingMana < 1.0F) {
            buff.getGndData().setBoolean("charging", false);
         }

         float stacksIncreaseBuffer = buff.getGndData().getFloat("stacksIncreaseBuffer", 0.0F) + stacksIncreasePerTick;
         if (stacksIncreaseBuffer >= 1.0F) {
            int stacksIncreaseBufferInt = (int)stacksIncreaseBuffer;
            stacksIncreaseBuffer -= stacksIncreaseBufferInt;

            for (int i = 0; i < stacksIncreaseBufferInt; i++) {
               buff.addStack(1000, null);
            }

            buff.forceManagerUpdate();
         }

         buff.getGndData().setFloat("stacksIncreaseBuffer", stacksIncreaseBuffer);
      }
   }

   @Override
   public int getStackSize(ActiveBuff buff) {
      return 100;
   }

   @Override
   public boolean overridesStackDuration() {
      return true;
   }

   @Override
   public int getStacksDisplayCount(ActiveBuff buff) {
      return 1;
   }

   @Override
   public String getDurationText(ActiveBuff buff) {
      return buff.getStacks() + "%";
   }

   @Override
   public int getRemainingStacksDuration(ActiveBuff buff, AtomicBoolean sendUpdatePacket) {
      return 25;
   }
}
