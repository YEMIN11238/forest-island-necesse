package necesse.level.maps.regionSystem.regionsStructure;

import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.RegionManager;

public abstract class RegionStructureDataAbstract implements Iterable<Region> {
   public final RegionManager manager;
   protected final RegionStructureDataAbstract.NewRegionConstructor constructor;
   protected final RegionStructureDataAbstract.NewRegionLoader generator;

   public RegionStructureDataAbstract(
      RegionManager manager, RegionStructureDataAbstract.NewRegionConstructor constructor, RegionStructureDataAbstract.NewRegionLoader loader
   ) {
      this.manager = manager;
      this.constructor = constructor;
      this.generator = loader;
   }

   public abstract Region getRegion(int var1, int var2, boolean var3, boolean var4);

   public abstract Region removeRegion(int var1, int var2);

   public abstract boolean isRegionLoaded(int var1, int var2);

   public abstract int getLoadedRegionsSize();

   @FunctionalInterface
   public interface NewRegionConstructor {
      Region construct(int var1, int var2);
   }

   @FunctionalInterface
   public interface NewRegionLoader {
      void load(Region var1, boolean var2);
   }
}
