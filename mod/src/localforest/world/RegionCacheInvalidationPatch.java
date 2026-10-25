package localforest.world;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.util.GameMath;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.RegionManager;
import necesse.level.maps.regionSystem.regionsStructure.RegionStructureDataMap;
import net.bytebuddy.asm.Advice;

/** The rebuilt 1.3.3 map removes the region but leaves its one-entry lookup
 * cache alive. A return to the same coordinate can otherwise receive a disposed
 * Region. RegionManager calls removal under the native entity lock. */
@ModMethodPatch(target=RegionStructureDataMap.class,name="removeRegion",arguments={int.class,int.class})
public final class RegionCacheInvalidationPatch {
    @Advice.OnMethodExit
    public static void after(@Advice.Argument(0) int x,@Advice.Argument(1) int y,
        @Advice.FieldValue("manager") RegionManager manager,
        @Advice.FieldValue(value="lastRegionKey",readOnly=false) long lastKey,
        @Advice.FieldValue(value="lastRegion",readOnly=false) Region lastRegion,
        @Advice.FieldValue(value="lastRegionLoadIfNotLoaded",readOnly=false) boolean lastLoad){
        if(IslandWorld.enabled()&&IslandWorld.isIsland(manager.level)&&lastKey==GameMath.getUniqueLongKey(x,y)){
            lastKey=Long.MIN_VALUE;lastRegion=null;lastLoad=false;RegionDiagnostics.invalidatedRegionCache();
        }
    }
    private RegionCacheInvalidationPatch(){}
}
