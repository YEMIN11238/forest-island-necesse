package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.util.GameRandom;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class AscendedDashBuff extends Buff {
   public AscendedDashBuff() {
      this.shouldSave = true;
      this.isVisible = false;
      this.canCancel = false;
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.SPEED, 3.0F);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);

      for (int i = 0; i < 5; i++) {
         buff.owner
            .getLevel()
            .entityManager
            .addParticle(
               buff.owner.x + GameRandom.globalRandom.getFloatBetween(-15.0F, 15.0F),
               buff.owner.y + GameRandom.globalRandom.getFloatBetween(-15.0F, 15.0F),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .sprite(GameResources.ascendedShadeParticle.sprite(0, 0, 12))
            .movesConstant(0.0F, -10.0F)
            .ignoreLight(true)
            .height(10.0F)
            .givesLight(200.0F, 0.5F)
            .sizeFades(12, 24)
            .lifeTime(500);
      }
   }
}
