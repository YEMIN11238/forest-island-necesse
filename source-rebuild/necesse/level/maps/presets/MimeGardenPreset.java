package necesse.level.maps.presets;

import necesse.engine.util.GameRandom;

public class MimeGardenPreset extends Preset {
   public MimeGardenPreset(GameRandom random) {
      super(
         "PRESET = {\n\twidth = 9,\n\theight = 7,\n\ttileIDs = [40, grasstile],\n\ttiles = [-1, -1, 40, 40, 40, 40, 40, -1, -1, -1, 40, 40, 40, 40, 40, 40, 40, -1, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, -1, 40, 40, 40, 40, 40, 40, 40, -1, -1, -1, 40, 40, 40, 40, 40, -1, -1],\n\tobjectIDs = [0, air, 1296, whiteflowerpatch, 327, stonecandlepedestal, 1293, redflowerpatch],\n\tobjects = [0, 0, 1296, 1293, 1293, 1296, 1293, 0, 0, 0, 1296, 1293, 1296, 1296, 1293, 1296, 1293, 0, 1296, 1293, 1296, 1293, 1296, 1296, 1293, 1296, 1293, 1296, 1293, 1296, 1293, 327, 1296, 1293, 1296, 1293, 1296, 1293, 1296, 1293, 1293, 1296, 1293, 1296, 1293, 0, 1296, 1293, 1296, 1293, 1293, 1296, 1293, 0, 0, 0, 1296, 1293, 1296, 1296, 1293, 0, 0],\n\trotations = [0, 0, 3, 3, 3, 0, 3, 0, 0, 0, 3, 3, 0, 0, 0, 0, 0, 0, 3, 3, 0, 2, 0, 2, 0, 0, 0, 3, 2, 2, 2, 1, 2, 0, 0, 0, 3, 3, 1, 2, 2, 2, 0, 0, 0, 0, 1, 3, 1, 2, 2, 1, 1, 0, 0, 0, 2, 1, 2, 1, 1, 1, 0],\n\ttileObjectsClear = true,\n\twallDecorObjectsClear = true,\n\ttableDecorObjectsClear = true,\n\tclearOtherWires = false\n}"
      );
      this.addMob("mime", 4, 5, false);
   }
}
