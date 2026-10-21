package necesse.level.gameObject;

import java.util.EnumMap;
import necesse.engine.localization.Localization;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.item.Item;

public interface HappinessObject {
   EnumMap<Item.Rarity, Integer> defaultHappinessBonuses = createDefaultHappinessBonuses();

   static EnumMap<Item.Rarity, Integer> createDefaultHappinessBonuses() {
      EnumMap<Item.Rarity, Integer> map = new EnumMap<>(Item.Rarity.class);

      for (Item.Rarity rarity : Item.Rarity.values()) {
         map.put(rarity, getDefaultHappinessBonus(rarity));
      }

      return map;
   }

   static int getDefaultHappinessBonus(Item.Rarity rarity) {
      switch (rarity) {
         case NORMAL:
         case COMMON:
         case UNCOMMON:
         case RARE:
            return 40;
         case EPIC:
            return 50;
         case LEGENDARY:
         case QUEST:
         case UNIQUE:
            return 60;
         default:
            return 40;
      }
   }

   default int getHappinessBonus() {
      return defaultHappinessBonuses.get(((GameObject)this).rarity);
   }

   default GameTooltips getHappinessObjectTooltip() {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(Localization.translate("itemtooltip", "happinessobjecttip1", "value", this.getHappinessBonus()), 400);
      tooltips.add(Localization.translate("itemtooltip", "happinessobjecttip2"), 400);
      return tooltips;
   }
}
