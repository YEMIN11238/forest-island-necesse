package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.awt.geom.Point2D.Float;
import java.util.LinkedList;
import necesse.engine.localization.Localization;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.hostile.EnchantedCrawlingZombieMob;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BabyCrawlingZombieFollowingMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.item.ItemStatTip;
import necesse.inventory.item.toolItem.summonToolItem.SummonToolItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;

public class WitchRobesSetBonusBuff extends SetBonusBuff {
   public FloatUpgradeValue zombieDamage = new FloatUpgradeValue(0.0F, 0.2F).setBaseValue(20.0F).setUpgradedValue(1.0F, 40.0F);
   public IntUpgradeValue maxMana = new IntUpgradeValue(0, 0.1F).setBaseValue(150).setUpgradedValue(1.0F, 250);

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.MAX_MANA_FLAT, this.maxMana.getValue(buff.getUpgradeTier()));
   }

   @Override
   public void onWasHit(ActiveBuff buff, MobWasHitEvent event) {
      super.onWasHit(buff, event);
      if (!event.wasPrevented && buff.owner.isServer()) {
         boolean isFromSelf = event.attacker != null && event.attacker.isInAttackOwnerChain(buff.owner);
         if (!isFromSelf) {
            float damagePercent = (float)event.damage / buff.owner.getMaxHealth();
            float zombieBuffer = buff.getGndData().getFloat("zombieBuffer");
            float bufferIncrease = Math.min(damagePercent * 10.0F + GameRandom.globalRandom.getFloatBetween(0.2F, 0.5F), 1.0F);
            zombieBuffer += bufferIncrease;
            if (zombieBuffer >= 1.0F) {
               zombieBuffer--;
               Mob attacker = event.attacker == null ? null : event.attacker.getFirstAttackOwner();
               if (attacker != null) {
                  float damage = this.zombieDamage.getValue(buff.getUpgradeTier());
                  spawnCrawlingZombie(buff.owner, attacker, new GameDamage(DamageTypeRegistry.SUMMON, damage));
               }
            }

            buff.getGndData().setFloat("zombieBuffer", zombieBuffer);
         }
      }
   }

   public static void spawnCrawlingZombie(Mob owner, Mob target, GameDamage zombieDamage) {
      if (target != null) {
         target.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.NECROTIC_SLOW, target, 4.0F, owner), true);
      }

      if (owner != null && owner.isServer()) {
         if (owner.isItemAttacker) {
            int maxSummons = 20;
            BabyCrawlingZombieFollowingMob summonedMob = (BabyCrawlingZombieFollowingMob)MobRegistry.getMob("babycrawlingzombie", owner.getLevel());
            ((ItemAttackerMob)owner)
               .serverFollowersManager
               .addFollower("summonedmobtemp", summonedMob, FollowPosition.WALK_CLOSE, "summonedmob", 1.0F, maxSummons, null, false);
            Float spawnPoint = SummonToolItem.findSpawnLocation(summonedMob, owner.getLevel(), owner.x, owner.y);
            summonedMob.updateDamage(zombieDamage);
            summonedMob.customFocus = target;
            owner.getLevel().entityManager.addMob(summonedMob, spawnPoint.x, spawnPoint.y);
         } else if (owner.isHostile) {
            EnchantedCrawlingZombieMob crawlingZombie = (EnchantedCrawlingZombieMob)MobRegistry.getMob("enchantedcrawlingzombie", owner.getLevel());
            Float spawnPoint = SummonToolItem.findSpawnLocation(crawlingZombie, owner.getLevel(), owner.x, owner.y);
            owner.getLevel().entityManager.addMob(crawlingZombie, spawnPoint.x, spawnPoint.y);
         }
      }
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "witchrobesset"), 400);
      return tooltips;
   }

   @Override
   public void addStatTooltips(LinkedList<ItemStatTip> list, ActiveBuff currentValues, ActiveBuff lastValues) {
      super.addStatTooltips(list, currentValues, lastValues);
      currentValues.getModifierTooltipsBuilder(true, true).addLastValues(lastValues).buildToStatList(list);
   }
}
