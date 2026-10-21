package necesse.level.gameTile;

import java.awt.Color;
import java.awt.Point;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;

public class FactoryFloorTile extends TerrainSplatterTile {
   public FactoryFloorTile() {
      super(false, "factoryfloortile", "splattingmaskwide");
      this.mapColor = new Color(16, 77, 78);
      this.canBeMined = true;
      this.lightLevel = 30;
   }

   @Override
   public Point getTerrainSprite(GameTextureSection terrainTexture, Level level, int tileX, int tileY) {
      return new Point(Math.floorMod(tileX, 3), Math.floorMod(tileY, 3));
   }

   @Override
   public int getTerrainPriority() {
      return 100;
   }
}
