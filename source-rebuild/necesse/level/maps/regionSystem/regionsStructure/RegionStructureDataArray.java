package necesse.level.maps.regionSystem.regionsStructure;

import java.util.Iterator;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.RegionManager;

public class RegionStructureDataArray extends RegionStructureDataAbstract {
   protected int regionsWidth;
   protected int regionsHeight;
   protected final Region[][] regions;
   protected final GameLinkedList<Region>.Element[][] listElements;
   protected final GameLinkedList<Region> list;

   public RegionStructureDataArray(
      RegionManager manager, RegionStructureDataAbstract.NewRegionConstructor constructor, RegionStructureDataAbstract.NewRegionLoader loader
   ) {
      super(manager, constructor, loader);
      if (manager.level.tileWidth >= 0 && manager.level.tileHeight >= 0) {
         this.regionsWidth = GameMath.divideByPowerOf2RoundedDown(manager.level.tileWidth, 4) + (manager.level.tileWidth % 16 > 0 ? 1 : 0);
         this.regionsHeight = GameMath.divideByPowerOf2RoundedDown(manager.level.tileHeight, 4) + (manager.level.tileHeight % 16 > 0 ? 1 : 0);
         this.regions = new Region[this.regionsWidth][this.regionsHeight];
         this.listElements = new GameLinkedList.Element[this.regionsWidth][this.regionsHeight];
         this.list = new GameLinkedList<>();
      } else {
         throw new IllegalArgumentException("Level must have a size to use RegionLayersDataArray");
      }
   }

   @Override
   public synchronized Region getRegion(int regionX, int regionY, boolean loadIfNotLoaded, boolean forceSkipGenerate) {
      if (regionX >= 0 && regionX < this.regionsWidth && regionY >= 0 && regionY < this.regionsHeight) {
         Region region = this.regions[regionX][regionY];
         if (region == null && loadIfNotLoaded) {
            region = this.constructor.construct(regionX, regionY);
            if (region != null) {
               synchronized (this.manager.level.entityManager.lock) {
                  this.regions[regionX][regionY] = region;
                  this.listElements[regionX][regionY] = this.list.addLast(region);
                  this.generator.load(region, forceSkipGenerate);
               }
            }
         }

         return region;
      } else {
         return null;
      }
   }

   @Override
   public synchronized Region removeRegion(int regionX, int regionY) {
      if (regionX >= 0 && regionX < this.regionsWidth && regionY >= 0 && regionY < this.regionsHeight) {
         Region region = this.regions[regionX][regionY];
         if (region != null) {
            this.listElements[regionX][regionY].remove();
            this.listElements[regionX][regionY] = null;
            this.regions[regionX][regionY] = null;
         }

         return region;
      } else {
         return null;
      }
   }

   @Override
   public synchronized boolean isRegionLoaded(int regionX, int regionY) {
      return regionX >= 0 && regionX < this.regionsWidth && regionY >= 0 && regionY < this.regionsHeight ? this.regions[regionX][regionY] != null : false;
   }

   @Override
   public int getLoadedRegionsSize() {
      return this.list.size();
   }

   @Override
   public Iterator<Region> iterator() {
      return this.list.iterator();
   }
}
