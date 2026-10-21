package necesse.entity.manager;

import necesse.engine.util.LevelIdentifier;

public interface WorldLevelUnloadedEntityComponent extends EntityComponent {
   void onLevelUnloaded(LevelIdentifier var1);
}
