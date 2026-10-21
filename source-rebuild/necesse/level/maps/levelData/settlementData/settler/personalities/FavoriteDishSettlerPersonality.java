package necesse.level.maps.levelData.settlementData.settler.personalities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.GameColor;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.FoodQuality;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleScalingHappinessSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class FavoriteDishSettlerPersonality extends SettlerPersonality {
   public int favoriteFoodItemID = -1;

   public FavoriteDishSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_yes", new SimpleScalingHappinessSettlerThought("personalities", "eatenfavoritedish", 10, -2, 0).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_no", new SimpleSettlerThought("personalities", "nothadfavoritedish", 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (this.favoriteFoodItemID != -1) {
         save.addSafeString("favoriteFoodItem", ItemRegistry.getItemStringID(this.favoriteFoodItemID));
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      String favoriteFoodItemStringID = save.getSafeString("favoriteFoodItem", null, false);
      if (favoriteFoodItemStringID != null) {
         this.favoriteFoodItemID = ItemRegistry.getItemID(favoriteFoodItemStringID);
         if (this.favoriteFoodItemID == -1) {
            if (this.mob.getSettlerSeed() == 0) {
               throw new LoadDataException("Could not find favorite food item with string ID " + favoriteFoodItemStringID + " for " + this.mob);
            }

            this.favoriteFoodItemID = this.refreshFavoriteFoodID();
         }
      } else {
         if (this.mob.getSettlerSeed() == 0) {
            throw new LoadDataException("Could not find favorite food item for " + this.mob);
         }

         this.favoriteFoodItemID = this.refreshFavoriteFoodID();
      }
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.favoriteFoodItemID);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.favoriteFoodItemID = reader.getNextInt();
   }

   @Override
   public void init() {
      super.init();
      if (this.favoriteFoodItemID == -1) {
         this.favoriteFoodItemID = this.refreshFavoriteFoodID();
      }
   }

   @Override
   public GameMessage getDescription() {
      Item item = this.favoriteFoodItemID == -1 ? null : ItemRegistry.getItem(this.favoriteFoodItemID);
      return new LocalMessage("personalities", this.getStringID() + "_desc")
         .addReplacement("name", GameColor.ITEM_QUEST.getColorCode() + this.mob.getSettlerName() + GameColor.NO_COLOR.getColorCode())
         .addReplacement(
            "dish",
            GameColor.ITEM_QUEST.getColorCode() + (item == null ? "N/A" : ItemRegistry.getDisplayName(item.getID())) + GameColor.NO_COLOR.getColorCode()
         );
   }

   protected int refreshFavoriteFoodID() {
      ArrayList<FoodQuality> validFoodQualities = new ArrayList<>(Settler.foodQualities.size());

      for (FoodQuality foodQuality : Settler.foodQualities) {
         if (foodQuality.happinessIncrease > 10) {
            validFoodQualities.add(foodQuality);
         }
      }

      if (validFoodQualities.isEmpty()) {
         validFoodQualities.addAll(Settler.foodQualities);
      }

      HashSet<Integer> validFoodItemIDs = new HashSet<>();

      for (FoodQuality validFoodQuality : validFoodQualities) {
         validFoodItemIDs.addAll(getAllItemIDs(validFoodQuality.foodCategory));
      }

      if (validFoodItemIDs.isEmpty()) {
         return -1;
      }

      GameRandom random = new GameRandom((long)this.mob.getSettlerSeed() * GameRandom.prime(53));
      return random.getOneOf(new ArrayList<>(validFoodItemIDs));
   }

   private static HashSet<Integer> getAllItemIDs(ItemCategory category) {
      HashSet<Integer> itemIDs = new HashSet<>();

      for (Item item : category.getItems()) {
         if (ItemRegistry.countsInStats(item.getID())) {
            itemIDs.add(item.getID());
         }
      }

      for (ItemCategory child : category.getChildren()) {
         itemIDs.addAll(getAllItemIDs(child));
      }

      return itemIDs;
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      if (this.favoriteFoodItemID == -1) {
         return null;
      }

      SettlerThoughtsList list = new SettlerThoughtsList();
      int favoriteFoodEatenCount = (int)this.mob.recentFoodItemIDsEaten.stream().filter(id -> id == this.favoriteFoodItemID).count();
      if (favoriteFoodEatenCount == 0) {
         list.addThought(this.getStringID() + "_no");
      } else {
         list.addThought(this.getStringID() + "_yes", favoriteFoodEatenCount);
      }

      int differentFoodsEaten = (int)this.mob.recentFoodItemIDsEaten.stream().filter(id -> id != this.favoriteFoodItemID).distinct().count();
      SettlerThought dietThought = SettlerThoughtRegistry.getDietVarietyThought(favoriteFoodEatenCount + differentFoodsEaten);
      if (dietThought != null) {
         list.addThought(dietThought);
      }

      return list;
   }

   @Override
   public boolean preventsDietVarietyThought() {
      return this.favoriteFoodItemID != -1;
   }

   @Override
   public HashSet<Integer> getPreferredFoodItemIDs() {
      return new HashSet<>(Collections.singletonList(this.favoriteFoodItemID));
   }
}
