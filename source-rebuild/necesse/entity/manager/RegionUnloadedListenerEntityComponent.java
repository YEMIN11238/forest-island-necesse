package necesse.entity.manager;

import necesse.level.maps.regionSystem.Region;

public interface RegionUnloadedListenerEntityComponent extends EntityComponent {
   void onRegionUnloaded(Region var1);
}
