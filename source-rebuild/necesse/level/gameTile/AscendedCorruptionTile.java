package necesse.level.gameTile;

import java.awt.Color;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Attacker;
import necesse.level.maps.Level;

public class AscendedCorruptionTile extends SimpleTerrainTile {
   public AscendedCorruptionTile() {
      super("ascendedcorruption", new Color(125, 30, 175));
      this.canBeMined = true;
   }

   @Override
   public boolean onDamaged(Level level, int x, int y, int damage, Attacker attacker, ServerClient client, boolean showEffect, int mouseX, int mouseY) {
      return !level.tileLayer.isPlayerPlaced(x, y) ? false : super.onDamaged(level, x, y, damage, attacker, client, showEffect, mouseX, mouseY);
   }
}
