package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.MovementTickBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class NitroActiveBuff extends Buff implements MovementTickBuff {
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public NitroActiveBuff() {
      this.shouldSave = false;
      this.isVisible = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.SPEED, 1.0F);
      buff.setModifier(BuffModifiers.FRICTION, 0.2F);
      buff.getGndData().setFloat("soundBuffer", 20.0F);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      for (int i = 0; i < 2; i++) {
         buff.owner
            .getLevel()
            .entityManager
            .addParticle(
               buff.owner.x + GameRandom.globalRandom.getIntBetween(-16, 16),
               buff.owner.y + GameRandom.globalRandom.getIntBetween(-5, 5),
               this.particleTypeSwitcher.next()
            )
            .sizeFades(10, 20)
            .heightMoves(0.0F, 16.0F)
            .ignoreLight(true)
            .givesLight(201.0F, 0.6F)
            .color(
               (options, lifeTime, timeAlive, lifePercent) -> {
                  float smokeProgress = GameMath.min(lifePercent * 2.0F, 1.0F);
                  Color fireColor = new Color(136, 214, 255);
                  Color smokeColor = new Color(64, 151, 234);
                  options.color(
                     new Color(
                        GameMath.lerp(smokeProgress, fireColor.getRed(), smokeColor.getRed()),
                        GameMath.lerp(smokeProgress, fireColor.getGreen(), smokeColor.getGreen()),
                        GameMath.lerp(smokeProgress, fireColor.getBlue(), smokeColor.getBlue())
                     )
                  );
               }
            )
            .lifeTime(200 + GameRandom.globalRandom.getIntBetween(0, 500));
      }
   }

   @Override
   public void tickMovement(ActiveBuff buff, float delta) {
      Mob owner = buff.owner;
      if (owner.isClient() && (owner.dx != 0.0F || owner.dy != 0.0F)) {
         float speed = owner.getCurrentSpeed() * delta / 250.0F;
         GNDItemMap gndData = buff.getGndData();
         float soundBuffer = gndData.getFloat("soundBuffer") + Math.min(speed, 80.0F * delta / 250.0F);
         if (soundBuffer >= 20.0F) {
            soundBuffer -= 20.0F;
            SoundManager.playSound(GameResources.fizz, SoundEffect.effect(owner).volume(1.0F).pitch(2.5F));
         }

         gndData.setFloat("soundBuffer", soundBuffer);
      }
   }
}
