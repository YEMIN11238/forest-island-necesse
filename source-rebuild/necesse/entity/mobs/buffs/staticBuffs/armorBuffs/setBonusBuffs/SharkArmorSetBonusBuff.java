package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import necesse.engine.localization.Localization;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffManager;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.item.ItemStatTip;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;

public class SharkArmorSetBonusBuff extends SetBonusBuff {
   protected final ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);
   protected final float bleedDurationSeconds = 4.0F;
   protected final float frenzyEndDuration = 5.0F;
   public FloatUpgradeValue bleedDamagePerSecond = new FloatUpgradeValue(0.0F, 0.0F)
      .setBaseValue(10.0F)
      .setUpgradedValue(1.0F, 85.0F)
      .setUpgradedValue(10.0F, 150.0F);
   protected List<Mob> bleedingMobs;

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public void serverTick(ActiveBuff buff) {
      super.serverTick(buff);
      this.globalTick(buff);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      this.globalTick(buff);
   }

   protected void globalTick(ActiveBuff buff) {
      int tick = buff.owner.getLevel().tickManager().getTick();
      if (tick % 10 == 0) {
         List<Mob> collected = buff.owner
            .getLevel()
            .entityManager
            .streamAreaMobsAndPlayersTileRange(buff.owner.getX(), buff.owner.getY(), 12)
            .filter(m -> m != buff.owner)
            .filter(m -> m.buffManager.getModifier(BuffModifiers.BLEED_DAMAGE_FLAT) > 0.0F)
            .collect(Collectors.toList());
         BuffManager buffManager = buff.owner.buffManager;
         if (buffManager.hasBuff(BuffRegistry.SHARK_FRENZY)) {
            if (collected.size() < buffManager.getStacks(BuffRegistry.SHARK_FRENZY)) {
               for (int i = 0; i < collected.size(); i++) {
                  buffManager.removeStack(BuffRegistry.SHARK_FRENZY, true, false);
               }
            } else if (!collected.isEmpty()) {
               buffManager.removeBuff(BuffRegistry.SHARK_FRENZY, false);
            }
         }

         for (int i = 0; i < collected.size(); i++) {
            buffManager.addBuff(new ActiveBuff(BuffRegistry.SHARK_FRENZY, buff.owner, 5.0F, null), false);
         }

         this.bleedingMobs = collected;
      }

      if (buff != null && this.bleedingMobs != null) {
         for (Mob bleedingMob : this.bleedingMobs) {
            if (bleedingMob != null
               && !bleedingMob.removed()
               && bleedingMob.isVisible()
               && bleedingMob.isClient()
               && (tick % 10 == 0 || GameRandom.globalRandom.getEveryXthChance(12))) {
               this.spawnBloodPathParticles(buff.owner, bleedingMob);
            }
         }
      }
   }

   protected void spawnBloodPathParticles(Mob ownerMob, Mob targetMob) {
      int particleLifetime = (int)GameMath.limit(500.0F + ownerMob.getDistance(targetMob), 500.0F, 1100.0F);
      ownerMob.getLevel()
         .entityManager
         .addParticle(targetMob.x, targetMob.y, this.particleTypeSwitcher.next())
         .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12))
         .color(new Color(168, 16, 16, 116))
         .height(0.0F)
         .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
            pos.x = GameMath.lerp(lifePercent, targetMob.x, ownerMob.x);
            pos.y = GameMath.lerp(lifePercent, targetMob.y, ownerMob.y);
         })
         .size(new ParticleOption.DrawModifier() {
            @Override
            public void modify(SharedTextureDrawOptions.Wrapper options, int lifeTime, int timeAlive, float lifePercent) {
               int size = (int)((1.0F - lifePercent) * 16.0F);
               options.size(size, size);
            }
         })
         .lifeTime(particleLifetime);
   }

   @Override
   public void onHasAttacked(ActiveBuff buff, MobWasHitEvent event) {
      super.onHasAttacked(buff, event);
      if (!event.wasPrevented) {
         ActiveBuff bleedDebuff = new ActiveBuff(BuffRegistry.Debuffs.SHARKARMOR_BLEED, event.target, 4.0F, buff.owner);
         bleedDebuff.getGndData().setFloat("damagepersec", this.bleedDamagePerSecond.getValue(buff.getUpgradeTier()));
         event.target.buffManager.addBuff(bleedDebuff, false);
      }
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      float fullDamage = this.bleedDamagePerSecond.getValue(ab.getUpgradeTier()) * 4.0F;
      tooltips.add(Localization.translate("itemtooltip", "sharkarmorsetbonustip1", "value", (int)fullDamage, "duration", 4), 320);
      tooltips.add(Localization.translate("itemtooltip", "sharkarmorsetbonustip2"), 320);
      return tooltips;
   }

   @Override
   public void addStatTooltips(LinkedList<ItemStatTip> list, ActiveBuff currentValues, ActiveBuff lastValues) {
      super.addStatTooltips(list, currentValues, lastValues);
      currentValues.getModifierTooltipsBuilder(true, true).addLastValues(lastValues).buildToStatList(list);
   }
}
