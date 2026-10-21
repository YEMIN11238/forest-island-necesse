package necesse.level.maps.levelData;

import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.manager.EntityComponent;
import necesse.level.maps.regionSystem.Region;

public interface RegionLevelDataComponent extends EntityComponent {
   void addRegionSaveData(Region var1, SaveData var2);

   void loadRegionSaveData(Region var1, LoadData var2);

   void onUnloadedRegion(Region var1);
}
