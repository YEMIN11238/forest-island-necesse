package necesse.entity;

import java.awt.Point;
import java.awt.Rectangle;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.network.client.Client;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.level.maps.mapData.ClientDiscoveredMap;

public interface DrawOnMapEntity {
   default boolean shouldDrawOnMap() {
      return false;
   }

   boolean isVisibleOnMap(Client var1, ClientDiscoveredMap var2);

   default Rectangle drawOnMapBox(double tileScale, boolean isMinimap) {
      return new Rectangle(-8, -8, 16, 16);
   }

   Point getMapPos();

   default void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
   }

   default GameTooltips getMapTooltips() {
      return null;
   }

   default String getMapInteractTooltip() {
      return null;
   }

   default void onMapInteract(InputEvent event, PlayerMob perspective) {
   }
}
