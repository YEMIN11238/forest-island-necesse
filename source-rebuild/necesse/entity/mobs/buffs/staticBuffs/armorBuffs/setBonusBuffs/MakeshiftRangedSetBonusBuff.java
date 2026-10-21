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
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.MakeshiftTurretMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.item.toolItem.summonToolItem.SummonToolItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;

public class MakeshiftRangedSetBonusBuff extends MakeshiftSetBonusBuff {
   public FloatUpgradeValue turretDamage = new FloatUpgradeValue(0.0F, 0.2F).setBaseValue(50.0F).setUpgradedValue(1.0F, 50.0F);

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "makeshiftrangedset"), 400);
      return tooltips;
   }

   @Override
   public void runAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      super.runAbility(player, buff, content);
      Mob owner = buff.owner;
      if (owner.isServer() && owner.isItemAttacker) {
         GameDamage damage = new GameDamage(DamageTypeRegistry.RANGED, this.turretDamage.getValue(buff.getUpgradeTier()));
         int maxSummons = 3;
         MakeshiftTurretMob summonedMob = (MakeshiftTurretMob)MobRegistry.getMob("makeshiftturret", owner.getLevel());
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
