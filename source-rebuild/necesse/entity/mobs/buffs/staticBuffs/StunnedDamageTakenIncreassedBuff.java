package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;

public class StunnedDamageTakenIncreassedBuff extends Buff {
   public StunnedDamageTakenIncreassedBuff() {
      this.shouldSave = false;
      this.isVisible = true;
      this.isImportant = true;
      this.canCancel = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.GROUNDED, true);
      buff.setModifier(BuffModifiers.FRICTION, 0.0F);
      buff.setModifier(BuffModifiers.KNOCKBACK_INCOMING_MOD, 0.1F);
      buff.setModifier(BuffModifiers.INCOMING_DAMAGE_MOD, 1.25F);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      if (buff.owner.isVisible() && GameRandom.globalRandom.getChance(0.5)) {
         Mob owner = buff.owner;
         owner.getLevel()
            .entityManager
            .addTopParticle(
               owner.x + (float)(GameRandom.globalRandom.nextGaussian() * 5.0),
               owner.y + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .sprite(GameResources.powerupParticle.sprite(0, 0, 8))
            .color(new Color(241, 99, 130, 255))
            .dontRotate()
            .height(0.0F)
            .movesConstant(0.0F, -4.0F)
            .fadesAlphaTimeToCustomAlpha(200, 200, 0.9F)
            .size(new ParticleOption.DrawModifier() {
               @Override
               public void modify(SharedTextureDrawOptions.Wrapper options, int lifeTime, int timeAlive, float lifePercent) {
                  options.size(12, 12);
               }
            })
            .lifeTime(1000);
      }
   }
}
