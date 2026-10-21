package necesse.level.gameTile;

import java.awt.Color;
import java.awt.Point;
import necesse.engine.util.GameRandom;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;

public class RavenFloorTile extends TerrainSplatterTile {
   private final GameRandom drawRandom;

   public RavenFloorTile() {
      super(true, "ravenfloor");
      this.mapColor = new Color(137, 79, 180);
      this.canBeMined = true;
      this.drawRandom = new GameRandom();
      this.isOrganic = true;
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
   public int getTerrainPriority() {
      return 0;
   }
}
