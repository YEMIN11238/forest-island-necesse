package necesse.level.maps.presets.set;

import necesse.engine.registries.ObjectRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class CarpetSet extends PresetSet<CarpetSet> {
   public static final CarpetSet wool = new CarpetSet("woolcarpet").all();
   public static final CarpetSet leather = new CarpetSet("leathercarpet").all();
   public static final CarpetSet brownbear = new CarpetSet("brownbearcarpet").all();
   public static final CarpetSet blue = new CarpetSet("bluecarpet").all();
   public static final CarpetSet goldgrid = new CarpetSet("goldgridcarpet").all();
   public static final CarpetSet green = new CarpetSet("greencarpet").all();
   public static final CarpetSet steelgrey = new CarpetSet("steelgreycarpet").all();
   public static final CarpetSet purple = new CarpetSet("purplecarpet").all();
   public static final CarpetSet velour = new CarpetSet("velourcarpet").all();
   public static final CarpetSet heart = new CarpetSet("heartcarpet").all();
   public static final CarpetSet redyarn = new CarpetSet("redyarncarpet").all();
   public final int carpet;

   protected CarpetSet(String carpet) {
      this.objectArrays = new int[][]{{this.carpet = ObjectRegistry.getObjectID(carpet)}};
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(CarpetSet.class);
   }
}
