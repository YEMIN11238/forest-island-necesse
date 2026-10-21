package necesse.engine.world.worldPresets;

import java.awt.Point;
import java.util.Objects;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.presets.PresetRotation;
import necesse.level.maps.presets.PresetUtils;

public class WorldApplyAreaPredicate implements WorldApplyPredicate {
   public final int startX;
   public final int startY;
   public final int endX;
   public final int endY;
   public final int dir;
   public final WorldApplyAreaPredicate.WorldApplyAreaTest test;

   public WorldApplyAreaPredicate(int startX, int startY, int endX, int endY, int dir, WorldApplyAreaPredicate.WorldApplyAreaTest test) {
      Objects.requireNonNull(test);
      this.startX = startX;
      this.startY = startY;
      this.endX = endX;
      this.endY = endY;
      this.dir = dir;
      this.test = test;
   }

   @Override
   public boolean canApply(WorldPreset preset, LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, int tileX, int tileY) {
      int startX = tileX + Math.min(this.startX, this.endX);
      int endX = tileX + Math.max(this.startX, this.endX);
      int startY = tileY + Math.min(this.startY, this.endY);
      int endY = tileY + Math.max(this.startY, this.endY);
      return this.test.canApply(preset, presetsRegion, generatorStack, startX, startY, endX, endY, this.dir);
   }

   @Override
   public WorldApplyPredicate mirrorX(int width) {
      int newDir = this.dir != 1 && this.dir != 3 ? this.dir : (this.dir + 2) % 4;
      return new WorldApplyAreaPredicate(
         PresetUtils.getMirroredValue(this.startX, width), this.startY, PresetUtils.getMirroredValue(this.endX, width), this.endY, newDir, this.test
      );
   }

   @Override
   public WorldApplyPredicate mirrorY(int height) {
      int newDir = this.dir != 0 && this.dir != 2 ? this.dir : (this.dir + 2) % 4;
      return new WorldApplyAreaPredicate(
         this.startX, PresetUtils.getMirroredValue(this.startY, height), this.endX, PresetUtils.getMirroredValue(this.endY, height), newDir, this.test
      );
   }

   @Override
   public WorldApplyPredicate rotate(PresetRotation angle, int width, int height) {
      int newDir = (this.dir + (angle == null ? 0 : angle.dirOffset)) % 4;
      Point pos1 = PresetUtils.getRotatedPointInSpace(this.startX, this.startY, width, height, angle);
      Point pos2 = PresetUtils.getRotatedPointInSpace(this.endX, this.endY, width, height, angle);
      return new WorldApplyAreaPredicate(pos1.x, pos1.y, pos2.x, pos2.y, newDir, this.test);
   }

   @FunctionalInterface
   public interface WorldApplyAreaTest {
      boolean canApply(WorldPreset var1, LevelPresetsRegion var2, BiomeGeneratorStack var3, int var4, int var5, int var6, int var7, int var8);
   }

   @FunctionalInterface
   public interface WorldApplyCornerTest extends WorldApplyAreaPredicate.WorldApplyDimensionTest {
      boolean isValidTile(WorldPreset var1, LevelPresetsRegion var2, BiomeGeneratorStack var3, int var4, int var5);

      @Override
      default boolean canApplyDimension(
         final WorldPreset preset,
         final LevelPresetsRegion presetsRegion,
         final BiomeGeneratorStack generatorStack,
         int tileX,
         int tileY,
         int width,
         int height,
         int dir
      ) {
         return preset.runCornerCheck(tileX, tileY, width, height, new WorldPreset.ValidTilePredicate() {
            @Override
            public boolean isValidPosition(int tileX, int tileYx) {
               return WorldApplyCornerTest.this.isValidTile(preset, presetsRegion, generatorStack, tileX, tileYx);
            }
         });
      }
   }

   @FunctionalInterface
   public interface WorldApplyDimensionTest extends WorldApplyAreaPredicate.WorldApplyAreaTest {
      boolean canApplyDimension(WorldPreset var1, LevelPresetsRegion var2, BiomeGeneratorStack var3, int var4, int var5, int var6, int var7, int var8);

      @Override
      default boolean canApply(
         WorldPreset preset,
         LevelPresetsRegion presetsRegion,
         BiomeGeneratorStack generatorStack,
         int startTileX,
         int startTileY,
         int endTileX,
         int endTileY,
         int dir
      ) {
         return this.canApplyDimension(preset, presetsRegion, generatorStack, startTileX, startTileY, endTileX - startTileX + 1, endTileY - startTileY + 1, dir);
      }
   }

   public abstract static class WorldApplyGridTest implements WorldApplyAreaPredicate.WorldApplyDimensionTest {
      private final int resolution;

      public WorldApplyGridTest(int resolution) {
         this.resolution = resolution;
      }

      public abstract boolean isValidTile(WorldPreset var1, LevelPresetsRegion var2, BiomeGeneratorStack var3, int var4, int var5);

      @Override
      public boolean canApplyDimension(
         final WorldPreset preset,
         final LevelPresetsRegion presetsRegion,
         final BiomeGeneratorStack generatorStack,
         int tileX,
         int tileY,
         int width,
         int height,
         int dir
      ) {
         return preset.runGridCheck(tileX, tileY, width, height, this.resolution, new WorldPreset.ValidTilePredicate() {
            @Override
            public boolean isValidPosition(int tileX, int tileYx) {
               return WorldApplyGridTest.this.isValidTile(preset, presetsRegion, generatorStack, tileX, tileYx);
            }
         });
      }
   }
}
