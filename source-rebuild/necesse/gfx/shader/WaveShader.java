package necesse.gfx.shader;

import java.awt.geom.Point2D.Double;
import java.util.function.Consumer;
import necesse.engine.Settings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsPositionMod;
import necesse.level.maps.Level;

public class WaveShader extends GameShader {
   public WaveShader() {
      super("vertScene", "fragWave");
   }

   public WaveShader.WaveState setupGrassWaveShader(
      Level level,
      int tileX,
      int tileY,
      long weaveTime,
      final float amount,
      final float height,
      int waves,
      final float textureHeightOffset,
      GameRandom random,
      long seed,
      boolean mirrored,
      float windMod
   ) {
      long weave = level.grassWeave(tileX, tileY);
      final float wind = Settings.wavyGrass
         ? (mirrored ? -level.weatherLayer.getWindAmount(tileX, tileY) : level.weatherLayer.getWindAmount(tileX, tileY)) * windMod
         : 0.0F;
      if (weave <= 0L && wind == 0.0F) {
         return null;
      }

      if (weave <= 0L) {
         weave = 0L;
      } else {
         synchronized (random) {
            if (random.seeded(seed).nextBoolean()) {
               weave = -weave;
            }
         }
      }

      final float bendTime = (float)weave / ((float)weaveTime / waves / (float) Math.PI);
      return new WaveShader.WaveState() {
         @Override
         public void start() {
            WaveShader.this.use();
            WaveShader.this.pass1f("bendAmount", amount);
            WaveShader.this.pass1f("bendHeight", height);
            WaveShader.this.pass1f("bendTime", bendTime);
            WaveShader.this.pass1f("windAmount", wind);
            WaveShader.this.pass1f("heightOffset", textureHeightOffset);
         }

         @Override
         public void end() {
            WaveShader.this.stop();
         }
      };
   }

   public WaveShader.WaveState setupGrassWaveShader(
      Level level, int tileX, int tileY, long weaveTime, float amount, float height, GameRandom random, long seed, boolean mirrored, float windMod
   ) {
      return this.setupGrassWaveShader(level, tileX, tileY, weaveTime, amount, height, 2, 0.0F, random, seed, mirrored, windMod);
   }

   public Consumer<TextureDrawOptionsPositionMod> setupGrassWaveMod(
      Level level, int tileX, int tileY, long weaveTime, float amount, int waves, GameRandom random, long seed, boolean mirrored, float windMod
   ) {
      long weave = level.grassWeave(tileX, tileY);
      float wind = Settings.windEffects && windMod != 0.0F ? level.weatherLayer.getWindAmount(tileX, tileY) * windMod : 0.0F;
      if (weave <= 0L && wind == 0.0F) {
         return o -> {};
      }

      if (weave <= 0L) {
         weave = 0L;
      } else {
         synchronized (random) {
            if (random.seeded(seed).nextBoolean()) {
               weave = -weave;
            }
         }
      }

      float weaveTimePerWave = (float)weaveTime / waves;
      float weaveProgress = (float)weave / weaveTimePerWave;
      float weaveOffset = GameMath.sin(weaveProgress * 180.0F) * amount;
      Double windDir = level.weatherLayer.getWindDirNormalized();
      float windOffset = wind * amount;
      return o -> {
         float offset = o.getWidth() * weaveOffset;
         o.addX1(offset);
         o.addX2(offset);
         float windOffsetX = o.getWidth() * (float)windDir.x * windOffset;
         float windOffsetY = o.getWidth() * (float)windDir.y * windOffset * 0.5F;
         o.addX1(windOffsetX);
         o.addX2(windOffsetX);
         o.addY1(windOffsetY);
         o.addY2(windOffsetY);
      };
   }

   public abstract static class WaveState {
      public abstract void start();

      public abstract void end();
   }
}
