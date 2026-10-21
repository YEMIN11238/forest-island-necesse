package necesse.entity.objectEntity;

import java.awt.Rectangle;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.ComputedFunction;
import necesse.engine.util.LevelIdentifier;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.level.maps.Level;

public class LadderUpObjectEntity extends PortalObjectEntity {
   private final int ladderDownID;
   private final GameSprite mapSprite;

   public LadderUpObjectEntity(String type, Level level, int x, int y, LevelIdentifier destination, int ladderDownID, GameSprite mapSprite) {
      super(level, type, x, y, destination, x, y);
      this.ladderDownID = ladderDownID;
      this.mapSprite = mapSprite;
      this.saveDestination = false;
   }

   @Override
   public void use(Server server, ServerClient client) {
      ComputedFunction<Level, GameMessage> isBlockingExit = new ComputedFunction<>(
         levelx -> {
            levelx.regionManager.ensureTilesAreLoaded(this.destinationTileX, this.destinationTileY, this.destinationTileX, this.destinationTileY);
            return levelx.getObjectID(this.destinationTileX, this.destinationTileY) != this.ladderDownID
               ? levelx.preventsLadderPlacement(this.destinationTileX, this.destinationTileY)
               : null;
         }
      );
      if (server.world.levelManager.isLoaded(this.getDestinationIdentifier())) {
         Level level = server.world.getLevel(this.getDestinationIdentifier());
         GameMessage error = isBlockingExit.get(level);
         if (error != null) {
            client.sendChatMessage(error);
            return;
         }
      }

      this.teleportClientToAroundDestination(client, levelx -> {
         if (!isBlockingExit.isComputed()) {
            GameMessage errorx = isBlockingExit.get(levelx);
            if (errorx != null) {
               client.sendChatMessage(errorx);
               return false;
            }
         }

         levelx.regionManager.ensureTileIsLoaded(this.destinationTileX, this.destinationTileY);
         if (levelx.getObjectID(this.destinationTileX, this.destinationTileY) != this.ladderDownID) {
            LadderDownObjectEntity.clearAndPlaceLadder(server, levelx, this.destinationTileX, this.destinationTileY, this.ladderDownID, true);
         }

         client.newStats.ladders_used.increment(1);
         this.runClearMobs(levelx, this.destinationTileX, this.destinationTileY);
         return true;
      }, true);
   }

   @Override
   public boolean shouldDrawOnMap() {
      return true;
   }

   @Override
   public Rectangle drawOnMapBox(double tileScale, boolean isMinimap) {
      return new Rectangle(-12, -12, 24, 24);
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      this.mapSprite.initDraw().size(24, 24).draw(x - 12, y - 12);
   }

   @Override
   public GameTooltips getMapTooltips() {
      return new StringTooltips(this.getObject().getDisplayName());
   }
}
