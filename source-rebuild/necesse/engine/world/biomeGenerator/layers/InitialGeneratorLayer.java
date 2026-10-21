package necesse.engine.world.biomeGenerator.layers;

import necesse.engine.util.GameRandom;

public class InitialGeneratorLayer extends GeneratorLayer {
   protected int waterTileID;
   protected int[] biomeTileIDs;
   protected float chance;

   public InitialGeneratorLayer(int layerSeed, float chance, int waterTileID, int[] biomeTileIDs) {
      super(layerSeed, 0);
      this.chance = chance;
      this.waterTileID = waterTileID;
      this.biomeTileIDs = biomeTileIDs;
   }

   @Override
   protected int sample(int x, int y) {
      GameRandom random = this.getRandom(x, y);
      return random.getChance(this.chance) ? this.biomeTileIDs[random.nextInt(this.biomeTileIDs.length)] : this.waterTileID;
   }
}
