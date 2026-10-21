package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.awt.geom.Point2D.Float;
import necesse.engine.localization.Localization;
import necesse.engine.network.Packet;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.MakeshiftArcThrowerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.item.toolItem.summonToolItem.SummonToolItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;

public class MakeshiftMagicSetBonusBuff extends MakeshiftSetBonusBuff {
   public FloatUpgradeValue arcThrowerDamage = new FloatUpgradeValue(0.0F, 0.2F).setBaseValue(50.0F).setUpgradedValue(1.0F, 50.0F);
   public IntUpgradeValue maxMana = new IntUpgradeValue(0, 0.1F).setBaseValue(300).setUpgradedValue(1.0F, 300);

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.MAX_MANA_FLAT, this.maxMana.getValue(buff.getUpgradeTier()));
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "makeshiftmagicset"), 400);
      return tooltips;
   }

   @Override
   public void runAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      super.runAbility(player, buff, content);
      Mob owner = buff.owner;
      if (owner.isServer() && owner.isItemAttacker) {
         GameDamage damage = new GameDamage(DamageTypeRegistry.MAGIC, this.arcThrowerDamage.getValue(buff.getUpgradeTier()));
         int maxSummons = 3;
         MakeshiftArcThrowerMob summonedMob = (MakeshiftArcThrowerMob)MobRegistry.getMob("makeshiftarcthrower", owner.getLevel());
         ((ItemAttackerMob)owner).serverFollowersManager.addFollower("summonedmobtemp", summonedMob, null, "summonedmob", 1.0F, p -> maxSummons, null, false);
         Float spawnPoint = SummonToolItem.findSpawnLocation(summonedMob, owner.getLevel(), owner.x, owner.y);
         summonedMob.updateDamage(damage);
         owner.getLevel().entityManager.addMob(summonedMob, spawnPoint.x, spawnPoint.y);
      }
   }

   @Override
   protected void addQuickTinkerStack(ActiveBuff buff) {
      if (buff.owner.isPlayer && buff.owner.buffManager.getStacks(BuffRegistry.QUICK_TINKER) >= 3) {
         this.runAbility((PlayerMob)buff.owner, buff, buff.getContentPacket());
      }

      super.addQuickTinkerStack(buff);
   }
}
