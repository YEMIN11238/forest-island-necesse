package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import java.awt.Rectangle;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.level.maps.Level;

public class MakeshiftRobotOverclockBuff extends Buff {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.ALL_DAMAGE, 1.0F);
      buff.setModifier(BuffModifiers.SPEED, 0.2F);
      if (buff.owner.isClient()) {
         Mob owner = buff.owner;

         for (int i = 0; i < 3; i++) {
            Level level = owner.getLevel();
            level.entityManager
               .addParticle(
                  new FleshParticle(level, MobRegistry.Textures.quickTinker, i, 1, 32, owner.x, owner.y, 20.0F, 0.0F, 0.0F), Particle.GType.IMPORTANT_COSMETIC
               );
            Rectangle selectBox = owner.getSelectBox();
            int xInterval = GameRandom.globalRandom.getIntBetween(-selectBox.width / 2, selectBox.width / 2);
            int yInterval = GameRandom.globalRandom.getIntBetween(-selectBox.height / 2, selectBox.height / 2);
            float var10001 = owner.x + xInterval;
            owner.getLevel()
               .entityManager
               .addParticle(var10001, owner.y + 8.0F + yInterval, Particle.GType.IMPORTANT_COSMETIC)
               .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.nextInt(3), 0, 22))
               .height(32.0F)
               .movesConstant(xInterval / 4.0F, -1.5F)
               .color(this.getParticleColor(buff))
               .ignoreLight(true)
               .fadesAlphaTimeToCustomAlpha(100, 100, 0.65F)
               .sizeFades(22, 11)
               .lifeTime(500);
         }

         SoundManager.playSound(GameResources.lightningHammer, SoundEffect.effect(owner).pitch(1.2F));
      }
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
            int xInterval = GameRandom.globalRandom.getIntBetween(-selectBox.width / 2, selectBox.width / 2);
            int yInterval = GameRandom.globalRandom.getIntBetween(-selectBox.height / 2, selectBox.height / 2);
            float var10001 = owner.x + xInterval;
            owner.getLevel()
               .entityManager
               .addParticle(var10001, owner.y + 16.0F + yInterval, Particle.GType.IMPORTANT_COSMETIC)
               .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.nextInt(3), 0, 22))
               .height(32.0F)
               .movesConstant(xInterval / 4.0F, -1.5F)
               .color(this.getParticleColor(buff))
               .ignoreLight(true)
               .sizeFades(22, 11)
               .lifeTime(500);
         }
      }
   }

   private Color getParticleColor(ActiveBuff buff) {
      switch (buff.getStacks()) {
         case 1:
            return new Color(180, 246, 255);
         case 2:
            return new Color(18, 204, 237);
         case 3:
            return new Color(223, 220, 80);
         case 4:
            return new Color(228, 152, 66);
         default:
            return new Color(255, 55, 55);
      }
   }
}
