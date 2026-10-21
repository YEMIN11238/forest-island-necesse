package necesse.level.maps.presets;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.function.BiPredicate;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.HashMapPointEntry;
import necesse.engine.util.PointHashSet;
import necesse.engine.util.PointTreeSet;
import necesse.engine.util.TicketSystemList;
import necesse.engine.util.Zoning;
import necesse.engine.world.worldPresets.RimworldThrumboDangerWorldPreset;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.generationModules.LinesGeneration;
import necesse.level.maps.multiTile.MultiTile;

public class RimworldThrumboDangerPreset extends Preset {
   public RimworldThrumboDangerPreset(GameRandom random, int shoreTileID) {
      super(RimworldThrumboDangerWorldPreset.size.width, RimworldThrumboDangerWorldPreset.size.height);
      int centerWidth = 4;
      int centerHeight = 4;
      int centerStartX = this.width / 2 - centerWidth / 2;
      int centerStartY = this.height / 2 - centerHeight / 2;
      Rectangle centerRectangle = new Rectangle(centerStartX, centerStartY, centerWidth, centerHeight);
      Zoning boxes = new Zoning(true, Level.adjacentGetters);
      boxes.addRectangle(new Rectangle(centerStartX - 1, centerStartY - 1, centerWidth + 2, centerHeight + 2));
      int totalRectangles = random.getIntBetween(4, 6);

      for (int i = 0; i < totalRectangles; i++) {
         this.addNewRectangle(boxes, random, centerRectangle);
      }

      int desiredFilledTiles = this.width * this.height / 4;
      Rectangle thrumboRectangle = new Rectangle(this.width / 3, this.height / 3);

      for (int thrumboCollisions = 0; thrumboCollisions < 3; thrumboRectangle.y++) {
         if (boxes.containsTile(thrumboRectangle.x + thrumboRectangle.width - 1, thrumboRectangle.y + thrumboRectangle.height - 1)) {
            thrumboCollisions++;
         }

         thrumboRectangle.x++;
      }

      Rectangle thrumboEdge = new Rectangle(thrumboRectangle.x + thrumboRectangle.width - 1, thrumboRectangle.y + thrumboRectangle.height - 1);

      for (int i = 0; i < 10; i++) {
         boxes.removeRectangle(thrumboEdge);
         if (boxes.size() >= desiredFilledTiles) {
            break;
         }

         this.addNewRectangle(boxes, random, centerRectangle);
      }

      float fenceChance = 0.6F;
      int fenceID = ObjectRegistry.getObjectID("woodfence");

      for (int tileX = thrumboRectangle.x; tileX < thrumboRectangle.x + thrumboRectangle.width; tileX++) {
         if (random.getChance(fenceChance) && !boxes.containsTile(tileX, thrumboRectangle.y)) {
            this.setObject(tileX, thrumboRectangle.y, fenceID);
         }

         if (random.getChance(fenceChance) && !boxes.containsTile(tileX, thrumboRectangle.y + thrumboRectangle.height - 1)) {
            this.setObject(tileX, thrumboRectangle.y + thrumboRectangle.height - 1, fenceID);
         }
      }

      for (int tileY = thrumboRectangle.y; tileY < thrumboRectangle.y + thrumboRectangle.height; tileY++) {
         if (random.getChance(fenceChance) && !boxes.containsTile(thrumboRectangle.x, tileY)) {
            this.setObject(thrumboRectangle.x, tileY, fenceID);
         }

         if (random.getChance(fenceChance) && !boxes.containsTile(thrumboRectangle.x + thrumboRectangle.width - 1, tileY)) {
            this.setObject(thrumboRectangle.x + thrumboRectangle.width - 1, tileY, fenceID);
         }
      }

      for (Point tile : boxes.getTiles()) {
         this.setTile(tile.x, tile.y, TileRegistry.rockID);
         this.setObject(tile.x, tile.y, 0);
      }

      if (shoreTileID > 0) {
         this.addCustomPreApplyRectEach(
            thrumboRectangle.x, thrumboRectangle.y, thrumboRectangle.width, thrumboRectangle.height, 0, (level, levelX, levelY, dir, blackboard) -> {
               if (level.getTile(levelX, levelY).isLiquid) {
                  level.setTile(levelX, levelY, shoreTileID);
               }

               if (level.getObject(levelX, levelY).isSolid(level, levelX, levelY)) {
                  level.setObject(levelX, levelY, 0);
               }

               return null;
            }
         );
      }

      this.addMob("thrumbo", thrumboRectangle.x + thrumboRectangle.width / 2, thrumboRectangle.y + thrumboRectangle.height / 2, false);
      int floorArms = random.getIntBetween(6, 7);
      LinesGeneration floorGeneration = this.getLinesGeneration(
         random, this.width / 2, this.height / 2, floorArms, 35, Math.min(this.width, this.height) / 2 - 1, 2, 4, 1.5F, 2.5F, boxes.getTiles()
      );

      for (HashMapPointEntry<Point, LinesGeneration.PointDistance> entry : floorGeneration.getSmoothPoints().getEntries()) {
         Point tile = entry.getKey();
         if (boxes.containsTile(tile.x, tile.y)) {
            double chance = entry.getValue().dist / 3.0;
            if (!random.getChance(chance) && random.getChance(0.8F)) {
               this.setTile(tile.x, tile.y, TileRegistry.stoneFloorID);
            }
         }
      }

      PointHashSet coffinTiles = new PointHashSet();
      PointHashSet illegalTiles = new PointHashSet();
      TicketSystemList<GameObject> randomObjects = new TicketSystemList<>();
      randomObjects.addObject(10, ObjectRegistry.getObject("stonecoffin"));
      this.placeObjects(
         boxes.getTiles(),
         randomObjects,
         random,
         0.1F,
         true,
         TileRegistry.stonePathID,
         (tileX, tileY) -> !boxes.isEdgeTile(tileX, tileY) && this.getObject(tileX, tileY) == 0,
         coffinTiles,
         illegalTiles
      );

      for (Point coffinTile : coffinTiles) {
         this.addInventory(LootTablePresets.thrumboDangerCoffins, random, coffinTile.x, coffinTile.y);
      }

      randomObjects = new TicketSystemList<>();
      randomObjects.addObject(40, ObjectRegistry.getObject("crate"));
      randomObjects.addObject(30, ObjectRegistry.getObject("stonecolumn"));
      randomObjects.addObject(20, ObjectRegistry.getObject("surfacerocksmall"));
      this.placeObjects(
         boxes.getTiles(),
         randomObjects,
         random,
         0.2F,
         false,
         -1,
         (tileX, tileY) -> !boxes.isEdgeTile(tileX, tileY) && this.getObject(tileX, tileY) == 0 && !illegalTiles.contains(tileX, tileY),
         null,
         null
      );
      int stoneWallID = ObjectRegistry.getObjectID("stonewall");

      for (Point edgeTile : boxes.getEdgeTiles()) {
         this.setObject(edgeTile.x, edgeTile.y, stoneWallID);
         this.setTile(edgeTile.x, edgeTile.y, TileRegistry.woodPathID);
      }
   }

   public void addNewRectangle(Zoning zoning, GameRandom random, Rectangle centerRectangle) {
      int tileWidth = random.getIntBetween((this.width - centerRectangle.width) / 4, (this.width - centerRectangle.width) / 2);
      int tileHeight = random.getIntBetween((this.height - centerRectangle.height) / 4, (this.height - centerRectangle.height) / 2);
      int startX = random.getIntBetween(centerRectangle.x - tileWidth + 1, centerRectangle.x + centerRectangle.width - 1);
      int startY = random.getIntBetween(centerRectangle.y - tileHeight + 1, centerRectangle.y + centerRectangle.height - 1);
      zoning.addRectangle(new Rectangle(startX, startY, tileWidth, tileHeight));
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
      PointTreeSet sandTiles
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

   public void placeObjects(
      Iterable<Point> tiles,
      TicketSystemList<GameObject> randomObjects,
      GameRandom random,
      float placeChance,
      boolean useAdjacentTileRectangle,
      int placeTileID,
      BiPredicate<Integer, Integer> isValidTile,
      PointHashSet placedTiles,
      PointHashSet coveredTiles
   ) {
      for (Point tile : tiles) {
         if (random.getChance(placeChance)) {
            GameObject object = randomObjects.getRandomObject(random);
            int rotation = random.getIntBetween(0, 3);
            MultiTile multiTile = object.getMultiTile(rotation);
            Rectangle tileRectangle = useAdjacentTileRectangle
               ? multiTile.getAdjacentTileRectangle(tile.x, tile.y)
               : multiTile.getTileRectangle(tile.x, tile.y);
            if (new Rectangle(this.width, this.height).contains(tileRectangle)) {
               boolean valid = true;

               for (int tileX = tileRectangle.x; tileX < tileRectangle.x + tileRectangle.width; tileX++) {
                  for (int tileY = tileRectangle.y; tileY < tileRectangle.y + tileRectangle.height; tileY++) {
                     if (!isValidTile.test(tileX, tileY)) {
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
                     this.setObject(idCoord.tileX, idCoord.tileY, idCoord.value, rotation);
                  }

                  if (placedTiles != null) {
                     placedTiles.add(tile.x, tile.y);
                  }

                  if (coveredTiles != null) {
                     for (int tileX = tileRectangle.x; tileX < tileRectangle.x + tileRectangle.width; tileX++) {
                        for (int tileY = tileRectangle.y; tileY < tileRectangle.y + tileRectangle.height; tileY++) {
                           coveredTiles.add(tileX, tileY);
                        }
                     }
                  }

                  if (placeTileID != -1) {
                     for (int tileX = tileRectangle.x; tileX < tileRectangle.x + tileRectangle.width; tileX++) {
                        for (int tileY = tileRectangle.y; tileY < tileRectangle.y + tileRectangle.height; tileY++) {
                           this.setTile(tileX, tileY, placeTileID);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
