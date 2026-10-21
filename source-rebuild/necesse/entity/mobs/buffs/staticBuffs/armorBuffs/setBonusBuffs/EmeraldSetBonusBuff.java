package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.awt.Color;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;

public class EmeraldSetBonusBuff extends SetBonusBuff {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      if (buff.owner.isClient() && !buff.getGndData().getBoolean("tooltipInit")) {
         this.spawnSetAssembledParticles(buff);
      }

      buff.setModifier(BuffModifiers.MAGIC_DAMAGE, 0.5F);
      buff.setModifier(BuffModifiers.INCOMING_DAMAGE_MOD, 1.25F);
   }

   private void spawnSetAssembledParticles(ActiveBuff buff) {
      int particleCount = 25;
      Mob owner = buff.owner;
      GameRandom random = GameRandom.globalRandom;
      ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);
      float anglePerParticle = 360.0F / particleCount;

      for (int i = 0; i < particleCount; i++) {
         int angle = (int)(i * anglePerParticle + random.nextFloat() * anglePerParticle);
         float dx = (float)Math.sin(Math.toRadians(angle)) * 50.0F;
         float dy = (float)Math.cos(Math.toRadians(angle)) * 50.0F;
         owner.getLevel()
            .entityManager
            .addParticle(owner, typeSwitcher.next())
            .sprite(GameResources.magicSparkParticles.sprite(random.nextInt(4), 0, 22))
            .sizeFades(22, 44)
            .movesFriction(dx * 2.0F, dy * 2.0F, 0.8F)
            .color(new Color(0, 191, 163))
            .givesLight(247.0F, 0.3F)
            .heightMoves(0.0F, 30.0F)
            .lifeTime(1500);
      }
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "emeraldset"), 400);
      return tooltips;
   }
}
