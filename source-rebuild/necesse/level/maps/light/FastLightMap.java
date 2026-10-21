package necesse.level.maps.light;

import java.awt.Point;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.level.maps.Level;

public abstract class FastLightMap implements LightMapInterface {
   public boolean printDebug;
   protected final LightManager manager;
   protected final Level level;
   protected final int maxLightDistance;
   protected final LightArea area;

   public FastLightMap(LightManager manager, int startX, int startY, int endX, int endY, int maxLightDistance) {
      this.manager = manager;
      this.level = manager.level;
      this.maxLightDistance = maxLightDistance;
      this.area = new LightArea(startX, startY, endX, endY);
      this.area.initLights();
      this.resetLights(manager);
   }

   @Override
   public void resetLights(LightManager manager) {
      synchronized (this.area) {
         for (int i = 0; i < this.area.lights.length; i++) {
            this.area.lights[i] = manager.newLight(0.0F);
         }
      }
   }

   @Override
   public GameLight getLight(int x, int y) {
      x = GameMath.limit(x, this.area.startX, this.area.endX);
      y = GameMath.limit(y, this.area.startY, this.area.endY);
      synchronized (this.area) {
         return this.area.lights[this.area.getIndex(x, y)];
      }
   }

   @Override
   public List<SourcedGameLight> getLightSources(int x, int y) {
      return Collections.singletonList(new SourcedGameLight(x, y, this.getLight(x, y)));
   }

   @Override
   public void update(int tileX, int tileY, boolean concurrently) {
      this.update(tileX - this.maxLightDistance, tileY - this.maxLightDistance, tileX + this.maxLightDistance, tileY + this.maxLightDistance, concurrently);
   }

   @Override
   public void update(int startX, int startY, int endX, int endY, boolean concurrently) {
      if (this.level.isLoadingComplete()) {
         long time = System.nanoTime();
         boolean minX = false;
         boolean maxX = false;
         boolean minY = false;
         boolean maxY = false;
         if (startX < this.area.startY) {
            startX = this.area.startX;
            minX = true;
         }

         if (endX > this.area.endX) {
            endX = this.area.endX;
            maxX = true;
         }

         if (startY < this.area.startY) {
            startY = this.area.startY;
            minY = true;
         }

         if (endY > this.area.endY) {
            endY = this.area.endY;
            maxY = true;
         }

         LinkedList<FastLightMap.LightComputeFuture> computes = new LinkedList<>();
         FastLightCompute edgeCompute = new FastLightCompute(this.level, this, startX, startY, endX, endY);
         computes.add(new FastLightMap.LightComputeFuture(edgeCompute));
         LightArea updateArea = concurrently ? new LightArea(startX, startY, endX, endY) : this.area;
         synchronized (updateArea) {
            updateArea.initLights();

            for (int i = startX; i <= endX; i++) {
               for (int j = startY; j <= endY; j++) {
                  if (i == startX && !minX || i == endX && !maxX || j == startY && !minY || j == endY && !maxY) {
                     GameLight currentLight = this.getLight(i, j);
                     if (currentLight.getLevel() > 0.0F) {
                        edgeCompute.addSource(i, j, currentLight);
                     }
                  } else {
                     GameLight newLight = this.getNewLight(i, j);
                     if (newLight.getLevel() > 0.0F) {
                        int maxLightDistance = (int)(newLight.getLevel() / 10.0F) + 1;
                        int cStartX = Math.max(startX, i - maxLightDistance);
                        int cStartY = Math.max(startY, j - maxLightDistance);
                        int cEndX = Math.min(endX, i + maxLightDistance);
                        int cEndY = Math.min(endY, j + maxLightDistance);
                        FastLightCompute compute = new FastLightCompute(this.level, this, cStartX, cStartY, cEndX, cEndY);
                        compute.addSource(i, j, newLight);
                        computes.add(new FastLightMap.LightComputeFuture(compute));
                     }
                  }

                  updateArea.lights[updateArea.getIndex(i, j)] = this.manager.newLight(0.0F);
               }
            }

            AtomicInteger computeIterations = new AtomicInteger();
            AtomicInteger applyIterations = new AtomicInteger();
            ExecutorService updateExecutor = this.manager.updateExecutor;
            if (updateExecutor != null) {
               int deltaX = endX - startX;
               int deltaY = endY - startY;
               if (concurrently) {
                  for (FastLightMap.LightComputeFuture lcf : computes) {
                     lcf.future = updateExecutor.submit(lcf.compute::compute);
                  }

                  updateExecutor.submit(
                     () -> {
                        this.waitForComputes(computes, computeIterations);

                        for (FastLightMap.LightComputeFuture lcfx : computes) {
                           if (lcfx.complete) {
                              applyIterations.addAndGet(lcfx.compute.apply(updateArea));
                           }
                        }

                        synchronized (this.area) {
                           updateArea.overwriteArea(this.area);
                        }

                        if (this.printDebug) {
                           System.out
                              .println(
                                 (this.level.isServer() ? "S" : "C")
                                    + ", C: "
                                    + computes.size()
                                    + ", DONE, I: "
                                    + computeIterations
                                    + ", a: "
                                    + applyIterations
                                    + ", "
                                    + deltaX
                                    + "x"
                                    + deltaY
                                    + ", "
                                    + GameUtils.getTimeStringNano(System.nanoTime() - time)
                              );
                        }

                        return null;
                     }
                  );
                  if (this.printDebug) {
                     System.out
                        .println(
                           (this.level.isServer() ? "S" : "C")
                              + ", C: "
                              + computes.size()
                              + ", WAITING, "
                              + deltaX
                              + "x"
                              + deltaY
                              + ", "
                              + GameUtils.getTimeStringNano(System.nanoTime() - time)
                        );
                  }
               } else {
                  if (this.printDebug) {
                     System.out
                        .println(
                           (this.level.isServer() ? "S" : "C")
                              + " START, C: "
                              + computes.size()
                              + ", "
                              + deltaX
                              + "x"
                              + deltaY
                              + ", "
                              + GameUtils.getTimeStringNano(System.nanoTime() - time)
                        );
                  }

                  for (FastLightMap.LightComputeFuture lcf : computes) {
                     computeIterations.addAndGet(lcf.compute.compute());
                     lcf.complete = true;
                  }

                  for (FastLightMap.LightComputeFuture lcf : computes) {
                     if (lcf.complete) {
                        applyIterations.addAndGet(lcf.compute.apply(updateArea));
                     }
                  }

                  if (this.printDebug) {
                     System.out
                        .println(
                           (this.level.isServer() ? "S" : "C")
                              + ", C: "
                              + computes.size()
                              + ", I: "
                              + computeIterations
                              + ", a: "
                              + applyIterations
                              + ", "
                              + deltaX
                              + "x"
                              + deltaY
                              + ", "
                              + GameUtils.getTimeStringNano(System.nanoTime() - time)
                        );
                  }
               }
            }
         }
      }
   }

   @Override
   public void update(Iterable<Point> tiles, boolean concurrently) {
      throw new UnsupportedOperationException("Cannot update using iterable and FastLightMap");
   }

   private void waitForComputes(LinkedList<FastLightMap.LightComputeFuture> futures, AtomicInteger iterations) {
      for (FastLightMap.LightComputeFuture lcf : futures) {
         try {
            iterations.addAndGet(lcf.future.get(5L, TimeUnit.SECONDS));
            lcf.complete = true;
         } catch (InterruptedException e) {
            lcf.complete = false;
         } catch (ExecutionException e) {
            lcf.complete = false;
            System.err.println("Error executing light update");
            e.printStackTrace();
         } catch (TimeoutException e) {
            lcf.complete = false;
            System.err.println("Timed out executing light update");
            e.printStackTrace();
         }
      }
   }

   protected abstract GameLight getNewLight(int var1, int var2);

   protected static class LightComputeFuture {
      public final FastLightCompute compute;
      public boolean complete;
      public Future<Integer> future;

      public LightComputeFuture(FastLightCompute compute) {
         this.compute = compute;
         this.complete = false;
      }
   }
}
