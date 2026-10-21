package necesse.level.maps.presets.set;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class FenceSet extends PresetSet<FenceSet> {
   public static final FenceSet wood = new FenceSet("wood").all();
   public static final FenceSet stone = new FenceSet("stone").all();
   public static final FenceSet iron = new FenceSet("iron").all();
   public static final FenceSet crypt = new FenceSet("crypt").incursion(BiomeRegistry.GRAVEYARD);
   public final int fence;
   public final int gateClosed;
   public final int gateOpen;

   public FenceSet(String prefixStringID) {
      this.objectArrays = new int[][]{
         {this.fence = ObjectRegistry.getObjectID(prefixStringID + "fence")},
         {this.gateClosed = ObjectRegistry.getObjectID(prefixStringID + "fencegate")},
         {this.gateOpen = ObjectRegistry.getObjectID(prefixStringID + "fencegateopen")}
      };
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(FenceSet.class);
   }
}
