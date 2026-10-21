package necesse.entity.manager;

import necesse.engine.events.loot.TileLootTableDropsEvent;

public interface TileLootTableDroppedListenerEntityComponent extends EntityComponent {
   void onTileLootTableDropped(TileLootTableDropsEvent var1);
}
