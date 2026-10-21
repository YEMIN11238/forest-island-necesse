package necesse.entity.manager;

import necesse.level.maps.regionSystem.Region;

public interface RegionLoadedListenerEntityComponent extends EntityComponent {
   void onRegionLoaded(Region var1);
}
