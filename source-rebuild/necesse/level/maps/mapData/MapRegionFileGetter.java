package necesse.level.maps.mapData;

import java.io.File;
import necesse.engine.util.LevelIdentifier;

@FunctionalInterface
public interface MapRegionFileGetter {
   File getFile(LevelIdentifier var1, int var2, int var3);
}
