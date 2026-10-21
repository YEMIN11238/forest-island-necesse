package necesse.entity;

import necesse.level.maps.regionSystem.Region;

public interface LoadingCompleteInterface {
   void onLoadingComplete();

   void onUnloading(Region var1);
}
