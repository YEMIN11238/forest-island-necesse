package necesse.engine.world.worldPresets;

import java.awt.Point;
import java.util.Objects;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.presets.PresetRotation;
import necesse.level.maps.presets.PresetUtils;

public class WorldApplySinglePredicate implements WorldApplyPredicate {
   public final int tileX;
   public final int tileY;
   public final int dir;
   public final WorldApplySinglePredicate.WorldApplyTileTest test;

   public WorldApplySinglePredicate(int tileX, int tileY, int dir, WorldApplySinglePredicate.WorldApplyTileTest test) {
      Objects.requireNonNull(test);
      this.tileX = tileX;
      this.tileY = tileY;
      this.dir = dir;
      this.test = test;
   }

   @Override
   public boolean canApply(WorldPreset preset, LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, int tileX, int tileY) {
      return this.test.canApply(preset, generatorStack, tileX + this.tileX, tileY + this.tileY, this.dir);
   }

   @Override
   public WorldApplyPredicate mirrorX(int width) {
      int newDir = this.dir != 1 && this.dir != 3 ? this.dir : (this.dir + 2) % 4;
      return new WorldApplySinglePredicate(PresetUtils.getMirroredValue(this.tileX, width), this.tileY, newDir, this.test);
   }

   @Override
   public WorldApplyPredicate mirrorY(int height) {
      int newDir = this.dir != 0 && this.dir != 2 ? this.dir : (this.dir + 2) % 4;
      return new WorldApplySinglePredicate(this.tileX, PresetUtils.getMirroredValue(this.tileY, height), newDir, this.test);
   }

   @Override
   public WorldApplyPredicate rotate(PresetRotation angle, int width, int height) {
      int newDir = (this.dir + (angle == null ? 0 : angle.dirOffset)) % 4;
      Point pos = PresetUtils.getRotatedPointInSpace(this.tileX, this.tileY, width, height, angle);
      return new WorldApplySinglePredicate(pos.x, pos.y, newDir, this.test);
   }

   @FunctionalInterface
   public interface WorldApplyTileTest {
      boolean canApply(WorldPreset var1, BiomeGeneratorStack var2, int var3, int var4, int var5);
   }
}
