package necesse.engine.incursionPerkTree;

import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.IncursionPerksRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.inventory.lootTable.presets.ArmorSetsLootTable;
import necesse.level.maps.Level;
import necesse.level.maps.incursion.IncursionData;

public class RescueSettlerRewardPerk extends IncursionPerk {
   public static HashMap<String, RescueSettlerRewardPerk.IncursionMobReward> settlers = new HashMap<>();

   public RescueSettlerRewardPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, false, prerequisitePerkRequired);
   }

   @Override
   public TicketSystemList<LootItemInterface> onGenerateTabletRewards(
      TicketSystemList<LootItemInterface> ticketedRewards, GameRandom seededRandom, int tier, IncursionData incursionData
   ) {
      int tabletTierWeight = tier * 10;
      OneOfLootItems lootItemList = new OneOfLootItems();

      for (Entry<String, RescueSettlerRewardPerk.IncursionMobReward> entry : settlers.entrySet()) {
         GNDItemMap gndData = new GNDItemMap();
         gndData.setString("settlerIncursionReward", entry.getKey());
         LootItem lootItem = new LootItem(entry.getValue().mobStringID + "spawnitem", gndData);
         lootItemList.add(lootItem);
      }

      ticketedRewards.addObject(40 + tabletTierWeight, lootItemList);
      return ticketedRewards;
   }

   static {
      settlers.put("settler", new RescueSettlerRewardPerk.SettlerIncursionReward("human"));
      settlers.put("farmersettler", new RescueSettlerRewardPerk.SettlerIncursionReward("farmerhuman"));
      settlers.put("blacksmithsettler", new RescueSettlerRewardPerk.SettlerIncursionReward("blacksmithhuman"));
      settlers.put("guardsettler", new RescueSettlerRewardPerk.SettlerIncursionReward("guardhuman"));
      settlers.put("magesettler", new RescueSettlerRewardPerk.SettlerIncursionReward("magehuman"));
      settlers.put("gunsmithsettler", new RescueSettlerRewardPerk.SettlerIncursionReward("gunsmithhuman"));
      settlers.put("alchemistsettler", new RescueSettlerRewardPerk.SettlerIncursionReward("alchemisthuman"));
      settlers.put("huntersettler", new RescueSettlerRewardPerk.SettlerIncursionReward("hunterhuman"));
      settlers.put("anglersettler", new RescueSettlerRewardPerk.SettlerIncursionReward("anglerhuman"));
      settlers.put("animalkeepersettler", new RescueSettlerRewardPerk.SettlerIncursionReward("animalkeeperhuman"));
      settlers.put("stylistsettler", new RescueSettlerRewardPerk.SettlerIncursionReward("stylisthuman"));
      settlers.put("piratesettler", new RescueSettlerRewardPerk.SettlerIncursionReward("piratehuman"));
      settlers.put("explorersettler", new RescueSettlerRewardPerk.SettlerIncursionReward("explorerhuman"));
      settlers.put("minersettler", new RescueSettlerRewardPerk.SettlerIncursionReward("minerhuman"));
      settlers.put("tradersettler", new RescueSettlerRewardPerk.SettlerIncursionReward("traderhuman"));
   }

   public abstract static class IncursionMobReward {
      public final String mobStringID;

      public IncursionMobReward(String mobStringID) {
         this.mobStringID = mobStringID;
      }

      public abstract void spawnMob(Level var1, int var2, int var3, IncursionData var4);
   }

   public static class SettlerIncursionReward extends RescueSettlerRewardPerk.IncursionMobReward {
      public SettlerIncursionReward(String mobStringID) {
         super(mobStringID);
      }

      @Override
      public void spawnMob(Level level, int levelX, int levelY, IncursionData incursionData) {
         Mob mob = MobRegistry.getMob(this.mobStringID, level);
         mob.onSpawned(levelX, levelY);
         if (mob instanceof HumanMob) {
            HumanMob humanMob = (HumanMob)mob;
            humanMob.setTrapped();
            if (incursionData.currentIncursionPerkIDs.contains(IncursionPerksRegistry.RESCUED_SETTLER_MORE_PERSONALITIES.getID())) {
               humanMob.markMorePersonalitiesFromIncursionPerk();
            }

            ArrayList<InventoryItem> items = new ArrayList<>();
            ArmorSetsLootTable.armorSets.addItems(items, GameRandom.globalRandom, 1.0F);

            for (InventoryItem invItem : items) {
               if (invItem.item.isArmorItem()) {
                  invItem.item.setUpgradeTier(invItem, 1.0F);
                  ArmorItem armorItem = (ArmorItem)invItem.item;
                  switch (armorItem.armorType) {
                     case HEAD:
                        humanMob.equipmentInventory.setItem(0, invItem);
                        break;
                     case CHEST:
                        humanMob.equipmentInventory.setItem(1, invItem);
                        break;
                     case FEET:
                        humanMob.equipmentInventory.setItem(2, invItem);
                  }
               }
            }
         }

         Float dir = GameMath.getAngleDir(GameRandom.globalRandom.nextInt(360));
         float speed = GameRandom.globalRandom.getFloatBetween(100.0F, 150.0F);
         mob.dx = dir.x * speed;
         mob.dy = dir.y * speed;
         mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.SPAWN_INVINCIBILITY, mob, 4.0F, null), false);
         level.entityManager.mobs.add(mob);
      }
   }
}
