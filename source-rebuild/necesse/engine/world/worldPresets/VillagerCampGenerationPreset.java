package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.VillagerCampPreset;
import necesse.level.maps.presets.set.BannerSet;
import necesse.level.maps.presets.set.CropSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.RockAndOreSet;
import necesse.level.maps.presets.set.TreeSet;

public class VillagerCampGenerationPreset extends SimpleGenerationPreset {
   protected Biome biome;
   protected LevelIdentifier levelIdentifier;
   public final TreeSet[] trees;
   public final CropSet[] crops;
   public final FurnitureSet[] furniture;
   public final RockAndOreSet[] rocks;
   public final BannerSet[] banner;

   public VillagerCampGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, false, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.trees = TreeSet.getReducedSetForBiome(
         new TreeSet[]{TreeSet.pine, TreeSet.birch, TreeSet.maple, TreeSet.palm, TreeSet.oak, TreeSet.spruce}, biome, levelIdentifier
      );
      this.crops = CropSet.getReducedSetForBiome(
         new CropSet[]{
            CropSet.cabbage,
            CropSet.corn,
            CropSet.onion,
            CropSet.strawberry,
            CropSet.tomato,
            CropSet.chilipepper,
            CropSet.sugarbeet,
            CropSet.rice,
            CropSet.potato,
            CropSet.wheat,
            CropSet.beet,
            CropSet.pumpkin,
            CropSet.eggplant,
            CropSet.carrot
         },
         biome,
         levelIdentifier
      );
      this.furniture = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{FurnitureSet.oak, FurnitureSet.maple, FurnitureSet.deadwood, FurnitureSet.dryad}, biome, levelIdentifier
      );
      this.rocks = RockAndOreSet.getReducedSetForBiome(
         new RockAndOreSet[]{RockAndOreSet.forest, RockAndOreSet.snow, RockAndOreSet.plains, RockAndOreSet.swamp, RockAndOreSet.desert}, biome, levelIdentifier
      );
      this.banner = BannerSet.getReducedSetForBiome(
         new BannerSet[]{BannerSet.eggcellent, BannerSet.fishian, BannerSet.frost, BannerSet.dryad}, biome, levelIdentifier
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
      return new VillagerCampPreset(
         random,
         this.biome,
         random.getOneOf(this.trees),
         random.getOneOf(this.crops),
         random.getOneOf(this.furniture),
         random.getOneOf(this.rocks),
         random.getOneOf(this.banner)
      );
   }
}
