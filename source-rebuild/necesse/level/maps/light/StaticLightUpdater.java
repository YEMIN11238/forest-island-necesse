package necesse.level.maps.light;

import java.awt.Point;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.util.BoundsPointIterator;
import necesse.engine.util.GameUtils;
import necesse.engine.util.PointHashSet;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.RegionBoundsExecutor;

public class StaticLightUpdater {
   public boolean printDebug;
   protected final Object updateLock = new Object();
   protected final int startTileX;
   protected final int startTileY;
   protected final int endTileX;
   protected final int endTileY;
   protected final RegionBoundsExecutor regions;
   protected final LightManager manager;
   protected final Level level;
   public static final Point[] crossChecks = new Point[]{new Point(-1, 0), new Point(1, 0), new Point(0, -1), new Point(0, 1)};

   public StaticLightUpdater(LightManager manager, int startTileX, int startTileY, int endTileX, int endTileY) {
      this.manager = manager;
      this.level = manager.level;
      this.startTileX = this.level.limitTileXToBounds(startTileX);
      this.startTileY = this.level.limitTileYToBounds(startTileY);
      this.endTileX = this.level.limitTileXToBounds(endTileX);
      this.endTileY = this.level.limitTileYToBounds(endTileY);
      this.regions = new RegionBoundsExecutor(
         this.level.regionManager,
         this.level.limitTileXToBounds(startTileX - 25),
         this.level.limitTileYToBounds(startTileY - 25),
         this.level.limitTileXToBounds(endTileX + 25),
         this.level.limitTileYToBounds(endTileY + 25),
         false
      );
   }

   public int getLightModifier(int tileX, int tileY) {
      return this.regions
         .getOnTile(
            tileX,
            tileY,
            (region, regionTileX, regionTileY) -> Math.max(
               10,
               region.objectLayer
                  .getObjectByRegion(0, regionTileX, regionTileY)
                  .getLightLevelMod(this.level, regionTileX + region.tileXOffset, regionTileY + region.tileYOffset)
            ),
            10
         );
   }

   public GameLight getNewLight(int tileX, int tileY) {
      GameLight light = this.regions.getOnTile(tileX, tileY, (region, regionTileX, regionTileY) -> {
         GameLight foundLight = region.objectLayer.getCombinedLightByRegion(regionTileX, regionTileY);
         foundLight.combine(region.tileLayer.getTileByRegion(regionTileX, regionTileY).getLight(this.level));
         return foundLight;
      }, null);
      if (light == null) {
         light = this.manager.newLight(0.0F);
      }

      return light;
   }

   public void setStaticLight(int tileX, int tileY, GameLight light) {
      synchronized (this.manager.lightsLock) {
         this.regions.runOnTile(tileX, tileY, (region, regionTileX, regionTileY) -> region.lightLayer.setStaticLightByRegion(regionTileX, regionTileY, light));
      }
   }

   public LinkedList<SourcedGameLight> getLightSources(int tileX, int tileY) {
      synchronized (this.manager.sourcesLock) {
         return this.regions
            .getOnTile(tileX, tileY, (region, regionTileX, regionTileY) -> region.lightLayer.getStaticSourcesByRegion(regionTileX, regionTileY), null);
      }
   }

   public void clearSources(int tileX, int tileY) {
      synchronized (this.manager.sourcesLock) {
         this.regions.runOnTile(tileX, tileY, (region, regionTileX, regionTileY) -> region.lightLayer.clearStaticSourcesByRegion(regionTileX, regionTileY));
      }
   }

   protected void addSourcedLight(int tileX, int tileY, SourcedGameLight sourcedLight, PointHashSet shouldUpdates) {
      synchronized (this.manager.sourcesLock) {
         this.regions
            .runOnTile(
               tileX,
               tileY,
               (region, regionTileX, regionTileY) -> region.lightLayer.addStaticSourcedLightByRegion(regionTileX, regionTileY, sourcedLight, shouldUpdates)
            );
      }
   }

   protected boolean hasNoBetterSameSource(int tileX, int tileY, SourcedGameLight source) {
      synchronized (this.manager.sourcesLock) {
         return this.regions
            .getOnTile(
               tileX,
               tileY,
               (region, regionTileX, regionTileY) -> region.lightLayer.hasNoBetterSameStaticSourceByRegion(regionTileX, regionTileY, source),
               false
            );
      }
   }

   protected boolean hasNoBetterSameColor(int tileX, int tileY, SourcedGameLight source) {
      synchronized (this.manager.sourcesLock) {
         return this.regions
            .getOnTile(
               tileX,
               tileY,
               (region, regionTileX, regionTileY) -> region.lightLayer.hasNoBetterSameStaticColorByRegion(regionTileX, regionTileY, source),
               false
            );
      }
   }

   public void runUpdate(boolean concurrently) {
      this.runUpdate(() -> new BoundsPointIterator(this.startTileX, this.endTileX, this.startTileY, this.endTileY), concurrently);
   }

   public void runUpdate(Iterable<Point> tiles, boolean concurrently) {
      if (this.level.isLoadingComplete()) {
         long time = System.nanoTime();
         HashMap<Integer, StaticLightUpdater.LightSources> sourceLists = new HashMap<>();
         LinkedList<StaticLightUpdater.LightComputeFuture> computes = new LinkedList<>();
         PointHashSet sourceRemoves = new PointHashSet();
         PointHashSet shouldUpdates = new PointHashSet();
         Runnable setupLogic = () -> {
            for (Point tile : tiles) {
               if (this.regions.isTileLoaded(tile.x, tile.y)) {
                  GameLight newLight = this.getNewLight(tile.x, tile.y);
                  if (newLight.getLevel() > 0.0F) {
                     SourcedGameLight sourcedNewLight = new SourcedGameLight(tile.x, tile.y, newLight);
                     sourceLists.compute(newLight.getColorHash(), (hash, last) -> {
                        if (last == null) {
                           return new StaticLightUpdater.LightSources(sourcedNewLight);
                        }

                        last.addSource(sourcedNewLight);
                        return (StaticLightUpdater.LightSources)last;
                     });
                     sourceRemoves.add(tile.x, tile.y);
                  } else {
                     synchronized (this.manager.sourcesLock) {
                        LinkedList<SourcedGameLight> sources = this.getLightSources(tile.x, tile.y);
                        if (sources != null) {
                           for (SourcedGameLight source : sources) {
                              if (source.sourceTileX == tile.x && source.sourceTileY == tile.y) {
                                 sourceRemoves.add(tile.x, tile.y);
                              } else {
                                 GameLight nextNewLight = this.getNewLight(source.sourceTileX, source.sourceTileY);
                                 if (nextNewLight.getLevel() > 0.0F) {
                                    SourcedGameLight nextSourcedNewLight = new SourcedGameLight(source.sourceTileX, source.sourceTileY, nextNewLight);
                                    sourceLists.compute(nextNewLight.getColorHash(), (hash, lastList) -> {
                                       if (lastList == null) {
                                          return new StaticLightUpdater.LightSources(nextSourcedNewLight);
                                       }

                                       lastList.addSource(nextSourcedNewLight);
                                       return (StaticLightUpdater.LightSources)lastList;
                                    });
                                    sourceRemoves.add(source.sourceTileX, source.sourceTileY);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            PointHashSet nextChecks = new PointHashSet();

            for (Point p : sourceRemoves) {
               this.removeSource(p.x, p.y, nextChecks, shouldUpdates);
            }

            synchronized (this.manager.sourcesLock) {
               nextChecks.forEach((tileX, tileY) -> {
                  LinkedList<SourcedGameLight> nextSources = this.getLightSources(tileX, tileY);
                  if (nextSources != null) {
                     for (SourcedGameLight nextSource : nextSources) {
                        sourceLists.compute(nextSource.light.getColorHash(), (hash, lastList) -> {
                           if (lastList == null) {
                              return new StaticLightUpdater.LightSources(tileX, tileY, nextSource);
                           }

                           lastList.addSource(tileX, tileY, nextSource);
                           return (StaticLightUpdater.LightSources)lastList;
                        });
                     }
                  }
               });
            }

            for (StaticLightUpdater.LightSources sources : sourceLists.values()) {
               int cStartX = this.level.limitTileXToBounds(Math.max(this.startTileX, sources.minTileX) - 25);
               int cStartY = this.level.limitTileYToBounds(Math.max(this.startTileY, sources.minTileY) - 25);
               int cEndX = this.level.limitTileXToBounds(Math.min(this.endTileX, sources.maxTileX) + 25);
               int cEndY = this.level.limitTileYToBounds(Math.min(this.endTileY, sources.maxTileY) + 25);
               StaticLightUpdaterCompute compute = new StaticLightUpdaterCompute(this, cStartX, cStartY, cEndX, cEndY);

               for (StaticLightUpdater.PointSourcedGameLight psgl : sources) {
                  compute.addSource(psgl.tileX, psgl.tileY, psgl.source);
               }

               computes.add(new StaticLightUpdater.LightComputeFuture(compute));
            }
         };
         AtomicInteger computeIterations = new AtomicInteger();
         AtomicInteger applyIterations = new AtomicInteger();
         ExecutorService updateExecutor = this.manager.updateExecutor;
         if (updateExecutor != null) {
            ThreadPoolExecutor computeExecutor = this.manager.computeExecutor;
            if (computeExecutor != null) {
               if (concurrently) {
                  updateExecutor.submit(
                     () -> {
                        synchronized (this.updateLock) {
                           if (this.printDebug) {
                              System.out
                                 .println(
                                    (this.level.isServer() ? "S" : "C")
                                       + " SETUP, E: "
                                       + computeExecutor.getActiveCount()
                                       + "/"
                                       + computeExecutor.getQueue().size()
                                       + ", "
                                       + tiles
                                       + ", "
                                       + GameUtils.getTimeStringNano(System.nanoTime() - time)
                                 );
                           }

                           setupLogic.run();
                           if (this.printDebug) {
                              System.out
                                 .println(
                                    (this.level.isServer() ? "S" : "C")
                                       + " WAITING, S: "
                                       + sourceLists.size()
                                       + "/"
                                       + sourceRemoves.size()
                                       + ", C: "
                                       + computes.size()
                                       + ", E: "
                                       + computeExecutor.getActiveCount()
                                       + "/"
                                       + computeExecutor.getQueue().size()
                                       + ", "
                                       + tiles
                                       + ", "
                                       + GameUtils.getTimeStringNano(System.nanoTime() - time)
                                 );
                           }

                           if (computes.size() > 1) {
                              for (StaticLightUpdater.LightComputeFuture lcfxxx : computes) {
                                 lcfxxx.future = computeExecutor.submit(lcfxxx.compute::compute);
                              }

                              this.waitForComputes(computes, computeIterations);
                           } else {
                              for (StaticLightUpdater.LightComputeFuture lcfxx : computes) {
                                 lcfxx.compute.compute();
                                 lcfxx.complete = true;
                              }
                           }

                           for (StaticLightUpdater.LightComputeFuture lcfx : computes) {
                              if (lcfx.complete) {
                                 applyIterations.addAndGet(lcfx.compute.apply(shouldUpdates));
                              }
                           }

                           synchronized (this.manager.lightsLock) {
                              shouldUpdates.forEach(this::updateLightSources);
                           }

                           if (this.printDebug) {
                              System.out
                                 .println(
                                    (this.level.isServer() ? "S" : "C")
                                       + " DONE, S: "
                                       + sourceLists.size()
                                       + "/"
                                       + sourceRemoves.size()
                                       + ", C: "
                                       + computes.size()
                                       + ", E: "
                                       + computeExecutor.getActiveCount()
                                       + "/"
                                       + computeExecutor.getQueue().size()
                                       + ", I: "
                                       + computeIterations
                                       + ", A: "
                                       + applyIterations
                                       + ", "
                                       + tiles
                                       + ", "
                                       + GameUtils.getTimeStringNano(System.nanoTime() - time)
                                 );
                           }
                        }
                     }
                  );
                  if (this.printDebug) {
                     System.out
                        .println(
                           (this.level.isServer() ? "S" : "C")
                              + " INITIAL, E: "
                              + computeExecutor.getActiveCount()
                              + "/"
                              + computeExecutor.getQueue().size()
                              + ", "
                              + tiles
                              + ", "
                              + GameUtils.getTimeStringNano(System.nanoTime() - time)
                        );
                  }
               } else {
                  synchronized (this.updateLock) {
                     setupLogic.run();
                     if (this.printDebug) {
                        System.out
                           .println(
                              (this.level.isServer() ? "S" : "C")
                                 + " START, S: "
                                 + sourceLists.size()
                                 + "/"
                                 + sourceRemoves.size()
                                 + ", C: "
                                 + computes.size()
                                 + ", "
                                 + tiles
                                 + ", "
                                 + GameUtils.getTimeStringNano(System.nanoTime() - time)
                           );
                     }

                     if (computes.size() <= 1) {
                        for (StaticLightUpdater.LightComputeFuture lcf : computes) {
                           computeIterations.addAndGet(lcf.compute.compute());
                           lcf.complete = true;
                        }
                     } else {
                        for (StaticLightUpdater.LightComputeFuture lcf : computes) {
                           lcf.future = computeExecutor.submit(lcf.compute::compute);
                        }

                        this.waitForComputes(computes, computeIterations);
                     }

                     for (StaticLightUpdater.LightComputeFuture lcf : computes) {
                        if (lcf.complete) {
                           applyIterations.addAndGet(lcf.compute.apply(shouldUpdates));
                        }
                     }

                     synchronized (this.manager.lightsLock) {
                        shouldUpdates.forEach(this::updateLightSources);
                     }

                     if (this.printDebug) {
                        System.out
                           .println(
                              (this.level.isServer() ? "S" : "C")
                                 + ", S: "
                                 + sourceLists.size()
                                 + "/"
                                 + sourceRemoves.size()
                                 + ", C: "
                                 + computes.size()
                                 + ", I: "
                                 + computeIterations
                                 + ", a: "
                                 + applyIterations
                                 + ", "
                                 + tiles
                                 + ", "
                                 + GameUtils.getTimeStringNano(System.nanoTime() - time)
                           );
                     }
                  }
               }
            }
         }
      }
   }

   private void removeSource(int x, int y, PointHashSet nextChecks, PointHashSet shouldUpdates) {
      synchronized (this.manager.sourcesLock) {
         LinkedList<Point> open = new LinkedList<>();
         PointHashSet closed = new PointHashSet();
         open.add(new Point(x, y));

         while (!open.isEmpty()) {
            Point current = open.removeFirst();
            closed.add(current.x, current.y);
            LinkedList<SourcedGameLight> currentSources = this.getLightSources(current.x, current.y);
            if (currentSources != null) {
               ListIterator<SourcedGameLight> currentSourcesLI = currentSources.listIterator();

               while (currentSourcesLI.hasNext()) {
                  SourcedGameLight currentSource = currentSourcesLI.next();
                  if (currentSource.sourceTileX == x && currentSource.sourceTileY == y) {
                     currentSourcesLI.remove();
                     shouldUpdates.add(current.x, current.y);

                     for (Point delta : crossChecks) {
                        Point next = new Point(current.x + delta.x, current.y + delta.y);
                        if (this.regions.isInsideBounds(next.x, next.y) && this.regions.isTileLoaded(next.x, next.y) && !closed.contains(next.x, next.y)) {
                           open.add(next);
                           nextChecks.add(next.x, next.y);
                        }
                     }
                     break;
                  }
               }
            }
         }
      }
   }

   private void updateLightSources(int tileX, int tileY) {
      GameLight newLight = null;
      synchronized (this.manager.sourcesLock) {
         LinkedList<SourcedGameLight> sources = this.getLightSources(tileX, tileY);
         if (sources != null) {
            if (sources.isEmpty()) {
               this.clearSources(tileX, tileY);
            } else {
               Comparator<SourcedGameLight> comparator = Comparator.comparingDouble(e -> e.light.getLevel());
               comparator = comparator.thenComparingInt(e -> e.sourceTileX);
               comparator = comparator.thenComparingInt(e -> e.sourceTileY);
               newLight = sources.stream().sorted(comparator).map(e -> e.light).reduce(null, (last, next) -> {
                  if (last == null) {
                     return next.copy();
                  }

                  last.combine(next);
                  return last;
               });
            }
         }
      }

      if (newLight == null) {
         newLight = this.manager.newLight(0.0F);
      }

      synchronized (this.manager.lightsLock) {
         this.setStaticLight(tileX, tileY, newLight);
      }
   }

   private void waitForComputes(LinkedList<StaticLightUpdater.LightComputeFuture> futures, AtomicInteger iterations) {
      for (StaticLightUpdater.LightComputeFuture lcf : futures) {
         try {
            iterations.addAndGet(lcf.future.get(20L, TimeUnit.SECONDS));
            lcf.complete = true;
         } catch (InterruptedException e) {
            lcf.complete = false;
         } catch (ExecutionException e) {
            lcf.complete = false;
            System.err.println("Error executing light update. I: " + iterations.get());
            e.printStackTrace();
         } catch (TimeoutException e) {
            lcf.complete = false;
            System.err.println("Timed out executing light update. I: " + iterations.get());
            lcf.printDebug(System.err);
            e.printStackTrace();
         }
      }
   }

   protected static class LightComputeFuture {
      public final StaticLightUpdaterCompute compute;
      public boolean complete;
      public Future<Integer> future;

      public LightComputeFuture(StaticLightUpdaterCompute compute) {
         this.compute = compute;
         this.complete = false;
      }

      public void printDebug(PrintStream stream) {
         this.compute.printDebug(stream);
         stream.println(this.complete);
         stream.println(this.future);
      }
   }

   protected static class LightSources implements Iterable<StaticLightUpdater.PointSourcedGameLight> {
      public int minTileX;
      public int maxTileX;
      public int minTileY;
      public int maxTileY;
      private final LinkedList<StaticLightUpdater.PointSourcedGameLight> sources = new LinkedList<>();

      public LightSources(int tileX, int tileY, SourcedGameLight first) {
         this.minTileX = first.sourceTileX;
         this.maxTileX = first.sourceTileX;
         this.minTileY = first.sourceTileY;
         this.maxTileY = first.sourceTileY;
         this.sources.add(new StaticLightUpdater.PointSourcedGameLight(tileX, tileY, first));
      }

      public LightSources(SourcedGameLight first) {
         this(first.sourceTileX, first.sourceTileY, first);
      }

      public void addSource(int x, int y, SourcedGameLight source) {
         this.minTileX = Math.min(this.minTileX, source.sourceTileX);
         this.maxTileX = Math.max(this.maxTileX, source.sourceTileX);
         this.minTileY = Math.min(this.minTileY, source.sourceTileY);
         this.maxTileY = Math.max(this.maxTileY, source.sourceTileY);
         this.sources.add(new StaticLightUpdater.PointSourcedGameLight(x, y, source));
      }

      public void addSource(SourcedGameLight source) {
         this.addSource(source.sourceTileX, source.sourceTileY, source);
      }

      @Override
      public Iterator<StaticLightUpdater.PointSourcedGameLight> iterator() {
         return this.sources.iterator();
      }
   }

   protected static class PointSourcedGameLight {
      public final int tileX;
      public final int tileY;
      public final SourcedGameLight source;

      public PointSourcedGameLight(int tileX, int tileY, SourcedGameLight source) {
         this.tileX = tileX;
         this.tileY = tileY;
         this.source = source;
      }
   }
}
