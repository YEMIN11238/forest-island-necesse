package necesse.level.maps.regionSystem.layers;

import java.awt.Point;
import java.util.Collection;
import java.util.stream.Stream;
import necesse.engine.GameLog;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.registries.LevelJobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.HashMapPointEntry;
import necesse.engine.util.ShortPointHashMap;
import necesse.engine.util.ShortPointHashSet;
import necesse.level.maps.levelData.jobs.TileLevelJob;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.layers.interfaces.SaveDataRegionLayer;
import necesse.level.maps.regionSystem.layers.interfaces.ServerTickRegionLayer;

public class JobsRegionLayer extends RegionLayer implements SaveDataRegionLayer, ServerTickRegionLayer {
   public static int secondsToTickLevel = 60;
   private final ShortPointHashMap<GameLinkedList<TileLevelJob>> jobs = new ShortPointHashMap<>();
   private double tilesPerJobTick;
   private final double nextTilesPerJobTick;
   private double tileJobTickBuffer;
   private int currentJobTileTickX;
   private int currentJobTileTickY;

   public JobsRegionLayer(Region region) {
      super(region);
      this.tilesPerJobTick = region.tileWidth * region.tileHeight / 20.0 / 5.0;
      this.nextTilesPerJobTick = region.tileWidth * region.tileHeight / 20.0 / secondsToTickLevel;
   }

   @Override
   public void init() {
   }

   @Override
   public void onLayerLoaded() {
   }

   @Override
   public void onLoadingComplete() {
   }

   @Override
   public void onLayerUnloaded() {
   }

   @Override
   public void addSaveData(SaveData save) {
      synchronized (this) {
         SaveData jobsSave = new SaveData("JOBS");

         for (HashMapPointEntry<Point, GameLinkedList<TileLevelJob>> entry : this.jobs.getEntries()) {
            for (TileLevelJob job : entry.getValue()) {
               if (job.shouldSave() && job.isValid()) {
                  SaveData jobSave = new SaveData(job.getStringID());
                  job.addSaveData(jobSave);
                  jobsSave.addSaveData(jobSave);
               }
            }
         }

         if (!jobsSave.isEmpty()) {
            save.addSaveData(jobsSave);
         }
      }
   }

   @Override
   public void loadSaveData(LoadData save) {
      LoadData jobsSave = save.getFirstLoadDataByName("JOBS");
      if (jobsSave != null) {
         for (LoadData jobSave : jobsSave.getLoadData()) {
            if (jobSave.isArray()) {
               String stringID = jobSave.getName();

               try {
                  TileLevelJob job = LevelJobRegistry.loadJob(stringID, jobSave);
                  this.addJob(job, false, true);
               } catch (LoadDataException e) {
                  GameLog.warn.println("Could not load level job: " + e.getMessage());
               } catch (Exception e) {
                  GameLog.warn.println("Could not load level job");
                  e.printStackTrace();
               }
            }
         }
      }
   }

   @Override
   public void serverTick() {
      Performance.record(this.level.tickManager(), "tileJobs", () -> {
         this.tileJobTickBuffer = this.tileJobTickBuffer + this.tilesPerJobTick;
         if (this.tileJobTickBuffer >= 1.0) {
            int ticks = (int)this.tileJobTickBuffer;
            this.tileJobTickBuffer -= ticks;

            for (int i = 0; i < ticks; i++) {
               this.region.layers.addLevelJobs(this, this.currentJobTileTickX, this.currentJobTileTickY);
               this.currentJobTileTickX++;
               if (this.currentJobTileTickX >= this.region.tileWidth) {
                  this.currentJobTileTickX = 0;
                  this.currentJobTileTickY++;
                  if (this.currentJobTileTickY >= this.region.tileHeight) {
                     this.currentJobTileTickY = 0;
                     this.tilesPerJobTick = this.nextTilesPerJobTick;
                     this.cleanInvalidJobs();
                  }
               }
            }
         }
      });
   }

   public Stream<TileLevelJob> streamValidJobsInRegion() {
      return this.jobs.values().stream().flatMap(list -> list.streamAndRemoveIf(lj -> !lj.isValid()));
   }

   public Stream<TileLevelJob> streamAllJobsInRegion() {
      return this.jobs.values().stream().flatMap(Collection::stream);
   }

   public GameLinkedList<TileLevelJob> getTileJobsInRegion(int regionTileX, int regionTileY) {
      synchronized (this) {
         return this.jobs.compute(regionTileX, regionTileY, (x, y, lastValue) -> {
            if (lastValue == null) {
               lastValue = new GameLinkedList<>();
            }

            return lastValue;
         });
      }
   }

   protected void cleanTileIfEmptyInRegion(int regionTileX, int regionTileY) {
      synchronized (this) {
         GameLinkedList<TileLevelJob> list = this.jobs.get(regionTileX, regionTileY);
         if (list != null && list.isEmpty()) {
            this.jobs.remove(regionTileX, regionTileY);
         }
      }
   }

   public void cleanInvalidJobs() {
      synchronized (this) {
         ShortPointHashSet removes = new ShortPointHashSet();

         for (HashMapPointEntry<Point, GameLinkedList<TileLevelJob>> entry : this.jobs.getEntries()) {
            GameLinkedList<TileLevelJob> list = entry.getValue();
            if (list.removeIf(j -> !j.isValid()) && list.isEmpty()) {
               removes.add(entry.getX(), entry.getY());
            }
         }

         for (Point regionTile : removes) {
            this.jobs.remove(regionTile.x, regionTile.y);
         }
      }
   }

   public TileLevelJob addJob(TileLevelJob job, boolean override, boolean forceAdd) {
      synchronized (this) {
         GameLinkedList<TileLevelJob> list = this.getTileJobsInRegion(job.tileX - this.region.tileXOffset, job.tileY - this.region.tileYOffset);
         if (!forceAdd) {
            GameLinkedList<TileLevelJob>.Element current = list.getFirstElement();

            while (current != null) {
               if (!((TileLevelJob)current.object).isValid()) {
                  GameLinkedList<TileLevelJob>.Element last = current;
                  current = current.next();
                  last.remove();
               } else {
                  if (((TileLevelJob)current.object).isSameJob(job)) {
                     if (!override) {
                        return (TileLevelJob)current.object;
                     }

                     current.remove();
                     break;
                  }

                  current = current.next();
               }
            }
         }

         GameLinkedList<TileLevelJob>.Element element = list.addLast(job);
         job.init(this.level, element);
         if (!forceAdd && !job.isValid()) {
            element.remove();
            if (list.isEmpty()) {
               this.jobs.remove(job.tileX - this.region.tileXOffset, job.tileY - this.region.tileYOffset);
            }

            return null;
         } else {
            return job;
         }
      }
   }
}
