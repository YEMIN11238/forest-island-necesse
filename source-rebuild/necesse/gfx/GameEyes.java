package necesse.gfx;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.gfx.gameTexture.GameTexture;

public class GameEyes {
   public static boolean printDebugs = false;
   public static GameSkinColors colors;
   private static ArrayList<GameEyes> eyes;
   public static GameTexture recolorLookupTexture;
   public static GameTexture defaultClosedEyeTexture;
   public static GameTexture defaultClosedSkinTexture;
   public static GameTexture defaultOpenEyeTexture;
   public static GameTexture defaultOpenSkinTexture;
   public final int eyeIndex;
   public final int weight;
   private final HumanGender gender;
   public GameTexture openEyeColorTexture;
   public GameTexture openSkinColorTexture;
   public GameTexture closedEyeColorTexture;
   public GameTexture closedSkinColorTexture;

   public static void loadEyeTypes() {
      eyes = new ArrayList<>();
      eyes.add(new GameEyes(1, 100, HumanGender.NEUTRAL));
      eyes.add(new GameEyes(2, 100, HumanGender.NEUTRAL));
      eyes.add(new GameEyes(3, 25, HumanGender.NEUTRAL));
      eyes.add(new GameEyes(4, 50, HumanGender.NEUTRAL));
      eyes.add(new GameEyes(5, 10, HumanGender.NEUTRAL));
      eyes.add(new GameEyes(6, 0, HumanGender.NEUTRAL));
      eyes.add(new GameEyes(7, 0, HumanGender.NEUTRAL));
      eyes.add(new GameEyes(8, 0, HumanGender.NEUTRAL));
      eyes.add(new GameEyes(9, 50, HumanGender.FEMALE));
      eyes.add(new GameEyes(10, 25, HumanGender.NEUTRAL));
      eyes.add(new GameEyes(11, 25, HumanGender.NEUTRAL));
      eyes.add(new GameEyes(12, 10, HumanGender.NEUTRAL));
      eyes.add(new GameEyes(13, 0, HumanGender.NEUTRAL));
      eyes.add(new GameEyes(14, 0, HumanGender.NEUTRAL));
   }

   public static void loadEyeTextures() {
      loadColors();
      GameTexture defaultClosedTexture = GameTexture.fromFile("player/eyes/eyes_closed", true);
      GameTexture[] textures = loadTextures(defaultClosedTexture);
      defaultClosedEyeTexture = textures[0];
      defaultClosedSkinTexture = textures[1];

      for (GameEyes eye : eyes) {
         eye.loadTextures();
      }
   }

   private static void loadColors() {
      colors = new GameSkinColors();
      String colorsPath = "player/eyes/eyecolors";

      try {
         recolorLookupTexture = GameTexture.fromFileRaw(colorsPath, true);
         colors.addBaseColors(recolorLookupTexture, 0, 1, recolorLookupTexture.getWidth() - 1);
      } catch (FileNotFoundException e) {
         throw new RuntimeException("Could not find eye colors texture file at " + colorsPath);
      }
   }

   private static GameTexture[] loadTextures(GameTexture originalTexture) {
      return new GameTexture[]{loadTexture(originalTexture, colors, GameSkin.colors), loadTexture(originalTexture, GameSkin.colors, colors)};
   }

   private static GameTexture loadTexture(GameTexture originalTexture, GameSkinColors colors, GameSkinColors removeTones) {
      GameTexture texture = new GameTexture(originalTexture);
      HashSet<Color> excludes = new HashSet<>(colors.getToneColors());
      colors.removeToneColors(texture, removeTones.getToneColors(), excludes);
      colors.replaceIndexColors(texture, 1);
      texture.runPreAntialias(false);
      texture.makeFinal();
      return texture;
   }

   private GameEyes(int eyeIndex, int weight, HumanGender gender) {
      this.eyeIndex = eyeIndex;
      this.weight = weight;
      this.gender = gender;
   }

   private void loadTextures() {
      GameTexture[] textures = new GameTexture[]{defaultOpenEyeTexture, defaultOpenSkinTexture};

      try {
         GameTexture openTexture = GameTexture.fromFileRaw("player/eyes/eyes" + this.eyeIndex, true);
         textures = loadTextures(openTexture);
      } catch (FileNotFoundException e) {
         if (defaultOpenEyeTexture == null || defaultOpenSkinTexture == null) {
            textures = loadTextures(GameResources.error);
         }
      }

      this.openEyeColorTexture = textures[0];
      this.openSkinColorTexture = textures[1];
      textures = new GameTexture[]{defaultClosedEyeTexture, defaultClosedSkinTexture};

      try {
         GameTexture closedTexture = GameTexture.fromFileRaw("player/eyes/eyes" + this.eyeIndex + "_closed", true);
         textures = loadTextures(closedTexture);
      } catch (FileNotFoundException e) {
         if (defaultClosedEyeTexture == null || defaultClosedSkinTexture == null) {
            loadTextures(GameResources.error);
         }
      }

      this.closedEyeColorTexture = textures[0];
      this.closedSkinColorTexture = textures[1];
   }

   public <T> List<T> getOpenTextures(Function<GameTexture, T> eyeMapper, Function<GameTexture, T> skinMapper) {
      return Arrays.asList(skinMapper.apply(this.openSkinColorTexture), eyeMapper.apply(this.openEyeColorTexture));
   }

   public <T> List<T> getClosedTextures(Function<GameTexture, T> eyeMapper, Function<GameTexture, T> skinMapper) {
      return Arrays.asList(skinMapper.apply(this.closedSkinColorTexture), eyeMapper.apply(this.closedEyeColorTexture));
   }

   public static int getTotalEyeTypes() {
      return eyes.size();
   }

   public static int getTotalColors() {
      return colors.getSize();
   }

   public static GameEyes getEyes(int eyeType) {
      return eyes.get(eyeType % eyes.size());
   }

   public static int getRandomEyeColor(GameRandom random) {
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

   public static int getRandomEyesBasedOnGender(GameRandom random, HumanGender gender) {
      if (eyes != null && !eyes.isEmpty()) {
         TicketSystemList<Integer> ticketList = new TicketSystemList<>();

         for (int i = 0; i < eyes.size(); i++) {
            HumanGender eyeGender = eyes.get(i).gender;
            if (eyeGender == gender || eyeGender == HumanGender.NEUTRAL) {
               ticketList.addObject(eyes.get(i).weight, i);
            }
         }

         return ticketList.isEmpty() ? random.nextInt() : ticketList.getRandomObject(random);
      } else {
         return random.nextInt();
      }
   }
}
