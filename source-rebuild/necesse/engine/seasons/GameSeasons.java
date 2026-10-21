package necesse.engine.seasons;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import necesse.engine.GameLaunch;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.ConditionLootItemList;

public class GameSeasons {
   private static int month;
   private static int day;
   private static boolean aprilFools;
   private static boolean halloween;
   private static boolean christmas;
   private static boolean newYear;
   private static boolean valentines;
   private static final ArrayList<SeasonalHat> hats = new ArrayList<>();
   private static final ArrayList<SeasonCrate> crates = new ArrayList<>();
   private static final ArrayList<SeasonalMobLootTable> mobLoot = new ArrayList<>();
   public static ArrayList<SeasonCrate> activeCrates = new ArrayList<>();

   public static void loadSeasons() {
      boolean ignore = GameLaunch.launchOptions != null && GameLaunch.launchOptions.containsKey("ignoreseasons");
      if (ignore) {
         System.out.println("Ignoring seasonal content with -ignoreseasons launch option");
         valentines = false;
         aprilFools = false;
         halloween = false;
         christmas = false;
         newYear = false;
      } else {
         Calendar instance = Calendar.getInstance(Locale.ENGLISH);
         month = instance.get(2) + 1;
         day = instance.get(5);
         valentines = isBetween(14, 2, 15, 2);
         aprilFools = isBetween(1, 4, 2, 4);
         halloween = isBetween(18, 10, 7, 11);
         christmas = isBetween(1, 12, 28, 12);
         newYear = isBetween(28, 12, 7, 1);
      }

      refreshActive();
   }

   public static void loadResources() {
      for (SeasonalHat hat : hats) {
         hat.loadTextures();
      }

      for (SeasonCrate crate : crates) {
         crate.loadTextures();
      }
   }

   public static void writeSeasons(PacketWriter writer) {
      writer.putNextBoolean(valentines);
      writer.putNextBoolean(aprilFools);
      writer.putNextBoolean(halloween);
      writer.putNextBoolean(christmas);
      writer.putNextBoolean(newYear);
   }

   public static void readSeasons(PacketReader reader) {
      valentines = reader.getNextBoolean();
      aprilFools = reader.getNextBoolean();
      halloween = reader.getNextBoolean();
      christmas = reader.getNextBoolean();
      newYear = reader.getNextBoolean();
      refreshActive();
   }

   private static void refreshActive() {
      activeCrates.clear();

      for (SeasonCrate crate : crates) {
         if (crate.isActive.get()) {
            activeCrates.add(crate);
         }
      }
   }

   public static boolean isBetween(int startDate, int startMonth, int endDate, int endMonth) {
      boolean reversed = startMonth == endMonth ? endDate < startDate : endMonth < startMonth;
      if (reversed) {
         if (month == startMonth && month == endMonth) {
            return day >= startDate || day <= endDate;
         } else if (month == startMonth) {
            return day >= startDate;
         } else {
            return month == endMonth ? day <= endDate : month > startMonth || month < endMonth;
         }
      } else if (month == startMonth && month == endMonth) {
         return day >= startDate && day <= endDate;
      } else if (month == startMonth) {
         return day >= startDate;
      } else {
         return month == endMonth ? day <= endDate : month > startMonth && month < endMonth;
      }
   }

   public static boolean isValentines() {
      return valentines;
   }

   public static boolean isAprilFools() {
      return aprilFools;
   }

   public static boolean isHalloween() {
      return halloween;
   }

   public static boolean isChristmas() {
      return christmas;
   }

   public static boolean isNewYear() {
      return newYear;
   }

   public static SeasonalHat getHat(GameRandom random) {
      for (SeasonalHat hat : hats) {
         if (hat.isActive.get() && random.getChance(hat.mobWearChance)) {
            return hat;
         }
      }

      return null;
   }

   public static SeasonCrate getCrate(GameRandom random) {
      if (activeCrates.isEmpty()) {
         return null;
      }

      for (SeasonCrate crate : activeCrates) {
         if (random.getChance(crate.crateChance)) {
            return crate;
         }
      }

      return null;
   }

   public static void addMobDrops(Mob mob, ArrayList<InventoryItem> drops, GameRandom random, float lootMultiplier) {
      for (SeasonalMobLootTable loot : mobLoot) {
         if (loot.isActive.get()) {
            loot.addDrops(mob, drops, random, lootMultiplier);
         }
      }
   }

   static {
      hats.add(new SeasonalHatLight(GameSeasons::isHalloween, 0.16666667F, "pumpkinmask", 0.1F, "pumpkinmask"));
      hats.add(new SeasonalHat(GameSeasons::isChristmas, 0.16666667F, "christmashat", 0.1F, "christmashat"));
      hats.add(new SeasonalHat(GameSeasons::isNewYear, 0.16666667F, "partyhat", 0.1F, "partyhat"));
      crates.add(new SeasonCrate(GameSeasons::isChristmas, 0.2F, "christmascrates"));
      mobLoot.add(new SeasonalMobLootTable(GameSeasons::isChristmas, new LootTable(new ConditionLootItemList((gameRandom, objects) -> {
         Mob mob = LootTable.expectExtra(Mob.class, objects, 0);
         return mob != null && mob.isHostile && !mob.isSummoned;
      }, new ChanceLootItem(0.04F, "christmaspresent")))));
   }
}
