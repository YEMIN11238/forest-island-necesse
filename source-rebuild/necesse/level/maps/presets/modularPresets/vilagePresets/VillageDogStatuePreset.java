package necesse.level.maps.presets.modularPresets.vilagePresets;

import java.awt.Point;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.level.maps.generationModules.ModularGeneration;

public class VillageDogStatuePreset extends VillagePreset {
   public static String[] dogStatueVariants = new String[]{"freyastatue", "zithastatue"};

   public VillageDogStatuePreset(GameRandom random) {
      super(3, 2, false, random);
      this.applyScript(
         "PRESET = {\n\twidth = 9,\n\theight = 6,\n\ttileIDs = [83, graveltile, 11, stonepathtile],\n\ttiles = [-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 11, 11, 11, 11, -1, -1, -1, -1, -1, 11, 83, 83, 11, -1, -1, -1, -1, -1, 11, 83, 83, 11, -1, -1, -1, -1, -1, 11, 11, 11, 11, -1, -1, -1, -1, -1, -1, 83, 83, -1, -1, -1, -1],\n\tobjectIDs = [0, air, 514, steppingstones, 515, largesteppingstone, 547, sprucebench, 548, sprucebench2, 411, freyastatue, 412, freyastatue2, 413, freyastatue3, 414, freyastatue4],\n\tobjects = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 515, 548, 547, 514, 0, 0, 0, 0, 0, 547, 411, 412, 548, 0, 0, 0, 0, 0, 548, 413, 414, 547, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],\n\trotations = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 3, 3, 0, 0, 0, 0, 0, 2, 2, 2, 0, 0, 0, 0, 0, 0, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0],\n\ttileObjectsClear = true,\n\twallDecorObjectsClear = true,\n\ttableDecorObjectsClear = true,\n\tclearOtherWires = false\n}"
      );
      this.open(1, 1, 2);
      String dogStatueVariant = dogStatueVariants[random.getIntBetween(0, dogStatueVariants.length - 1)];
      this.setObject(3, 2, ObjectRegistry.getObjectID(dogStatueVariant));
      this.setObject(4, 2, ObjectRegistry.getObjectID(dogStatueVariant + "2"));
      this.setObject(3, 3, ObjectRegistry.getObjectID(dogStatueVariant + "3"));
      this.setObject(4, 3, ObjectRegistry.getObjectID(dogStatueVariant + "4"));
   }

   @Override
   public boolean canApplyToModularGeneration(ModularGeneration modularGeneration, Point cell, boolean xMirror, boolean yMirror, Point fromCell) {
      return !xMirror && !yMirror;
   }
}
