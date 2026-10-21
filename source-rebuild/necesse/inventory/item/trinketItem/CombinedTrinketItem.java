package necesse.inventory.item.trinketItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs.TrinketBuff;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.TrinketsLootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class CombinedTrinketItem extends TrinketItem {
   public ArrayList<String> trinketStringIDs;

   public CombinedTrinketItem(Item.Rarity rarity, int enchantCost, String... trinketStringIDs) {
      super(rarity, enchantCost, TrinketsLootTable.trinkets);
      this.trinketStringIDs = new ArrayList<>(Arrays.asList(trinketStringIDs));
   }

   @Override
   public void onItemRegistryClosed() {
      super.onItemRegistryClosed();
      if (this.getWorldDrawLight() == null) {
         GameLight out = null;
         ArrayList<TrinketItem> array = this.streamCombinedTrinkets().collect(Collectors.toCollection(ArrayList::new));

         for (int i = 0; i < array.size(); i++) {
            if (out == null && array.get(i).getWorldDrawLight() != null) {
               out = array.get(i).getWorldDrawLight();
            } else if (array.get(i).getWorldDrawLight() != null) {
               out.combine(array.get(i).getWorldDrawLight());
            }
         }

         if (out != null) {
            this.worldDrawLight = out;
         }
      }
   }

   @Override
   public TrinketBuff[] getBuffs(InventoryItem item) {
      TrinketBuff[] out = new TrinketBuff[0];

      for (TrinketItem trinketItem : this.streamCombinedTrinkets().toArray(TrinketItem[]::new)) {
         out = GameUtils.concat(out, trinketItem.getBuffs(item));
      }

      return out;
   }

   @Override
   public void refreshLight(Level level, float x, float y, InventoryItem item, boolean isHolding) {
      super.refreshLight(level, x, y, item, isHolding);
      this.streamCombinedTrinkets().forEach(tI -> tI.refreshLight(level, x, y, item, isHolding));
   }

   @Override
   public GameLight getWorldDrawLight() {
      return this.worldDrawLight;
   }

   @Override
   public boolean disabledBy(InventoryItem item) {
      return super.disabledBy(item) ? true : this.streamCombinedTrinkets().anyMatch(i -> i.disabledBy(item));
   }

   @Override
   public boolean disables(InventoryItem item) {
      if (super.disables(item)) {
         return true;
      } else {
         return this.trinketStringIDs.stream().anyMatch(s -> s.equals(item.item.getStringID()))
            ? true
            : this.streamCombinedTrinkets().anyMatch(i -> i.disables(item));
      }
   }

   public Stream<TrinketItem> streamCombinedTrinkets() {
      return this.trinketStringIDs.stream().map(s -> (TrinketItem)ItemRegistry.getItem(s));
   }
}
