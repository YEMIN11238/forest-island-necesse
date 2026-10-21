package necesse.level.maps.regionSystem.layers.interfaces;

import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public interface SaveDataRegionLayer {
   void addSaveData(SaveData var1);

   void loadSaveData(LoadData var1);
}
