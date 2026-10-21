package necesse.level.maps.presets.set;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class TreeSet extends PresetSet<TreeSet> {
   public static final TreeSet oak = new TreeSet("oak").surface(BiomeRegistry.FOREST).cave(BiomeRegistry.FOREST).deepCave(BiomeRegistry.FOREST);
   public static final TreeSet spruce = new TreeSet("spruce")
      .surface(BiomeRegistry.FOREST, BiomeRegistry.SNOW)
      .cave(BiomeRegistry.FOREST, BiomeRegistry.SNOW)
      .deepCave(BiomeRegistry.FOREST, BiomeRegistry.SNOW);
   public static final TreeSet pine = new TreeSet("pine").surface(BiomeRegistry.SNOW).cave(BiomeRegistry.SNOW).deepCave(BiomeRegistry.SNOW);
   public static final TreeSet palm = new TreeSet("palm").surface(BiomeRegistry.DESERT).cave(BiomeRegistry.DESERT).deepCave(BiomeRegistry.DESERT);
   public static final TreeSet willow = new TreeSet("willow").surface(BiomeRegistry.SWAMP).cave(BiomeRegistry.SWAMP).deepCave(BiomeRegistry.SWAMP);
   public static final TreeSet maple = new TreeSet("maple").surface(BiomeRegistry.PLAINS).cave(BiomeRegistry.PLAINS).deepCave(BiomeRegistry.PLAINS);
   public static final TreeSet birch = new TreeSet("birch").surface(BiomeRegistry.PLAINS).cave(BiomeRegistry.PLAINS).deepCave(BiomeRegistry.PLAINS);
   public static final TreeSet cactus = new TreeSet("cactus").surface(BiomeRegistry.DESERT).cave(BiomeRegistry.DESERT).deepCave(BiomeRegistry.DESERT);
   public static final TreeSet dryad = new TreeSet("dryad").deepCave(BiomeRegistry.PLAINS);
   public static final TreeSet deadwood = new TreeSet("deadwood");
   public final int tree;
   public final int treeStump;
   public final int sapling;

   public TreeSet(String prefixStringID) {
      this.objectArrays = new int[][]{
         {this.tree = ObjectRegistry.getObjectID(prefixStringID + "tree")},
         {this.treeStump = ObjectRegistry.getObjectID(prefixStringID + "treestump")},
         {this.sapling = ObjectRegistry.getObjectID(prefixStringID + "sapling")}
      };
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(TreeSet.class);
   }
}
