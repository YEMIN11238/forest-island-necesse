package necesse.inventory.container.settlement.data;

import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public interface SettlerSearchData {
   boolean matchesSearch(String var1, SettlerMob var2, PlayerMob var3);
}
