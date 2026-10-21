package necesse.level.maps.presets.set;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class HedgeSet extends PresetSet<HedgeSet> {
   public static final HedgeSet forest = new HedgeSet("forest").surface(BiomeRegistry.FOREST);
   public static final HedgeSet snow = new HedgeSet("snow").surface(BiomeRegistry.SNOW);
   public static final HedgeSet plains = new HedgeSet("plains").surface(BiomeRegistry.PLAINS);
   public static final HedgeSet swamp = new HedgeSet("swamp").surface(BiomeRegistry.SWAMP);
   public final int hedge;
   public final int hedgegate;

   public HedgeSet(String prefixStringID) {
      this.objectArrays = new int[][]{
         {this.hedge = ObjectRegistry.getObjectID(prefixStringID + "hedge")}, {this.hedgegate = ObjectRegistry.getObjectID(prefixStringID + "hedgegate")}
      };
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(HedgeSet.class);
   }
}
