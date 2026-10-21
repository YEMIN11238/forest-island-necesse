package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.PriorityMap;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.ai.behaviourTree.event.AIWasHitEvent;
import necesse.entity.mobs.ai.behaviourTree.event.WanderingNowAIEvent;
import necesse.entity.mobs.ai.behaviourTree.util.WandererBaseOptions;
import necesse.level.maps.Level;
import necesse.level.maps.TilePosition;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.regionSystem.ConnectedSubRegionsResult;
import necesse.level.maps.regionSystem.SubRegion;

public class WandererAINode<T extends Mob> extends MoveTaskAINode<T> {
   private static int MAX_ROOM_SEARCH_SIZE = 500;
   private static int MAX_HOUSE_SEARCH_SIZE = 1500;
   public int frequency;
   public int randomMod;
   public int searchRadius;
   public Predicate<T> hideInside;
   public boolean runAwayFromAttacker = true;
   public boolean runAwayFromAttackerToBase;
   public Function<T, ZoneTester> getZoneTester;
   private boolean isRunningAway;
   public String baseOptionsKey = "baseOptions";

   public WandererAINode(int wanderFrequency) {
      this.randomMod = 0;
      this.frequency = wanderFrequency;
      this.searchRadius = 10;
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      blackboard.onEvent("wanderNow", e -> this.randomMod = this.frequency);
      if (this.hideInside != null) {
         WandererBaseOptions<T> baseOptions = this.getBaseOptions();
         if (baseOptions != null) {
            Point base = baseOptions.getBaseTile(mob);
            if (base != null && this.hideInside.test(mob)) {
               boolean isBaseInside = !mob.getLevel().isOutside(base.x, base.y);
               if (isBaseInside) {
                  if (baseOptions.isBaseRoom(mob, base)) {
                     ConnectedSubRegionsResult regions = mob.getLevel().regionManager.getRoomConnectedByTile(base.x, base.y, true, MAX_ROOM_SEARCH_SIZE);
                     if (regions != null) {
                        int tileX = mob.getTileX();
                        int tileY = mob.getTileY();
                        if (regions.connectedRegions.stream().anyMatch(r -> r.hasLevelTile(tileX, tileY))) {
                           this.getBlackboard().put("isHidingInside", true);
                           this.getBlackboard().put("isHidingInsideRoom", true);
                           this.getBlackboard().put("isHidingInsideTile", base);
                        }
                     }
                  } else if (baseOptions.isBaseHouse(mob, base)) {
                     ConnectedSubRegionsResult regions = mob.getLevel().regionManager.getHouseConnectedByTile(base.x, base.y, MAX_HOUSE_SEARCH_SIZE);
                     if (regions != null) {
                        int tileX = mob.getTileX();
                        int tileY = mob.getTileY();
                        if (regions.connectedRegions.stream().anyMatch(r -> r.hasLevelTile(tileX, tileY))) {
                           this.getBlackboard().put("isHidingInside", true);
                           this.getBlackboard().put("isHidingInsideHouse", true);
                           this.getBlackboard().put("isHidingInsideTile", base);
                        }
                     }
                  }
               } else {
                  int radius = baseOptions.getBaseRadius(mob, this);
                  int tileX = mob.getTileX();
                  int tileY = mob.getTileY();
                  if (GameMath.preciseDistance(tileX, tileY, base.x, base.y) <= radius) {
                     this.getBlackboard().put("isHidingInside", true);
                     this.getBlackboard().put("isHidingInsideTile", base);
                  }
               }
            }
         }
      }
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      if (this.runAwayFromAttacker || this.runAwayFromAttackerToBase) {
         WandererBaseOptions<T> baseOptions = this.getBaseOptions();
         Point base = this.runAwayFromAttackerToBase ? (baseOptions == null ? null : baseOptions.getBaseTile(mob)) : null;
         if (base != null || this.runAwayFromAttacker) {
            for (AIWasHitEvent e : blackboard.getLastHits()) {
               Mob attackOwner = e.event.attacker != null ? e.event.attacker.getAttackOwner() : null;
               if (attackOwner != null) {
                  if (base != null) {
                     WandererAINode<T>.FoundTile foundTile = this.findNewPositionInsideBase(mob, base, mob::getTileWanderPriority, baseOptions);
                     Point housePoint = this.updateHidingInsideAndReturnTile(foundTile, base);
                     if (housePoint != null) {
                        return this.moveToTileTask(housePoint.x, housePoint.y, null, path -> {
                           if (path.moveIfWithin(-1, -1, null)) {
                              blackboard.submitEvent("wanderingNow", new WanderingNowAIEvent(housePoint.x, housePoint.y, true));
                           }

                           return AINodeResult.SUCCESS;
                        });
                     }
                  }

                  if (this.runAwayFromAttacker) {
                     Float dir = GameMath.normalize(mob.x - attackOwner.x, mob.y - attackOwner.y);
                     float dirMod;
                     if (Math.abs(dir.x) > Math.abs(dir.y)) {
                        dirMod = 1.0F / Math.abs(dir.x);
                     } else {
                        dirMod = 1.0F / Math.abs(dir.y);
                     }

                     dir.x *= dirMod;
                     dir.y *= dirMod;
                     int baseRadius = baseOptions == null ? this.searchRadius : baseOptions.getBaseRadius(mob, this);
                     if (baseRadius == 0 && base != null) {
                        this.isRunningAway = true;
                        if (mob.getTileX() == base.x && mob.getTileY() == base.y) {
                           return AINodeResult.SUCCESS;
                        }

                        if (mob.estimateCanMoveTo(base.x, base.y, false)) {
                           return this.moveToTileTask(base.x, base.y, null, path -> {
                              if (path.moveIfWithin(-1, -1, null)) {
                                 blackboard.submitEvent("wanderingNow", new WanderingNowAIEvent(base.x, base.y, true));
                              }

                              return AINodeResult.SUCCESS;
                           });
                        }
                     } else {
                        int xOffset = (int)(dir.x * baseRadius);
                        int yOffset = (int)(dir.y * baseRadius);
                        Point nextPoint = this.findNewPosition(mob, xOffset, yOffset, baseOptions);
                        if (nextPoint != null) {
                           this.isRunningAway = true;
                           return this.moveToTileTask(nextPoint.x, nextPoint.y, null, path -> {
                              if (path.moveIfWithin(-1, -1, null)) {
                                 blackboard.submitEvent("wanderingNow", new WanderingNowAIEvent(nextPoint.x, nextPoint.y, true));
                              }

                              return AINodeResult.SUCCESS;
                           });
                        }
                     }
                  }
                  break;
               }
            }
         }
      }

      if (!blackboard.mover.isMoving() && this.isRunningAway) {
         blackboard.submitEvent("ranAway", new AIEvent());
         this.isRunningAway = false;
      }

      if (!blackboard.mover.isMoving() || !blackboard.mover.isCurrentlyMovingFor(this)) {
         if (this.frequency - this.randomMod > 0 && GameRandom.globalRandom.nextInt(this.frequency - this.randomMod) >= 50) {
            this.randomMod += 50;
         } else {
            WandererBaseOptions<T> baseOptions = this.getBaseOptions();
            int baseRadius = baseOptions == null ? this.searchRadius : baseOptions.getBaseRadius(mob, this);
            if (baseRadius == 0 && baseOptions != null) {
               Point baseTile = baseOptions.getBaseTile(mob);
               if (baseTile != null) {
                  if (mob.getTileX() == baseTile.x && mob.getTileY() == baseTile.y) {
                     return AINodeResult.SUCCESS;
                  }

                  if (mob.estimateCanMoveTo(baseTile.x, baseTile.y, false)) {
                     return this.moveToTileTask(baseTile.x, baseTile.y, null, path -> {
                        if (path.moveIfWithin(-1, -1, null)) {
                           blackboard.submitEvent("wanderingNow", new WanderingNowAIEvent(baseTile.x, baseTile.y, false));
                        }

                        return AINodeResult.SUCCESS;
                     });
                  }
               }
            }

            Point nextPoint = this.findNewPosition(mob, baseOptions);
            if (nextPoint == null) {
               Point baseTile = baseOptions == null ? null : baseOptions.getBaseTile(mob);
               WandererAINode<T>.FoundTile foundTile = this.findNewPositionInsideBase(mob, baseTile, baseOptions);
               nextPoint = this.updateHidingInsideAndReturnTile(foundTile, baseTile);
            }

            if (nextPoint != null) {
               int tileX = nextPoint.x;
               int tileY = nextPoint.y;
               return this.moveToTileTask(nextPoint.x, nextPoint.y, null, path -> {
                  if (path.moveIfWithin(-1, -1, null)) {
                     blackboard.submitEvent("wanderingNow", new WanderingNowAIEvent(tileX, tileY, false));
                  }

                  return AINodeResult.SUCCESS;
               });
            }
         }
      }

      return AINodeResult.SUCCESS;
   }

   public WandererBaseOptions<T> getBaseOptions() {
      WandererBaseOptions baseOptions = this.getBlackboard().getObject(WandererBaseOptions.class, this.baseOptionsKey);
      if (baseOptions != null) {
         return baseOptions;
      }

      Point wanderBaseTile = this.mob().getWanderBaseTile();
      return wanderBaseTile != null ? mob -> wanderBaseTile : null;
   }

   public Point findNewPosition(T mob, WandererBaseOptions<T> baseOptions) {
      return this.findNewPosition(mob, 0, 0, baseOptions);
   }

   public Point findNewPosition(T mob, int xOffset, int yOffset, WandererBaseOptions<T> baseOptions) {
      return this.findNewPosition(mob, xOffset, yOffset, baseOptions, mob::getTileWanderPriority);
   }

   public Point findNewPosition(T mob, int xOffset, int yOffset, WandererBaseOptions<T> baseOptions, BiFunction<TilePosition, Biome, Integer> tilePriority) {
      this.randomMod = 0;
      this.getBlackboard().put("isHidingInside", false);
      this.getBlackboard().put("isHidingInsideRoom", false);
      this.getBlackboard().put("isHidingInsideHouse", false);
      this.getBlackboard().put("isHidingInsideTile", null);
      Point baseTile = baseOptions == null ? null : baseOptions.getBaseTile(mob);
      if (baseTile != null && this.hideInside != null && this.hideInside.test(mob)) {
         WandererAINode<T>.FoundTile houseTile = this.findNewPositionInsideBase(mob, baseTile, tilePriority, baseOptions);
         return this.updateHidingInsideAndReturnTile(houseTile, baseTile);
      }

      int mobTileX = mob.getTileX();
      int mobTileY = mob.getTileY();
      if (baseTile != null) {
         int baseRadius = baseOptions.getBaseRadius(mob, this);
         Rectangle baseRect = new Rectangle(baseTile.x - baseRadius, baseTile.y - baseRadius, baseRadius * 2, baseRadius * 2);
         if (!baseRect.contains(mobTileX, mobTileY)) {
            xOffset = (int)Math.signum(baseTile.x - mobTileX) * baseRadius / 2;
            yOffset = (int)Math.signum(baseTile.y - mobTileY) * baseRadius / 2;
         }
      }

      ZoneTester zoneTester = null;
      if (this.getZoneTester != null) {
         zoneTester = this.getZoneTester.apply(mob);
      }

      return baseTile != null && baseOptions.forceFindAroundBase(mob)
         ? findWanderingPointAround(mob, baseTile.x, baseTile.y, baseOptions.getBaseRadius(mob, this), zoneTester, tilePriority, 20, 5)
         : findWanderingPoint(
            mob, xOffset, yOffset, baseOptions == null ? this.searchRadius : baseOptions.getBaseRadius(mob, this), zoneTester, tilePriority, 20, 5
         );
   }

   public static boolean isTargetValidHidingInside(Blackboard<?> blackboard, Level level, int targetTileX, int targetTileY) {
      Point hidingInsideTile = blackboard.getObject(Point.class, "isHidingInsideTile", null);
      if (hidingInsideTile == null) {
         return false;
      } else if (level.isOutside(targetTileX, targetTileY)) {
         return false;
      } else {
         int targetRoomID = level.regionManager.getRoomIDByTile(targetTileX, targetTileY);
         if (blackboard.getObject(Boolean.class, "isHidingInsideRoom", false)) {
            return level.regionManager
                  .findFirstRoomConnectedByTile(
                     hidingInsideTile.x, hidingInsideTile.y, true, MAX_ROOM_SEARCH_SIZE, subRegion -> subRegion.getRoomID() == targetRoomID
                  )
               != null;
         } else {
            return blackboard.getObject(Boolean.class, "isHidingInsideHouse", false)
               ? level.regionManager
                     .findFirstHouseConnectedByTile(
                        hidingInsideTile.x, hidingInsideTile.y, MAX_HOUSE_SEARCH_SIZE, subRegion -> subRegion.getRoomID() == targetRoomID
                     )
                  != null
               : false;
         }
      }
   }

   protected Point updateHidingInsideAndReturnTile(WandererAINode<T>.FoundTile foundTile, Point baseTile) {
      if (foundTile != null) {
         this.getBlackboard().put("isHidingInside", true);
         this.getBlackboard().put("isHidingInsideRoom", foundTile.isInsideRoom);
         this.getBlackboard().put("isHidingInsideHouse", foundTile.isInsideHouse);
         this.getBlackboard().put("isHidingInsideTile", baseTile);
         return new Point(foundTile.tileX, foundTile.tileY);
      } else {
         return null;
      }
   }

   public WandererAINode<T>.FoundTile findNewPositionInsideBase(T mob, Point baseTile, WandererBaseOptions<T> baseOptions) {
      return this.findNewPositionInsideBase(mob, baseTile, mob::getTileWanderPriority, baseOptions);
   }

   public WandererAINode<T>.FoundTile findNewPositionInsideBase(
      T mob, Point baseTile, BiFunction<TilePosition, Biome, Integer> tilePriority, WandererBaseOptions<T> baseOptions
   ) {
      if (baseTile == null) {
         return null;
      }

      boolean isBaseInside = !mob.getLevel().isOutside(baseTile.x, baseTile.y);
      if (isBaseInside) {
         if (baseOptions.isBaseRoom(mob, baseTile)) {
            ConnectedSubRegionsResult regions = mob.getLevel().regionManager.getRoomConnectedByTile(baseTile.x, baseTile.y, true, MAX_ROOM_SEARCH_SIZE);
            if (regions != null) {
               int maxHouseSize = baseOptions.getBaseRadius(mob, this) * 8;
               Point tile = findWanderingPointInsideRegions(mob, regions, maxHouseSize, tilePriority, 20, 5);
               if (tile != null) {
                  return new WandererAINode.FoundTile(tile.x, tile.y, true, false);
               }

               return null;
            }
         } else if (baseOptions.isBaseHouse(mob, baseTile)) {
            ConnectedSubRegionsResult regions = mob.getLevel().regionManager.getHouseConnectedByTile(baseTile.x, baseTile.y, MAX_HOUSE_SEARCH_SIZE);
            if (regions != null) {
               int maxHouseSize = baseOptions.getBaseRadius(mob, this) * 8;
               Point tile = findWanderingPointInsideRegions(mob, regions, maxHouseSize, tilePriority, 20, 5);
               if (tile != null) {
                  return new WandererAINode.FoundTile(tile.x, tile.y, false, true);
               }

               return null;
            }
         }

         return null;
      } else {
         int radius = baseOptions.getBaseRadius(mob, this);
         Point tile = findWanderingPointAround(
            mob,
            baseTile.x,
            baseTile.y,
            radius,
            null,
            (tp, biome) -> mob.getDistance(tp.tileX * 32 + 16, tp.tileY * 32 + 16) > radius * 32 ? Integer.MIN_VALUE : mob.getTileWanderPriority(tp, biome),
            20,
            5
         );
         return tile != null ? new WandererAINode.FoundTile(tile.x, tile.y, false, false) : null;
      }
   }

   public static Point getWanderingPoint(Mob mob, PriorityMap<Point> priorityMap, int minRandomList, int attempts) {
      if (attempts > minRandomList) {
         throw new IllegalArgumentException("Attempts cannot be larger than minimum number of items in random list");
      }

      for (ArrayList<Point> positions = priorityMap.getBestObjects(minRandomList); !positions.isEmpty() && attempts > 0; attempts--) {
         int index = GameRandom.globalRandom.nextInt(positions.size());
         Point point = positions.get(index);
         if (mob.estimateCanMoveTo(point.x, point.y, false)) {
            return point;
         }

         positions.remove(index);
      }

      return null;
   }

   public static Point findWanderingPointInsideRegions(
      Mob mob,
      ConnectedSubRegionsResult regions,
      int maxTiles,
      BiFunction<TilePosition, Biome, Integer> tilePriority,
      int minRandomList,
      int regionPathAttempts
   ) {
      Point pathOffset = mob.getPathMoveOffset();
      PriorityMap<Point> priorityMap = new PriorityMap<>();
      int added = 0;
      Biome baseBiome = mob.getWanderBaseBiome(mob.getLevel());

      for (SubRegion sr : regions.connectedRegions) {
         if (!sr.getType().isSolid) {
            Iterator var12 = sr.getLevelTiles().iterator();

            while (true) {
               if (var12.hasNext()) {
                  Point tile = (Point)var12.next();
                  if (mob.getLevel().isSolidTile(tile.x, tile.y) || mob.collidesWith(mob.getLevel(), tile.x * 32 + pathOffset.x, tile.y * 32 + pathOffset.y)) {
                     continue;
                  }

                  priorityMap.add(tilePriority == null ? 0 : tilePriority.apply(new TilePosition(mob.getLevel(), tile), baseBiome), tile);
                  if (++added <= maxTiles) {
                     continue;
                  }
               }

               if (added > maxTiles) {
                  return getWanderingPoint(mob, priorityMap, minRandomList, regionPathAttempts);
               }
               break;
            }
         }
      }

      return getWanderingPoint(mob, priorityMap, minRandomList, regionPathAttempts);
   }

   public static Point findWanderingPoint(
      Mob mob,
      int xOffset,
      int yOffset,
      int searchRadius,
      ZoneTester zoneTester,
      BiFunction<TilePosition, Biome, Integer> tilePriority,
      int minRandomList,
      int regionPathAttempts
   ) {
      return findWanderingPointAround(
         mob, mob.getTileX() + xOffset, mob.getTileY() + yOffset, searchRadius, zoneTester, tilePriority, minRandomList, regionPathAttempts
      );
   }

   public static Point findWanderingPoint(Mob mob, int xOffset, int yOffset, int searchRadius, ZoneTester zoneTester, int minRandomList, int regionPathAttempts) {
      return findWanderingPointAround(
         mob, mob.getTileX() + xOffset, mob.getTileY() + yOffset, searchRadius, zoneTester, mob::getTileWanderPriority, minRandomList, regionPathAttempts
      );
   }

   public static Point findWanderingPointAround(
      Mob mob,
      int tileX,
      int tileY,
      int searchRadius,
      ZoneTester zoneTester,
      BiFunction<TilePosition, Biome, Integer> tilePriority,
      int minRandomList,
      int regionPathAttempts
   ) {
      Point pathOffset = mob.getPathMoveOffset();
      PriorityMap<Point> priorityMap = new PriorityMap<>();
      Biome baseBiome = mob.getWanderBaseBiome(mob.getLevel());

      for (int x = -searchRadius; x <= searchRadius; x++) {
         for (int y = -searchRadius; y <= searchRadius; y++) {
            Point lp = new Point(tileX + x, tileY + y);
            if ((zoneTester == null || zoneTester.containsTile(lp.x, lp.y))
               && !mob.getLevel().isSolidTile(lp.x, lp.y)
               && !mob.collidesWith(mob.getLevel(), lp.x * 32 + pathOffset.x, lp.y * 32 + pathOffset.y)) {
               priorityMap.add(tilePriority == null ? 0 : tilePriority.apply(new TilePosition(mob.getLevel(), lp), baseBiome), lp);
            }
         }
      }

      return getWanderingPoint(mob, priorityMap, minRandomList, regionPathAttempts);
   }

   public static Point findWanderingPointAround(
      Mob mob, int tileX, int tileY, int searchRadius, ZoneTester zoneTester, int minRandomList, int regionPathAttempts
   ) {
      return findWanderingPointAround(mob, tileX, tileY, searchRadius, zoneTester, mob::getTileWanderPriority, minRandomList, regionPathAttempts);
   }

   public class FoundTile {
      public int tileX;
      public int tileY;
      public boolean isInsideRoom;
      public boolean isInsideHouse;

      public FoundTile(int tileX, int tileY, boolean isInsideRoom, boolean isInsideHouse) {
         this.tileX = tileX;
         this.tileY = tileY;
         this.isInsideRoom = isInsideRoom;
         this.isInsideHouse = isInsideHouse;
      }
   }
}
