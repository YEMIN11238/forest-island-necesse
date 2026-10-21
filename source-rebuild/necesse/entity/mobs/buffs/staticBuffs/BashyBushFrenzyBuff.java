package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import java.awt.Rectangle;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;

public class BashyBushFrenzyBuff extends Buff {
   public BashyBushFrenzyBuff() {
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.ATTACK_SPEED, 0.2F);
      buff.setModifier(BuffModifiers.SPEED_FLAT, 10.0F);
   }

   @Override
   public int getStackSize(ActiveBuff buff) {
      return 5;
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      if (buff.owner.isVisible()) {
         Mob owner = buff.owner;
         Rectangle selectBox = owner.getSelectBox();
         int particleMultiplier = Math.round(buff.getStacks() * 0.25F);

         for (int i = 0; i < particleMultiplier; i++) {
            int xInterval = GameRandom.globalRandom.getIntBetween(-selectBox.width / 4, selectBox.width / 4);
            int yInterval = GameRandom.globalRandom.getIntBetween(-selectBox.height / 3, selectBox.height / 3);
            float var10001 = owner.x + xInterval;
            owner.getLevel()
               .entityManager
               .addParticle(var10001, owner.y + 16.0F + yInterval, Particle.GType.IMPORTANT_COSMETIC)
               .sprite(GameResources.bubbleParticle.sprite(0, 0, 12))
               .height(32.0F)
               .movesConstant(xInterval / 4.0F, -1.5F)
               .color(new Color(227, 103, 31, 139))
               .fadesAlphaTimeToCustomAlpha(100, 100, 0.65F)
               .size(new ParticleOption.DrawModifier() {
                  @Override
                  public void modify(SharedTextureDrawOptions.Wrapper options, int lifeTime, int timeAlive, float lifePercent) {
                     options.size(8, 8);
                  }
               })
               .lifeTime(500);
         }
      }
   }
}
