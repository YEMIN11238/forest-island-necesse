package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.MovementTickBuff;
import necesse.entity.particle.AfterimageParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class KineticBootsActiveBuff extends Buff implements MovementTickBuff {
   public KineticBootsActiveBuff() {
      this.shouldSave = false;
      this.isVisible = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.SPEED_FLAT, 20.0F);
      buff.setModifier(BuffModifiers.SPEED, 0.8F);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      Mob owner = buff.owner;
      if ((owner.dx != 0.0F || owner.dy != 0.0F) && !owner.isRiding() && owner.isPlayer) {
         owner.getLevel().entityManager.addParticle(new AfterimageParticle(owner.getLevel(), (PlayerMob)owner), Particle.GType.COSMETIC);
      }
   }

   @Override
   public void tickMovement(ActiveBuff buff, float delta) {
      Mob owner = buff.owner;
      if (owner.isClient() && (owner.dx != 0.0F || owner.dy != 0.0F)) {
         float speed = owner.getCurrentSpeed() * delta / 250.0F;
         GNDItemMap gndData = buff.getGndData();
         float soundBuffer = gndData.getFloat("soundBuffer") + Math.min(speed, 80.0F * delta / 250.0F);
         if (soundBuffer >= 45.0F) {
            soundBuffer -= 45.0F;
            SoundManager.playSound(GameResources.run, SoundEffect.effect(owner).pitch(1.2F));
            SoundManager.playSound(GameResources.laserBlast2, SoundEffect.effect(owner).volume(0.5F).pitch(2.0F));
         }

         gndData.setFloat("soundBuffer", soundBuffer);
      }
   }
}
