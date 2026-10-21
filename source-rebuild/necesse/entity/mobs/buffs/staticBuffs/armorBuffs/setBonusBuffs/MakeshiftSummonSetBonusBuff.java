package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.awt.geom.Point2D.Float;
import java.util.Optional;
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
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.itemAttacker.MobFollower;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.MakeshiftRobotSettlerFollowingMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.item.toolItem.summonToolItem.SummonToolItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;

public class MakeshiftSummonSetBonusBuff extends MakeshiftSetBonusBuff {
   public final int summonDuration = 15000;
   public FloatUpgradeValue robotDamage = new FloatUpgradeValue(0.0F, 0.2F).setBaseValue(50.0F).setUpgradedValue(1.0F, 50.0F);
   public IntUpgradeValue maxSummons = new IntUpgradeValue().setBaseValue(2).setUpgradedValue(1.0F, 2);

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.MAX_SUMMONS, this.maxSummons.getValue(buff.getUpgradeTier()));
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "makeshiftsummonset1"), 400);
      tooltips.add(Localization.translate("itemtooltip", "makeshiftsummonset2"), 400);
      return tooltips;
   }

   @Override
   public void runAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      Mob owner = buff.owner;
      if (owner.isServer()) {
         int stacksAvailable = player.buffManager.getStacks(BuffRegistry.QUICK_TINKER);
         this.updateSummon(stacksAvailable, player, buff);
         owner.buffManager.removeBuff(BuffRegistry.QUICK_TINKER, true);
      }
   }

   public void updateSummon(int stacksAvailable, PlayerMob player, ActiveBuff buff) {
      if (this.getSummon(player) != null && stacksAvailable > 0) {
         Mob summon = this.getSummon(player);
         this.addOverclockToSummon(summon, stacksAvailable, buff);
      } else {
         Mob owner = buff.owner;
         MakeshiftRobotSettlerFollowingMob summonedMob = (MakeshiftRobotSettlerFollowingMob)MobRegistry.getMob("makeshiftrobotsettler", owner.getLevel());
         ((ItemAttackerMob)owner)
            .serverFollowersManager
            .addFollower("summonedmakeshiftrobot", summonedMob, FollowPosition.WALK_CLOSE, "summonedmob", 1.0F, 1, null, false);
         Float spawnPoint = SummonToolItem.findSpawnLocation(summonedMob, owner.getLevel(), owner.x, owner.y);
         summonedMob.updateDamage(new GameDamage(DamageTypeRegistry.SUMMON, this.robotDamage.getValue(buff.getUpgradeTier())));
         if (stacksAvailable > 1) {
            this.addOverclockToSummon(summonedMob, stacksAvailable - 1, buff);
         }

         owner.getLevel().entityManager.addMob(summonedMob, spawnPoint.x, spawnPoint.y);
      }
   }

   public void addOverclockToSummon(Mob summon, int stacksAdded, ActiveBuff buff) {
      int currentStacks = 0;
      if (summon.buffManager.hasBuff(BuffRegistry.MAKESHIFT_ROBOT_OVERCLOCK)) {
         currentStacks = summon.buffManager.getStacks(BuffRegistry.MAKESHIFT_ROBOT_OVERCLOCK);
      }

      ActiveBuff ab = new ActiveBuff(BuffRegistry.MAKESHIFT_ROBOT_OVERCLOCK, summon, 15000, null);
      ab.setStacks(currentStacks + stacksAdded, 15000, null);
      summon.buffManager.addBuff(ab, true);
      if (summon instanceof MakeshiftRobotSettlerFollowingMob) {
         ((MakeshiftRobotSettlerFollowingMob)summon).lifeTime = 15000;
      }
   }

   @Override
   protected void addQuickTinkerStack(ActiveBuff buff) {
      if (buff.owner.isServer()) {
         if (buff.owner instanceof PlayerMob) {
            PlayerMob player = (PlayerMob)buff.owner;
            if (this.getSummon(player) != null) {
               this.updateSummon(1, player, buff);
               return;
            }
         }

         super.addQuickTinkerStack(buff);
      }
   }

   public Mob getSummon(PlayerMob player) {
      Optional<MobFollower> robotSummons = player.serverFollowersManager.streamFollowers().filter(f -> f.summonType.equals("summonedmakeshiftrobot")).findAny();
      return robotSummons.<Mob>map(mobFollower -> mobFollower.mob).orElse(null);
   }
}
