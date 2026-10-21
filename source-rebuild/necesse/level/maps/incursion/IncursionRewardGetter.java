package necesse.level.maps.incursion;

import java.util.ArrayList;
import necesse.inventory.InventoryItem;

@FunctionalInterface
public interface IncursionRewardGetter {
   ArrayList<InventoryItem> getRewards(boolean var1);
}
