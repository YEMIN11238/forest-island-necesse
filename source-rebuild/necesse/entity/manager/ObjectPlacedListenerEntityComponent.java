package necesse.entity.manager;

import necesse.engine.network.server.ServerClient;
import necesse.level.gameObject.GameObject;

public interface ObjectPlacedListenerEntityComponent extends EntityComponent {
   void onObjectPlaced(GameObject var1, int var2, int var3, int var4, ServerClient var5);
}
