package necesse.engine.registries;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.level.maps.levelData.settlementData.settler.personalities.AdventurerSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.ArtConnoisseurSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.AudiophileSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.BeachLoverSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.BiggestFanSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.BloodThirstySettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.ClumsySettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.EcologistSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.ElderSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.ExoticMerchantSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.ExpeditionerSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.FashionistaSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.FastWorkerSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.FavoriteDishSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.FlowerCollectorSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.FriendlyWitchSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.FurnitureSetPreferenceSettlerPersonalityConstructor;
import necesse.level.maps.levelData.settlementData.settler.personalities.GameDevOnNecesseSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.GardenerSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.GemCollectorSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.GlyphManiacSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.HiredHandCollectorSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.IndoorsySettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.InspiringSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.JoggerSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.LumberjackSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.MagicianSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.MakerSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.OrderlySettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.PacifistSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.PawnBrokerSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.PersistentSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.PotionSellerSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.RangerSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.RoomieSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonalityConstructor;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonalityFilter;
import necesse.level.maps.levelData.settlementData.settler.personalities.SimplePersonalityFilter;
import necesse.level.maps.levelData.settlementData.settler.personalities.StressedOutSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.SummonerSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.TouristSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.VengefulSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.VoyagerSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.WallSetPreferenceSettlerPersonalityConstructor;
import necesse.level.maps.levelData.settlementData.settler.personalities.WarriorSettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.personalities.WeatherBoundSettlerPersonality;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.WallSet;

public class SettlerPersonalityRegistry extends GameRegistry<SettlerPersonalityRegistry.SettlerPersonalityRegistryElement> {
   public static final SettlerPersonalityRegistry instance = new SettlerPersonalityRegistry();

   private SettlerPersonalityRegistry() {
      super("SettlerPersonality", 32767);
   }

   @Override
   public void registerCore() {
      registerSettlerPersonality(
         "elder", ElderSettlerPersonality.class, new SimplePersonalityFilter(100).makeSettlerStringIDsWhitelist().filterSettlerStringID("elder"), false
      );
      registerSettlerPersonality("jogger", JoggerSettlerPersonality.class, new SimplePersonalityFilter(100), true);
      registerSettlerPersonality("fastworker", FastWorkerSettlerPersonality.class, new SimplePersonalityFilter(100), true);
      registerSettlerPersonality(
         "warrior", WarriorSettlerPersonality.class, new SimplePersonalityFilter(100).filterPersonalityStringID("ranger", "magician", "summoner"), true
      );
      registerSettlerPersonality(
         "ranger", RangerSettlerPersonality.class, new SimplePersonalityFilter(100).filterPersonalityStringID("warrior", "magician", "summoner"), true
      );
      registerSettlerPersonality(
         "magician", MagicianSettlerPersonality.class, new SimplePersonalityFilter(100).filterPersonalityStringID("warrior", "ranger", "summoner"), true
      );
      registerSettlerPersonality(
         "summoner", SummonerSettlerPersonality.class, new SimplePersonalityFilter(100).filterPersonalityStringID("warrior", "ranger", "magician"), true
      );
      registerSettlerPersonality("persistent", PersistentSettlerPersonality.class, new SimplePersonalityFilter(100), true);
      registerSettlerPersonality("biggestfan", BiggestFanSettlerPersonality.class, new SimplePersonalityFilter(100), true);
      registerSettlerPersonality("inspiring", InspiringSettlerPersonality.class, new SimplePersonalityFilter(100), true);
      registerSettlerPersonality("glyphmaniac", GlyphManiacSettlerPersonality.class, new SimplePersonalityFilter(100), true);
      registerSettlerPersonality(
         "voyager",
         VoyagerSettlerPersonality.class,
         new SimplePersonalityFilter(150).makeSettlerStringIDsWhitelist().filterSettlerStringID("miner", "explorer", "angler", "trader"),
         true
      );
      registerSettlerPersonality(
         "potionseller",
         PotionSellerSettlerPersonality.class,
         new SimplePersonalityFilter(125)
            .filterSettlerStringID("alchemist")
            .addHumanMobFilter(mob -> mob instanceof HumanShop && ((HumanShop)mob).shop.sellingShop.size() > 0),
         true
      );
      FurnitureSet[] furnitureSets = new FurnitureSet[]{
         FurnitureSet.oak, FurnitureSet.spruce, FurnitureSet.maple, FurnitureSet.birch, FurnitureSet.pine, FurnitureSet.palm
      };

      for (FurnitureSet furnitureSet : furnitureSets) {
         registerSettlerPersonality(
            furnitureSet.prefixStringID + "enthusiast",
            new FurnitureSetPreferenceSettlerPersonalityConstructor(furnitureSet),
            new SimplePersonalityFilter(10),
            false
         );
      }

      WallSet[] wallSets = new WallSet[]{WallSet.pine, WallSet.palm, WallSet.willow, WallSet.bamboo, WallSet.stone, WallSet.deepStone, WallSet.brick};

      for (WallSet wallSet : wallSets) {
         registerSettlerPersonality(
            wallSet.prefixStringID + "architect", new WallSetPreferenceSettlerPersonalityConstructor(wallSet), new SimplePersonalityFilter(10), false
         );
      }

      registerSettlerPersonality("maker", MakerSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("beachlover", BeachLoverSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("roomie", RoomieSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("favoritedish", FavoriteDishSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("artconnoisseur", ArtConnoisseurSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("fashionista", FashionistaSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("audiophile", AudiophileSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("ecologist", EcologistSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("clumsy", ClumsySettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("indoorsy", IndoorsySettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("gardener", GardenerSettlerPersonality.class, new SimplePersonalityFilter(100).filterSettlerStringID("guard"), false);
      registerSettlerPersonality("lumberjack", LumberjackSettlerPersonality.class, new SimplePersonalityFilter(100).filterSettlerStringID("guard"), false);
      registerSettlerPersonality("orderly", OrderlySettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality(
         "expeditioner",
         ExpeditionerSettlerPersonality.class,
         new SimplePersonalityFilter(150).makeSettlerStringIDsWhitelist().filterSettlerStringID("miner", "explorer", "angler", "trader"),
         false
      );
      registerSettlerPersonality("bloodthirsty", BloodThirstySettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("adventurer", AdventurerSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("tourist", TouristSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("vengeful", VengefulSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality(
         "pacifist",
         PacifistSettlerPersonality.class,
         new SimplePersonalityFilter(100).filterSettlerStringID("guard").filterPersonalityStringID("warrior", "ranger", "magician", "summoner", "bloodthirsty"),
         false
      );
      registerSettlerPersonality("gemcollector", GemCollectorSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("flowercollector", FlowerCollectorSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("hiredhand", HiredHandCollectorSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("stressedout", StressedOutSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality("weatherbound", WeatherBoundSettlerPersonality.class, new SimplePersonalityFilter(100), false);
      registerSettlerPersonality(
         "gamedevonnecesse",
         GameDevOnNecesseSettlerPersonality.class,
         new SimplePersonalityFilter(100).makeSettlerStringIDsWhitelist().filterSettlerStringID("gamedevonnecesse"),
         false
      );
      registerSettlerPersonality(
         "friendlywitch",
         FriendlyWitchSettlerPersonality.class,
         new SimplePersonalityFilter(100).makeSettlerStringIDsWhitelist().filterSettlerStringID("friendlywitch"),
         false
      );
      registerSettlerPersonality(
         "exoticmerchant",
         ExoticMerchantSettlerPersonality.class,
         new SimplePersonalityFilter(100).makeSettlerStringIDsWhitelist().filterSettlerStringID("exoticmerchant"),
         false
      );
      registerSettlerPersonality(
         "pawnbroker",
         PawnBrokerSettlerPersonality.class,
         new SimplePersonalityFilter(100).makeSettlerStringIDsWhitelist().filterSettlerStringID("pawnbroker"),
         false
      );
   }

   protected void onRegister(SettlerPersonalityRegistry.SettlerPersonalityRegistryElement object, int id, String stringID, boolean isReplace) {
      try {
         object.newInstance(null).onRegistered();
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         throw new RuntimeException(
            "Error registering settler personality \"" + stringID + "\". Make sure we can construct a personality with null HumanMob parameter", e
         );
      }
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerSettlerPersonality(
      String stringID, Class<? extends SettlerPersonality> personalityClass, SettlerPersonalityFilter filter, boolean isBonusPerk
   ) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register settler personalities");
      }

      try {
         return instance.register(stringID, new SettlerPersonalityRegistry.SettlerPersonalityRegistryElement(personalityClass, filter, isBonusPerk));
      } catch (NoSuchMethodException e) {
         System.err.println("Could not register SettlerPersonality " + personalityClass.getSimpleName() + ": Missing constructor with HumanMob parameter");
         return -1;
      }
   }

   public static int replaceSettlerPersonality(
      String stringID, Class<? extends SettlerPersonality> personalityClass, SettlerPersonalityFilter filter, boolean isBonusPerk
   ) {
      try {
         return instance.replace(stringID, new SettlerPersonalityRegistry.SettlerPersonalityRegistryElement(personalityClass, filter, isBonusPerk));
      } catch (NoSuchMethodException e) {
         System.err.println("Could not register SettlerPersonality " + personalityClass.getSimpleName() + ": Missing constructor with HumanMob parameter");
         return -1;
      }
   }

   public static int registerSettlerPersonality(
      String stringID, SettlerPersonalityConstructor personalityConstructor, SettlerPersonalityFilter filter, boolean isBonusPerk
   ) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot settler personalities");
      } else {
         return instance.register(stringID, new SettlerPersonalityRegistry.SettlerPersonalityRegistryElement(personalityConstructor, filter, isBonusPerk));
      }
   }

   public static int replaceSettlerPersonality(
      String stringID, SettlerPersonalityConstructor personalityConstructor, SettlerPersonalityFilter filter, boolean isBonusPerk
   ) {
      return instance.replace(stringID, new SettlerPersonalityRegistry.SettlerPersonalityRegistryElement(personalityConstructor, filter, isBonusPerk));
   }

   public static SettlerPersonality getNewSettlerPersonality(int id, HumanMob mob) {
      if (id >= 0 && id < instance.size()) {
         try {
            return instance.getElement(id).newInstance(mob);
         } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
         }
      } else {
         return null;
      }
   }

   public static SettlerPersonality getNewSettlerPersonality(String stringID, HumanMob mob) {
      return getNewSettlerPersonality(getSettlerPersonalityID(stringID), mob);
   }

   public static ArrayList<SettlerPersonality> getNewRandomSettlerPersonalities(HumanMob mob, GameRandom random, int regularCount, int bonusCount) {
      regularCount = Math.min(regularCount, instance.size());
      TicketSystemList<SettlerPersonalityRegistry.SettlerPersonalityRegistryElement> regularElements = new TicketSystemList<>();
      TicketSystemList<SettlerPersonalityRegistry.SettlerPersonalityRegistryElement> bonusElements = new TicketSystemList<>();

      for (SettlerPersonalityRegistry.SettlerPersonalityRegistryElement element : instance.getElements()) {
         int tickets = element.filter.getTickets(mob);
         if (element.filter.canBeAppliedTo(mob)) {
            if (element.isBonusPerk) {
               bonusElements.addObject(tickets, element);
            } else {
               regularElements.addObject(tickets, element);
            }
         }
      }

      ArrayList<SettlerPersonalityRegistry.SettlerPersonalityRegistryElement> elements = new ArrayList<>(regularCount);

      while (elements.size() < bonusCount && !bonusElements.isEmpty()) {
         SettlerPersonalityRegistry.SettlerPersonalityRegistryElement element = bonusElements.getAndRemoveRandomObject(random);
         boolean isExcluded = false;
         Iterator elementx = elements.iterator();

         while (true) {
            if (elementx.hasNext()) {
               SettlerPersonalityRegistry.SettlerPersonalityRegistryElement other = (SettlerPersonalityRegistry.SettlerPersonalityRegistryElement)elementx.next();
               if (!element.filter.isExcludedBy(other.getStringID()) && !other.filter.excludesOther(element.getStringID())) {
                  continue;
               }

               isExcluded = true;
            }

            if (!isExcluded) {
               elements.add(element);
            }
            break;
         }
      }

      regularCount += elements.size();

      while (elements.size() < regularCount && !regularElements.isEmpty()) {
         SettlerPersonalityRegistry.SettlerPersonalityRegistryElement element = regularElements.getAndRemoveRandomObject(random);
         boolean isExcluded = false;
         Iterator var20 = elements.iterator();

         while (true) {
            if (var20.hasNext()) {
               SettlerPersonalityRegistry.SettlerPersonalityRegistryElement other = (SettlerPersonalityRegistry.SettlerPersonalityRegistryElement)var20.next();
               if (!element.filter.isExcludedBy(other.getStringID()) && !other.filter.excludesOther(element.getStringID())) {
                  continue;
               }

               isExcluded = true;
            }

            if (!isExcluded) {
               elements.add(element);
            }
            break;
         }
      }

      ArrayList<SettlerPersonality> personalities = new ArrayList<>(elements.size());

      for (SettlerPersonalityRegistry.SettlerPersonalityRegistryElement element : elements) {
         personalities.add(getNewSettlerPersonality(element.getID(), mob));
      }

      return personalities;
   }

   public static int getSettlerPersonalityID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static String getSettlerPersonalityStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static boolean isBonusPerk(int id) {
      SettlerPersonalityRegistry.SettlerPersonalityRegistryElement element = instance.getElement(id);
      return element == null ? false : element.isBonusPerk;
   }

   protected static class SettlerPersonalityRegistryElement implements IDDataContainer {
      public final IDData idData = new IDData();
      private final SettlerPersonalityConstructor constructor;
      public SettlerPersonalityFilter filter;
      public boolean isBonusPerk;

      public SettlerPersonalityRegistryElement(Class<? extends SettlerPersonality> personalityClass, SettlerPersonalityFilter filter, boolean isBonusPerk) throws NoSuchMethodException {
         Objects.requireNonNull(filter);
         this.filter = filter;
         Constructor<? extends SettlerPersonality> constructor = personalityClass.getConstructor(HumanMob.class);
         this.constructor = xva$0 -> constructor.newInstance(xva$0);
         this.isBonusPerk = isBonusPerk;
      }

      public SettlerPersonalityRegistryElement(SettlerPersonalityConstructor constructor, SettlerPersonalityFilter filter, boolean isBonusPerk) {
         Objects.requireNonNull(filter);
         this.filter = filter;
         this.constructor = constructor;
         this.isBonusPerk = isBonusPerk;
      }

      @Override
      public IDData getIDData() {
         return this.idData;
      }

      public SettlerPersonality newInstance(HumanMob mob) throws IllegalAccessException, InvocationTargetException, InstantiationException {
         SettlerPersonality personality = this.constructor.construct(mob);
         personality.getIDData().setData(this.idData.getID(), this.idData.getStringID());
         return personality;
      }
   }
}
