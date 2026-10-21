package necesse.level.gameTile;

import java.awt.Color;
import java.awt.Point;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;

public class SandGravelTile extends TerrainSplatterTile {
   private final GameRandom drawRandom;

   public SandGravelTile() {
      super(false, "sandgravel");
      this.mapColor = new Color(204, 154, 92);
      this.canBeMined = true;
      this.drawRandom = new GameRandom();
   }

   @Override
   public LootTable getLootTable(Level level, int tileX, int tileY) {
      return new LootTable(new LootItem("stone").preventLootMultiplier());
   }

   @Override
   public Point getTerrainSprite(GameTextureSection terrainTexture, Level level, int tileX, int tileY) {
      int tile;
      synchronized (this.drawRandom) {
         tile = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(terrainTexture.getHeight() / 32);
      }

      return new Point(0, tile);
   }

   @Override
   public int getDestroyedTile() {
      return TileRegistry.sandID;
   }

   @Override
   public boolean canBePlacedOn(Level level, int tileX, int tileY, GameTile placing) {
      return false;
   }

   @Override
   public int getTerrainPriority() {
      return 300;
   }
}
