package necesse.level.maps.presets.set;

import java.util.ArrayList;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;
import necesse.level.maps.presets.Preset;

public class CropSet extends PresetSet<CropSet> {
   public static final CropSet wheat = new CropSet("wheatseed", "wheat")
      .surface(BiomeRegistry.FOREST)
      .cave(BiomeRegistry.FOREST)
      .deepCave(BiomeRegistry.FOREST);
   public static final CropSet corn = new CropSet("cornseed", "corn").surface(BiomeRegistry.FOREST).cave(BiomeRegistry.FOREST).deepCave(BiomeRegistry.FOREST);
   public static final CropSet tomato = new CropSet("tomatoseed", "tomato")
      .surface(BiomeRegistry.FOREST, BiomeRegistry.SNOW)
      .cave(BiomeRegistry.FOREST, BiomeRegistry.SNOW)
      .deepCave(BiomeRegistry.FOREST, BiomeRegistry.SNOW);
   public static final CropSet cabbage = new CropSet("cabbageseed", "cabbage")
      .surface(BiomeRegistry.FOREST, BiomeRegistry.SNOW)
      .cave(BiomeRegistry.FOREST, BiomeRegistry.SNOW)
      .deepCave(BiomeRegistry.FOREST, BiomeRegistry.SNOW);
   public static final CropSet chilipepper = new CropSet("chilipepperseed", "chilipepper")
      .surface(BiomeRegistry.SNOW, BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.SNOW, BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.SNOW, BiomeRegistry.SWAMP);
   public static final CropSet sugarbeet = new CropSet("sugarbeetseed", "sugarbeet")
      .surface(BiomeRegistry.SNOW, BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.SNOW, BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.SNOW, BiomeRegistry.SWAMP);
   public static final CropSet beet = new CropSet("beetseed", "beet");
   public static final CropSet eggplant = new CropSet("eggplantseed", "eggplant")
      .surface(BiomeRegistry.SWAMP, BiomeRegistry.DESERT)
      .cave(BiomeRegistry.SWAMP, BiomeRegistry.DESERT)
      .deepCave(BiomeRegistry.SWAMP, BiomeRegistry.DESERT);
   public static final CropSet potato = new CropSet("potatoseed", "potato")
      .surface(BiomeRegistry.SWAMP, BiomeRegistry.DESERT)
      .cave(BiomeRegistry.SWAMP, BiomeRegistry.DESERT)
      .deepCave(BiomeRegistry.SWAMP, BiomeRegistry.DESERT);
   public static final CropSet rice = new CropSet("riceseed", "riceseed")
      .surface(BiomeRegistry.DESERT)
      .cave(BiomeRegistry.DESERT)
      .deepCave(BiomeRegistry.DESERT);
   public static final CropSet carrot = new CropSet("carrotseed", "carrot")
      .surface(BiomeRegistry.DESERT)
      .cave(BiomeRegistry.DESERT)
      .deepCave(BiomeRegistry.DESERT);
   public static final CropSet onion = new CropSet("onionseed", "onion").deepCave(BiomeRegistry.FOREST);
   public static final CropSet pumpkin = new CropSet("pumpkinseed", "pumpkin").deepCave(BiomeRegistry.FOREST, BiomeRegistry.SNOW, BiomeRegistry.SWAMP);
   public static final CropSet strawberry = new CropSet("strawberryseed", "strawberry").deepCave(BiomeRegistry.SNOW, BiomeRegistry.SWAMP, BiomeRegistry.DESERT);
   public static final CropSet coffee = new CropSet("coffeebeans", "coffeebeans").deepCave(BiomeRegistry.DESERT);
   public final int[] seedIDs;
   public final int productID;

   public CropSet(String seedPrefixStringID, String productStringID) {
      this.objectArrays = new int[][]{this.seedIDs = loadSeeds(seedPrefixStringID), {this.productID = ItemRegistry.getItemID(productStringID)}};
   }

   private static int[] loadSeeds(String seedPrefixStringID) {
      ArrayList<Integer> seedIDs = new ArrayList<>();

      for (int stage = 0; stage < 1000; stage++) {
         String seedStringID = seedPrefixStringID + (stage == 0 ? "" : stage);
         int seedID = ObjectRegistry.getObjectID(seedStringID);
         if (seedID == -1) {
            break;
         }

         seedIDs.add(seedID);
      }

      return seedIDs.stream().mapToInt(i -> i).toArray();
   }

   public <C extends Preset> C replaceWith(CropSet replacementPresetSet, C preset) {
      return this.replacePreset(replacementPresetSet, preset, null);
   }

   public <C extends Preset> C replacePreset(CropSet presetSet, C preset, GameRandom stageRandomizer) {
      if (presetSet == this && stageRandomizer == null) {
         return preset;
      }

      if (presetSet.seedIDs.length == 0) {
         return preset;
      }

      if (this.seedIDs.length == 0) {
         return preset;
      }

      if (stageRandomizer != null) {
         int[] objects = preset.objects[0];

         for (int i = 0; i < objects.length; i++) {
            int selectedSeed = this.seedIDs[stageRandomizer.nextInt(this.seedIDs.length)];
            if (objects[i] == presetSet.seedIDs[0]) {
               objects[i] = selectedSeed;
            }
         }
      } else {
         int myMax = this.seedIDs.length;
         int toReplaceMax = presetSet.seedIDs.length;

         for (int i = 0; i < toReplaceMax; i++) {
            preset.replaceNonEmptyObjects(presetSet.seedIDs[i], this.seedIDs[Math.min(i, myMax - 1)]);
         }

         preset.replaceNonEmptyObjects(presetSet.seedIDs[0], this.seedIDs[0]);
      }

      return preset;
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(CropSet.class);
   }
}
