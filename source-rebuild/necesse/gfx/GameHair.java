package necesse.gfx;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.gfx.gameTexture.GameTexture;

public class GameHair {
   public static int COMMON_HAIR_COLOR_WEIGHT = 255;
   public static int UNCOMMON_HAIR_COLOR_WEIGHT = 153;
   public static int RARE_HAIR_COLOR_WEIGHT = 64;
   public static boolean printDebugs = false;
   public static GameTexture recolorLookupTexture;
   public static GameSkinColors colors;
   private static ArrayList<GameHair> hairs;
   private static ArrayList<GameHair> facialFeatures;
   public final int hairIndex;
   public final int weight;
   private GameTexture texture;
   private GameTexture backTexture;
   private GameTexture wigTexture;
   private final HumanGender gender;
   private final boolean isFacialFeature;

   public static void loadHairTypes() {
      hairs = new ArrayList<>();
      hairs.add(new GameHair(0, 100, HumanGender.NEUTRAL, false));
      hairs.add(new GameHair(1, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(2, 100, HumanGender.FEMALE, false));
      hairs.add(new GameHair(3, 100, HumanGender.NEUTRAL, false));
      hairs.add(new GameHair(4, 100, HumanGender.NEUTRAL, false));
      hairs.add(new GameHair(5, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(6, 100, HumanGender.NEUTRAL, false));
      hairs.add(new GameHair(7, 100, HumanGender.FEMALE, false));
      hairs.add(new GameHair(8, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(9, 100, HumanGender.NEUTRAL, false));
      hairs.add(new GameHair(10, 100, HumanGender.NEUTRAL, false));
      hairs.add(new GameHair(11, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(12, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(13, 50, HumanGender.FEMALE, false));
      hairs.add(new GameHair(14, 100, HumanGender.FEMALE, false));
      hairs.add(new GameHair(15, 100, HumanGender.FEMALE, false));
      hairs.add(new GameHair(16, 100, HumanGender.NEUTRAL, false));
      hairs.add(new GameHair(17, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(18, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(19, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(20, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(21, 100, HumanGender.NEUTRAL, false));
      hairs.add(new GameHair(22, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(23, 100, HumanGender.NEUTRAL, false));
      hairs.add(new GameHair(24, 100, HumanGender.NEUTRAL, false));
      hairs.add(new GameHair(25, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(26, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(27, 100, HumanGender.FEMALE, false));
      hairs.add(new GameHair(28, 100, HumanGender.NEUTRAL, false));
      hairs.add(new GameHair(29, 100, HumanGender.FEMALE, false));
      hairs.add(new GameHair(30, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(31, 100, HumanGender.FEMALE, false));
      hairs.add(new GameHair(32, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(33, 100, HumanGender.FEMALE, false));
      hairs.add(new GameHair(34, 100, HumanGender.FEMALE, false));
      hairs.add(new GameHair(35, 100, HumanGender.MALE, false));
      hairs.add(new GameHair(36, 100, HumanGender.NEUTRAL, false));
      hairs.add(new GameHair(37, 100, HumanGender.FEMALE, false));
      facialFeatures = new ArrayList<>();
      facialFeatures.add(new GameHair(0, 500, HumanGender.MALE, true));
      facialFeatures.add(new GameHair(1, 100, HumanGender.MALE, true));
      facialFeatures.add(new GameHair(2, 100, HumanGender.MALE, true));
      facialFeatures.add(new GameHair(3, 100, HumanGender.MALE, true));
      facialFeatures.add(new GameHair(4, 100, HumanGender.MALE, true));
      facialFeatures.add(new GameHair(5, 100, HumanGender.MALE, true));
      facialFeatures.add(new GameHair(6, 100, HumanGender.MALE, true));
      facialFeatures.add(new GameHair(7, 100, HumanGender.MALE, true));
      facialFeatures.add(new GameHair(8, 100, HumanGender.MALE, true));
      facialFeatures.add(new GameHair(9, 100, HumanGender.MALE, true));
   }

   public static void loadHairTextures() {
      if (hairs == null) {
         throw new NullPointerException("Load hair before loading hair textures.");
      }

      loadColors();

      for (GameHair c : hairs) {
         c.loadTextures("hair", "hair");
      }

      for (GameHair c : facialFeatures) {
         c.loadTextures("facialfeature", "facialfeature");
      }
   }

   private static void loadColors() {
      colors = new GameSkinColors();
      String colorsPath = "player/hair/haircolors";

      try {
         recolorLookupTexture = GameTexture.fromFileRaw(colorsPath, true);
         colors.addBaseColors(recolorLookupTexture, 0, 1, recolorLookupTexture.getWidth() - 1);
      } catch (FileNotFoundException e) {
         throw new RuntimeException("Could not find hair colors texture file at " + colorsPath);
      }
   }

   public GameHair(int hairIndex, int weight, HumanGender gender, boolean isFacialFeature) {
      this.weight = weight;
      this.hairIndex = hairIndex;
      this.gender = gender;
      this.isFacialFeature = isFacialFeature;
   }

   public GameTexture getHairTexture() {
      return this.hairIndex == 0 ? null : this.texture;
   }

   public GameTexture getBackHairTexture() {
      return this.hairIndex != 0 && this.backTexture != null ? this.backTexture : null;
   }

   public GameTexture getWigTexture() {
      return this.wigTexture;
   }

   public static GameHair getHair(int id) {
      return hairs.get(id % hairs.size());
   }

   public static GameHair getFacialFeature(int id) {
      return facialFeatures.get(id % facialFeatures.size());
   }

   public static HumanGender getHairGender(int id) {
      return hairs.get(id % hairs.size()).gender;
   }

   public static HumanGender getFacialFeatureGender(int id) {
      return facialFeatures.get(id % facialFeatures.size()).gender;
   }

   public static ArrayList<Integer> getMaleHairIDs() {
      ArrayList<Integer> maleHairIDs = new ArrayList<>();

      for (GameHair hair : hairs) {
         if (hair.gender == HumanGender.MALE) {
            maleHairIDs.add(hair.hairIndex);
         }
      }

      return maleHairIDs;
   }

   public static ArrayList<Integer> getFemaleHairIDs() {
      ArrayList<Integer> femaleHairIDs = new ArrayList<>();

      for (GameHair hair : hairs) {
         if (hair.gender == HumanGender.FEMALE) {
            femaleHairIDs.add(hair.hairIndex);
         }
      }

      return femaleHairIDs;
   }

   public static int getTotalHair() {
      return hairs.size();
   }

   public static int getTotalFacialFeatures() {
      return facialFeatures.size();
   }

   public static int getTotalHairColors() {
      return colors.getSize();
   }

   public static int getRandomHairColor(GameRandom random) {
      if (colors != null && colors.getSize() > 0) {
         TicketSystemList<Integer> ticketList = new TicketSystemList<>();

         for (int i = 0; i < colors.getSize(); i++) {
            ticketList.addObject(colors.getWeight(i), i);
         }

         return ticketList.getRandomObject(random);
      } else {
         return random.nextInt();
      }
   }

   public static int getRandomHairColorAboveColorWeight(GameRandom random, int weight) {
      if (colors != null && colors.getSize() > 0) {
         TicketSystemList<Integer> ticketList = new TicketSystemList<>();

         for (int i = 0; i < colors.getSize(); i++) {
            if (colors.getWeight(i) >= weight) {
               ticketList.addObject(colors.getWeight(i), i);
            }
         }

         return ticketList.isEmpty() ? random.nextInt() : ticketList.getRandomObject(random);
      } else {
         return random.nextInt();
      }
   }

   public static int getRandomHairColorAtSpecificWeight(GameRandom random, int weight) {
      if (colors != null && colors.getSize() > 0) {
         TicketSystemList<Integer> ticketList = new TicketSystemList<>();

         for (int i = 0; i < colors.getSize(); i++) {
            if (colors.getWeight(i) == weight) {
               ticketList.addObject(colors.getWeight(i), i);
            }
         }

         return ticketList.isEmpty() ? random.nextInt() : ticketList.getRandomObject(random);
      } else {
         return random.nextInt();
      }
   }

   public static int getRandomHair(GameRandom random) {
      if (hairs != null && !hairs.isEmpty()) {
         TicketSystemList<Integer> ticketList = new TicketSystemList<>();

         for (int i = 0; i < hairs.size(); i++) {
            ticketList.addObject(hairs.get(i).weight, i);
         }

         return ticketList.getRandomObject(random);
      } else {
         return random.nextInt();
      }
   }

   public static int getRandomHairBasedOnGender(GameRandom random, HumanGender gender) {
      if (hairs != null && !hairs.isEmpty()) {
         TicketSystemList<Integer> ticketList = new TicketSystemList<>();

         for (int i = 0; i < hairs.size(); i++) {
            HumanGender hairGender = hairs.get(i).gender;
            if (hairGender == gender || hairGender == HumanGender.NEUTRAL) {
               ticketList.addObject(hairs.get(i).weight, i);
            }
         }

         return ticketList.isEmpty() ? random.nextInt() : ticketList.getRandomObject(random);
      } else {
         return random.nextInt();
      }
   }

   public static int getRandomFacialFeature(GameRandom random) {
      if (facialFeatures != null && !facialFeatures.isEmpty()) {
         TicketSystemList<Integer> ticketList = new TicketSystemList<>();

         for (int i = 0; i < facialFeatures.size(); i++) {
            ticketList.addObject(facialFeatures.get(i).weight, i);
         }

         return ticketList.getRandomObject(random);
      } else {
         return random.nextInt();
      }
   }

   private void loadTextures(String folderName, String hairPrefix) {
      this.loadFrontTexture(folderName, hairPrefix);
      this.loadBackTexture(folderName, hairPrefix);
      this.loadWigTexture(folderName);
   }

   public void loadFrontTexture(String folderName, String hairPrefix) {
      if (this.hairIndex != 0) {
         GameTexture originalTexture = GameTexture.fromFile("player/" + folderName + "/" + hairPrefix + this.hairIndex, true);
         this.texture = new GameTexture(originalTexture);
         colors.replaceIndexColors(this.texture, 1);
         this.texture.runPreAntialias(false);
         this.texture.makeFinal();
      }
   }

   public void loadBackTexture(String folderName, String hairPrefix) {
      if (this.hairIndex != 0) {
         GameTexture originalTexture;
         try {
            originalTexture = GameTexture.fromFileRaw("player/" + folderName + "/" + hairPrefix + this.hairIndex + "_back", true);
         } catch (FileNotFoundException e) {
            return;
         }

         this.backTexture = new GameTexture(originalTexture);
         if (this.hairIndex > 0) {
            colors.replaceIndexColors(this.backTexture, 1);
         }

         this.backTexture.runPreAntialias(false);
         this.backTexture.makeFinal();
      }
   }

   public void loadWigTexture(String folderName) {
      GameTexture wigsTexture = GameTexture.fromFile("player/" + folderName + "/wigs", true);
      GameTexture originalTexture;
      if (wigsTexture.getWidth() >= 32 * (this.hairIndex + 1)) {
         originalTexture = new GameTexture(wigsTexture, 32 * this.hairIndex, 0, 32, 32);
      } else {
         originalTexture = new GameTexture(GameResources.error);
      }

      this.wigTexture = new GameTexture(originalTexture);
      colors.replaceIndexColors(this.wigTexture, 1);
      this.wigTexture.runPreAntialias(false);
      this.wigTexture.makeFinal();
   }
}
