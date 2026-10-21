package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.util.LinkedList;
import necesse.engine.localization.Localization;
import necesse.engine.network.Packet;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffAbility;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.item.ItemStatTip;

public class MakeshiftSetBonusBuff extends SetBonusBuff implements BuffAbility {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public void onWasHit(ActiveBuff buff, MobWasHitEvent event) {
      super.onWasHit(buff, event);
      if (buff.owner.isServer() && GameRandom.globalRandom.getChance(0.25F)) {
         this.addQuickTinkerStack(buff);
      }
   }

   @Override
   public void onHasAttacked(ActiveBuff buff, MobWasHitEvent event) {
      if (!event.wasPrevented) {
         float quickTinkerChance = (float)event.damage / event.target.getMaxHealth() / 4.0F;
         if (event.target.isBoss()) {
            quickTinkerChance *= 10.0F;
         }

         if (GameRandom.globalRandom.getChance(quickTinkerChance)) {
            this.addQuickTinkerStack(buff);
         }
      }
   }

   protected void addQuickTinkerStack(ActiveBuff buff) {
      if (buff.owner.isServer()) {
         Mob owner = buff.owner;
         ActiveBuff ab = new ActiveBuff(BuffRegistry.QUICK_TINKER, owner, 60000, null);
         owner.buffManager.addBuff(ab, true);
      }
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "makeshiftset"), 400);
      return tooltips;
   }

   @Override
   public void addStatTooltips(LinkedList<ItemStatTip> list, ActiveBuff currentValues, ActiveBuff lastValues) {
      super.addStatTooltips(list, currentValues, lastValues);
      currentValues.getModifierTooltipsBuilder(true, true).addLastValues(lastValues).buildToStatList(list);
   }

   @Override
   public void runAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      player.buffManager.removeStack(BuffRegistry.QUICK_TINKER, true, false);
   }

   @Override
   public boolean canRunAbility(PlayerMob player, ActiveBuff buff, Packet content) {
      return player.buffManager.hasBuff(BuffRegistry.QUICK_TINKER);
   }
}
