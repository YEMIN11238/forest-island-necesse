package necesse.level.maps.hudManager;

import java.awt.Point;
import java.awt.Rectangle;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.client.Client;
import necesse.level.maps.mapData.ClientDiscoveredMap;

public interface HudDrawOnMap {
   default boolean shouldDrawOnMap(Client client, ClientDiscoveredMap map) {
      return true;
   }

   Rectangle getMapLevelDrawBounds();

   Point getMapLevelPos();

   void drawOnMap(TickManager var1, Client var2, int var3, int var4, double var5, Rectangle var7, boolean var8, int var9, int var10);
}
