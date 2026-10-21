package necesse.level.maps.regionSystem;

import java.util.HashSet;

public class ConnectedSubRegionsResult {
   public final SubRegion base;
   public final HashSet<SubRegion> connectedRegions;
   public final int size;

   public ConnectedSubRegionsResult(SubRegion base, HashSet<SubRegion> connectedRegions, int size) {
      this.base = base;
      this.connectedRegions = connectedRegions;
      this.size = size;
   }
}
