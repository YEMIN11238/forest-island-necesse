package necesse.level.maps.regionSystem.layers.interfaces;

import java.util.List;
import necesse.level.maps.levelData.jobs.TileLevelJob;

public interface LevelJobsSubmitterRegionLayer {
   List<TileLevelJob> getLevelJobsByRegion(int var1, int var2);
}
