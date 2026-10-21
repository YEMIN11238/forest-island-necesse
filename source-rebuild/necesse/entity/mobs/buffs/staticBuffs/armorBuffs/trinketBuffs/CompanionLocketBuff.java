package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs;

import necesse.engine.localization.Localization;
import necesse.engine.registries.BuffRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.trinketItem.TrinketItem;

public class CompanionLocketBuff extends TrinketBuff {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      this.updateModifiers(buff);
   }

   @Override
   public void serverTick(ActiveBuff buff) {
      this.updateModifiers(buff);
   }

   public void updateModifiers(ActiveBuff buff) {
      int maxStacks = buff.owner.buffManager.getModifier(BuffModifiers.MAX_SUMMONS);
      int usedStacks = buff.owner.buffManager.getStacks(BuffRegistry.SUMMONED_MOB);
      if (maxStacks != buff.getGndData().getInt("lastMaxStacks") || usedStacks != buff.getGndData().getInt("lastUsedStacks")) {
         float damageModifier = Math.max((float)maxStacks / Math.max(usedStacks, 1) - 1.0F, 0.0F);
         float speedModifier = damageModifier / 4.0F;
         buff.setModifier(BuffModifiers.SUMMON_DAMAGE, damageModifier);
         buff.setModifier(BuffModifiers.SUMMONS_SPEED, speedModifier);
         buff.getGndData().setInt("lastMaxStacks", maxStacks);
         buff.getGndData().setInt("lastUsedStacks", usedStacks);
      }
   }

   @Override
   public ListGameTooltips getTrinketTooltip(TrinketItem trinketItem, InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getTrinketTooltip(trinketItem, item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "companionlockettipnew"), 400);
      return tooltips;
   }
}
