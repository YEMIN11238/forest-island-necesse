package necesse.entity.manager;

import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

public interface WorldRegionLoadedEntityComponent extends EntityComponent {
   void onLevelRegionLoaded(Level var1, Region var2);
}
