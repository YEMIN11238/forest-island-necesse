package necesse.entity.manager;

import necesse.engine.events.loot.ObjectLootTableDropsEvent;

public interface ObjectLootTableDroppedListenerEntityComponent extends EntityComponent {
   void onObjectLootTableDropped(ObjectLootTableDropsEvent var1);
}
