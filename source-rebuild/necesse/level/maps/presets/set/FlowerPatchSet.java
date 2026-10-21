package necesse.level.maps.presets.set;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class FlowerPatchSet extends PresetSet<FlowerPatchSet> {
   public static final FlowerPatchSet blue = new FlowerPatchSet("blue").surface(BiomeRegistry.SNOW).cave(BiomeRegistry.SNOW).deepCave(BiomeRegistry.SNOW);
   public static final FlowerPatchSet purple = new FlowerPatchSet("purple")
      .surface(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP);
   public static final FlowerPatchSet red = new FlowerPatchSet("red")
      .surface(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP);
   public static final FlowerPatchSet white = new FlowerPatchSet("white")
      .surface(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP);
   public static final FlowerPatchSet yellow = new FlowerPatchSet("yellow")
      .surface(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP);
   public final int wildFlower;

   public FlowerPatchSet(String prefix) {
      this.objectArrays = new int[][]{{this.wildFlower = ObjectRegistry.getObjectID(prefix + "flowerpatch")}};
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(FlowerPatchSet.class);
   }
}
