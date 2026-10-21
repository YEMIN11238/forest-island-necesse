package necesse.level.maps.presets.set;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class BushSet extends PresetSet<BushSet> {
   public static final BushSet blueberry = new BushSet("blueberry").surface(BiomeRegistry.FOREST);
   public static final BushSet raspberry = new BushSet("raspberry").surface(BiomeRegistry.PLAINS);
   public static final BushSet blackberry = new BushSet("blackberry").surface(BiomeRegistry.SNOW);
   public final int bush;
   public final int sapling;

   public BushSet(String prefixStringID) {
      this.objectArrays = new int[][]{
         {this.bush = ObjectRegistry.getObjectID(prefixStringID + "bush")}, {this.sapling = ObjectRegistry.getObjectID(prefixStringID + "sapling")}
      };
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(BushSet.class);
   }
}
