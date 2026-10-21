package necesse.level.maps.presets.set;

import necesse.engine.registries.ObjectRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class PottedPlantSet extends PresetSet<PottedPlantSet> {
   public static final PottedPlantSet pottedFlower1 = new PottedPlantSet("pottedflower1").all();
   public static final PottedPlantSet pottedFlower2 = new PottedPlantSet("pottedflower2").all();
   public static final PottedPlantSet pottedFlower3 = new PottedPlantSet("pottedflower3").all();
   public static final PottedPlantSet pottedFlower4 = new PottedPlantSet("pottedflower4").all();
   public static final PottedPlantSet pottedFlower5 = new PottedPlantSet("pottedflower5").all();
   public static final PottedPlantSet pottedFlower6 = new PottedPlantSet("pottedflower6").all();
   public static final PottedPlantSet pottedCactus1 = new PottedPlantSet("pottedcactus1").all();
   public static final PottedPlantSet pottedCactus2 = new PottedPlantSet("pottedcactus2").all();
   public static final PottedPlantSet pottedCactus3 = new PottedPlantSet("pottedcactus3").all();
   public static final PottedPlantSet pottedCactus4 = new PottedPlantSet("pottedcactus4").all();
   public static final PottedPlantSet pottedPlant1 = new PottedPlantSet("pottedplant1").all();
   public static final PottedPlantSet pottedPlant2 = new PottedPlantSet("pottedplant2").all();
   public static final PottedPlantSet pottedPlant3 = new PottedPlantSet("pottedplant3").all();
   public static final PottedPlantSet pottedPlant4 = new PottedPlantSet("pottedplant4").all();
   public static final PottedPlantSet pottedPlant5 = new PottedPlantSet("pottedplant5").all();
   public static final PottedPlantSet pottedPlant6 = new PottedPlantSet("pottedplant6").all();
   public static final PottedPlantSet pottedPlant7 = new PottedPlantSet("pottedplant7").all();
   public final int pottedItem;

   public PottedPlantSet(String objectID) {
      this.objectArrays = new int[][]{{this.pottedItem = ObjectRegistry.getObjectID(objectID)}};
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(PottedPlantSet.class);
   }
}
