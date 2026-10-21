package necesse.level.maps.presets.set;

import necesse.engine.registries.ObjectRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class LargelPaintingSet extends PresetSet<LargelPaintingSet> {
   public static final LargelPaintingSet rareWorldMap = new LargelPaintingSet("paintinglargeworldmap");
   public static final LargelPaintingSet rareShip = new LargelPaintingSet("paintinglargeship");
   public static final LargelPaintingSet rareCastle = new LargelPaintingSet("paintinglargecastle");
   public static final LargelPaintingSet rareAbstract = new LargelPaintingSet("paintinglargeabstract");
   public final int painting;
   public final int paintingR;

   protected LargelPaintingSet(String painting) {
      this.objectArrays = new int[][]{{this.painting = ObjectRegistry.getObjectID(painting)}, {this.paintingR = this.painting + 1}};
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(LargelPaintingSet.class);
   }
}
