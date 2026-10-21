package necesse.level.maps.liquidManager;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.ObjectValue;
import necesse.engine.util.PointSearchList;
import necesse.entity.mobs.Mob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.QuadDrawOptionsList;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.level.gameTile.GameTile;
import necesse.level.gameTile.LiquidTile;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.RegionBoundsExecutor;

public class LiquidManager {
   public static final byte maxHeight = 10;
   public static final byte minDepth = -10;
   public static final byte saltWaterDepth = -10;
   public static final byte saltWaterRange = 11;
   private static final int range = Math.max(10, Math.abs(-10));
   private final Level level;
   private final HashSet<Long> textureUpdates = new HashSet<>();
   private static final Point[] crossPoints = new Point[]{new Point(0, -1), new Point(-1, 0), new Point(1, 0), new Point(0, 1)};

   public LiquidManager(Level level) {
      this.level = level;
   }

   public void clientTick() {
      if (!this.textureUpdates.isEmpty()) {
         boolean recordConstant = this.level.debugLoadingPerformance != null;
         PerformanceTimerManager tickManager = this.level.debugLoadingPerformance != null ? this.level.debugLoadingPerformance : this.level.tickManager();
         Performance.record(tickManager, "liquidTexture", recordConstant, this::runTextureUpdates);
      }
   }

   public void queueTextureUpdate(int tileX, int tileY) {
      if (!this.level.isServer()) {
         this.textureUpdates.add(GameMath.getUniqueLongKey(tileX, tileY));
      }
   }

   protected void runTextureUpdates() {
      for (long tileKey : this.textureUpdates) {
         int tileX = GameMath.getXFromUniqueLongKey(tileKey);
         int tileY = GameMath.getYFromUniqueLongKey(tileKey);
         this.updateTextures(tileX, tileY);
      }

      this.textureUpdates.clear();
   }

   public void updateTextures(int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      if (region != null) {
         int regionTileX = tileX - region.tileXOffset;
         int regionTileY = tileY - region.tileYOffset;
         int height = region.liquidData.heights[regionTileX][regionTileY];
         int smoothGreen;
         if (height < 0) {
            float depthPercent = height / -10.0F;
            smoothGreen = GameMath.lerp(depthPercent, 127, 0);
         } else {
            float heightPercent = height / 10.0F;
            smoothGreen = GameMath.lerp(heightPercent, 128, 255);
         }

         int smoothBlue = region.liquidData.isSaltWaterByRegion(regionTileX, regionTileY) ? 255 : 0;
         int nearestRed = region.liquidData.isShoreByRegion(regionTileX, regionTileY) ? 255 : 0;
         int nearestGreen = 0;
         int nearestBlue = 0;
         GameTile tile = region.tileLayer.getTileByRegion(regionTileX, regionTileY);
         if (tile.isLiquid) {
            LiquidTile liquidTile = (LiquidTile)tile;
            nearestGreen = GameMath.lerp(liquidTile.getMinLiquidAlpha(this.level), 0, 255);
            nearestBlue = GameMath.lerp(liquidTile.getMaxLiquidAlpha(this.level), 0, 255);
         } else if (region.liquidData.isShoreByRegion(regionTileX, regionTileY)) {
            int count = 0;
            float min = 0.0F;
            float max = 0.0F;

            for (GameTile adjacentTile : this.level.getAdjacentTiles(tileX, tileY)) {
               if (adjacentTile.isLiquid) {
                  count++;
                  LiquidTile liquidTile = (LiquidTile)adjacentTile;
                  min += liquidTile.getMinLiquidAlpha(this.level);
                  max += liquidTile.getMaxLiquidAlpha(this.level);
               }
            }

            if (count > 0) {
               min /= count;
               max /= count;
            }

            nearestGreen = GameMath.lerp(min, 0, 255);
            nearestBlue = GameMath.lerp(max, 0, 255);
         }

         region.liquidData.updateTextureByRegion(regionTileX + 1, regionTileY + 1, smoothGreen, smoothBlue, nearestRed, nearestGreen, nearestBlue);
         if (regionTileY == 0) {
            Region topRegion = this.level.regionManager.getRegionByTile(tileX, tileY - 1, false);
            if (topRegion != null) {
               topRegion.liquidData
                  .updateTextureByRegion(regionTileX + 1, topRegion.tileHeight - 1 + 2, smoothGreen, smoothBlue, nearestRed, nearestGreen, nearestBlue);
            }

            if (regionTileX == 0) {
               Region topLeftRegion = this.level.regionManager.getRegionByTile(tileX - 1, tileY - 1, false);
               if (topLeftRegion != null) {
                  topLeftRegion.liquidData
                     .updateTextureByRegion(
                        topLeftRegion.tileWidth - 1 + 2, topLeftRegion.tileHeight - 1 + 2, smoothGreen, smoothBlue, nearestRed, nearestGreen, nearestBlue
                     );
               }
            }

            if (regionTileX == region.tileWidth - 1) {
               Region topRightRegion = this.level.regionManager.getRegionByTile(tileX + 1, tileY - 1, false);
               if (topRightRegion != null) {
                  topRightRegion.liquidData
                     .updateTextureByRegion(0, topRightRegion.tileHeight - 1 + 2, smoothGreen, smoothBlue, nearestRed, nearestGreen, nearestBlue);
               }
            }
         }

         if (regionTileY == region.tileHeight - 1) {
            Region bottomRegion = this.level.regionManager.getRegionByTile(tileX, tileY + 1, false);
            if (bottomRegion != null) {
               bottomRegion.liquidData.updateTextureByRegion(regionTileX + 1, 0, smoothGreen, smoothBlue, nearestRed, nearestGreen, nearestBlue);
            }

            if (regionTileX == 0) {
               Region bottomLeftRegion = this.level.regionManager.getRegionByTile(tileX - 1, tileY + 1, false);
               if (bottomLeftRegion != null) {
                  bottomLeftRegion.liquidData
                     .updateTextureByRegion(bottomLeftRegion.tileWidth - 1 + 2, 0, smoothGreen, smoothBlue, nearestRed, nearestGreen, nearestBlue);
               }
            }

            if (regionTileX == region.tileWidth - 1) {
               Region bottomRightRegion = this.level.regionManager.getRegionByTile(tileX + 1, tileY + 1, false);
               if (bottomRightRegion != null) {
                  bottomRightRegion.liquidData.updateTextureByRegion(0, 0, smoothGreen, smoothBlue, nearestRed, nearestGreen, nearestBlue);
               }
            }
         }

         if (regionTileX == 0) {
            Region leftRegion = this.level.regionManager.getRegionByTile(tileX - 1, tileY, false);
            if (leftRegion != null) {
               leftRegion.liquidData
                  .updateTextureByRegion(leftRegion.tileWidth - 1 + 2, regionTileY + 1, smoothGreen, smoothBlue, nearestRed, nearestGreen, nearestBlue);
            }
         }

         if (regionTileX == region.tileWidth - 1) {
            Region rightRegion = this.level.regionManager.getRegionByTile(tileX + 1, tileY, false);
            if (rightRegion != null) {
               rightRegion.liquidData.updateTextureByRegion(0, regionTileY + 1, smoothGreen, smoothBlue, nearestRed, nearestGreen, nearestBlue);
            }
         }
      }
   }

   public GameTooltips getAdvancedHeightTooltips(Mob mob, float posX, float posY) {
      ListGameTooltips out = new ListGameTooltips();
      float tileX = GameMath.getTileFloatCoordinate(posX);
      float tileY = GameMath.getTileFloatCoordinate(posY);
      Float inTilePos = this.getInTilePos(tileX, tileY);
      out.add("INT   TILE:   " + tileX + ", " + tileY);
      out.add("FLOAT TILE:   " + inTilePos.x + ", " + inTilePos.y);
      out.add("INT   HEIGHT: " + this.getHeight((int)Math.floor(tileX), (int)Math.floor(tileY)));
      out.add("FLOAT HEIGHT: " + this.getAdvancedHeight((int)Math.floor(tileX), (int)Math.floor(tileY), inTilePos.x, inTilePos.y));
      out.add("MOB   HEIGHT: " + this.getAdvancedMobHeightPercent(mob, (int)Math.floor(tileX), (int)Math.floor(tileY), inTilePos.x, inTilePos.y));
      return out;
   }

   private Float getInTilePos(float tileX, float tileY) {
      return new Float((tileX - (int)Math.floor(tileX)) * 2.0F - 1.0F, (tileY - (int)Math.floor(tileY)) * 2.0F - 1.0F);
   }

   public float getAdvancedHeight(float posX, float posY) {
      float tileX = GameMath.getTileFloatCoordinate(posX);
      float tileY = GameMath.getTileFloatCoordinate(posY);
      Float inTilePos = this.getInTilePos(tileX, tileY);
      return this.getAdvancedHeight((int)Math.floor(tileX), (int)Math.floor(tileY), inTilePos.x, inTilePos.y);
   }

   public float getAdvancedHeight(int tileX, int tileY, float inTileX, float inTileY) {
      if (this.level
         .getObject(tileX, tileY)
         .overridesInLiquid(this.level, tileX, tileY, tileX + (int)((inTileX + 1.0F) / 2.0F), tileY + (int)((inTileY + 1.0F) / 2.0F))) {
         return 0.0F;
      }

      float height = this.getHeight(tileX, tileY);
      float div = 1.0F;
      if (inTileX < 0.0F) {
         if (inTileY < 0.0F) {
            float diagonalHeight = this.getHeight(tileX - 1, tileY - 1);
            height += diagonalHeight * Math.abs(inTileX) * Math.abs(inTileY);
            div += Math.abs(inTileX) * Math.abs(inTileY);
            float nextHeight = this.getHeight(tileX, tileY - 1);
            height += nextHeight * Math.abs(inTileY);
            div += Math.abs(inTileY);
         } else {
            float diagonalHeight = this.getHeight(tileX - 1, tileY + 1);
            height += diagonalHeight * Math.abs(inTileX) * inTileY;
            div += Math.abs(inTileX) * inTileY;
            float nextHeight = this.getHeight(tileX, tileY + 1);
            height += nextHeight * inTileY;
            div += inTileY;
         }

         float nextHeight = this.getHeight(tileX - 1, tileY);
         height += nextHeight * Math.abs(inTileX);
         div += Math.abs(inTileX);
      } else {
         if (inTileY < 0.0F) {
            float diagonalHeight = this.getHeight(tileX + 1, tileY - 1);
            height += diagonalHeight * Math.abs(inTileX) * Math.abs(inTileY);
            div += inTileX * Math.abs(inTileY);
            float nextHeight = this.getHeight(tileX, tileY - 1);
            height += nextHeight * Math.abs(inTileY);
            div += Math.abs(inTileY);
         } else {
            float diagonalHeight = this.getHeight(tileX + 1, tileY + 1);
            height += diagonalHeight * Math.abs(inTileX) * inTileY;
            div += inTileX * inTileY;
            float nextHeight = this.getHeight(tileX, tileY + 1);
            height += nextHeight * inTileY;
            div += inTileY;
         }

         float nextHeight = this.getHeight(tileX + 1, tileY);
         height += nextHeight * inTileX;
         div += inTileX;
      }

      return height / div;
   }

   public float getAdvancedMobHeightPercent(Mob perspective, float posX, float posY) {
      float tileX = GameMath.getTileFloatCoordinate(posX);
      float tileY = GameMath.getTileFloatCoordinate(posY);
      Float inTilePos = this.getInTilePos(tileX, tileY);
      return this.getAdvancedMobHeightPercent(perspective, (int)Math.floor(tileX), (int)Math.floor(tileY), inTilePos.x, inTilePos.y);
   }

   public float getAdvancedMobHeightPercent(Mob perspective, int tileX, int tileY, float inTileX, float inTileY) {
      if (this.level
         .getObject(tileX, tileY)
         .overridesInLiquid(this.level, tileX, tileY, tileX + (int)Math.floor((inTileX + 1.0F) / 2.0F), tileY + (int)Math.floor((inTileY + 1.0F) / 2.0F))) {
         return 0.0F;
      }

      float height = this.getTileMobHeightPercent(perspective, tileX, tileY);
      float div = 1.0F;
      if (inTileX < 0.0F) {
         if (inTileY < 0.0F) {
            float diagonalHeight = this.getTileMobHeightPercent(perspective, tileX - 1, tileY - 1);
            height += diagonalHeight * Math.abs(inTileX) * Math.abs(inTileY);
            div += Math.abs(inTileX) * Math.abs(inTileY);
            float nextHeight = this.getTileMobHeightPercent(perspective, tileX, tileY - 1);
            height += nextHeight * Math.abs(inTileY);
            div += Math.abs(inTileY);
         } else {
            float diagonalHeight = this.getTileMobHeightPercent(perspective, tileX - 1, tileY + 1);
            height += diagonalHeight * Math.abs(inTileX) * inTileY;
            div += Math.abs(inTileX) * inTileY;
            float nextHeight = this.getTileMobHeightPercent(perspective, tileX, tileY + 1);
            height += nextHeight * inTileY;
            div += inTileY;
         }

         float nextHeight = this.getTileMobHeightPercent(perspective, tileX - 1, tileY);
         height += nextHeight * Math.abs(inTileX);
         div += Math.abs(inTileX);
      } else {
         if (inTileY < 0.0F) {
            float diagonalHeight = this.getTileMobHeightPercent(perspective, tileX + 1, tileY - 1);
            height += diagonalHeight * Math.abs(inTileX) * Math.abs(inTileY);
            div += inTileX * Math.abs(inTileY);
            float nextHeight = this.getTileMobHeightPercent(perspective, tileX, tileY - 1);
            height += nextHeight * Math.abs(inTileY);
            div += Math.abs(inTileY);
         } else {
            float diagonalHeight = this.getTileMobHeightPercent(perspective, tileX + 1, tileY + 1);
            height += diagonalHeight * Math.abs(inTileX) * inTileY;
            div += inTileX * inTileY;
            float nextHeight = this.getTileMobHeightPercent(perspective, tileX, tileY + 1);
            height += nextHeight * inTileY;
            div += inTileY;
         }

         float nextHeight = this.getTileMobHeightPercent(perspective, tileX + 1, tileY);
         height += nextHeight * inTileX;
         div += inTileX;
      }

      return height / div;
   }

   public DrawOptions getAdvancedMobHeightPercentDrawTest(Mob mob, GameCamera camera, int startPosX, int startPosY, int width, int height) {
      int startDrawX = camera.getDrawX(startPosX);
      int startDrawY = camera.getDrawY(startPosY);
      float[][] heights = new float[width][height];
      float minHeight = java.lang.Float.MAX_VALUE;
      float maxHeight = java.lang.Float.MIN_VALUE;

      for (int i = 0; i < width; i++) {
         int posX = startPosX + i;

         for (int j = 0; j < height; j++) {
            int posY = startPosY + j;
            float advHeight = this.getAdvancedMobHeightPercent(mob, posX, posY);
            if (advHeight < minHeight) {
               minHeight = advHeight;
            }

            if (advHeight > maxHeight) {
               maxHeight = advHeight;
            }

            heights[i][j] = advHeight;
         }
      }

      float deltaHeight = maxHeight - minHeight;
      QuadDrawOptionsList drawOptions = new QuadDrawOptionsList();

      for (int i = 0; i < width; i++) {
         int drawX = startDrawX + i;

         for (int j = 0; j < height; j++) {
            int drawY = startDrawY + j;
            float advHeight = heights[i][j];
            float percentHeight = (advHeight - minHeight) / deltaHeight;
            drawOptions.add(drawX, drawY, 1, 1, percentHeight, 0.0F, 0.0F, 0.9F);
         }
      }

      return drawOptions::draw;
   }

   public float getAdvancedMobSinkPercent(Mob perspective, float posX, float posY) {
      return Math.max(this.getAdvancedMobHeightPercent(perspective, posX, posY), 0.0F);
   }

   public float getInLiquidPercent(int tileX, int tileY) {
      return Math.min(this.getDepthPercent(tileX, tileY) * this.level.getLiquidMobSinkRate(), 1.0F);
   }

   public float getAdvancedDepthPercent(float posX, float posY) {
      return Math.max(0.0F, this.getAdvancedHeight(posX, posY) / -10.0F);
   }

   public float getDepthPercent(int tileX, int tileY) {
      return Math.max(0.0F, this.getHeight(tileX, tileY) / -10.0F);
   }

   public float getHeightPercent(int tileX, int tileY) {
      return Math.max(0.0F, this.getHeight(tileX, tileY) / 10.0F);
   }

   public float getTileMobHeightPercent(Mob perspective, int tileX, int tileY) {
      int height = this.getHeight(tileX, tileY);
      return this.level.getTile(tileX, tileY).getLiquidMobHeightPercent(this.level, tileX, tileY, perspective, height);
   }

   public int getHeight(int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null ? 0 : region.liquidData.getHeightByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public boolean isSaltWater(int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null ? false : region.liquidData.isSaltWaterByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public int getSaltWaterDepth(int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null ? 0 : region.liquidData.getSaltWaterDepthByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public boolean isFreshWater(int tileX, int tileY) {
      return !this.isSaltWater(tileX, tileY);
   }

   public boolean isShore(int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null ? false : region.liquidData.isShoreByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public void calculateShores() {
      this.level.regionManager.calculateShoresLiquidData();
   }

   public void calculateFull() {
      this.level.regionManager.calculateFullLiquidData();
   }

   public void onTileUpdated(Region region, int tileX, int tileY, GameTile oldTile, GameTile newTile) {
      if (region.isLoadingComplete() && this.level.isLoadingComplete()) {
         if (oldTile.isLiquid != newTile.isLiquid) {
            this.updateLevel(region, tileX - range, tileY - range, tileX + range, tileY + range, true, true);
         } else if (oldTile != newTile) {
            this.queueTextureUpdate(tileX, tileY);
         }
      }
   }

   public void updateAdjacentShores(int tileX, int tileY) {
      RegionBoundsExecutor executor = new RegionBoundsExecutor(this.level.regionManager, tileX - 1, tileY - 1, tileX + 1, tileY + 1, false);
      executor.runBounds(
         (region, regionStartTileX, regionStartTileY, regionEndTileX, regionEndTileY) -> region.liquidData
            .updateShoresByRegion(regionStartTileX, regionStartTileY, regionEndTileX, regionEndTileY)
      );
   }

   public void updateLevel(Region from, int startTileX, int startTileY, int endTileX, int endTileY, boolean updateShores, boolean updateAllTiles) {
      boolean recordConstant = this.level.debugLoadingPerformance != null;
      PerformanceTimerManager tickManager = this.level.debugLoadingPerformance != null ? this.level.debugLoadingPerformance : this.level.tickManager();
      Performance.record(tickManager, "updateHeights", recordConstant, () -> {
         PointSearchList depthTiles = new PointSearchList();
         PointSearchList heightTiles = new PointSearchList();

         for (int tileX = startTileX; tileX <= endTileX; tileX++) {
            for (int tileY = startTileY; tileY <= endTileY; tileY++) {
               Region region = from != null ? from.getRegionByTile(tileX, tileY, false) : this.level.regionManager.getRegionByTile(tileX, tileY, false);
               if (region != null) {
                  int regionTileX = tileX - region.tileXOffset;
                  int regionTileY = tileY - region.tileYOffset;
                  boolean isShore;
                  if (updateShores) {
                     isShore = region.liquidData.updateIsShoreByRegion(regionTileX, regionTileY);
                  } else {
                     isShore = region.liquidData.shores[regionTileX][regionTileY];
                  }

                  if (region.tileLayer.isTileLiquidByRegion(regionTileX, regionTileY)) {
                     if (updateAllTiles || region.liquidData.heights[regionTileX][regionTileY] == 0) {
                        region.liquidData.heights[regionTileX][regionTileY] = -10;
                     }
                  } else if (isShore) {
                     region.liquidData.heights[regionTileX][regionTileY] = 0;
                  } else if (updateAllTiles || region.liquidData.heights[regionTileX][regionTileY] == 0) {
                     region.liquidData.heights[regionTileX][regionTileY] = 10;
                  }

                  this.queueTextureUpdate(tileX, tileY);
                  if (isShore) {
                     depthTiles.addLast(tileX, tileY);
                     heightTiles.addLast(tileX, tileY);
                  }
               }
            }
         }

         for (int tileX = startTileX; tileX <= endTileX; tileX++) {
            this.addIfEdgeTile(from, tileX, startTileY - 1, depthTiles, heightTiles);
            this.addIfEdgeTile(from, tileX, endTileY + 1, depthTiles, heightTiles);
         }

         for (int tileY = startTileY; tileY <= endTileY; tileY++) {
            this.addIfEdgeTile(from, startTileX - 1, tileY, depthTiles, heightTiles);
            this.addIfEdgeTile(from, endTileX + 1, tileY, depthTiles, heightTiles);
         }

         Performance.record(tickManager, "depth", recordConstant, () -> this.updateDepthFromShoreTiles(from, depthTiles));
         Performance.record(tickManager, "height", recordConstant, () -> this.updateHeightFromShoreTiles(from, heightTiles));
         if (!this.level.isCave) {
            Performance.record(tickManager, "water", recordConstant, () -> this.updateSaltWater(from, startTileX, startTileY, endTileX, endTileY));
         }
      });
   }

   protected void addIfEdgeTile(Region from, int tileX, int tileY, PointSearchList depthTiles, PointSearchList heightTiles) {
      Region nextRegion = from != null ? from.getRegionByTile(tileX, tileY, false) : this.level.regionManager.getRegionByTile(tileX, tileY, false);
      if (nextRegion != null) {
         if (nextRegion.liquidData.shores[tileX - nextRegion.tileXOffset][tileY - nextRegion.tileYOffset]) {
            depthTiles.addLast(tileX, tileY);
            heightTiles.addLast(tileX, tileY);
         } else {
            boolean isLiquid = nextRegion.tileLayer.isTileLiquidByRegion(tileX - nextRegion.tileXOffset, tileY - nextRegion.tileYOffset);
            if (nextRegion.liquidData.heights[tileX - nextRegion.tileXOffset][tileY - nextRegion.tileYOffset] != 0) {
               if (isLiquid) {
                  depthTiles.addLast(tileX, tileY);
               } else {
                  heightTiles.addLast(tileX, tileY);
               }
            }
         }
      }
   }

   public void updateDepthFromShoreTiles(Region from, PointSearchList list) {
      while (!list.isEmpty()) {
         Point tile = list.removeFirstAndAddClosed();
         Region region = from != null ? from.getRegionByTile(tile.x, tile.y, false) : this.level.regionManager.getRegionByTile(tile.x, tile.y, false);
         if (region != null) {
            int currentDepth = region.liquidData.heights[tile.x - region.tileXOffset][tile.y - region.tileYOffset];
            int nextDepth = Math.max(currentDepth - 1, -10);

            for (Point offset : crossPoints) {
               this.updateDepth(region, tile.x + offset.x, tile.y + offset.y, nextDepth, list);
            }
         }
      }
   }

   protected void updateDepth(Region from, int nextTileX, int nextTileY, int nextDepth, PointSearchList list) {
      Region region = from.getRegionByTile(nextTileX, nextTileY, false);
      if (region != null) {
         int regionTileX = nextTileX - region.tileXOffset;
         int regionTileY = nextTileY - region.tileYOffset;
         if (region.tileLayer.isTileLiquidByRegion(regionTileX, regionTileY)) {
            byte currentHeight = region.liquidData.heights[regionTileX][regionTileY];
            if (currentHeight >= 0 || currentHeight < nextDepth) {
               region.liquidData.heights[regionTileX][regionTileY] = (byte)nextDepth;
               this.queueTextureUpdate(nextTileX, nextTileY);
               list.addLast(nextTileX, nextTileY);
            }
         }
      }
   }

   public void updateHeightFromShoreTiles(Region from, PointSearchList list) {
      while (!list.isEmpty()) {
         Point tile = list.removeFirstAndAddClosed();
         Region region = from != null ? from.getRegionByTile(tile.x, tile.y, false) : this.level.regionManager.getRegionByTile(tile.x, tile.y, false);
         if (region != null) {
            int currentHeight = region.liquidData.heights[tile.x - region.tileXOffset][tile.y - region.tileYOffset];
            int nextHeight = Math.min(currentHeight + 1, 10);

            for (Point offset : crossPoints) {
               this.updateHeight(region, tile.x + offset.x, tile.y + offset.y, nextHeight, list);
            }
         }
      }
   }

   protected void updateHeight(Region from, int nextTileX, int nextTileY, int nextHeight, PointSearchList list) {
      Region region = from.getRegionByTile(nextTileX, nextTileY, false);
      if (region != null) {
         int regionTileX = nextTileX - region.tileXOffset;
         int regionTileY = nextTileY - region.tileYOffset;
         if (!region.tileLayer.isTileLiquidByRegion(regionTileX, regionTileY)) {
            byte currentHeight = region.liquidData.heights[regionTileX][regionTileY];
            if (currentHeight == 0 && !region.liquidData.shores[regionTileX][regionTileY] || currentHeight > nextHeight) {
               region.liquidData.heights[regionTileX][regionTileY] = (byte)nextHeight;
               this.queueTextureUpdate(nextTileX, nextTileY);
               list.addLast(nextTileX, nextTileY);
            }
         }
      }
   }

   private void updateSaltWater(Region from, int startTileX, int startTileY, int endTileX, int endTileY) {
      int minStartTileX = startTileX - 11;
      int minStartTileY = startTileY - 11;
      int maxEndTileX = endTileX + 11;
      int maxEndTileY = endTileY + 11;
      if (this.level.tileWidth > 0) {
         if (minStartTileX < 0) {
            minStartTileX = 0;
            if (startTileX < 0) {
               startTileX = 0;
            }
         }

         if (maxEndTileX >= this.level.tileWidth) {
            maxEndTileX = this.level.tileWidth - 1;
            if (endTileX >= this.level.tileWidth) {
               endTileX = this.level.tileWidth - 1;
            }
         }
      }

      if (this.level.tileHeight > 0) {
         if (minStartTileY < 0) {
            minStartTileY = 0;
            if (startTileY < 0) {
               startTileY = 0;
            }
         }

         if (maxEndTileY >= this.level.tileHeight) {
            maxEndTileY = this.level.tileHeight - 1;
            if (endTileY >= this.level.tileHeight) {
               endTileY = this.level.tileHeight - 1;
            }
         }
      }

      LinkedList<Long> invalidCores = new LinkedList<>();
      LinkedList<Long> validCores = new LinkedList<>();

      for (int tileX = startTileX; tileX <= endTileX; tileX++) {
         for (int tileY = startTileY; tileY <= endTileY; tileY++) {
            Region region = from != null ? from.getRegionByTile(tileX, tileY, false) : this.level.regionManager.getRegionByTile(tileX, tileY, false);
            if (region != null) {
               int regionTileX = tileX - region.tileXOffset;
               int regionTileY = tileY - region.tileYOffset;
               long key = GameMath.getUniqueLongKey(tileX, tileY);
               byte currentHeight = region.liquidData.heights[regionTileX][regionTileY];
               byte currentSaltWaterDepth = region.liquidData.saltWaterDepth[regionTileX][regionTileY];
               if (currentHeight <= -10) {
                  region.liquidData.saltWaterDepth[regionTileX][regionTileY] = 11;
                  validCores.addLast(key);
               } else if (currentSaltWaterDepth == 11) {
                  invalidCores.addLast(key);
               }

               this.queueTextureUpdate(tileX, tileY);
            }
         }
      }

      for (int tileX = minStartTileX; tileX < startTileX; tileX++) {
         for (int tileY = minStartTileY; tileY <= maxEndTileY; tileY++) {
            this.checkValidCores(from, tileX, tileY, invalidCores, validCores);
         }
      }

      for (int tileX = endTileX + 1; tileX <= maxEndTileX; tileX++) {
         for (int tileY = minStartTileY; tileY <= maxEndTileY; tileY++) {
            this.checkValidCores(from, tileX, tileY, invalidCores, validCores);
         }
      }

      for (int tileY = minStartTileY; tileY < startTileY; tileY++) {
         for (int tileX = startTileX; tileX <= endTileX; tileX++) {
            this.checkValidCores(from, tileX, tileY, invalidCores, validCores);
         }
      }

      for (int tileY = endTileY + 1; tileY <= maxEndTileY; tileY++) {
         for (int tileX = startTileX; tileX <= endTileX; tileX++) {
            this.checkValidCores(from, tileX, tileY, invalidCores, validCores);
         }
      }

      this.clearSaltWaterCores(from, invalidCores, validCores);

      for (int tileX = startTileX; tileX <= endTileX; tileX++) {
         for (int tileY = startTileY; tileY <= endTileY; tileY++) {
            Region region = from != null ? from.getRegionByTile(tileX, tileY, false) : this.level.regionManager.getRegionByTile(tileX, tileY, false);
            if (region != null) {
               int regionTileX = tileX - region.tileXOffset;
               int regionTileY = tileY - region.tileYOffset;
               byte currentHeight = region.liquidData.heights[regionTileX][regionTileY];
               if (currentHeight > -10) {
                  region.liquidData.saltWaterDepth[regionTileX][regionTileY] = 0;
               }
            }
         }
      }

      for (int tileX = startTileX; tileX <= endTileX; tileX++) {
         this.addIfSaltWater(from, tileX, startTileY - 1, validCores);
         this.addIfSaltWater(from, tileX, endTileY + 1, validCores);
      }

      for (int tileY = startTileY; tileY <= endTileY; tileY++) {
         this.addIfSaltWater(from, startTileX - 1, tileY, validCores);
         this.addIfSaltWater(from, endTileX + 1, tileY, validCores);
      }

      this.expandSaltWaterCores(from, validCores);
   }

   protected void checkValidCores(Region from, int tileX, int tileY, LinkedList<Long> invalidCores, LinkedList<Long> validCores) {
      Region nextRegion = from != null ? from.getRegionByTile(tileX, tileY, false) : this.level.regionManager.getRegionByTile(tileX, tileY, false);
      if (nextRegion != null) {
         int regionTileX = tileX - nextRegion.tileXOffset;
         int regionTileY = tileY - nextRegion.tileYOffset;
         if (nextRegion.liquidData.saltWaterDepth[regionTileX][regionTileY] == 11) {
            if (nextRegion.liquidData.heights[regionTileX][regionTileY] > -10) {
               invalidCores.add(GameMath.getUniqueLongKey(tileX, tileY));
            } else {
               validCores.add(GameMath.getUniqueLongKey(tileX, tileY));
            }
         }
      }
   }

   protected void addIfSaltWater(Region from, int tileX, int tileY, LinkedList<Long> validCores) {
      Region nextRegion = from != null ? from.getRegionByTile(tileX, tileY, false) : this.level.regionManager.getRegionByTile(tileX, tileY, false);
      if (nextRegion != null) {
         int regionTileX = tileX - nextRegion.tileXOffset;
         int regionTileY = tileY - nextRegion.tileYOffset;
         if (nextRegion.liquidData.isSaltWaterByRegion(regionTileX, regionTileY)) {
            validCores.add(GameMath.getUniqueLongKey(tileX, tileY));
         }
      }
   }

   protected void clearSaltWaterCores(Region from, LinkedList<Long> tilesToClear, LinkedList<Long> tilesToExpand) {
      HashSet<Long> closedTiles = new HashSet<>();

      while (!tilesToClear.isEmpty()) {
         long currentKey = tilesToClear.removeFirst();
         int tileX = GameMath.getXFromUniqueLongKey(currentKey);
         int tileY = GameMath.getYFromUniqueLongKey(currentKey);
         Region region = from != null ? from.getRegionByTile(tileX, tileY, false) : this.level.regionManager.getRegionByTile(tileX, tileY, false);
         if (region != null) {
            int regionTileX = tileX - region.tileXOffset;
            int regionTileY = tileY - region.tileYOffset;
            byte currentHeight = region.liquidData.heights[regionTileX][regionTileY];
            if (currentHeight > -10) {
               byte lastSaltWaterDepth = region.liquidData.saltWaterDepth[regionTileX][regionTileY];
               boolean foundLowerSaltWater = false;

               for (Point offset : crossPoints) {
                  if (this.checkIfIsHigherSaltWater(region, tileX + offset.x, tileY + offset.y, lastSaltWaterDepth, tilesToClear, closedTiles)) {
                     tilesToExpand.addLast(currentKey);
                     closedTiles.add(currentKey);
                     foundLowerSaltWater = true;
                  }
               }

               if (!foundLowerSaltWater) {
                  region.liquidData.saltWaterDepth[regionTileX][regionTileY] = 0;
                  this.queueTextureUpdate(tileX, tileY);
               }
            }
         }
      }
   }

   protected boolean checkIfIsHigherSaltWater(
      Region from, int tileX, int tileY, byte lastSaltWaterDepth, LinkedList<Long> tilesToClear, HashSet<Long> closedTiles
   ) {
      long key = GameMath.getUniqueLongKey(tileX, tileY);
      if (closedTiles.contains(key)) {
         return false;
      }

      Region nextRegion = from != null ? from.getRegionByTile(tileX, tileY, false) : this.level.regionManager.getRegionByTile(tileX, tileY, false);
      if (nextRegion == null) {
         return false;
      }

      int regionTileX = tileX - nextRegion.tileXOffset;
      int regionTileY = tileY - nextRegion.tileYOffset;
      byte currentSaltWaterDepth = nextRegion.liquidData.saltWaterDepth[regionTileX][regionTileY];
      if (currentSaltWaterDepth > lastSaltWaterDepth) {
         return true;
      }

      if (currentSaltWaterDepth > 0) {
         tilesToClear.addLast(key);
      }

      return false;
   }

   public void expandSaltWaterCores(Region from, LinkedList<Long> list) {
      while (!list.isEmpty()) {
         long currentKey = list.removeFirst();
         int tileX = GameMath.getXFromUniqueLongKey(currentKey);
         int tileY = GameMath.getYFromUniqueLongKey(currentKey);
         Region region = from != null ? from.getRegionByTile(tileX, tileY, false) : this.level.regionManager.getRegionByTile(tileX, tileY, false);
         if (region != null) {
            int currentSaltWaterDepth = region.liquidData.saltWaterDepth[tileX - region.tileXOffset][tileY - region.tileYOffset];
            int nextDepth = Math.max(currentSaltWaterDepth - 1, 0);
            if (nextDepth != 0) {
               for (Point offset : crossPoints) {
                  this.updateSaltWater(region, tileX + offset.x, tileY + offset.y, nextDepth, list);
               }
            }
         }
      }
   }

   protected void updateSaltWater(Region from, int nextTileX, int nextTileY, int nextDepth, LinkedList<Long> list) {
      Region region = from.getRegionByTile(nextTileX, nextTileY, false);
      if (region != null) {
         int regionTileX = nextTileX - region.tileXOffset;
         int regionTileY = nextTileY - region.tileYOffset;
         byte currentSaltWaterDepth = region.liquidData.saltWaterDepth[regionTileX][regionTileY];
         if (currentSaltWaterDepth < nextDepth) {
            region.liquidData.saltWaterDepth[regionTileX][regionTileY] = (byte)nextDepth;
            this.queueTextureUpdate(nextTileX, nextTileY);
            list.addLast(GameMath.getUniqueLongKey(nextTileX, nextTileY));
         }
      }
   }

   public ClosestHeightResult findClosestHeightTile(int tileX, int tileY, int desiredHeight, Predicate<Point> tileFilter) {
      return this.findClosestHeightTile(tileX, tileY, desiredHeight, 0, tileFilter);
   }

   public ClosestHeightResult findClosestHeightTile(int tileX, int tileY, int desiredHeight, int maxSameHeightTravel, Predicate<Point> tileFilter) {
      int startHeight = this.level.liquidManager.getHeight(tileX, tileY);
      if (startHeight == desiredHeight) {
         return new ClosestHeightResult(tileX, tileY, new Point(tileX, tileY), new Point(tileX, tileY), new HashSet<>(), new LinkedList<>());
      }

      HashMap<Long, NextHeightTile> closedTiles = new HashMap<>();
      HashMap<Long, NextHeightTile> processedTiles = new HashMap<>();
      LinkedList<NextHeightTile> openTiles = new LinkedList<>();
      NextHeightTile first = new NextHeightTile(tileX, tileY, startHeight, 0);
      openTiles.add(first);
      processedTiles.put(GameMath.getUniqueLongKey(first.x, first.y), first);
      AtomicReference<ObjectValue<Point, Integer>> best = new AtomicReference<>(new ObjectValue<>(new Point(tileX, tileY), startHeight));

      while (!openTiles.isEmpty()) {
         NextHeightTile current = openTiles.removeLast();
         closedTiles.put(GameMath.getUniqueLongKey(current.x, current.y), current);

         for (Point p : crossPoints) {
            Point found = this.checkNextHeight(
               current.x + p.x, current.y + p.y, closedTiles, openTiles, processedTiles, current, maxSameHeightTravel, desiredHeight, best, tileFilter
            );
            if (found != null) {
               return new ClosestHeightResult(tileX, tileY, found, found, closedTiles.values(), openTiles);
            }
         }
      }

      return new ClosestHeightResult(tileX, tileY, (Point)best.get().object, null, closedTiles.values(), openTiles);
   }

   private Point checkNextHeight(
      int tileX,
      int tileY,
      HashMap<Long, NextHeightTile> closedTiles,
      LinkedList<NextHeightTile> openTiles,
      HashMap<Long, NextHeightTile> processedTiles,
      NextHeightTile current,
      int maxSameHeightTravel,
      int desiredHeight,
      AtomicReference<ObjectValue<Point, Integer>> best,
      Predicate<Point> tileFilter
   ) {
      if (this.level.isTileWithinBounds(tileX, tileY) && this.level.regionManager.isTileLoaded(tileX, tileY)) {
         long key = GameMath.getUniqueLongKey(tileX, tileY);
         NextHeightTile last = closedTiles.get(key);
         if (last == null) {
            int nextHeight = this.level.liquidManager.getHeight(tileX, tileY);
            NextHeightTile next = new NextHeightTile(tileX, tileY, nextHeight, nextHeight == current.height ? current.sameHeightTraveled + 1 : 0);
            if (nextHeight == desiredHeight && tileFilter.test(next)) {
               return next;
            }

            boolean goDown = desiredHeight < current.height;
            if (goDown ? nextHeight < current.height : nextHeight > current.height) {
               if ((goDown ? nextHeight > desiredHeight : nextHeight < desiredHeight)
                  && (goDown ? nextHeight < (Integer)best.get().value : nextHeight > (Integer)best.get().value)
                  && tileFilter.test(next)) {
                  best.set(new ObjectValue<>(next, nextHeight));
               }

               processedTiles.compute(key, (i, lastNext) -> {
                  if (lastNext == null) {
                     openTiles.addLast(next);
                     return next;
                  } else {
                     lastNext.sameHeightTraveled = Math.min(lastNext.sameHeightTraveled, next.sameHeightTraveled);
                     return (NextHeightTile)lastNext;
                  }
               });
            } else if (nextHeight == current.height && current.sameHeightTraveled + 1 <= maxSameHeightTravel) {
               if ((goDown ? nextHeight < (Integer)best.get().value : nextHeight > (Integer)best.get().value) && tileFilter.test(next)) {
                  best.set(new ObjectValue<>(next, nextHeight));
               }

               processedTiles.compute(key, (i, lastNext) -> {
                  if (lastNext == null) {
                     openTiles.addFirst(next);
                     return next;
                  } else {
                     lastNext.sameHeightTraveled = Math.min(lastNext.sameHeightTraveled, next.sameHeightTraveled);
                     return (NextHeightTile)lastNext;
                  }
               });
            }
         }

         return null;
      } else {
         return null;
      }
   }
}
