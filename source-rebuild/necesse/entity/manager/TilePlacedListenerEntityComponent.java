package necesse.entity.manager;

import necesse.engine.network.server.ServerClient;
import necesse.level.gameTile.GameTile;

public interface TilePlacedListenerEntityComponent extends EntityComponent {
   void onTilePlaced(GameTile var1, int var2, int var3, ServerClient var4);
}
