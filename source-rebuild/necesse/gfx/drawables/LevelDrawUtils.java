package necesse.gfx.drawables;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceTimer;
import necesse.engine.gameLoop.tickManager.PerformanceWrapper;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.gameTool.GameToolManager;
import necesse.engine.input.Input;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.network.client.Client;
import necesse.engine.util.GameMath;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.engine.world.WorldEntity;
import necesse.entity.Entity;
import necesse.entity.chains.Chain;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.manager.GroundPillarHandler;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.ImpossibleDrawException;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawOptions.texture.StaticShaderSprite2f;
import necesse.gfx.drawOptions.texture.StaticShaderSprite4f;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.gfx.ui.HUD;
import necesse.gfx.ui.debug.Debug;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.level.gameLogicGate.GameLogicGate;
import necesse.level.gameLogicGate.entities.LogicGateEntity;
import necesse.level.gameObject.GameObject;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.regionSystem.RegionType;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public class LevelDrawUtils {
   private static final int DRAW_THREADS = 4;
   private static final Queue<Long> lastSetupExceptions = new ArrayDeque<>();
   private static long timeOfLastSuccessfulSetup = -1L;
   private final Level level;
   private LevelDrawUtils.TrackedThreadPoolExecutor executor;
   private boolean drawPrepared = false;
   private LevelTileTerrainDrawOptions tileUnderLiquidDrawables;
   private LevelTileLiquidDrawOptions tileLiquidDrawables;
   private LevelTileTerrainDrawOptions tileOverLiquidDrawables;
   private LevelTileDamageDrawOptions tileDamageDrawables;
   private LevelTileLightDrawOptions tileLightDrawables;
   private SharedTextureDrawOptions logicDrawables;
   private SharedTextureDrawOptions wireDrawables;
   private OrderableDrawables objectTileDrawables;
   private OrderableDrawables entityTileDrawables;
   private OrderableDrawables entityTopDrawables;
   private OrderableDrawables overlayDrawables;
   private List<Drawable> wallShadowDrawables;
   private List<LevelSortedDrawable> sortedDrawables;
   private AtomicReference<Drawable> rainDrawable;
   private List<SortedDrawable> hudDrawables;
   private List<SortedDrawable> lastHudDrawables;
   private List<LevelDrawUtils.TrackedThreadPoolExecutor.TrackedFuture> setupLogic;
   private PerformanceTimer lastRoot;

   public LevelDrawUtils(Level level) {
      this.level = level;
      if (!GlobalData.isServer()) {
         this.resetDrawables();
      }

      this.executor = new LevelDrawUtils.TrackedThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(), this.defaultThreadFactory());
   }

   private void resetDrawables() {
      this.tileUnderLiquidDrawables = new LevelTileTerrainDrawOptions();
      this.tileLiquidDrawables = new LevelTileLiquidDrawOptions(this.level);
      this.tileOverLiquidDrawables = new LevelTileTerrainDrawOptions();
      this.tileDamageDrawables = new LevelTileDamageDrawOptions();
      this.tileLightDrawables = new LevelTileLightDrawOptions();
      this.logicDrawables = new SharedTextureDrawOptions(GameLogicGate.generatedLogicGateTexture);
      this.wireDrawables = new SharedTextureDrawOptions(GameResources.wire);
      this.objectTileDrawables = new OrderableDrawables(
         Collections.synchronizedNavigableMap(new TreeMap<>()), () -> Collections.synchronizedList(new ArrayList<>())
      );
      this.wallShadowDrawables = Collections.synchronizedList(new ArrayList<>());
      this.entityTileDrawables = new OrderableDrawables(
         Collections.synchronizedNavigableMap(new TreeMap<>()), () -> Collections.synchronizedList(new ArrayList<>())
      );
      this.entityTopDrawables = new OrderableDrawables(
         Collections.synchronizedNavigableMap(new TreeMap<>()), () -> Collections.synchronizedList(new ArrayList<>())
      );
      this.overlayDrawables = new OrderableDrawables(
         Collections.synchronizedNavigableMap(new TreeMap<>()), () -> Collections.synchronizedList(new ArrayList<>())
      );
      this.hudDrawables = Collections.synchronizedList(new ArrayList<>());
      this.sortedDrawables = Collections.synchronizedList(new ArrayList<>());
      this.rainDrawable = new AtomicReference<>(tm -> {});
      this.setupLogic = Collections.synchronizedList(new ArrayList<>());
   }

   private ThreadFactory defaultThreadFactory() {
      AtomicInteger threadNum = new AtomicInteger(0);
      return r -> {
         Thread thread = new Thread(null, r, "level-" + this.level.getHostString() + "-" + this.level.getIdentifier() + "-draw-" + threadNum.incrementAndGet());
         thread.setDaemon(true);
         return thread;
      };
   }

   public void dispose() {
      if (this.executor != null) {
         this.executor.shutdownNow();
      }

      this.executor = null;
   }

   private LevelDrawUtils.TrackedThreadPoolExecutor.TrackedFuture executeLogic(TickManager tickManager, Consumer<TickManager> logic) {
      TickManager child = tickManager == null ? null : tickManager.getChild();
      return this.executor.submitTrackedTask(() -> logic.accept(child));
   }

   private LevelDrawUtils.TrackedThreadPoolExecutor.TrackedFuture addExecuteList(
      List<LevelDrawUtils.TrackedThreadPoolExecutor.TrackedFuture> list, TickManager tickManager, Consumer<TickManager> logic
   ) {
      LevelDrawUtils.TrackedThreadPoolExecutor.TrackedFuture out = this.executeLogic(tickManager, logic);
      list.add(out);
      return out;
   }

   private void awaitExecuteRaw(LevelDrawUtils.TrackedThreadPoolExecutor.TrackedFuture trackedFuture) throws InterruptedException, ExecutionException, LevelDrawUtils.ThreadTimeOutException {
      try {
         trackedFuture.future.get(2L, TimeUnit.SECONDS);
      } catch (TimeoutException e) {
         throw new LevelDrawUtils.ThreadTimeOutException(trackedFuture.thread);
      }
   }

   private void awaitExecuteList(List<LevelDrawUtils.TrackedThreadPoolExecutor.TrackedFuture> list) {
      int index = 0;
      Exception exception = null;

      while (index < list.size()) {
         try {
            this.awaitExecuteRaw(list.get(index));
         } catch (InterruptedException | ExecutionException | LevelDrawUtils.ThreadTimeOutException e) {
            if (exception == null) {
               exception = e;
            }
         }

         index++;
      }

      if (exception != null) {
         throw new RuntimeException(exception.getClass().getName(), exception);
      }
   }

   @SafeVarargs
   private final void runParallel(TickManager tickManager, Consumer<TickManager>... logic) {
      ArrayList<LevelDrawUtils.TrackedThreadPoolExecutor.TrackedFuture> futures = new ArrayList<>();

      for (Consumer<TickManager> f : logic) {
         this.addExecuteList(futures, tickManager, f);
      }

      this.awaitExecuteList(futures);
   }

   private void runParallel(TickManager tickManager, List<Consumer<TickManager>> logic) {
      this.runParallel(tickManager, logic.toArray(new Consumer[0]));
   }

   private void addEntityDrawProcesses(
      TickManager tickManager,
      GameCamera camera,
      LevelDrawUtils.DrawArea area,
      List<LevelSortedDrawable> sortedDrawables,
      OrderableDrawables tileDrawables,
      OrderableDrawables topDrawables,
      OrderableDrawables overlayDrawables,
      PlayerMob perspective
   ) {
      this.addExecuteList(
         this.setupLogic,
         tickManager,
         tm -> Performance.record(
            tm,
            "particleSetup",
            () -> this.level.entityManager.particleOptions.addDrawables(sortedDrawables, tileDrawables, topDrawables, this.level, tm, camera, perspective)
         )
      );
      boolean isDebuggingOn = GlobalData.debugActive();
      this.addExecuteList(this.setupLogic, tickManager, tm -> Performance.record(tm, "entitySetup", () -> {
         int startRegionY = this.level.regionManager.getRegionYByTileLimited(area.startTileY);
         int endRegionY = this.level.regionManager.getRegionYByTileLimited(area.endTileY);
         int startRegionX = this.level.regionManager.getRegionXByTileLimited(area.startTileX);
         int endRegionX = this.level.regionManager.getRegionXByTileLimited(area.endTileX);

         for (int j = endRegionY; j >= startRegionY; j--) {
            for (int i = startRegionX; i <= endRegionX; i++) {
               for (Entity entity : this.level.entityManager.getRegionDrawEntities(i, j)) {
                  if (area.isIn(entity)) {
                     entity.addDrawables(sortedDrawables, tileDrawables, topDrawables, overlayDrawables, this.level, tm, camera, perspective);
                     if (isDebuggingOn) {
                        entity.addDebugDrawables(sortedDrawables, tileDrawables, topDrawables, overlayDrawables, this.level, tm, camera, perspective);
                     }
                  }
               }
            }
         }
      }));
   }

   private void addTrailDrawProcesses(
      TickManager tickManager, GameCamera camera, LevelDrawUtils.DrawArea area, List<LevelSortedDrawable> sortedDrawables, OrderableDrawables topDrawables
   ) {
      this.addExecuteList(this.setupLogic, tickManager, tm -> Performance.record(tm, "trailSetup", () -> {
         synchronized (this.level.entityManager.trails) {
            for (Trail t : this.level.entityManager.trails) {
               if (t.drawOnTop) {
                  t.addDrawables(topDrawables, area.startTileY, area.endTileY, tm, camera);
               } else {
                  t.addDrawables(sortedDrawables, area.startTileY, area.endTileY, tm, camera);
               }
            }
         }
      }));
   }

   private void addChainDrawProcesses(
      TickManager tickManager, GameCamera camera, LevelDrawUtils.DrawArea area, List<LevelSortedDrawable> sortedDrawables, OrderableDrawables topDrawables
   ) {
      this.addExecuteList(this.setupLogic, tickManager, tm -> Performance.record(tm, "chainSetup", () -> {
         synchronized (this.level.entityManager.chains) {
            for (Chain c : this.level.entityManager.chains) {
               if (c.drawOnTop) {
                  c.addDrawables(topDrawables, area.startTileY, area.endTileY, this.level, tm, camera);
               } else {
                  c.addDrawables(sortedDrawables, area.startTileY, area.endTileY, this.level, tm, camera);
               }
            }
         }
      }));
   }

   private void addGroundPillarHandlers(
      TickManager tickManager, GameCamera camera, LevelDrawUtils.DrawArea area, List<LevelSortedDrawable> sortedDrawables, PlayerMob perspective
   ) {
      this.addExecuteList(this.setupLogic, tickManager, tm -> Performance.record(tm, "groundPillarSetup", () -> {
         synchronized (this.level.entityManager.pillarHandlers) {
            for (GroundPillarHandler<?> handler : this.level.entityManager.pillarHandlers) {
               handler.addDrawables(sortedDrawables, area, this.level, tm, camera, perspective);
            }
         }
      }));
   }

   private void addLevelEventDrawProcesses(
      TickManager tickManager,
      GameCamera camera,
      LevelDrawUtils.DrawArea area,
      List<LevelSortedDrawable> sortedDrawables,
      OrderableDrawables tileDrawables,
      OrderableDrawables topDrawables
   ) {
      this.addExecuteList(this.setupLogic, tickManager, tm -> Performance.record(tm, "eventSetup", () -> {
         synchronized (this.level.entityManager.lock) {
            for (LevelEvent event : this.level.entityManager.events) {
               event.addDrawables(sortedDrawables, tileDrawables, topDrawables, area, this.level, tm, camera);
            }
         }

         WorldEntity worldEntity = this.level.getWorldEntity();
         if (worldEntity != null) {
            worldEntity.addWorldEventDrawables(tm, camera, area, this.level, sortedDrawables, tileDrawables, topDrawables);
         }
      }));
   }

   private void addTileBasedDrawProcesses(
      TickManager tickManager,
      GameCamera camera,
      LevelDrawUtils.DrawArea tileArea,
      LevelDrawUtils.DrawArea objectArea,
      LevelTileTerrainDrawOptions underLiquidDrawables,
      LevelTileLiquidDrawOptions liquidDrawables,
      LevelTileTerrainDrawOptions overLiquidDrawables,
      LevelTileDamageDrawOptions tileDamageDrawables,
      LevelTileLightDrawOptions lightDrawables,
      SharedTextureDrawOptions logicDrawables,
      SharedTextureDrawOptions wireDrawables,
      OrderableDrawables objectTileDrawables,
      List<LevelSortedDrawable> objectDrawables,
      boolean drawWire,
      PlayerMob perspective
   ) {
      int rowSize = 16;
      int rows = (objectArea.endTileY - objectArea.startTileY) / rowSize;

      for (int i = 0; i <= rows; i++) {
         int finalI = i;
         this.addExecuteList(
            this.setupLogic,
            tickManager,
            tm -> Performance.record(
               tm,
               "tileSetup",
               () -> {
                  int rowY = objectArea.startTileY + finalI * rowSize;

                  for (int y = Math.min(rowY + rowSize - 1, objectArea.endTileY); y >= rowY; y--) {
                     for (int x = objectArea.startTileX; x <= objectArea.endTileX; x++) {
                        GameObject obj = this.level
                           .objectLayer
                           .addObjectDrawables(objectDrawables, objectTileDrawables, this.level, x, y, tm, camera, perspective);
                        if (x >= tileArea.startTileX && x <= tileArea.endTileX && y >= tileArea.startTileY && y <= tileArea.endTileY) {
                           GameTile tile = this.level.getTile(x, y);
                           if (!obj.drawsFullTile()) {
                              tile.addDrawables(
                                 underLiquidDrawables, liquidDrawables, overLiquidDrawables, objectTileDrawables, objectDrawables, this.level, x, y, camera, tm
                              );
                              tileDamageDrawables.addDamage(tm, tile, this.level, x, y, camera);
                           }

                           tile.addLightDrawables(lightDrawables, this.level, x, y, camera, tm);
                           if (Settings.smoothLighting) {
                              if (x == tileArea.startTileX) {
                                 tile.addLightDrawables(lightDrawables, this.level, x - 1, y, camera, tm);
                                 if (y == tileArea.startTileY) {
                                    tile.addLightDrawables(lightDrawables, this.level, x - 1, y - 1, camera, tm);
                                 }
                              }

                              if (y == tileArea.startTileY) {
                                 tile.addLightDrawables(lightDrawables, this.level, x, y - 1, camera, tm);
                              }
                           }

                           if (drawWire) {
                              if (this.level.logicLayer.hasGate(x, y)) {
                                 LogicGateEntity entity = this.level.logicLayer.getEntity(x, y);
                                 if (entity != null) {
                                    entity.getLogicGate().addDrawables(logicDrawables, this.level, x, y, entity, tm, camera);
                                 }
                              }

                              this.level.wireManager.addWireDrawables(wireDrawables, x, y, camera, tm);
                           }
                        }
                     }
                  }
               }
            )
         );
      }
   }

   private void addWallShadowDrawables(TickManager tickManager, GameCamera camera, LevelDrawUtils.DrawArea tileArea, List<Drawable> wallShadowDrawables) {
      this.addExecuteList(
         this.setupLogic,
         tickManager,
         tm -> {
            if (!this.level.isCave) {
               Performance.record(
                  tm,
                  "wallShadowSetup",
                  () -> {
                     LinkedList<WallShadowVariables> wallShadows = this.level
                        .getWallShadows()
                        .filter(s -> s.lightLevel > 0.0F && s.range > 0.0F)
                        .collect(Collectors.toCollection(LinkedList::new));
                     if (!wallShadows.isEmpty()) {
                        int startTileX = tileArea.startTileX;
                        int endTileX = tileArea.endTileX;
                        int startTileY = tileArea.startTileY;
                        int endTileY = tileArea.endTileY;

                        for (WallShadowVariables wallShadow : wallShadows) {
                           wallShadow.calculate();
                           int dirXOffsetCeil = (int)Math.ceil(Math.abs(wallShadow.dirXOffset / 32.0F));
                           if (wallShadow.dirXOffset > 0.0F) {
                              startTileX = this.level.limitTileXToBounds(startTileX - dirXOffsetCeil);
                           }

                           if (wallShadow.dirXOffset < 0.0F) {
                              endTileX = this.level.limitTileXToBounds(endTileX + dirXOffsetCeil);
                           }

                           int dirYOffsetCeil = (int)Math.ceil(Math.abs(wallShadow.dirYOffset / 32.0F));
                           if (wallShadow.dirYOffset > 0.0F) {
                              startTileY = this.level.limitTileYToBounds(startTileY - dirYOffsetCeil);
                           }

                           if (wallShadow.dirYOffset < 0.0F) {
                              endTileY = this.level.limitTileYToBounds(endTileY + dirYOffsetCeil);
                           }
                        }

                        HashSet<Point> roofs = new HashSet<>();

                        for (int y = startTileY; y <= endTileY; y++) {
                           for (int x = startTileX; x <= endTileX; x++) {
                              if (!this.level.isOutside(x, y)) {
                                 roofs.add(new Point(x, y));
                              }
                           }
                        }

                        CustomGLDrawOptionsList fillDrawOptions = new CustomGLDrawOptionsList();
                        fillDrawOptions.setupRunnable = () -> GL14.glBlendEquation(32776);

                        for (WallShadowVariables wallShadow : wallShadows) {
                           for (Point tile : roofs) {
                              int drawX = camera.getTileDrawX(tile.x);
                              int drawY = camera.getTileDrawY(tile.y);
                              int width = 32;
                              int height = 32;
                              fillDrawOptions.add(() -> {
                                 GL11.glColor4f(0.0F, 0.0F, 0.0F, wallShadow.startAlpha);
                                 GL11.glVertex2f(drawX, drawY);
                                 GL11.glVertex2f(drawX + width, drawY);
                                 GL11.glVertex2f(drawX + width, drawY + height);
                                 GL11.glVertex2f(drawX, drawY + height);
                              });
                              if (wallShadow.east) {
                                 Point eastTile = new Point(tile.x + 1, tile.y);
                                 if (!roofs.contains(eastTile)) {
                                    fillDrawOptions.add(() -> {
                                       GL11.glColor4f(0.0F, 0.0F, 0.0F, wallShadow.startAlpha);
                                       GL11.glVertex2f(drawX + width, drawY);
                                       GL11.glVertex2f(drawX + width, drawY + height);
                                       GL11.glColor4f(0.0F, 0.0F, 0.0F, wallShadow.endAlpha);
                                       GL11.glVertex2f(drawX + width + wallShadow.dirXOffset, drawY + height + wallShadow.dirYOffset);
                                       GL11.glVertex2f(drawX + width + wallShadow.dirXOffset, drawY + wallShadow.dirYOffset);
                                    });
                                 }
                              }

                              if (wallShadow.south) {
                                 Point southTile = new Point(tile.x, tile.y + 1);
                                 if (!roofs.contains(southTile)) {
                                    fillDrawOptions.add(() -> {
                                       GL11.glColor4f(0.0F, 0.0F, 0.0F, wallShadow.startAlpha);
                                       GL11.glVertex2f(drawX, drawY + height);
                                       GL11.glVertex2f(drawX + width, drawY + height);
                                       GL11.glColor4f(0.0F, 0.0F, 0.0F, wallShadow.endAlpha);
                                       GL11.glVertex2f(drawX + width + wallShadow.dirXOffset, drawY + height + wallShadow.dirYOffset);
                                       GL11.glVertex2f(drawX + wallShadow.dirXOffset, drawY + height + wallShadow.dirYOffset);
                                    });
                                 }
                              }

                              if (wallShadow.west) {
                                 Point westTile = new Point(tile.x - 1, tile.y);
                                 if (!roofs.contains(westTile)) {
                                    fillDrawOptions.add(() -> {
                                       GL11.glColor4f(0.0F, 0.0F, 0.0F, wallShadow.startAlpha);
                                       GL11.glVertex2f(drawX, drawY);
                                       GL11.glVertex2f(drawX, drawY + height);
                                       GL11.glColor4f(0.0F, 0.0F, 0.0F, wallShadow.endAlpha);
                                       GL11.glVertex2f(drawX + wallShadow.dirXOffset, drawY + height + wallShadow.dirYOffset);
                                       GL11.glVertex2f(drawX + wallShadow.dirXOffset, drawY + wallShadow.dirYOffset);
                                    });
                                 }
                              }

                              if (wallShadow.north) {
                                 Point northTile = new Point(tile.x, tile.y - 1);
                                 if (!roofs.contains(northTile)) {
                                    fillDrawOptions.add(() -> {
                                       GL11.glColor4f(0.0F, 0.0F, 0.0F, wallShadow.startAlpha);
                                       GL11.glVertex2f(drawX, drawY);
                                       GL11.glVertex2f(drawX + width, drawY);
                                       GL11.glColor4f(0.0F, 0.0F, 0.0F, wallShadow.endAlpha);
                                       GL11.glVertex2f(drawX + width + wallShadow.dirXOffset, drawY + wallShadow.dirYOffset);
                                       GL11.glVertex2f(drawX + wallShadow.dirXOffset, drawY + wallShadow.dirYOffset);
                                    });
                                 }
                              }
                           }
                        }

                        CustomGLDrawOptionsList removeDrawOptions = new CustomGLDrawOptionsList();
                        removeDrawOptions.setupRunnable = () -> {
                           GL14.glBlendEquation(32774);
                           GL11.glBlendFunc(770, 0);
                           GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.0F);
                        };

                        for (WallShadowVariables wallShadow : wallShadows) {
                           for (Point tile : roofs) {
                              int tileDrawX = camera.getTileDrawX(tile.x);
                              int tileDrawY = camera.getTileDrawY(tile.y);
                              if (!roofs.contains(new Point(tile.x, tile.y + 1))) {
                                 if (!roofs.contains(new Point(tile.x, tile.y - 1))) {
                                    continue;
                                 }

                                 RegionType northType = this.level.regionManager.getRegionTypeByTile(tile.x, tile.y - 1);
                                 if (northType == RegionType.WALL || northType == RegionType.DOOR) {
                                    continue;
                                 }
                              }

                              if (!wallShadow.west && !wallShadow.east
                                 || roofs.contains(new Point(tile.x - 1, tile.y)) && roofs.contains(new Point(tile.x + 1, tile.y))) {
                                 removeDrawOptions.add(() -> {
                                    GL11.glVertex2f(tileDrawX, tileDrawY);
                                    GL11.glVertex2f(tileDrawX + 32, tileDrawY);
                                    GL11.glVertex2f(tileDrawX + 32, tileDrawY + 32);
                                    GL11.glVertex2f(tileDrawX, tileDrawY + 32);
                                 });
                              }
                           }
                        }

                        wallShadowDrawables.add(tm2 -> {
                           fillDrawOptions.draw(7);
                           removeDrawOptions.draw(7);
                        });
                     }
                  }
               );
            }
         }
      );
   }

   private void addRainDrawProcesses(
      TickManager tickManager, AtomicReference<Drawable> drawable, GameCamera camera, LevelDrawUtils.DrawArea area, long globalTime
   ) {
      this.addExecuteList(
         this.setupLogic,
         tickManager,
         tm1 -> Performance.record(
            tm1,
            "rainSetup",
            () -> {
               float rainAlpha = this.level.weatherLayer.getRainAlpha();
               if (!(rainAlpha <= 0.0F)) {
                  List<Consumer<TickManager>> rainProcessList = new ArrayList<>();
                  SharedTextureDrawOptions drawables = new SharedTextureDrawOptions(Biome.generatedRainTexture);
                  int rowSize = 16;
                  int rows = (area.endTileY - area.startTileY) / rowSize;

                  for (int i = 0; i <= rows; i++) {
                     int finalI = i;
                     rainProcessList.add(
                        tm2 -> {
                           int rowY = area.startTileY + finalI * rowSize;
                           double xSpeedDivider = 5000.0;
                           double ySpeedDivider = 1000.0;
                           double xOffset = globalTime / xSpeedDivider % 1.0;
                           xOffset = 1.0 - xOffset;
                           double yOffset = globalTime / ySpeedDivider % 1.0;
                           yOffset = 1.0 - yOffset;
                           float floatXOffset = (float)xOffset;
                           float floatYOffset = (float)yOffset;

                           for (int tileY = Math.min(rowY + rowSize - 1, area.endTileY); tileY >= rowY; tileY--) {
                              int drawY = camera.getTileDrawY(tileY);

                              for (int tileX = area.startTileX; tileX <= area.endTileX; tileX++) {
                                 if (this.level.isOutside(tileX, tileY)) {
                                    int drawX = camera.getTileDrawX(tileX);
                                    Biome biome = this.level.getBiome(tileX, tileY);
                                    if (biome.canRain(this.level)) {
                                       GameTextureSection texture = biome.getRainTexture(this.level, tileX, tileY);
                                       Color rainColor = biome.getRainColor(this.level, tileX, tileY);
                                       int textureTilesX = GameMath.getTileCoordinate(texture.getWidth());
                                       int textureTilesY = GameMath.getTileCoordinate(texture.getHeight());
                                       float red = rainColor.getRed() / 255.0F;
                                       float green = rainColor.getGreen() / 255.0F;
                                       float blue = rainColor.getBlue() / 255.0F;
                                       float alpha = rainColor.getAlpha() / 255.0F * rainAlpha;
                                       int rainTileY = Math.floorMod(tileY, textureTilesY);
                                       int startY = rainTileY * 32;
                                       int rainTileX = Math.floorMod(tileX, textureTilesX);
                                       int startX = rainTileX * 32;
                                       StaticShaderSprite4f textureBounds = new StaticShaderSprite4f(
                                          1,
                                          TextureDrawOptions.pixel(texture.getStartX(), Biome.generatedRainTexture.getWidth()),
                                          TextureDrawOptions.pixel(texture.getStartY(), Biome.generatedRainTexture.getHeight()),
                                          TextureDrawOptions.pixel(texture.getEndX(), Biome.generatedRainTexture.getWidth()),
                                          TextureDrawOptions.pixel(texture.getEndY(), Biome.generatedRainTexture.getHeight())
                                       );
                                       drawables.add(texture.section(startX, startX + 32, startY, startY + 32))
                                          .addShaderSprite(textureBounds)
                                          .addShaderSprite(new StaticShaderSprite2f(2, floatXOffset, floatYOffset))
                                          .colorLight(red, green, blue, alpha, this.level.getLightLevel(tileX, tileY))
                                          .size(32, 32)
                                          .pos(drawX, drawY);
                                    }
                                 }
                              }
                           }
                        }
                     );
                  }

                  this.runParallel(tm1, rainProcessList);
                  drawable.set(tm -> {
                     GameResources.rainShader.use();
                     drawables.draw();
                     GameResources.rainShader.stop();
                  });
               }
            }
         )
      );
   }

   public void addHudDrawProcesses(TickManager tickManager, List<SortedDrawable> hudDrawables, GameCamera camera, PlayerMob perspective) {
      this.addExecuteList(this.setupLogic, tickManager, tm -> Performance.record(tm, "hudSetup", () -> {
         this.level.hudManager.addDrawables(hudDrawables, camera, perspective);
         final DrawOptions options;
         if (perspective != null) {
            PlayerInventorySlot slot = perspective.getSelectedItemSlot();
            InventoryItem selectedItem = slot.getInv(perspective.getInv()).getItem(slot.slot);
            if (selectedItem != null && selectedItem.item.isPlaceable()) {
               options = () -> {
                  Point levelPos;
                  if (Input.lastInputIsController && !ControllerInput.isCursorVisible()) {
                     Float aimDir = perspective.getControllerAimDir();
                     levelPos = selectedItem.item.getControllerAttackLevelPos(this.level, aimDir.x, aimDir.y, perspective, selectedItem);
                  } else {
                     levelPos = new Point(camera.getMouseLevelPosX(), camera.getMouseLevelPosY());
                  }

                  selectedItem.item.getPlaceable().drawPlacePreview(this.level, levelPos.x, levelPos.y, camera, perspective, selectedItem, slot);
               };
            } else {
               options = null;
            }
         } else {
            options = null;
         }

         hudDrawables.add(new SortedDrawable() {
            @Override
            public int getPriority() {
               return Integer.MIN_VALUE;
            }

            @Override
            public void draw(TickManager tickManager) {
               if (options != null) {
                  options.draw();
               }
            }
         });
         final DrawOptions storyObjective;
         if (perspective != null && perspective.isClient()) {
            Client client = perspective.getClient();
            storyObjective = client.storyManager.getCurrentObjectiveLevelHudDrawOptions(camera, perspective);
         } else {
            storyObjective = null;
         }

         hudDrawables.add(new SortedDrawable() {
            @Override
            public int getPriority() {
               return Integer.MAX_VALUE;
            }

            @Override
            public void draw(TickManager tickManager) {
               if (storyObjective != null) {
                  storyObjective.draw();
               }

               HUD.draw(LevelDrawUtils.this.level, camera, perspective, tickManager);
               Debug.drawHUD(LevelDrawUtils.this.level, camera, perspective);
            }
         });
      }));
   }

   public boolean prepareDraw(GameCamera camera, PlayerMob perspective, TickManager tickManager, boolean singleDraw) {
      this.drawPrepared = true;
      GameCamera fCamera = new GameCamera(camera.getX(), camera.getY(), camera.getWidth(), camera.getHeight());
      boolean success = Performance.record(
         tickManager,
         "prepareLevelDraw",
         () -> {
            if (singleDraw || this.setupLogic.isEmpty()) {
               this.setupNextLogic(fCamera, perspective, tickManager, singleDraw);
            }

            PerformanceWrapper awaitTimer = Performance.wrapTimer(tickManager, "awaitLast");

            try {
               this.awaitExecuteList(this.setupLogic);
            } catch (Exception e) {
               Throwable error = e;

               while (
                  error != null
                     && !(error instanceof ConcurrentModificationException)
                     && !(error instanceof NullPointerException)
                     && !(error instanceof LevelDrawUtils.ThreadTimeOutException)
               ) {
                  Throwable next = error.getCause();
                  if (next == error) {
                     break;
                  }

                  error = next;
               }

               if (error != null) {
                  if (timeOfLastSuccessfulSetup == -1L) {
                     timeOfLastSuccessfulSetup = System.currentTimeMillis();
                  }

                  lastSetupExceptions.removeIf(x -> timeOfLastSuccessfulSetup - x >= 10000L);
                  lastSetupExceptions.add(System.currentTimeMillis());
                  if (lastSetupExceptions.size() >= 4) {
                     throw e;
                  }

                  GameLog.err.println("Detected an error under rendering: " + error.getClass().getSimpleName() + " (" + lastSetupExceptions.size() + ")");
                  e.printStackTrace(GameLog.err);
                  this.setupLogic = Collections.synchronizedList(new ArrayList<>());
                  return false;
               }

               throw e;
            } finally {
               awaitTimer.end();
            }

            return true;
         }
      );
      if (success) {
         timeOfLastSuccessfulSetup = System.currentTimeMillis();
      }

      return success;
   }

   public void draw(GameCamera camera, PlayerMob perspective, TickManager tickManager, boolean singleDraw) {
      if (!this.drawPrepared) {
         GameLog.warn.println("Level draw was not prepared before calling draw(...). Call prepareDraw() first.");
         if (!this.prepareDraw(camera, perspective, tickManager, singleDraw)) {
            return;
         }
      }

      this.drawPrepared = false;
      GameCamera fCamera = new GameCamera(camera.getX(), camera.getY(), camera.getWidth(), camera.getHeight());
      Performance.record(tickManager, "levelDraw", () -> {
         Performance.record(tickManager, "sortDraws", () -> {
            try {
               this.sortedDrawables.sort(null);
               this.hudDrawables.sort(null);
            } catch (Exception e) {
               ImpossibleDrawException.submitDrawError(e);
            }
         });
         LevelTileTerrainDrawOptions tileUnderLiquidDrawables = this.tileUnderLiquidDrawables;
         LevelTileLiquidDrawOptions tileLiquidDrawables = this.tileLiquidDrawables;
         LevelTileTerrainDrawOptions tileOverLiquidDrawables = this.tileOverLiquidDrawables;
         LevelTileDamageDrawOptions tileDamageDrawables = this.tileDamageDrawables;
         LevelTileLightDrawOptions tileLightDrawables = this.tileLightDrawables;
         SharedTextureDrawOptions logicDrawables = this.logicDrawables;
         SharedTextureDrawOptions wireDrawables = this.wireDrawables;
         OrderableDrawables objectTileDrawables = this.objectTileDrawables;
         List<Drawable> wallShadowDrawables = this.wallShadowDrawables;
         OrderableDrawables entityTileDrawables = this.entityTileDrawables;
         OrderableDrawables entityTopDrawables = this.entityTopDrawables;
         OrderableDrawables overlayDrawables = this.overlayDrawables;
         this.lastHudDrawables = this.hudDrawables;
         List<LevelSortedDrawable> sortedDrawables = this.sortedDrawables;
         AtomicReference<Drawable> rainDrawable = this.rainDrawable;
         if (!singleDraw) {
            this.setupNextLogic(fCamera, perspective, tickManager, singleDraw);
         }

         GameWindow window = WindowManager.getWindow();
         Performance.record(tickManager, "draw", () -> {
            Performance.record(tickManager, "tileDraw", () -> {
               try {
                  tileUnderLiquidDrawables.draw();
               } catch (Exception e) {
                  ImpossibleDrawException.submitDrawError(e);
               }

               GameResources.liquidShader.use(this.level);

               try {
                  tileLiquidDrawables.draw();
               } catch (Exception e) {
                  ImpossibleDrawException.submitDrawError(e);
               } finally {
                  GameResources.liquidShader.stop();
               }

               try {
                  tileOverLiquidDrawables.draw();
               } catch (Exception e) {
                  ImpossibleDrawException.submitDrawError(e);
               }

               try {
                  tileDamageDrawables.draw();
               } catch (Exception e) {
                  ImpossibleDrawException.submitDrawError(e);
               }

               GL14.glBlendFuncSeparate(0, 768, 0, 770);
               GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
               tileLightDrawables.draw();
               GL14.glBlendFuncSeparate(770, 771, 1, 771);
            });
            Performance.record(tickManager, "oTileDraw", () -> {
               try {
                  objectTileDrawables.draw(tickManager);
               } catch (Exception e) {
                  ImpossibleDrawException.submitDrawError(e);
               }
            });
            Performance.record(tickManager, "eTileDraw", () -> {
               try {
                  entityTileDrawables.forEach(e -> e.draw(tickManager));
               } catch (Exception e) {
                  ImpossibleDrawException.submitDrawError(e);
               }
            });
            Performance.record(tickManager, "wallShadowDraw", () -> window.applyDraw(() -> wallShadowDrawables.forEach(e -> e.draw(tickManager)), () -> {
               GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
               GL14.glBlendFuncSeparate(770, 771, 1, 771);
            }, null));
            Performance.record(tickManager, "sortedDraw", () -> {
               try {
                  sortedDrawables.forEach(e -> e.draw(tickManager));
               } catch (Exception e) {
                  ImpossibleDrawException.submitDrawError(e);
               }
            });
            Performance.record(tickManager, "wireDraw", () -> {
               try {
                  wireDrawables.draw();
               } catch (Exception e) {
                  ImpossibleDrawException.submitDrawError(e);
               }
            });
            Performance.record(tickManager, "logicDraw", () -> {
               try {
                  logicDrawables.draw();
               } catch (Exception e) {
                  ImpossibleDrawException.submitDrawError(e);
               }
            });
            Performance.record(tickManager, "eTopDraw", () -> {
               try {
                  entityTopDrawables.forEach(e -> e.draw(tickManager));
                  overlayDrawables.forEach(e -> e.draw(tickManager));
               } catch (Exception e) {
                  ImpossibleDrawException.submitDrawError(e);
               }
            });
            Performance.record(tickManager, "rainDraw", () -> {
               try {
                  if (rainDrawable.get() != null) {
                     rainDrawable.get().draw(tickManager);
                  }
               } catch (Exception e) {
                  ImpossibleDrawException.submitDrawError(e);
               }
            });
         });
      });
   }

   public void drawLastHudDrawables(GameCamera camera, PlayerMob perspective, TickManager tickManager) {
      List<SortedDrawable> lastHudDrawables = this.lastHudDrawables;
      if (lastHudDrawables != null) {
         Performance.record(tickManager, "levelHud", () -> lastHudDrawables.forEach(e -> e.draw(tickManager)));
      }
   }

   public boolean drawWire(PlayerMob perspective) {
      if (this.level.alwaysDrawWire) {
         return true;
      } else if (GameToolManager.doesToolShowWires()) {
         return true;
      } else if (this.level.isTrialRoom && !GlobalData.isDevMode() && !GlobalData.debugCheatActive()) {
         return false;
      } else {
         return GlobalData.debugActive()
            ? true
            : perspective != null && perspective.getSelectedItem() != null && perspective.getSelectedItem().item.showWires();
      }
   }

   private void setupNextLogic(GameCamera camera, PlayerMob perspective, TickManager tickManager, boolean singleDraw) {
      this.lastRoot = tickManager == null ? null : tickManager.getCurrentRootPerformanceTimer();
      this.level.entityManager.updateParticlesAllowed(camera);
      LevelDrawUtils.DrawArea tileArea = new LevelDrawUtils.DrawArea(this.level, camera);
      LevelDrawUtils.DrawArea objectArea = new LevelDrawUtils.DrawArea(
         this.level, tileArea.startTileX - 2, tileArea.endTileX + 2, tileArea.startTileY - 1, tileArea.endTileY + 3
      );
      LevelDrawUtils.DrawArea entityArea = new LevelDrawUtils.DrawArea(
         this.level, objectArea.startTileX - 2, objectArea.endTileX + 2, objectArea.startTileY - 3, objectArea.endTileY + 7
      );
      List<LevelDrawUtils.TrackedThreadPoolExecutor.TrackedFuture> pLightSetup = new LinkedList<>();
      this.addExecuteList(pLightSetup, tickManager, tm -> Performance.record(tm, "pLight", () -> {
         if (this.level.tickManager().isGameTick() || singleDraw) {
            this.level.lightManager.setDrawArea(objectArea.startTileX, objectArea.startTileY, objectArea.endTileX, objectArea.endTileY);
         }
      }));
      boolean drawWire = this.drawWire(perspective);
      this.resetDrawables();
      this.addExecuteList(
         this.setupLogic,
         tickManager,
         tm -> {
            this.awaitExecuteList(pLightSetup);
            Performance.record(
               tm,
               "setup",
               () -> {
                  this.addTileBasedDrawProcesses(
                     tm,
                     camera,
                     tileArea,
                     objectArea,
                     this.tileUnderLiquidDrawables,
                     this.tileLiquidDrawables,
                     this.tileOverLiquidDrawables,
                     this.tileDamageDrawables,
                     this.tileLightDrawables,
                     this.logicDrawables,
                     this.wireDrawables,
                     this.objectTileDrawables,
                     this.sortedDrawables,
                     drawWire,
                     perspective
                  );
                  this.addWallShadowDrawables(tm, camera, tileArea, this.wallShadowDrawables);
                  this.addEntityDrawProcesses(
                     tm, camera, entityArea, this.sortedDrawables, this.entityTileDrawables, this.entityTopDrawables, this.overlayDrawables, perspective
                  );
                  this.addTrailDrawProcesses(tm, camera, objectArea, this.sortedDrawables, this.entityTopDrawables);
                  this.addChainDrawProcesses(tm, camera, objectArea, this.sortedDrawables, this.entityTopDrawables);
                  this.addGroundPillarHandlers(tm, camera, objectArea, this.sortedDrawables, perspective);
                  this.addLevelEventDrawProcesses(tm, camera, objectArea, this.sortedDrawables, this.entityTileDrawables, this.entityTopDrawables);
                  this.addRainDrawProcesses(tm, this.rainDrawable, camera, tileArea, this.level.getWorldEntity().getTime());
                  this.addHudDrawProcesses(tm, this.hudDrawables, camera, perspective);
               }
            );
         }
      );
   }

   public static class DrawArea {
      public final int startTileX;
      public final int endTileX;
      public final int startTileY;
      public final int endTileY;

      public DrawArea(Level level, GameCamera camera) {
         this(
            level,
            GameMath.getTileCoordinate(camera.getX() - 16),
            camera.getEndTileX() + 1,
            GameMath.getTileCoordinate(camera.getY() - 16),
            camera.getEndTileY() + 1
         );
      }

      public DrawArea(Level level, int startTileX, int endTileX, int startTileY, int endTileY) {
         this.startTileX = level.limitTileXToBounds(startTileX);
         this.endTileX = Math.max(level.limitTileXToBounds(endTileX), this.startTileX);
         this.startTileY = level.limitTileYToBounds(startTileY);
         this.endTileY = Math.max(level.limitTileYToBounds(endTileY), this.startTileY);
      }

      public boolean isIn(int tileX, int tileY) {
         return tileX >= this.startTileX && tileX <= this.endTileX && tileY >= this.startTileY && tileY <= this.endTileY;
      }

      public boolean isIn(Entity entity) {
         return entity == null ? false : this.isIn(entity.getTileX(), entity.getTileY());
      }

      public boolean isInPos(float x, float y) {
         return this.isIn(GameMath.getTileCoordinate(x), GameMath.getTileCoordinate(y));
      }
   }

   public static class ThreadTimeOutException extends RuntimeException {
      public ThreadTimeOutException(Thread thread) {
         super(formatMessage(thread));
      }

      private static String formatMessage(Thread thread) {
         if (thread == null) {
            return "Unknown thread timed out";
         }

         StringBuilder sb = new StringBuilder("Thread timed out: \n");
         sb.append("\t Thread name: ").append(thread.getName()).append("\n");
         sb.append("\t Thread stack:\n");
         StackTraceElement[] stackTraces = thread.getStackTrace();

         for (StackTraceElement stackTrace : stackTraces) {
            sb.append("\t\t ").append(stackTrace).append("\n");
         }

         return sb.toString();
      }
   }

   public static class TrackedThreadPoolExecutor extends ThreadPoolExecutor {
      private final ConcurrentHashMap<FutureTask<?>, LevelDrawUtils.TrackedThreadPoolExecutor.TrackedFuture> trackedFutures = new ConcurrentHashMap<>();

      public TrackedThreadPoolExecutor(
         int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory
      ) {
         super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
      }

      @Override
      protected void beforeExecute(Thread t, Runnable r) {
         if (r instanceof FutureTask && this.trackedFutures.containsKey(r)) {
            this.trackedFutures.get(r).thread = t;
         }

         super.beforeExecute(t, r);
      }

      @Override
      protected void afterExecute(Runnable r, Throwable t) {
         if (r instanceof FutureTask && this.trackedFutures.containsKey(r)) {
            this.trackedFutures.get(r).thread = null;
            this.trackedFutures.remove(r);
         }

         super.afterExecute(r, t);
      }

      public LevelDrawUtils.TrackedThreadPoolExecutor.TrackedFuture submitTrackedTask(Runnable task) {
         FutureTask<?> future = (FutureTask<?>)super.submit(task);
         LevelDrawUtils.TrackedThreadPoolExecutor.TrackedFuture trackedFuture = new LevelDrawUtils.TrackedThreadPoolExecutor.TrackedFuture(future);
         this.trackedFutures.put(future, trackedFuture);
         return trackedFuture;
      }

      public static class TrackedFuture {
         public final FutureTask<?> future;
         private Thread thread = null;

         public TrackedFuture(FutureTask<?> future) {
            this.future = future;
         }

         public Thread getExecutingThread() {
            return this.thread;
         }
      }
   }
}
