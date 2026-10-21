package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MobBeforeDamageOverTimeTakenEvent;
import necesse.entity.mobs.MobBeforeHitCalculatedEvent;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class MagicianInvisBuff extends Buff {
   public MagicianInvisBuff() {
      this.shouldSave = true;
      this.canCancel = false;
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.INVISIBILITY, true);
      buff.setModifier(BuffModifiers.TARGET_RANGE, -1.0F);
      eventSubscriber.subscribeEvent(MobBeforeDamageOverTimeTakenEvent.class, MobBeforeDamageOverTimeTakenEvent::prevent);
      if (buff.owner.isClient()) {
         int particleCount = 40;

         for (int i = 0; i < particleCount; i++) {
            buff.owner
               .getLevel()
               .entityManager
               .addParticle(
                  buff.owner.x + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
                  buff.owner.y + 16.0F + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
                  Particle.GType.IMPORTANT_COSMETIC
               )
               .sprite(GameResources.smokePuff.sprite(0, 0, 32))
               .lifeTime(750)
               .fadesAlphaTime(100, 250)
               .movesFriction(16.0F * (float)GameRandom.globalRandom.nextGaussian(), 5.0F * (float)GameRandom.globalRandom.nextGaussian(), 1.0F)
               .sizeFades(14, 18)
               .heightMoves(20.0F, 64.0F);
         }
      }
   }

   @Override
   public void onBeforeHitCalculated(ActiveBuff buff, MobBeforeHitCalculatedEvent event) {
      super.onBeforeHitCalculated(buff, event);
      event.prevent();
      event.showDamageTip = false;
      event.playHitSound = false;
   }
}
