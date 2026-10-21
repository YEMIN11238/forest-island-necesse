package necesse.engine.postProcessing;

import java.awt.Color;
import java.awt.Point;
import java.util.LinkedList;
import java.util.function.Supplier;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.client.Client;
import necesse.engine.postProcessing.effects.AbstractHeatDistortionEffect;
import necesse.engine.postProcessing.effects.AbstractShockwaveEffect;
import necesse.engine.postProcessing.effects.DarknessFade;
import necesse.engine.postProcessing.effects.HeatDistortionEffect;
import necesse.engine.postProcessing.effects.ShockwaveEffect;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.engine.world.WorldEntity;
import necesse.gfx.GameResources;

public class PostProcessingEffects {
   private static float sceneRed = 1.0F;
   private static float sceneGreen = 1.0F;
   private static float sceneBlue = 1.0F;
   private static boolean nextSceneUpdate = false;
   private static float nextSceneRed;
   private static float nextSceneGreen;
   private static float nextSceneBlue;
   private static boolean nextSceneDarknessUpdate = false;
   private static float nextSceneDarkness;
   private static int nextSceneDarknessMinViewDist;
   private static int nextSceneDarknessFadeDist;
   private static int sceneDarknessMidX;
   private static int sceneDarknessMidY;
   private static DarknessFade sceneDarknessFade;
   public static LinkedList<AbstractShockwaveEffect> shockwaveEffects = new LinkedList<>();
   public static LinkedList<AbstractHeatDistortionEffect> heatDistortEffects = new LinkedList<>();

   public static void initialize() {
   }

   public static void preGameTick(TickManager tickManager) {
      if (tickManager.isGameTick()) {
         nextSceneUpdate = false;
         nextSceneDarknessUpdate = false;
         nextSceneDarkness = 0.0F;
         nextSceneDarknessMinViewDist = 0;
         nextSceneDarknessFadeDist = 1000;
      }
   }

   public static void postGameTick(TickManager tickManager) {
      if (tickManager.isGameTick()) {
         GameWindow window = WindowManager.getWindow();
         if (nextSceneUpdate) {
            sceneRed = nextSceneRed;
            sceneGreen = nextSceneGreen;
            sceneBlue = nextSceneBlue;
         } else {
            sceneRed = 1.0F;
            sceneGreen = 1.0F;
            sceneBlue = 1.0F;
         }

         window.setSceneShade(sceneRed, sceneGreen, sceneBlue);
         if (sceneDarknessFade != null && !sceneDarknessFade.apply()) {
            sceneDarknessFade = null;
         }

         GameResources.darknessShader.midScreenX = sceneDarknessMidX;
         GameResources.darknessShader.midScreenY = sceneDarknessMidY;
         window.setSceneDarkness(nextSceneDarkness, nextSceneDarknessMinViewDist, nextSceneDarknessFadeDist);
         shockwaveEffects.removeIf(AbstractShockwaveEffect::shouldRemove);
         heatDistortEffects.removeIf(AbstractHeatDistortionEffect::shouldRemove);
      }
   }

   public static void setSceneShade(float red, float green, float blue) {
      if (!nextSceneUpdate) {
         nextSceneRed = red;
         nextSceneGreen = green;
         nextSceneBlue = blue;
         nextSceneUpdate = true;
      } else if (red == sceneRed && green == sceneGreen && blue == sceneBlue) {
         nextSceneRed = red;
         nextSceneGreen = green;
         nextSceneBlue = blue;
      }
   }

   public static void setSceneShade(Color color) {
      setSceneShade(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
   }

   public static void setSceneDarkness(float percent, int minViewDist, int fadeDist, int midX, int midY) {
      if (!nextSceneDarknessUpdate || percent > nextSceneDarkness) {
         nextSceneDarkness = percent;
      }

      if (!nextSceneDarknessUpdate || minViewDist < nextSceneDarknessMinViewDist) {
         nextSceneDarknessMinViewDist = minViewDist;
      }

      if (!nextSceneDarknessUpdate || fadeDist < nextSceneDarknessFadeDist) {
         nextSceneDarknessFadeDist = fadeDist;
      }

      sceneDarknessMidX = midX;
      sceneDarknessMidY = midY;
      nextSceneDarknessUpdate = true;
   }

   public static void setSceneDarknessFade(
      float startPercent, float endPercent, int minViewDist, int fadeDist, int timeSpan, Supplier<Point> midGetter, Supplier<Boolean> isValid
   ) {
      sceneDarknessFade = new DarknessFade(startPercent, endPercent, minViewDist, fadeDist, System.currentTimeMillis(), timeSpan, midGetter, isValid);
   }

   public static void addShockwaveEffect(AbstractShockwaveEffect effect) {
      shockwaveEffects.add(effect);
   }

   public static void addShockwaveEffect(
      final Client client, int centerX, int centerY, float maxDistance, float fadeInDistance, float fadeOutDistance, float size, int speed
   ) {
      final WorldEntity worldEntity = client.worldEntity;
      if (worldEntity != null) {
         shockwaveEffects.add(new ShockwaveEffect(centerX, centerY, maxDistance, fadeInDistance, fadeOutDistance, size, speed) {
            @Override
            public long getCurrentTime() {
               return worldEntity.getLocalTime();
            }

            @Override
            public boolean shouldRemove() {
               return super.shouldRemove() || client.hasDisconnected();
            }
         });
      }
   }

   public static <T extends AbstractHeatDistortionEffect> T addHeatDistortEffect(T effect) {
      heatDistortEffects.add(effect);
      return effect;
   }

   public static HeatDistortionEffect addHeatDistortEffect(
      final Client client, int centerX, int centerY, float radiusX, float radiusY, float speed, int totalTime
   ) {
      final WorldEntity worldEntity = client.worldEntity;
      if (worldEntity == null) {
         return null;
      }

      HeatDistortionEffect effect = new HeatDistortionEffect(centerX, centerY, radiusX, radiusY, speed, totalTime) {
         @Override
         public long getCurrentTime() {
            return worldEntity.getLocalTime();
         }

         @Override
         public boolean shouldRemove() {
            return super.shouldRemove() || client.hasDisconnected();
         }
      };
      heatDistortEffects.add(effect);
      return effect;
   }
}
