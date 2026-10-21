package necesse.entity.manager;

import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

public interface WorldRegionUnloadedEntityComponent extends EntityComponent {
   void onLevelRegionUnloaded(Level var1, Region var2);
}
