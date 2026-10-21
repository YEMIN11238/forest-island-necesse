package necesse.level.maps.light;

import java.awt.Point;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.util.BoundsPointIterator;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.level.maps.Level;

public abstract class LightMap implements LightMapInterface {
   public boolean printDebug;
   protected final Object updateLock = new Object();
   protected final LightManager manager;
   protected final Level level;
   protected final int maxLightDistance;
   protected final int startX;
   protected final int startY;
   protected final int endX;
   protected final int endY;
   protected final int width;
   protected final LinkedList<SourcedGameLight>[] lightSources;
   protected final GameLight[] lights;
   public static final Point[] crossChecks = new Point[]{new Point(-1, 0), new Point(1, 0), new Point(0, -1), new Point(0, 1)};

   public LightMap(LightManager manager, int startX, int startY, int endX, int endY, int maxLightDistance) {
      this.manager = manager;
      this.level = manager.level;
      this.startX = startX;
      this.startY = startY;
      this.endX = endX;
      this.endY = endY;
      this.width = endX - startX + 1;
      int height = endY - startY + 1;
      this.maxLightDistance = maxLightDistance;
      this.lightSources = new LinkedList[this.width * height];
      this.lights = new GameLight[this.width * height];
      this.resetLights(manager);
   }

   @Override
   public void resetLights(LightManager manager) {
      synchronized (this.lights) {
         synchronized (this.lightSources) {
            Arrays.fill(this.lightSources, null);
         }
      }
   }

   @Override
   public GameLight getLight(int x, int y) {
      x = GameMath.limit(x, this.startX, this.endX);
      y = GameMath.limit(y, this.startY, this.endY);
      int index = this.getIndex(x, y);
      synchronized (this.lights) {
         GameLight light = this.lights[index];
         if (light == null) {
            light = this.manager.newLight(0.0F);
            this.lights[index] = light;
         }

         return light;
      }
   }

   @Override
   public List<SourcedGameLight> getLightSources(int x, int y) {
      if (this.isOutsideMap(x, y)) {
         return Collections.emptyList();
      }

      int index = this.getIndex(x, y);
      synchronized (this.lightSources) {
         LinkedList<SourcedGameLight> sources = this.lightSources[index];
         return sources == null ? Collections.emptyList() : new ArrayList<>(sources);
      }
   }

   protected int getIndex(int x, int y) {
      return x - this.startX + (y - this.startY) * this.width;
   }

   public boolean isOutsideMap(int x, int y) {
      return x < this.startX || y < this.startY || x > this.endX || y > this.endY;
   }

   protected void addSourcedLight(int x, int y, SourcedGameLight sourcedLight, Collection<Integer> shouldUpdates) {
      int index = this.getIndex(x, y);
      synchronized (this.lightSources) {
         LinkedList<SourcedGameLight> sources = this.lightSources[index];
         if (sources == null) {
            sources = new LinkedList<>();
            this.lightSources[index] = sources;
         }

         sources.removeIf(e -> {
            if (e.light.getColorHash() != sourcedLight.light.getColorHash()) {
               return false;
            } else if (e.light.getLevel() > sourcedLight.light.getLevel()) {
               return false;
            } else {
               return sourcedLight.sourceTileX == x && sourcedLight.sourceTileY == y ? true : e.sourceTileX != x || e.sourceTileY != y;
            }
         });
         sources.add(sourcedLight);
         shouldUpdates.add(index);
      }
   }

   protected boolean hasNoBetterSameSource(int x, int y, SourcedGameLight source) {
      if (this.isOutsideMap(x, y)) {
         return false;
      }

      int index = this.getIndex(x, y);
      synchronized (this.lightSources) {
         LinkedList<SourcedGameLight> sources = this.lightSources[index];
         return sources == null
            ? true
            : sources.stream()
               .filter(e -> e.sourceTileX == source.sourceTileX && e.sourceTileY == source.sourceTileY)
               .noneMatch(e -> e.light.getLevel() >= source.light.getLevel());
      }
   }

   protected boolean hasNoBetterSameColor(int x, int y, SourcedGameLight source) {
      if (this.isOutsideMap(x, y)) {
         return false;
      }

      int index = this.getIndex(x, y);
      synchronized (this.lightSources) {
         LinkedList<SourcedGameLight> sources = this.lightSources[index];
         return sources == null
            ? true
            : sources.stream()
               .filter(e -> e.sourceTileX != source.sourceTileX || e.sourceTileY != source.sourceTileY)
               .noneMatch(e -> e.light.getColorHash() == source.light.getColorHash() && e.light.getLevel() >= source.light.getLevel());
      }
   }

   @Override
   public void update(int tileX, int tileY, boolean concurrently) {
      if (!this.isOutsideMap(tileX, tileY)) {
         this.update(tileX, tileY, tileX, tileY, concurrently);
      }
   }

   @Override
   public void update(int startX, int startY, int endX, int endY, boolean concurrently) {
      int finalStartX = Math.max(startX, this.startX);
      int finalStartY = Math.max(startY, this.startY);
      int finalEndX = Math.min(endX, this.endX);
      int finalEndY = Math.min(endY, this.endY);
      this.update(() -> new BoundsPointIterator(finalStartX, finalEndX, finalStartY, finalEndY), concurrently);
   }

   @Override
   public void update(Iterable<Point> tiles, boolean concurrently) {
      if (this.level.isLoadingComplete()) {
         long time = System.nanoTime();
         HashMap<Integer, LightMap.LightSources> sourceLists = new HashMap<>();
         LinkedList<LightMap.LightComputeFuture> computes = new LinkedList<>();
         HashSet<Point> sourceRemoves = new HashSet<>();
         HashSet<Integer> shouldUpdates = new HashSet<>();
         Runnable setupLogic = () -> {
            for (Point tile : tiles) {
               if (tile.x >= this.startX && tile.y >= this.startY && tile.x <= this.endX && tile.y <= this.endY) {
                  GameLight newLight = this.getNewLight(tile.x, tile.y);
                  if (newLight.getLevel() > 0.0F) {
                     SourcedGameLight sourcedNewLight = new SourcedGameLight(tile.x, tile.y, newLight);
                     sourceLists.compute(newLight.getColorHash(), (hash, last) -> {
                        if (last == null) {
                           return new LightMap.LightSources(sourcedNewLight);
                        }

                        last.addSource(sourcedNewLight);
                        return (LightMap.LightSources)last;
                     });
                     sourceRemoves.add(tile);
                  } else {
                     int indexx = this.getIndex(tile.x, tile.y);
                     synchronized (this.lightSources) {
                        LinkedList<SourcedGameLight> sources = this.lightSources[indexx];
                        if (sources != null) {
                           for (SourcedGameLight source : sources) {
                              if (source.sourceTileX == tile.x && source.sourceTileY == tile.y) {
                                 sourceRemoves.add(tile);
                              } else {
                                 GameLight nextNewLight = this.getNewLight(source.sourceTileX, source.sourceTileY);
                                 if (nextNewLight.getLevel() > 0.0F) {
                                    SourcedGameLight nextSourcedNewLight = new SourcedGameLight(source.sourceTileX, source.sourceTileY, nextNewLight);
                                    sourceLists.compute(nextNewLight.getColorHash(), (hash, lastList) -> {
                                       if (lastList == null) {
                                          return new LightMap.LightSources(nextSourcedNewLight);
                                       }

                                       lastList.addSource(nextSourcedNewLight);
                                       return (LightMap.LightSources)lastList;
                                    });
                                    sourceRemoves.add(new Point(source.sourceTileX, source.sourceTileY));
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            HashSet<Point> nextChecks = new HashSet<>();

            for (Point p : sourceRemoves) {
               this.removeSource(p.x, p.y, nextChecks, shouldUpdates);
            }

            synchronized (this.lightSources) {
               for (Point next : nextChecks) {
                  LinkedList<SourcedGameLight> nextSources = this.lightSources[this.getIndex(next.x, next.y)];
                  if (nextSources != null) {
                     for (SourcedGameLight nextSource : nextSources) {
                        sourceLists.compute(nextSource.light.getColorHash(), (hash, lastList) -> {
                           if (lastList == null) {
                              return new LightMap.LightSources(next.x, next.y, nextSource);
                           }

                           lastList.addSource(next.x, next.y, nextSource);
                           return (LightMap.LightSources)lastList;
                        });
                     }
                  }
               }
            }

            for (LightMap.LightSources sources : sourceLists.values()) {
               int cStartX = Math.max(this.startX, sources.minX - this.maxLightDistance);
               int cStartY = Math.max(this.startY, sources.minY - this.maxLightDistance);
               int cEndX = Math.min(this.endX, sources.maxX + this.maxLightDistance);
               int cEndY = Math.min(this.endY, sources.maxY + this.maxLightDistance);
               LightCompute compute = new LightCompute(this.level, this, cStartX, cStartY, cEndX, cEndY);

               for (LightMap.PointSourcedGameLight psgl : sources) {
                  compute.addSource(psgl.x, psgl.y, psgl.source);
               }

               computes.add(new LightMap.LightComputeFuture(compute));
            }
         };
         AtomicInteger computeIterations = new AtomicInteger();
         AtomicInteger applyIterations = new AtomicInteger();
         ExecutorService updateExecutor = this.level.lightManager.updateExecutor;
         if (updateExecutor != null) {
            ThreadPoolExecutor computeExecutor = this.level.lightManager.computeExecutor;
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
                              for (LightMap.LightComputeFuture lcfxxx : computes) {
                                 lcfxxx.future = computeExecutor.submit(lcfxxx.compute::compute);
                              }

                              this.waitForComputes(computes, computeIterations);
                           } else {
                              for (LightMap.LightComputeFuture lcfxx : computes) {
                                 lcfxx.compute.compute();
                                 lcfxx.complete = true;
                              }
                           }

                           for (LightMap.LightComputeFuture lcfx : computes) {
                              if (lcfx.complete) {
                                 applyIterations.addAndGet(lcfx.compute.apply(shouldUpdates));
                              }
                           }

                           synchronized (this.lights) {
                              for (Integer indexx : shouldUpdates) {
                                 this.updateLightSources(indexx);
                              }
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
                        for (LightMap.LightComputeFuture lcf : computes) {
                           computeIterations.addAndGet(lcf.compute.compute());
                           lcf.complete = true;
                        }
                     } else {
                        for (LightMap.LightComputeFuture lcf : computes) {
                           lcf.future = computeExecutor.submit(lcf.compute::compute);
                        }

                        this.waitForComputes(computes, computeIterations);
                     }

                     for (LightMap.LightComputeFuture lcf : computes) {
                        if (lcf.complete) {
                           applyIterations.addAndGet(lcf.compute.apply(shouldUpdates));
                        }
                     }

                     synchronized (this.lights) {
                        for (Integer index : shouldUpdates) {
                           this.updateLightSources(index);
                        }
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

   private void removeSource(int x, int y, HashSet<Point> nextChecks, HashSet<Integer> shouldUpdates) {
      synchronized (this.lightSources) {
         LinkedList<Point> open = new LinkedList<>();
         HashSet<Point> closed = new HashSet<>();
         open.add(new Point(x, y));

         while (!open.isEmpty()) {
            Point current = open.removeFirst();
            closed.add(new Point(current.x, current.y));
            int index = this.getIndex(current.x, current.y);
            LinkedList<SourcedGameLight> currentSources = this.lightSources[index];
            if (currentSources != null) {
               ListIterator<SourcedGameLight> currentSourcesLI = currentSources.listIterator();

               while (currentSourcesLI.hasNext()) {
                  SourcedGameLight currentSource = currentSourcesLI.next();
                  if (currentSource.sourceTileX == x && currentSource.sourceTileY == y) {
                     currentSourcesLI.remove();
                     shouldUpdates.add(index);

                     for (Point delta : crossChecks) {
                        Point next = new Point(current.x + delta.x, current.y + delta.y);
                        if (!this.isOutsideMap(next.x, next.y) && !closed.contains(next)) {
                           open.add(next);
                           nextChecks.add(next);
                        }
                     }
                     break;
                  }
               }
            }
         }
      }
   }

   private void updateLightSources(int index) {
      GameLight newLight = null;
      synchronized (this.lightSources) {
         LinkedList<SourcedGameLight> sources = this.lightSources[index];
         if (sources != null) {
            if (sources.isEmpty()) {
               this.lightSources[index] = null;
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

      synchronized (this.lights) {
         this.lights[index] = newLight;
      }
   }

   private void waitForComputes(LinkedList<LightMap.LightComputeFuture> futures, AtomicInteger iterations) {
      for (LightMap.LightComputeFuture lcf : futures) {
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

   protected abstract GameLight getNewLight(int var1, int var2);

   protected static class LightComputeFuture {
      public final LightCompute compute;
      public boolean complete;
      public Future<Integer> future;

      public LightComputeFuture(LightCompute compute) {
         this.compute = compute;
         this.complete = false;
      }

      public void printDebug(PrintStream stream) {
         this.compute.printDebug(stream);
         stream.println(this.complete);
         stream.println(this.future);
      }
   }

   protected static class LightSources implements Iterable<LightMap.PointSourcedGameLight> {
      public int minX;
      public int maxX;
      public int minY;
      public int maxY;
      private LinkedList<LightMap.PointSourcedGameLight> sources = new LinkedList<>();

      public LightSources(int x, int y, SourcedGameLight first) {
         this.minX = first.sourceTileX;
         this.maxX = first.sourceTileX;
         this.minY = first.sourceTileY;
         this.maxY = first.sourceTileY;
         this.sources.add(new LightMap.PointSourcedGameLight(x, y, first));
      }

      public LightSources(SourcedGameLight first) {
         this(first.sourceTileX, first.sourceTileY, first);
      }

      public void addSource(int x, int y, SourcedGameLight source) {
         this.minX = Math.min(this.minX, source.sourceTileX);
         this.maxX = Math.max(this.maxX, source.sourceTileX);
         this.minY = Math.min(this.minY, source.sourceTileY);
         this.maxY = Math.max(this.maxY, source.sourceTileY);
         this.sources.add(new LightMap.PointSourcedGameLight(x, y, source));
      }

      public void addSource(SourcedGameLight source) {
         this.addSource(source.sourceTileX, source.sourceTileY, source);
      }

      @Override
      public Iterator<LightMap.PointSourcedGameLight> iterator() {
         return this.sources.iterator();
      }
   }

   protected static class PointSourcedGameLight {
      public final int x;
      public final int y;
      public final SourcedGameLight source;

      public PointSourcedGameLight(int x, int y, SourcedGameLight source) {
         this.x = x;
         this.y = y;
         this.source = source;
      }
   }
}
