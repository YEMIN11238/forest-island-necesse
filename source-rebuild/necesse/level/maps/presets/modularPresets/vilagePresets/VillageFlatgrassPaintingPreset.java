package necesse.level.maps.presets.modularPresets.vilagePresets;

import necesse.engine.util.GameRandom;

public class VillageFlatgrassPaintingPreset extends VillagePreset {
   public VillageFlatgrassPaintingPreset(GameRandom random) {
      super(3, 4, false, random);
      this.applyScript(
         "PRESET = {\n\twidth = 9,\n\theight = 12,\n\ttileIDs = [8, stonefloor, 11, stonepathtile],\n\ttiles = [-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 8, 8, 8, 8, 8, 8, -1, -1, 8, 8, 8, 8, 8, 8, 8, -1, -1, 8, 8, 8, 8, 8, 8, 8, -1, -1, 8, 8, 8, 8, 8, 8, 8, -1, -1, 8, 8, 8, 8, 8, 8, 8, -1, -1, 8, 8, 8, 8, 8, 8, 8, 11, -1, 8, 8, 8, 8, 8, 8, 8, 11, -1, 8, 8, 8, 8, 8, 8, 8, 11, -1, -1, 8, 8, 8, 8, 8, 8, -1, -1, -1, -1, 8, 8, 8, 8, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1],\n\tobjectIDs = [0, air, 371, paintinglargeflatgrass, 275, jailfence, 372, paintinglargeflatgrass2, 326, walllantern, 1319, ladderdown, 121, stonewall, 122, stonedoor],\n\tobjects = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 121, 121, 121, 121, 121, 121, 0, 0, 121, 121, 0, 372, 371, 0, 121, 0, 0, 121, 0, 0, 0, 0, 0, 121, 0, 0, 121, 275, 275, 275, 275, 275, 121, 0, 0, 121, 0, 0, 0, 0, 0, 121, 0, 0, 121, 326, 0, 0, 0, 0, 121, 0, 0, 121, 0, 0, 1319, 0, 0, 122, 0, 0, 121, 121, 0, 0, 0, 0, 121, 0, 0, 0, 121, 121, 0, 326, 0, 121, 0, 0, 0, 0, 121, 121, 121, 121, 121, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],\n\trotations = [2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 3, 3, 3, 3, 3, 2, 2, 0, 0, 1, 2, 2, 2, 3, 2, 2, 3, 2, 2, 2, 2, 2, 3, 2, 2, 3, 2, 2, 2, 2, 2, 3, 2, 2, 3, 2, 2, 2, 2, 2, 3, 2, 2, 3, 1, 2, 2, 2, 2, 3, 2, 2, 3, 2, 2, 1, 2, 2, 1, 2, 2, 3, 3, 2, 2, 2, 2, 0, 2, 2, 2, 3, 3, 2, 0, 2, 1, 2, 2, 2, 2, 3, 3, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2],\n\ttileObjectsClear = true,\n\twallDecorObjectsClear = true,\n\ttableDecorObjectsClear = true,\n\tclearOtherWires = false\n}"
      );
      this.open(2, 2, 1);
      this.addHumanMob(4, 7, "minerhuman");
      this.addHumanMob(6, 7, "guardhuman");
   }
}
