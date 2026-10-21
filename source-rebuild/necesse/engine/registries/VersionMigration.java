package necesse.engine.registries;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public class VersionMigration {
   public static String[][] oldTileStringIDs = new String[][]{{"stonefloor", "rockfloor"}, {"deepstonefloor", "deeprockfloor"}};
   public static String[][] oldObjectStringIDs = new String[][]{
      {"sprucedinnertable", "wooddinnertable"},
      {"sprucedinnertable2", "wooddinnertable2"},
      {"sprucedesk", "wooddesk"},
      {"sprucemodulartable", "woodmodulartable"},
      {"sprucechair", "woodchair"},
      {"sprucebookshelf", "woodbookshelf"},
      {"sprucebathtub", "woodbathtub"},
      {"sprucebathtub2", "woodbathtubr"},
      {"sprucetoilet", "woodtoilet"},
      {"sprucebed", "woodbed"},
      {"sprucebed2", "woodbed2"},
      {"sprucedisplay", "itemstand"},
      {"woolcarpet", "woolcarpetr", "woolcarpetd", "woolcarpetdr"},
      {"leathercarpet", "leathercarpetr", "leathercarpetd", "leathercarpetdr"},
      {"stonepressureplate", "rockpressureplate"},
      {"deepstonepressureplate", "deeprockpressureplate"},
      {"tungstenworkstation", "advancedworkstation"},
      {"tungstenworkstation2", "advancedworkstation2"},
      {"teleportationstone", "travelstone"},
      {"crystalrock", "crystal"},
      {"upgradeshardcrystalrock", "upgradeshardcrystal"},
      {"alchemyshardcrystalrock", "alchemyshardcrystal"},
      {"crystalessencecrystalrock", "omnicrystalcrystal"}
   };
   public static String[][] oldLogicGateStringIDs = new String[0][];
   public static String[][] oldItemStringIDs = new String[][]{
      {"brainonastick", "babyzombie"},
      {"inefficientfeather", "tameostrich"},
      {"weticicle", "petpenguin"},
      {"exoticseeds", "petparrot"},
      {"magicstilts", "petwalkingtorch"},
      {"sprucelog", "log"},
      {"healthregenpotion", "combatregenpotion"},
      {"waterfaevinyl", "surfacevinyl"},
      {"musesvinyl", "surfacenightvinyl"},
      {"runningvinyl", "cavevinyl"},
      {"grindthealarmsvinyl", "deepcavevinyl"},
      {"bythefieldvinyl", "desertsurfacevinyl"},
      {"sunstonesvinyl", "desertnightvinyl"},
      {"caravantusksvinyl", "desertcavevinyl"},
      {"homeatlastvinyl", "snowsurfacevinyl"},
      {"telltalevinyl", "snownightvinyl"},
      {"icyrusevinyl", "snowcavevinyl"},
      {"icestarvinyl", "deepsnowcavevinyl"},
      {"eyesofthedesertvinyl", "swampsurfacevinyl"},
      {"rialtovinyl", "swampnightvinyl"},
      {"silverlakevinyl", "swampcavevinyl"},
      {"awayvinyl", "piratesurfacevinyl"},
      {"kronosvinyl", "dungeonvinyl"},
      {"lostgripvinyl", "raidvinyl"},
      {"elektrakvinyl", "boss1vinyl"},
      {"thecontrolroomvinyl", "boss2vinyl"},
      {"airlockfailurevinyl", "boss3vinyl"},
      {"konsoleglitchvinyl", "boss4vinyl"},
      {"beatdownvinyl", "boss5vinyl"},
      {"siegevinyl", "boss6vinyl"},
      {"halodromevinyl", "boss7vinyl"},
      {"milleniumvinyl", "boss8vinyl"},
      {"kandiruvinyl", "boss9vinyl"},
      {"spiritboard", "ouijaboard"},
      {"teleportationscroll", "travelscroll"},
      {"teleportationstone", "travelstone"},
      {"sharkscales", "sharkfin"}
   };
   public static String[][] oldMobStringIDs = new String[][]{{"exoticmerchanthuman", "travelingmerchant"}};
   public static String[][] oldBiomeStringIDs = new String[][]{
      {"dungeon", "dungeonisland", "DungeonIsland"},
      {"piratevillage", "pirateisland", "PirateIsland"},
      {"forest", "forestvillage", "villageisland", "VillageIsland"},
      {"plains", "plainsvillage"},
      {"desert", "desertvillage"},
      {"snow", "snowvillage"}
   };

   public static String tryFixStringID(String stringID, String[][] oldStringIDs, String defaultValue) {
      int index = getFixedConversionIndex(stringID, oldStringIDs);
      return index != -1 ? oldStringIDs[index][0] : defaultValue;
   }

   public static String tryFixStringID(String stringID, String[][] oldStringIDs) {
      return tryFixStringID(stringID, oldStringIDs, stringID);
   }

   private static int getFixedConversionIndex(String stringID, String[][] oldStringIDs) {
      for (int i = 0; i < oldStringIDs.length; i++) {
         if (oldStringIDs[i].length >= 2) {
            for (int j = 0; j < oldStringIDs[i].length; j++) {
               if (stringID.equals(oldStringIDs[i][j])) {
                  return i;
               }
            }
         }
      }

      return -1;
   }

   private static int getNewStringIDIndex(String fromStringID, String[] toStringIDs, String[][] oldStringIDs, int unknownID) {
      for (int j = 0; j < toStringIDs.length; j++) {
         if (toStringIDs[j] != null && toStringIDs[j].equals(fromStringID)) {
            return j;
         }
      }

      if (oldStringIDs != null) {
         int newIndex = getFixedConversionIndex(fromStringID, oldStringIDs);
         if (newIndex != -1) {
            return getNewStringIDIndex(oldStringIDs[newIndex][0], toStringIDs, null, unknownID);
         }
      }

      return unknownID;
   }

   public static int[] getConversionArray(String[] fromStringIDs, String[] toStringIDs, String[][] oldStringIDs, int unknownID) {
      if (fromStringIDs != null && toStringIDs != null) {
         int[] out = new int[fromStringIDs.length];
         boolean updated = false;

         for (int i = 0; i < fromStringIDs.length; i++) {
            String fromStringID = fromStringIDs[i];
            if (fromStringID != null) {
               int newIndex = getNewStringIDIndex(fromStringID, toStringIDs, oldStringIDs, unknownID);
               out[i] = newIndex;
               if (newIndex != i) {
                  updated = true;
               }
            }
         }

         return !updated ? null : out;
      } else {
         return null;
      }
   }

   public static boolean convertArray(int[] array, int[] conversionArray) {
      if (conversionArray == null) {
         return false;
      }

      for (int i = 0; i < array.length; i++) {
         int old = array[i];
         if (old >= 0 && old < conversionArray.length) {
            array[i] = conversionArray[old];
         }
      }

      return true;
   }

   public static int convertIndex(int index, int[] conversionArray) {
      return index >= 0 && index < conversionArray.length ? conversionArray[index] : index;
   }

   public static String[][] concatOldStringIDs(String[][]... arrays) {
      int totalLength = 0;

      for (String[][] array : arrays) {
         totalLength += array.length;
      }

      String[][] result = new String[totalLength][];
      int lengthCounter = 0;

      for (String[][] array : arrays) {
         System.arraycopy(array, 0, result, lengthCounter, array.length);
         lengthCounter += array.length;
      }

      return result;
   }

   public static boolean convertArray(int[] array, String[] fromArray, String[] toArray, int unknownID, String[][] oldStringIDs) {
      return convertArray(array, getConversionArray(fromArray, toArray, oldStringIDs, unknownID));
   }

   public static boolean convertArray(int[] array, String[] fromArray, String[] toArray, int unknownID) {
      return convertArray(array, fromArray, toArray, unknownID, null);
   }

   public static String[] generateStringIDsArray(Collection<Integer> ids, Function<Integer, String> toStringID) {
      String[] array = new String[ids.size() * 2];
      int i = 0;

      for (int id : ids) {
         array[i++] = Integer.toString(id);
         array[i++] = toStringID.apply(id);
      }

      return array;
   }

   public static int[] generateStringIDsArrayConversionArray(String[] stringIDsArray, String[] toStringIDs, int unknownID, String[][] oldStringIDs) {
      String[] fromStringIDs = new String[toStringIDs.length];

      for (int i = 0; i < stringIDsArray.length; i += 2) {
         try {
            int id = Integer.parseInt(stringIDsArray[i]);
            if (id >= 0) {
               if (fromStringIDs.length <= id) {
                  fromStringIDs = Arrays.copyOf(fromStringIDs, id + 1);
               }

               fromStringIDs[id] = stringIDsArray[i + 1];
            }
         } catch (NumberFormatException var7) {
         }
      }

      return getConversionArray(fromStringIDs, toStringIDs, oldStringIDs, unknownID);
   }

   static {
      oldItemStringIDs = concatOldStringIDs(oldTileStringIDs, oldObjectStringIDs, oldItemStringIDs);
   }
}
