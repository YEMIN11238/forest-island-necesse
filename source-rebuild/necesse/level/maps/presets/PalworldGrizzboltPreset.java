package necesse.level.maps.presets;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.function.BiPredicate;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.HashMapPointEntry;
import necesse.engine.util.PointHashMap;
import necesse.engine.util.PointHashSet;
import necesse.engine.util.TicketSystemList;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.generationModules.LinesGeneration;
import necesse.level.maps.multiTile.MultiTile;

public class PalworldGrizzboltPreset extends Preset {
   public PalworldGrizzboltPreset(GameRandom random) {
      super(32, 32);
      int centerWidth = 5;
      int centerHeight = 5;
      int centerStartX = this.width / 2 - centerWidth / 2;
      int centerStartY = this.height / 2 - centerHeight / 2;
      LinesGeneration sandLines = new LinesGeneration(this.width / 2, this.height / 2);
      int halfSize = Math.min(this.width, this.height) / 2;
      sandLines.addRandomArms(random, 7, halfSize - 8, halfSize - 6, 3.0F, 4.5F);
      PointHashMap<LinesGeneration.PointDistance> sandLinePoints = sandLines.getSmoothPoints();
      PointHashSet sandTiles = new PointHashSet();
      PointHashSet edgeTiles = new PointHashSet();
      PointHashSet wallTiles = new PointHashSet();

      for (Point tile : sandLinePoints.getKeys()) {
         if (tile.x >= 0 && tile.x < this.width && tile.y >= 0 && tile.y < this.height) {
            this.setTile(tile.x, tile.y, TileRegistry.deepSandstoneID);
            this.setObject(tile.x, tile.y, 0);
            sandTiles.add(tile.x, tile.y);
            if (this.isEdge(tile.x, tile.y, sandLinePoints::containsKey)) {
               edgeTiles.add(tile.x, tile.y);
            }
         }
      }

      int wallID = ObjectRegistry.getObjectID("arcanicwall");
      int wallArms = random.getIntBetween(4, 5);
      LinesGeneration wallGeneration = this.getLinesGeneration(
         random, this.width / 2, this.height / 2, wallArms, 0, Math.min(this.width, this.height) / 2 - 1, 2, 4, 2.0F, 3.0F, sandTiles
      );

      for (Point tile : wallGeneration.getDiamondPoints()) {
         if (edgeTiles.contains(tile.x, tile.y)) {
            wallTiles.add(tile.x, tile.y);
            sandTiles.remove(tile.x, tile.y);
            this.setObject(tile.x, tile.y, wallID);
            this.setTile(tile.x, tile.y, TileRegistry.arcanicPathID);
         }
      }

      for (int i = 0; i < 2; i++) {
         this.expandWalls(wallTiles, edgeTiles, sandTiles, wallID, TileRegistry.arcanicPathID);
      }

      int jailFenceID = ObjectRegistry.getObjectID("jailfence");

      for (Point edgeTile : edgeTiles) {
         if (this.getObject(edgeTile.x, edgeTile.y) != wallID) {
            this.setObject(edgeTile.x, edgeTile.y, jailFenceID);
         }
      }

      int floorArms = random.getIntBetween(4, 5);
      LinesGeneration floorGeneration = this.getLinesGeneration(
         random, this.width / 2, this.height / 2, floorArms, 35, Math.min(this.width, this.height) / 2 - 1, 2, 4, 1.5F, 2.5F, sandTiles
      );

      for (HashMapPointEntry<Point, LinesGeneration.PointDistance> entry : floorGeneration.getSmoothPoints().getEntries()) {
         Point tile = entry.getKey();
         if (sandTiles.contains(tile.x, tile.y)) {
            double chance = entry.getValue().dist / 3.0;
            if (!random.getChance(chance) && random.getChance(0.8F)) {
               this.setTile(tile.x, tile.y, TileRegistry.arcanicFloorID);
            }
         }
      }

      PointHashSet wallAdjacentTiles = this.getWallAdjacentTiles(wallTiles, sandTiles, edgeTiles);
      TicketSystemList<GameObject> randomObjects = new TicketSystemList<>();
      randomObjects.addObject(100, ObjectRegistry.getObject("crate"));
      randomObjects.addObject(100, ObjectRegistry.getObject("arcaniccrate"));
      randomObjects.addObject(10, ObjectRegistry.getObject("arcanicmachinery"));
      randomObjects.addObject(30, ObjectRegistry.getObject("arcaniccolumn"));
      randomObjects.addObject(20, ObjectRegistry.getObject("arcaniclamp"));
      randomObjects.addObject(50, ObjectRegistry.getObject("topazclustersmall"));
      randomObjects.addObject(50, ObjectRegistry.getObject("topazclusterpure"));
      randomObjects.addObject(10, ObjectRegistry.getObject("tikitorch"));

      for (Point tile : wallAdjacentTiles) {
         if (random.getChance(0.9F)) {
            GameObject object = randomObjects.getRandomObject(random);
            MultiTile multiTile = object.getMultiTile(0);
            Rectangle tileRectangle = multiTile.getTileRectangle(tile.x, tile.y);
            boolean valid = true;

            for (int tileX = tileRectangle.x; tileX < tileRectangle.x + tileRectangle.width; tileX++) {
               for (int tileY = tileRectangle.y; tileY < tileRectangle.y + tileRectangle.height; tileY++) {
                  if (this.getObject(tileX, tileY) != 0) {
                     valid = false;
                     break;
                  }
               }

               if (!valid) {
                  break;
               }
            }

            if (valid) {
               for (MultiTile.CoordinateValue<Integer> idCoord : multiTile.getIDs(tile.x, tile.y)) {
                  this.setObject(idCoord.tileX, idCoord.tileY, idCoord.value);
               }
            }
         }
      }

      for (int x = centerStartX; x < centerStartX + centerWidth; x++) {
         for (int y = centerStartY; y < centerStartY + centerHeight; y++) {
            if (x != centerStartX && x != centerStartX + centerWidth - 1 && y != centerStartY && y != centerStartY + centerHeight - 1) {
               this.setTile(x, y, TileRegistry.arcanicFloorID);
            } else {
               this.setTile(x, y, TileRegistry.arcanicPathID);
            }

            sandTiles.remove(x, y);
         }
      }

      this.addMob("grizzbolt", centerStartX + centerWidth / 2, centerStartY + centerHeight / 2, false);
      ArrayList<Point> sandTilesList = new ArrayList<>(sandTiles.size());

      for (Point sandTile : sandTiles) {
         sandTilesList.add(sandTile);
      }

      int syndicateSpawns = random.getIntBetween(5, 7);
      int spawnedSyndicates = 0;

      while (spawnedSyndicates < syndicateSpawns && !sandTilesList.isEmpty()) {
         Point spawnTile = sandTilesList.remove(random.nextInt(sandTilesList.size()));
         if (this.getObject(spawnTile.x, spawnTile.y) == 0) {
            this.addMob("palworldsyndicatethug", spawnTile.x, spawnTile.y, false);
            spawnedSyndicates++;
         }
      }
   }

   public LinesGeneration getLinesGeneration(
      GameRandom random,
      int startX,
      int startY,
      int arms,
      int angleOffset,
      int maxLength,
      int armMinLength,
      int armMaxLength,
      float armStartWidth,
      float armEndWidth,
      PointHashSet sandTiles
   ) {
      LinesGeneration lg = new LinesGeneration(startX, startY);
      int armAngle = random.nextInt(360);
      int anglePerArm = 360 / arms;

      for (int i = 0; i < arms; i++) {
         armAngle += anglePerArm;
         lg.addMultiArm(
            random, armAngle, angleOffset, maxLength, armMinLength, armMaxLength, armStartWidth, armEndWidth, armLG -> !sandTiles.contains(armLG.x2, armLG.y2)
         );
      }

      return lg;
   }

   public boolean isEdge(int tileX, int tileY, BiPredicate<Integer, Integer> isPartOfTiles) {
      for (Point offset : Level.adjacentGetters) {
         if (!isPartOfTiles.test(tileX + offset.x, tileY + offset.y)) {
            return true;
         }
      }

      return false;
   }

   public void expandWalls(PointHashSet wallTiles, PointHashSet edgeTiles, PointHashSet sandTiles, int wallID, int tileID) {
      PointHashSet addedWallTiles = new PointHashSet();

      for (Point wallTile : wallTiles) {
         for (Point offset : Level.adjacentGettersNotDiagonal) {
            int adjacentX = wallTile.x + offset.x;
            int adjacentY = wallTile.y + offset.y;
            if (edgeTiles.contains(adjacentX, adjacentY)) {
               this.setObject(adjacentX, adjacentY, wallID);
               this.setTile(adjacentX, adjacentY, tileID);
               addedWallTiles.add(adjacentX, adjacentY);
               sandTiles.remove(adjacentX, adjacentY);
            }
         }
      }

      wallTiles.addAll(addedWallTiles);
   }

   public PointHashSet getWallAdjacentTiles(PointHashSet wallTiles, PointHashSet sandTiles, PointHashSet edgeTiles) {
      PointHashSet adjacentTiles = new PointHashSet();

      for (Point wallTile : wallTiles) {
         for (Point offset : Level.adjacentGettersNotDiagonal) {
            int adjacentX = wallTile.x + offset.x;
            int adjacentY = wallTile.y + offset.y;
            if (sandTiles.contains(adjacentX, adjacentY) && !wallTiles.contains(adjacentX, adjacentY) && !edgeTiles.contains(adjacentX, adjacentY)) {
               adjacentTiles.add(adjacentX, adjacentY);
            }
         }
      }

      return adjacentTiles;
   }
}
