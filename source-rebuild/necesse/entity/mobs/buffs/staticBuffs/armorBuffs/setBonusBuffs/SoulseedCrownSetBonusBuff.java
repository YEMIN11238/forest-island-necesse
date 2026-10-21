package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.util.LinkedList;
import necesse.engine.localization.Localization;
import necesse.engine.network.Packet;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.entity.levelEvent.explosionEvent.SoulseedExplosionEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffAbility;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.itemAttacker.MobFollower;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.item.ItemStatTip;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;

public class SoulseedCrownSetBonusBuff extends SetBonusBuff implements BuffAbility {
   protected int timeBetweenExplosions = 150;
   protected int abilityCooldown = 60000;
   public FloatUpgradeValue explosionDamage = new FloatUpgradeValue(0.0F, 0.2F).setBaseValue(65.0F).setUpgradedValue(1.0F, 85.0F);
   public IntUpgradeValue maxSummons = new IntUpgradeValue().setBaseValue(1).setUpgradedValue(1.0F, 2);

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.MAX_SUMMONS, this.maxSummons.getValue(buff.getUpgradeTier()));
   }

   @Override
   public void serverTick(ActiveBuff buff) {
      long nextExplosionTime = buff.getGndData().getLong("nextExplosionTime");
      if (nextExplosionTime != 0L) {
         long currentTime = buff.owner.getTime();
         if (currentTime >= nextExplosionTime) {
            if (this.explodeNextSummon(buff)) {
               buff.getGndData().setLong("nextExplosionTime", currentTime + this.timeBetweenExplosions);
            } else {
               buff.getGndData().clearItem("nextExplosionTime");
            }
         }
      }
   }

   @Override
   public boolean canRunAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      return player.buffManager.getStacks(BuffRegistry.SUMMONED_MOB) > 0
         && !buff.owner.buffManager.hasBuff(BuffRegistry.Debuffs.SOULSEED_COOLDOWN)
         && !buff.owner.buffManager.hasBuff(BuffRegistry.SOULSEED_CONSECUTIVE);
   }

   @Override
   public void runAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      player.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.SOULSEED_COOLDOWN, player, this.abilityCooldown, null), false);
      buff.getGndData().setLong("nextExplosionTime", buff.owner.getTime());
      this.timeBetweenExplosions = 150;
   }

   protected boolean explodeNextSummon(ActiveBuff ab) {
      if (!(ab.owner instanceof ItemAttackerMob)) {
         return false;
      }

      MobFollower firstMob = ((ItemAttackerMob)ab.owner)
         .serverFollowersManager
         .streamFollowers()
         .filter(e -> e.buffType != null && e.buffType.equals("summonedmob"))
         .findAny()
         .orElse(null);
      if (firstMob == null) {
         return false;
      }

      Mob chosenMob = firstMob.mob;
      this.timeBetweenExplosions = (int)(this.timeBetweenExplosions - GameMath.max(this.timeBetweenExplosions * 0.02, 8.0));
      int explosionRange = 80;
      ActiveBuff consecutiveBuff = ab.owner.buffManager.getBuff(BuffRegistry.SOULSEED_CONSECUTIVE);
      if (consecutiveBuff != null) {
         explosionRange += GameMath.min(60, consecutiveBuff.getStacks() * 4);
      }

      GameDamage damage = new GameDamage(DamageTypeRegistry.SUMMON, this.explosionDamage.getValue(ab.getUpgradeTier()));
      SoulseedExplosionEvent event = new SoulseedExplosionEvent(chosenMob.x, chosenMob.y, explosionRange, damage, false, 0.0F, ab.owner);
      ab.owner.getLevel().entityManager.addLevelEvent(event);
      chosenMob.remove(0.0F, 0.0F, null, true);
      ActiveBuff stackBuff = new ActiveBuff(BuffRegistry.SOULSEED_CONSECUTIVE, ab.owner, 10.0F, ab.owner);
      ab.owner.addBuff(stackBuff, true);
      ActiveBuff cooldownBuff = ab.owner.buffManager.getBuff(BuffRegistry.Debuffs.SOULSEED_COOLDOWN.getID());
      if (cooldownBuff != null && cooldownBuff.getDurationLeft() < stackBuff.getDuration()) {
         ab.owner.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.SOULSEED_COOLDOWN, ab.owner, stackBuff.getDuration(), null), true);
      }

      return true;
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltip = super.getTooltip(ab, blackboard);
      tooltip.add(Localization.translate("itemtooltip", "soulseedset"), 400);
      return tooltip;
   }

   @Override
   public void addStatTooltips(LinkedList<ItemStatTip> list, ActiveBuff currentValues, ActiveBuff lastValues) {
      super.addStatTooltips(list, currentValues, lastValues);
      currentValues.getModifierTooltipsBuilder(true, true).addLastValues(lastValues).excludeLimits(BuffModifiers.SLOW).buildToStatList(list);
   }
}
