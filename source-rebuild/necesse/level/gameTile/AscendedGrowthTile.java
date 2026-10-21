package necesse.level.gameTile;

import java.awt.Color;
import java.awt.Point;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Attacker;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;

public class AscendedGrowthTile extends TerrainSplatterTile {
   public AscendedGrowthTile() {
      super(false, "ascendedgrowth", "splattingmaskwide");
      this.mapColor = new Color(104, 60, 157);
      this.canBeMined = true;
   }

   @Override
   public Point getTerrainSprite(GameTextureSection terrainTexture, Level level, int tileX, int tileY) {
      return new Point(Math.floorMod(tileX, 4), Math.floorMod(tileY, 4));
   }

   @Override
   public int getTerrainPriority() {
      return 100;
   }

   @Override
   public boolean onDamaged(Level level, int x, int y, int damage, Attacker attacker, ServerClient client, boolean showEffect, int mouseX, int mouseY) {
      return !level.tileLayer.isPlayerPlaced(x, y) ? false : super.onDamaged(level, x, y, damage, attacker, client, showEffect, mouseX, mouseY);
   }
}
