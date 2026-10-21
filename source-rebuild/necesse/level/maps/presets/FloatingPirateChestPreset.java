package necesse.level.maps.presets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.inventory.lootTable.presets.FloatingPirateChestLootTable;
import necesse.level.maps.biomes.Biome;

public class FloatingPirateChestPreset extends Preset {
   public FloatingPirateChestPreset(Biome biome, LevelIdentifier levelIdentifier, GameRandom random) {
      super(
         "PRESET = {\n\twidth = 5,\n\theight = 5,\n\ttileIDs = [2, watertile],\n\ttiles = [2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2],\n\tobjectIDs = [0, air, 771, piratechest],\n\tobjects = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 771, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],\n\trotations = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],\n\ttileObjectsClear = true,\n\twallDecorObjectsClear = true,\n\ttableDecorObjectsClear = true,\n\tclearOtherWires = false\n}"
      );
      this.addInventory(FloatingPirateChestLootTable.instance, random, 2, 2);
   }
}
