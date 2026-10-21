package necesse.gfx;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.gfx.gameTexture.GameTexture;

public class GameSkin {
   public static boolean printDebugs = false;
   public static GameSkinColors colors;
   public static GameTexture recolorLookupTexture;
   private static final ArrayList<GameSkin> skins = new ArrayList<>();
   private static final int humanLikeSkinColors = 4;
   public static GameTexture head;
   public static GameTexture body;
   public static GameTexture leftArms;
   public static GameTexture rightArms;
   public static GameTexture feet;
   public final int colorIndex;
   public final boolean isHumanlike;

   public static void loadSkinTextures() {
      loadColors();
      loadTextures();

      for (int i = 0; i < colors.getSize(); i++) {
         GameSkin skin = new GameSkin(i, i < 4);
         skins.add(skin);
      }
   }

   private static void loadColors() {
      colors = new GameSkinColors();
      String colorsPath = "player/skin/skincolors";

      try {
         recolorLookupTexture = GameTexture.fromFileRaw(colorsPath, true);
         colors.addBaseColors(recolorLookupTexture, 0, 1, recolorLookupTexture.getWidth() - 1);
      } catch (FileNotFoundException e) {
         throw new RuntimeException("Could not find skin colors texture file at " + colorsPath);
      }
   }

   private static void loadTextures() {
      head = loadTexture("head");
      body = loadTexture("body");
      leftArms = loadTexture("arms_left");
      rightArms = loadTexture("arms_right");
      feet = loadTexture("feet");
   }

   protected static GameTexture loadTexture(String fileName) {
      GameTexture texture = GameTexture.fromFile("player/skin/" + fileName, true);
      texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);
      colors.replaceIndexColors(texture, 1);
      texture.runPreAntialias(false);
      return texture;
   }

   private GameSkin(int colorIndex, boolean isHumanlike) {
      this.colorIndex = colorIndex;
      this.isHumanlike = isHumanlike;
   }

   public GameTexture getHeadTexture() {
      return head;
   }

   public GameTexture getBodyTexture() {
      return body;
   }

   public GameTexture getLeftArmsTexture() {
      return leftArms;
   }

   public GameTexture getRightArmsTexture() {
      return rightArms;
   }

   public GameTexture getFeetTexture() {
      return feet;
   }

   public static int getTotalSkins() {
      return skins.size();
   }

   public static GameSkin getSkin(int id, boolean onlyHumanlike) {
      return skins.get(getSkinColorIndex(id, onlyHumanlike));
   }

   public static int getSkinColorIndex(int id, boolean onlyHumanlike) {
      return onlyHumanlike ? id % Math.min(4, skins.size()) : id % skins.size();
   }

   public static int getRandomSkinColor(GameRandom random, boolean onlyHumanLike) {
      if (colors != null && colors.getSize() > 0) {
         TicketSystemList<Integer> ticketList = new TicketSystemList<>();

         for (int i = 0; i < colors.getSize(); i++) {
            if (i < 4 || !onlyHumanLike) {
               ticketList.addObject(colors.getWeight(i), i);
            }
         }

         return ticketList.getRandomObject(random);
      } else {
         return random.nextInt();
      }
   }
}
