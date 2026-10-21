package necesse.level.maps.presets.set;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class FlowerSet extends PresetSet<FlowerSet> {
   public static final FlowerSet iceblossom = new FlowerSet("iceblossom").surface(BiomeRegistry.SNOW).cave(BiomeRegistry.SNOW).deepCave(BiomeRegistry.SNOW);
   public static final FlowerSet sunflower = new FlowerSet("sunflower")
      .surface(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP);
   public static final FlowerSet firemone = new FlowerSet("firemone")
      .surface(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP);
   public static final FlowerSet mushroom = new FlowerSet("mushroom")
      .surface(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP);
   public static final FlowerSet caveglow = new FlowerSet("caveglow")
      .cave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.PLAINS, BiomeRegistry.FOREST, BiomeRegistry.SWAMP);
   public final int flower;
   public final int wildflower;

   public FlowerSet(String objectID) {
      this.objectArrays = new int[][]{{this.flower = ObjectRegistry.getObjectID(objectID)}, {this.wildflower = ObjectRegistry.getObjectID("wild" + objectID)}};
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(FlowerSet.class);
   }
}
