package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.awt.Color;
import java.util.LinkedList;
import necesse.engine.localization.Localization;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobBeforeDamageOverTimeTakenEvent;
import necesse.entity.mobs.MobBeforeHitCalculatedEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.HumanDrawBuff;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.particle.Particle;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemStatTip;
import necesse.level.maps.Level;

public class PhoenixSetBonusBuff extends SetBonusBuff implements HumanDrawBuff {
   public static float EGG_DURATION_SECONDS = 5.0F;
   public static float EGG_COOLDOWN_SECONDS = 300.0F;
   private GameTexture wingsTexture;

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      eventSubscriber.subscribeEvent(MobBeforeDamageOverTimeTakenEvent.class, event -> {
         if (this.runLifeLineLogic(buff, event.getExpectedHealth())) {
            event.prevent();
         }
      });
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.wingsTexture = GameTexture.fromFile("particles/phoenixsetwings");
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      if (this.shouldShowWings(buff) && !this.isOnCooldown(buff)) {
         Mob owner = buff.owner;
         owner.getLevel()
            .entityManager
            .addParticle(
               owner.x - 20.0F + (float)(GameRandom.globalRandom.nextGaussian() * 6.0),
               owner.y - 20.0F + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .color(
               (options, lifeTime, timeAlive, lifePercent) -> {
                  Color startColor = new Color(223, 220, 80);
                  Color endColor = new Color(225, 30, 15);
                  options.color(
                     new Color(
                        GameMath.lerp(lifePercent, startColor.getRed(), endColor.getRed()),
                        GameMath.lerp(lifePercent, startColor.getGreen(), endColor.getGreen()),
                        GameMath.lerp(lifePercent, startColor.getBlue(), endColor.getBlue())
                     )
                  );
               }
            )
            .ignoreLight(true)
            .givesLight(0.0F, 0.5F)
            .alpha(0.75F)
            .lifeTime(200);
         owner.getLevel()
            .entityManager
            .addParticle(
               owner.x + 20.0F + (float)(GameRandom.globalRandom.nextGaussian() * 6.0),
               owner.y - 20.0F + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .color(
               (options, lifeTime, timeAlive, lifePercent) -> {
                  Color startColor = new Color(223, 220, 80);
                  Color endColor = new Color(225, 30, 15);
                  options.color(
                     new Color(
                        GameMath.lerp(lifePercent, startColor.getRed(), endColor.getRed()),
                        GameMath.lerp(lifePercent, startColor.getGreen(), endColor.getGreen()),
                        GameMath.lerp(lifePercent, startColor.getBlue(), endColor.getBlue())
                     )
                  );
               }
            )
            .ignoreLight(true)
            .givesLight(0.0F, 0.5F)
            .alpha(0.75F)
            .lifeTime(200);
      }
   }

   @Override
   public void onBeforeHitCalculated(ActiveBuff buff, MobBeforeHitCalculatedEvent event) {
      super.onBeforeHitCalculated(buff, event);
      if (!event.isPrevented()) {
         if (this.runLifeLineLogic(buff, event.getExpectedHealth())) {
            event.prevent();
         }
      }
   }

   protected boolean runLifeLineLogic(ActiveBuff buff, int expectedHealth) {
      Level level = buff.owner.getLevel();
      if (level.isServer() && !this.isOnCooldown(buff) && expectedHealth <= 0) {
         buff.owner.setHealth(Math.max(10, buff.owner.getMaxHealth() / 4));
         buff.owner.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.PHOENIX_COOLDOWN, buff.owner, EGG_COOLDOWN_SECONDS, null), true);
         buff.owner.buffManager.addBuff(new ActiveBuff(BuffRegistry.REBORN, buff.owner, EGG_DURATION_SECONDS, null), true);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "phoenixsettip"), 400);
      return tooltips;
   }

   @Override
   public void addStatTooltips(LinkedList<ItemStatTip> list, ActiveBuff currentValues, ActiveBuff lastValues) {
      super.addStatTooltips(list, currentValues, lastValues);
      currentValues.getModifierTooltipsBuilder(true, true).addLastValues(lastValues).excludeLimits(BuffModifiers.SLOW).buildToStatList(list);
   }

   @Override
   public void addHumanDraw(ActiveBuff buff, HumanDrawOptions drawOptions) {
      if (this.shouldShowWings(buff)) {
         int anim = GameUtils.getAnim(buff.owner.getLevel().getWorldEntity().getTime(), 4, 400);
         if (drawOptions.getDir() == 0) {
            drawOptions.addTopDraw(
               (level, player, dir, spriteX, spriteY, spriteRes, drawX, drawY, width, height, mirrorX, mirrorY, light, hasGlowEffect, glowHash, alpha, mask) -> this.wingsTexture
                  .initDraw()
                  .sprite(anim, drawOptions.getDir(), 64)
                  .size(width, height)
                  .startGlowOptions(level, glowHash)
                  .light(light.minLevelCopy(150.0F))
                  .apply(hasGlowEffect)
                  .addMaskShader(mask)
                  .alpha(0.75F)
                  .pos(drawX, drawY)
            );
         } else {
            drawOptions.addBehindDraw(
               (level, player, dir, spriteX, spriteY, spriteRes, drawX, drawY, width, height, mirrorX, mirrorY, light, hasGlowEffect, glowHash, alpha, mask) -> this.wingsTexture
                  .initDraw()
                  .sprite(anim, drawOptions.getDir(), 64)
                  .size(width, height)
                  .startGlowOptions(level, glowHash)
                  .light(light.minLevelCopy(150.0F))
                  .apply(hasGlowEffect)
                  .addMaskShader(mask)
                  .alpha(0.75F)
                  .pos(drawX, drawY)
            );
         }
      }
   }

   private boolean shouldShowWings(ActiveBuff buff) {
      if (buff.owner.isVisible() && !this.isOnCooldown(buff)) {
         if (buff.owner instanceof PlayerMob) {
            PlayerMob player = (PlayerMob)buff.owner;

            for (int i = 0; i < 3; i++) {
               InventoryItem invItem = player.getInv().equipment.getSelectedCosmeticSlot(i).getItem();
               if (invItem != null && invItem.item.isArmorItem()) {
                  return false;
               }
            }
         }

         if (buff.owner instanceof HumanMob) {
            HumanMob human = (HumanMob)buff.owner;

            for (int i = 0; i < 3; i++) {
               InventoryItem invItem = human.getCosmeticArmorItem(i);
               if (invItem != null && invItem.item.isArmorItem()) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isOnCooldown(ActiveBuff buff) {
      return buff.owner.buffManager.hasBuff(BuffRegistry.Debuffs.PHOENIX_COOLDOWN);
   }
}
