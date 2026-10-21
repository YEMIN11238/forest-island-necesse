package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.MovementTickBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class SettlerJogBuff extends Buff implements MovementTickBuff {
   public SettlerJogBuff() {
      this.shouldSave = false;
      this.isVisible = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.SPEED_FLAT, 5.0F);
      buff.setModifier(BuffModifiers.SPEED, 0.2F);
   }

   @Override
   public void tickMovement(ActiveBuff buff, float delta) {
      Mob owner = buff.owner;
      if (owner.isClient() && (owner.dx != 0.0F || owner.dy != 0.0F)) {
         float speed = owner.getCurrentSpeed() * delta / 250.0F;
         GNDItemMap gndData = buff.getGndData();
         float particleBuffer = gndData.getFloat("particleBuffer") + speed;
         if (particleBuffer >= 15.0F) {
            particleBuffer -= 15.0F;
            float xOffset = GameRandom.globalRandom.floatGaussian() * 2.0F;
            float yOffset = GameRandom.globalRandom.floatGaussian() * 2.0F;
            boolean alternate = gndData.getBoolean("particleAlternate");
            gndData.setBoolean("particleAlternate", !alternate);
            int dir = owner.getDir();
            if (dir != 0 && dir != 2) {
               yOffset += alternate ? 4.0F : -4.0F;
            } else {
               xOffset += alternate ? 4.0F : -4.0F;
            }

            float var10001 = owner.x + xOffset;
            owner.getLevel()
               .entityManager
               .addParticle(var10001, owner.y + yOffset - 2.0F, Particle.GType.IMPORTANT_COSMETIC)
               .color(new Color(195, 222, 202))
               .sizeFadesInAndOut(10, 16, 50, 200)
               .movesConstant(owner.dx / 10.0F, owner.dy / 10.0F)
               .lifeTime(300)
               .height(2.0F);
         }

         gndData.setFloat("particleBuffer", particleBuffer);
         float soundBuffer = gndData.getFloat("soundBuffer") + Math.min(speed, 80.0F * delta / 250.0F);
         if (soundBuffer >= 45.0F) {
            soundBuffer -= 45.0F;
            SoundManager.playSound(GameResources.run, SoundEffect.effect(owner).volume(0.1F).pitch(1.1F));
         }

         gndData.setFloat("soundBuffer", soundBuffer);
      }
   }
}
