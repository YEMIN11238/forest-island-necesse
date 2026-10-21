package necesse.engine.world.worldPresets;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.SurfaceBelowSurfacePreset;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.GroundSet;
import necesse.level.maps.presets.set.RockAndOreSet;
import necesse.level.maps.presets.set.TreeSet;

public class SurfaceBelowSurfaceGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final FurnitureSet[] bench;
   public final RockAndOreSet[] walls;
   public final GroundSet[] ground;
   public final TreeSet[] trees;

   public SurfaceBelowSurfaceGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, true, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.bench = FurnitureSet.getReducedSetForBiome(new FurnitureSet[]{FurnitureSet.oak, FurnitureSet.maple, FurnitureSet.dungeon}, biome, levelIdentifier);
      this.walls = RockAndOreSet.getReducedSetForBiome(
         new RockAndOreSet[]{
            RockAndOreSet.deepdesertcave,
            RockAndOreSet.swampcave,
            RockAndOreSet.forestcave,
            RockAndOreSet.deepplainscave,
            RockAndOreSet.desertcave,
            RockAndOreSet.deepforestcave,
            RockAndOreSet.snowcave,
            RockAndOreSet.deepswampcave,
            RockAndOreSet.plainscave,
            RockAndOreSet.deepsnowcave
         },
         biome,
         levelIdentifier
      );
      this.ground = GroundSet.getReducedSetForBiome(
         new GroundSet[]{GroundSet.forest, GroundSet.snow, GroundSet.plains, GroundSet.swamp, GroundSet.desert}, biome, levelIdentifier
      );
      this.trees = TreeSet.getReducedSetForBiome(
         new TreeSet[]{TreeSet.willow, TreeSet.pine, TreeSet.palm, TreeSet.birch, TreeSet.maple, TreeSet.oak, TreeSet.spruce}, biome, levelIdentifier
      );
   }

   @Override
   public void setupTester(WorldPresetTester tester) {
      tester.addApplyPredicate(new WorldApplyAreaPredicate(0, 0, tester.width - 1, tester.height - 1, 0, new WorldApplyAreaPredicate.WorldApplyCornerTest() {
         @Override
         public boolean isValidTile(WorldPreset preset, LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, int tileX, int tileY) {
            if (presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER)) {
               return !generatorStack.isSurfaceOceanOrRiver(tileX, tileY);
            } else if (presetsRegion.identifier.equals(LevelIdentifier.CAVE_IDENTIFIER)) {
               return !generatorStack.isCaveRiverOrLava(tileX, tileY);
            } else {
               return presetsRegion.identifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER) ? !generatorStack.isDeepCaveRiverOrLava(tileX, tileY) : false;
            }
         }
      }));
   }

   @Override
   public Preset getPreset(GameRandom random) {
      RockAndOreSet chosenWalls = random.getOneOf(this.walls);
      return new SurfaceBelowSurfacePreset(
            this.biome, this.levelIdentifier, random, random.getOneOf(this.bench), chosenWalls, random.getOneOf(this.ground), random.getOneOf(this.trees)
         )
         .randomlyReplaceObjects(ObjectRegistry.getObjectID("woodcolumn"), () -> random.getOneOf(ObjectRegistry.getObjectID("air"), chosenWalls.rock));
   }
}
