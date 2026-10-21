package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs;

import necesse.engine.localization.Localization;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.trinketItem.TrinketItem;

public class MagicFociBuff extends TrinketBuff {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.MAGIC_DAMAGE, 0.2F);
      buff.setModifier(BuffModifiers.MELEE_DAMAGE, -0.2F);
      buff.setModifier(BuffModifiers.RANGED_DAMAGE, -0.2F);
      buff.setModifier(BuffModifiers.SUMMON_DAMAGE, -0.2F);
   }

   @Override
   public ListGameTooltips getTrinketTooltip(TrinketItem trinketItem, InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getTrinketTooltip(trinketItem, item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "magicfoci1"));
      tooltips.add(Localization.translate("itemtooltip", "magicfoci2"));
      return tooltips;
   }
}
