package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.levelEvent.explosionEvent.RebornExplosionEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobBeforeHitCalculatedEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.HumanDrawBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class RebornBuff extends Buff implements HumanDrawBuff {
   public GameTexture eggTexture;
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );
   public FloatUpgradeValue explosionDamage = new FloatUpgradeValue(0.0F, 0.2F).setBaseValue(300.0F).setUpgradedValue(1.0F, 600.0F);

   public RebornBuff() {
      this.canCancel = false;
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.PARALYZED, true);
      buff.setModifier(BuffModifiers.GROUNDED, true);
      buff.setModifier(BuffModifiers.SLOW, 1.0F);
      buff.setModifier(BuffModifiers.INCOMING_DAMAGE_MOD, 0.0F);
      buff.setModifier(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, buff.owner.getMaxHealth() / 2.0F / (buff.getDuration() / 1000.0F));
      if (buff.owner.isClient()) {
         SoundManager.playSound(GameResources.magicbolt1, SoundEffect.effect(buff.owner.x, buff.owner.y).volume(1.5F).pitch(0.5F));

         for (int i = 0; i < 30; i++) {
            buff.owner
               .getLevel()
               .entityManager
               .addTopParticle(
                  buff.owner.x + GameRandom.globalRandom.getFloatBetween(-15.0F, 15.0F),
                  buff.owner.y + GameRandom.globalRandom.getFloatBetween(-15.0F, 15.0F),
                  Particle.GType.IMPORTANT_COSMETIC
               )
               .movesFriction(GameRandom.globalRandom.getFloatBetween(-30.0F, 30.0F), GameRandom.globalRandom.getFloatBetween(-60.0F, 15.0F), 0.8F)
               .ignoreLight(true)
               .height(10.0F)
               .givesLight(35.0F, 0.5F)
               .sizeFades(40, 20)
               .color(
                  (options, lifeTime, timeAlive, lifePercent) -> {
                     Color startColor = new Color(223, 220, 80);
                     Color endColor = new Color(223, 80, 28);
                     options.color(
                        new Color(
                           GameMath.lerp(lifePercent, startColor.getRed(), endColor.getRed()),
                           GameMath.lerp(lifePercent, startColor.getGreen(), endColor.getGreen()),
                           GameMath.lerp(lifePercent, startColor.getBlue(), endColor.getBlue())
                        )
                     );
                  }
               )
               .lifeTime(500);
         }
      }
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.eggTexture = GameTexture.fromFile("particles/rebornegg");
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);

      for (int i = 0; i < 10.0F * this.getParticleIntensity(buff); i++) {
         GameRandom random = GameRandom.globalRandom;
         if (buff.getDurationLeft() > buff.getDuration() / 10) {
            AtomicReference<Float> currentAngle = new AtomicReference<>(random.nextFloat() * 360.0F);
            float distance = random.getIntBetween(20, 30);
            float intensityMultiplier = random.getIntBetween(2, 3) * this.getParticleIntensity(buff);
            buff.owner
               .getLevel()
               .entityManager
               .addTopParticle(
                  buff.owner.x + GameMath.sin(currentAngle.get()) * distance,
                  buff.owner.y + GameMath.cos(currentAngle.get()) * distance,
                  this.particleTypeSwitcher.next()
               )
               .color(
                  (options, lifeTime, timeAlive, lifePercent) -> {
                     Color startColor = new Color(223, 220, 80);
                     Color endColor = new Color(223, 80, 28);
                     options.color(
                        new Color(
                           GameMath.lerp(lifePercent, startColor.getRed(), endColor.getRed()),
                           GameMath.lerp(lifePercent, startColor.getGreen(), endColor.getGreen()),
                           GameMath.lerp(lifePercent, startColor.getBlue(), endColor.getBlue())
                        )
                     );
                  }
               )
               .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
                  float anglex = currentAngle.accumulateAndGet(delta * 10.0F / 250.0F, Float::sum);
                  pos.x = buff.owner.x - GameMath.sin(anglex * 10.0F) * distance * intensityMultiplier;
                  pos.y = buff.owner.y - anglex / 5.0F + GameMath.cos(anglex) * intensityMultiplier;
               })
               .ignoreLight(true)
               .givesLight(35.0F, 0.8F)
               .heightMoves(0.0F, 50.0F)
               .sizeFades(10, 20)
               .lifeTime(500);
         }

         if (buff.getDurationLeft() < buff.getDuration() / 2) {
            int angle = random.nextInt(360);
            java.awt.geom.Point2D.Float dir = GameMath.getAngleDir(angle);
            float range = random.getFloatBetween(25.0F, 75.0F);
            float startX = buff.owner.x + dir.x * range;
            float startY = buff.owner.y - 16.0F;
            float endHeight = 0.0F;
            float startHeight = endHeight + dir.y * range;
            int randomLifeTime = random.getIntBetween(100, 300);
            float speed = dir.x * range * 250.0F / randomLifeTime;
            buff.owner
               .getLevel()
               .entityManager
               .addTopParticle(startX, startY, this.particleTypeSwitcher.next())
               .color(
                  (options, lifeTime, timeAlive, lifePercent) -> {
                     Color startColor = new Color(223, 220, 80);
                     Color endColor = new Color(223, 80, 28);
                     options.color(
                        new Color(
                           GameMath.lerp(lifePercent, startColor.getRed(), endColor.getRed()),
                           GameMath.lerp(lifePercent, startColor.getGreen(), endColor.getGreen()),
                           GameMath.lerp(lifePercent, startColor.getBlue(), endColor.getBlue())
                        )
                     );
                  }
               )
               .movesConstant(-speed, 0.0F)
               .sizeFades(20, 40)
               .rotates()
               .heightMoves(startHeight, endHeight)
               .fadesAlphaTime(100, 50)
               .ignoreLight(true)
               .givesLight(35.0F, 0.8F)
               .lifeTime(randomLifeTime);
         }
      }
   }

   @Override
   public void onBeforeHitCalculated(ActiveBuff buff, MobBeforeHitCalculatedEvent event) {
      super.onBeforeHitCalculated(buff, event);
      event.prevent();
   }

   @Override
   public void onRemoved(ActiveBuff buff) {
      super.onRemoved(buff);
      if (buff.owner.isServer()) {
         RebornExplosionEvent e = new RebornExplosionEvent(
            buff.owner.x, buff.owner.y, 200, new GameDamage(this.explosionDamage.getValue(buff.getUpgradeTier())), buff.owner
         );
         buff.owner.getLevel().entityManager.events.add(e);
      }
   }

   public float getPercentDone(ActiveBuff buff) {
      return GameMath.limit(1.0F - (float)buff.getDurationLeft() / buff.getDuration(), 0.0F, 1.0F);
   }

   public float getParticleIntensity(ActiveBuff buff) {
      return this.getPercentDone(buff) > 0.5F ? 1.0F - this.getPercentDone(buff) : this.getPercentDone(buff);
   }

   @Override
   public void addHumanDraw(ActiveBuff buff, HumanDrawOptions drawOptions) {
      drawOptions.alpha(0.0F);
      drawOptions.addTopDraw(
         new HumanDrawOptions.HumanDrawOptionsGetter() {
            @Override
            public DrawOptions getDrawOptions(
               Level level,
               PlayerMob player,
               int dir,
               int spriteX,
               int spriteY,
               int spriteRes,
               int drawX,
               int drawY,
               int width,
               int height,
               boolean mirrorX,
               boolean mirrorY,
               GameLight light,
               boolean hasGlowEffect,
               int glowHash,
               float alpha,
               MaskShaderOptions mask
            ) {
               return RebornBuff.this.eggTexture
                  .initDraw()
                  .size(width, height)
                  .startGlowOptions(level, glowHash)
                  .light(light)
                  .apply(hasGlowEffect)
                  .addMaskShader(mask)
                  .pos(drawX, drawY);
            }
         }
      );
   }
}
