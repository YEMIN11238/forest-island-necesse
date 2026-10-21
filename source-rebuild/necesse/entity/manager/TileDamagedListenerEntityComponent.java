package necesse.entity.manager;

import necesse.engine.network.server.ServerClient;
import necesse.entity.TileDamageResult;
import necesse.level.gameTile.GameTile;

public interface TileDamagedListenerEntityComponent extends EntityComponent {
   void onTileDamaged(GameTile var1, int var2, int var3, ServerClient var4, TileDamageResult var5);
}
