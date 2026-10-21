package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs;

import java.awt.Color;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.level.maps.Level;

public class MoonShrineBuff extends Buff {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      Level level = buff.owner.getLevel();
      if (level.tickManager().getTotalTicks() % 3L == 0L) {
         level.entityManager
            .addParticle(
               buff.owner.x + (float)(GameRandom.globalRandom.nextGaussian() * 6.0),
               buff.owner.y + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
               Particle.GType.COSMETIC
            )
            .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 3), 0, 22))
            .movesConstant(buff.owner.dx / 10.0F, buff.owner.dy / 10.0F)
            .color(new Color(200, 200, 255))
            .sizeFades(6, 10)
            .height(16.0F);
      }

      level.lightManager.refreshParticleLightFloat(buff.owner.x, buff.owner.y, 240.0F, 0.35F, 135);
   }
}
