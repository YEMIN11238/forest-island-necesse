package necesse.entity.levelEvent.settlementRaidEvent;

import java.util.ArrayList;
import necesse.level.maps.incursion.IncursionData;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadout;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadoutGenerator;

public class RogueHuntersSettlementRaidLevelEvent extends BasicSettlementRaidLevelEvent {
   public RogueHuntersSettlementRaidLevelEvent() {
      super("roguehunterraider", "roguehuntersraidertype");
   }

   @Override
   public void modifyLoadouts(SettlementRaidLoadoutGenerator generator) {
      ArrayList<SettlementRaidLoadout.Weapon> weapons = new ArrayList<>();
      this.addWeapon(weapons, "woodbow");
      this.addWeapon(weapons, "copperbow");
      this.addWeapon(weapons, "ironbow");
      this.addWeapon(weapons, "goldbow");
      this.addWeapon(weapons, "frostbow");
      this.addWeapon(weapons, "demonicbow");
      this.addWeapon(weapons, "ivybow");
      this.addWeapon(weapons, "vulturesburst");
      this.addWeapon(weapons, "tungstenbow");
      this.addWeapon(weapons, "glacialbow");
      this.addWeapon(weapons, "bowofdualism");
      this.addWeapon(weapons, "antiquebow");
      this.addWeapon(weapons, "thecrimsonsky");
      this.addWeapon(weapons, "arachnidwebbow");
      this.addWeapon(weapons, "goldgreatbow");
      this.addWeapon(weapons, "voidgreatbow");
      this.addWeapon(weapons, "ivygreatbow");
      this.addWeapon(weapons, "tungstengreatbow");
      this.addWeapon(weapons, "myceliumgreatbow");
      this.addWeapon(weapons, "druidsgreatbow");
      this.addWeapon(weapons, "slimegreatbow");
      this.addWeapon(weapons, "nightpiercer");
      this.addWeapon(weapons, "theravensnest");
      generator.weapons(weapons);
      generator.maxWeaponsInPool(5);
   }

   public void addWeapon(ArrayList<SettlementRaidLoadout.Weapon> weapons, String weaponStringID) {
      for (int tier = 0; tier <= IncursionData.ITEM_TIER_UPGRADE_CAP; tier++) {
         SettlementRaidLoadout.Weapon weapon = new SettlementRaidLoadout.Weapon(weaponStringID);
         if (tier > 0) {
            weapon.setTier(tier);
         }

         weapons.add(weapon);
      }
   }
}
