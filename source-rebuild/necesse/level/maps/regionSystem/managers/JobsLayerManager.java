package necesse.level.maps.regionSystem.managers;

import java.awt.Rectangle;
import java.util.stream.Stream;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.engine.util.gameAreaSearch.JobsRegionSearch;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.jobs.TileLevelJob;
import necesse.level.maps.regionSystem.LevelRegionsSpliterator;
import necesse.level.maps.regionSystem.Region;

public class JobsLayerManager {
   protected final Level level;

   public JobsLayerManager(Level level) {
      this.level = level;
   }

   public Iterable<TileLevelJob> getValidJobsInRegion(int regionX, int regionY) {
      Region region = this.level.regionManager.getRegion(regionX, regionY, false);
      return region == null ? new GameLinkedList<>() : () -> region.jobsLayer.streamValidJobsInRegion().iterator();
   }

   public Stream<TileLevelJob> streamValidJobsInRegion(int regionX, int regionY) {
      Region region = this.level.regionManager.getRegion(regionX, regionY, false);
      return region == null ? Stream.empty() : region.jobsLayer.streamValidJobsInRegion();
   }

   public Stream<TileLevelJob> streamJobsInTile(int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null ? Stream.empty() : region.jobsLayer.getTileJobsInRegion(tileX - region.tileXOffset, tileY - region.tileYOffset).stream();
   }

   public GameAreaStream<TileLevelJob> streamAreaJobs(int tileX, int tileY, int maxTileDistance) {
      return new JobsRegionSearch(this.level, tileX, tileY, maxTileDistance).streamEach();
   }

   public Stream<TileLevelJob> streamValidJobsInRegionsShape(int minX, int minY, int maxX, int maxY, int extraRegionRange) {
      return new LevelRegionsSpliterator(this.level, minX, minY, maxX, maxY, extraRegionRange).stream().flatMap(rp -> this.streamValidJobsInRegion(rp.x, rp.y));
   }

   public Stream<TileLevelJob> streamValidJobsInRegionsShape(Rectangle bounds, int extraRegionRange) {
      return new LevelRegionsSpliterator(this.level, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, extraRegionRange)
         .stream()
         .flatMap(rp -> this.streamValidJobsInRegion(rp.x, rp.y));
   }

   public Stream<TileLevelJob> streamInRegionsInRange(float levelX, float levelY, int range) {
      int minX = (int)(levelX - range);
      int minY = (int)(levelY - range);
      int maxX = (int)(levelX + range);
      int maxY = (int)(levelY + range);
      return this.streamValidJobsInRegionsShape(minX, minY, maxX, maxY, 0);
   }

   public Stream<TileLevelJob> streamInRegionsInTileRange(int levelX, int levelY, int tileRange) {
      int levelRange = tileRange * 32;
      int minX = levelX - levelRange;
      int minY = levelY - levelRange;
      int maxX = levelX + levelRange;
      int maxY = levelY + levelRange;
      return this.streamValidJobsInRegionsShape(minX, minY, maxX, maxY, 0);
   }

   public TileLevelJob addJob(TileLevelJob job, boolean override, boolean forceAdd) {
      Region region = this.level.regionManager.getRegionByTile(job.tileX, job.tileY, forceAdd);
      return region == null ? null : region.jobsLayer.addJob(job, override, forceAdd);
   }

   public TileLevelJob addJob(TileLevelJob job, boolean override) {
      return this.addJob(job, override, false);
   }

   public TileLevelJob addJob(TileLevelJob job) {
      return this.addJob(job, false);
   }
}
