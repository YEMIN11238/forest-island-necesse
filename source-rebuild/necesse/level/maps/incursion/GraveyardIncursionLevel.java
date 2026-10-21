package necesse.level.maps.incursion;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import necesse.engine.AreaFinder;
import necesse.engine.Settings;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.IncursionPerksRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.TicketSystemList;
import necesse.engine.util.voronoi.DelaunayTriangulator;
import necesse.engine.util.voronoi.TriangleLine;
import necesse.engine.world.WorldEntity;
import necesse.gfx.drawables.WallShadowVariables;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.generationModules.GenerationTools;
import necesse.level.maps.generationModules.LinesGeneration;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.light.LightManager;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.presets.FurnitureHousePreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.PresetRotateException;
import necesse.level.maps.presets.PresetRotation;
import necesse.level.maps.presets.furniturePresets.BedDresserPreset;
import necesse.level.maps.presets.furniturePresets.BenchPreset;
import necesse.level.maps.presets.furniturePresets.BookshelfClockPreset;
import necesse.level.maps.presets.furniturePresets.BookshelvesPreset;
import necesse.level.maps.presets.furniturePresets.CabinetsPreset;
import necesse.level.maps.presets.furniturePresets.DeskBookshelfPreset;
import necesse.level.maps.presets.furniturePresets.DinnerTablePreset;
import necesse.level.maps.presets.furniturePresets.DisplayStandClockPreset;
import necesse.level.maps.presets.furniturePresets.ModularDinnerTablePreset;
import necesse.level.maps.presets.furniturePresets.ModularTablesPreset;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.WallSet;

public class GraveyardIncursionLevel extends IncursionLevel {
   public static int GRAVEYARD_AMBIENT_LIGHT = 60;

   public GraveyardIncursionLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
   }

   public GraveyardIncursionLevel(LevelIdentifier identifier, BiomeMissionIncursionData incursion, WorldEntity worldEntity, AltarData altarData) {
      super(identifier, 150, 150, incursion, worldEntity);
      this.baseBiome = BiomeRegistry.GRAVEYARD;
      this.upgradeShardsRockID = "cryptupgradeshardorerocksmall";
      this.alchemyShardsRockID = "cryptalchemyshardorerocksmall";
      this.essenceRockID = "bloodessenceoreorerocksmall";
      this.generateLevel(incursion, altarData);
   }

   public void generateLevel(BiomeMissionIncursionData incursionData, AltarData altarData) {
      GameRandom random = new GameRandom(incursionData.getUniqueID());
      int cryptAshID = TileRegistry.getTileID("cryptash");

      for (int tileX = 0; tileX < this.tileWidth; tileX++) {
         for (int tileY = 0; tileY < this.tileHeight; tileY++) {
            this.setTile(tileX, tileY, cryptAshID);
         }
      }

      TicketSystemList<GameObject> objects = new TicketSystemList<>();
      GameObject air = ObjectRegistry.getObject("air");
      objects.addObject(2000, air);
      objects.addObject(100, ObjectRegistry.getObject("cryptcoffin"));
      objects.addObject(100, ObjectRegistry.getObject("cryptcolumn"));
      objects.addObject(100, ObjectRegistry.getObject("cryptgravestone1"));
      objects.addObject(100, ObjectRegistry.getObject("cryptgravestone2"));
      objects.addObject(100, ObjectRegistry.getObject("deadwoodtree"));
      objects.addObject(100, ObjectRegistry.getObject("vase"));
      objects.addObject(30, ObjectRegistry.getObject("deadwoodcandles"));
      objects.addObject(5, ObjectRegistry.getObject("deadwoodbench"));
      objects.addObject(5, ObjectRegistry.getObject("deadwoodchair"));

      for (int tileX = 0; tileX < this.tileWidth; tileX++) {
         for (int tileY = 0; tileY < this.tileHeight; tileY++) {
            GameObject object = objects.getRandomObject(random);
            if (object.getID() != air.getID()) {
               int rotation = random.nextInt(4);
               if (object.canPlace(this, tileX, tileY, rotation, false) == null) {
                  MultiTile multiTile = object.getMultiTile(rotation);
                  if (!multiTile.streamOtherIDs(tileX, tileY).anyMatch(c -> this.getObjectID(c.tileX, c.tileY) != 0)
                     && !multiTile.getAdjacentTiles(tileX, tileY, true).stream().anyMatch(adjTile -> this.getObjectID(adjTile.x, adjTile.y) != 0)) {
                     object.placeObject(this, tileX, tileY, rotation, false);
                  }
               }
            }
         }
      }

      GenerationTools.fillMap(this, random, cryptAshID, -1, 0.0F, ObjectRegistry.getObjectID("cryptgrass"), -1, 0.2F, false, true);
      ArrayList<Float> points = new ArrayList<>();
      int edgePadding = -20;
      int usableWidth = this.tileWidth - edgePadding * 2;
      int usableHeight = this.tileHeight - edgePadding * 2;
      int resolution = 30;
      int resWidth = usableWidth / resolution;
      int resHeight = usableHeight / resolution;
      int xOffset = usableWidth % resolution / 2;
      int yOffset = usableHeight % resolution / 2;

      for (int x = 0; x < resWidth; x++) {
         int minX = x * resolution + xOffset + edgePadding;
         int maxX = minX + resolution;

         for (int y = 0; y < resHeight; y++) {
            int minY = y * resolution + yOffset + edgePadding;
            int maxY = minY + resolution;
            Float point = new Float(random.getIntBetween(minX, maxX), random.getIntBetween(minY, maxY));
            points.add(point);
         }
      }

      ArrayList<TriangleLine> voronoiLines = new ArrayList<>();
      DelaunayTriangulator.compute(points, false, voronoiLines);
      final int cryptPathTileID = TileRegistry.getTileID("cryptpath");

      for (TriangleLine line : voronoiLines) {
         LinesGeneration.pathTiles(new java.awt.geom.Line2D.Float(line.p1, line.p2), true, (from, next) -> {
            for (int x = 0; x < 2; x++) {
               for (int y = 0; y < 2; y++) {
                  int tileX = next.x + x;
                  int tileY = next.y + y;
                  if (this.isTileWithinBounds(tileX, tileY)) {
                     this.setTile(tileX, tileY, cryptPathTileID);
                     this.setObject(tileX, tileY, 0);
                  }
               }
            }
         });
      }

      usableHeight = Math.min(points.size(), random.getIntBetween(12, 18));
      resolution = ObjectRegistry.getObjectID("cryptfence");
      resWidth = ObjectRegistry.getObjectID("cryptfencegate");
      BiConsumer<Point, Integer> pointProgression = (point, dirx) -> {
         switch (dirx) {
            case 0:
               point.y--;
               break;
            case 1:
               point.x++;
               break;
            case 2:
               point.y++;
               break;
            default:
               point.x--;
         }
      };

      for (int i = 0; i < usableHeight && !points.isEmpty(); i++) {
         yOffset = random.nextInt(points.size());
         Float floatStartTile = points.remove(yOffset);
         Point startTile = new Point((int)floatStartTile.x, (int)floatStartTile.y);
         if (this.isTileWithinBounds(startTile.x, startTile.y)) {
            ArrayList<GraveyardIncursionLevel.OpenDirection> openDirections = new ArrayList<>();
            int startDirOffset = random.nextInt(4);

            for (int j = 0; j < 4; j++) {
               if (j == 0 || random.getEveryXthChance(2)) {
                  openDirections.add(new GraveyardIncursionLevel.OpenDirection(startTile.x, startTile.y, (j + startDirOffset) % 4, 0));
               }
            }

            ArrayList<ArrayList<Point>> fenceSections = new ArrayList<>();

            while (!openDirections.isEmpty()) {
               ArrayList<Point> fenceSection = new ArrayList<>();
               GraveyardIncursionLevel.OpenDirection current = openDirections.remove(0);
               Point check = new Point(current.x, current.y);
               int maxLength = random.getIntBetween(8, 20);
               int checkAhead = random.nextInt(5);

               for (int j = 0; j < checkAhead; j++) {
                  pointProgression.accept(check, current.dir);
               }

               int currentLength;
               for (currentLength = 0;
                  currentLength < maxLength
                     && !this.getTile(current.x, current.y).isFloor
                     && !this.getTile(check.x, check.y).isFloor
                     && (currentLength == 0 || !this.getObject(current.x, current.y).isFence && !this.getObject(check.x, check.y).isFence);
                  currentLength++
               ) {
                  if (currentLength > 0 && currentLength < maxLength - 1) {
                     fenceSection.add(new Point(current));
                  }

                  this.setObject(current.x, current.y, resolution);
                  pointProgression.accept(current, current.dir);
                  pointProgression.accept(check, current.dir);
               }

               fenceSections.add(fenceSection);
               if (currentLength > 3 && random.getEveryXthChance(current.depth + 2)) {
                  int dirOffset = random.nextInt(4);

                  for (int j = 0; j < 4; j++) {
                     int dir = (j + dirOffset) % 4;
                     if (dir != current.dir && random.getEveryXthChance(1 + j)) {
                        openDirections.add(new GraveyardIncursionLevel.OpenDirection(current.x, current.y, dir, current.depth + 1));
                     }
                  }
               }
            }

            if (!fenceSections.isEmpty()) {
               int gates = 1 + random.nextInt(fenceSections.size());

               for (int j = 0; j < gates && !fenceSections.isEmpty(); j++) {
                  int sectionIndex = random.nextInt(fenceSections.size());
                  ArrayList<Point> section = fenceSections.remove(sectionIndex);
                  Point gateTile = random.getOneOf(section);
                  if (gateTile != null) {
                     this.setObject(gateTile.x, gateTile.y, resWidth);
                  }
               }
            }
         }
      }

      xOffset = Math.min(points.size(), random.getIntBetween(12, 18));
      yOffset = TileRegistry.getTileID("deadwoodfloor");
      TicketSystemList<Preset> furniture = new TicketSystemList<>();
      furniture.addObject(100, new BedDresserPreset(FurnitureSet.deadwood, 2));
      furniture.addObject(100, new BenchPreset(FurnitureSet.deadwood, 2));
      furniture.addObject(100, new BookshelfClockPreset(FurnitureSet.deadwood, 2));
      furniture.addObject(100, new BookshelvesPreset(FurnitureSet.deadwood, 2, 3));
      furniture.addObject(100, new CabinetsPreset(FurnitureSet.deadwood, 2, 3));
      furniture.addObject(100, new DeskBookshelfPreset(FurnitureSet.deadwood, 2));
      furniture.addObject(100, new DinnerTablePreset(FurnitureSet.deadwood, 2));
      furniture.addObject(100, new DisplayStandClockPreset(FurnitureSet.deadwood, 2, null, null));
      furniture.addObject(100, new ModularDinnerTablePreset(FurnitureSet.deadwood, 2, 1));
      furniture.addObject(50, new ModularTablesPreset(FurnitureSet.deadwood, 2, 2, true));

      for (int i = 0; i < xOffset && !points.isEmpty(); i++) {
         int pointsIndex = random.nextInt(points.size());
         Float centerTile = points.remove(pointsIndex);
         if (this.isTileWithinBounds((int)centerTile.x, (int)centerTile.y)) {
            int houseWidth = random.getIntBetween(8, 14);
            int houseHeight = random.getIntBetween(7, 10);
            Preset preset = new FurnitureHousePreset(houseWidth, houseHeight, yOffset, WallSet.crypt, random, furniture, 0.8F);
            preset.addCanApplyRectEachPredicate(0, 0, houseWidth, houseHeight, 0, (level, levelX, levelY, dirx) -> !level.getTile(levelX, levelY).isFloor);
            AtomicReference<Point> doorTileRef = new AtomicReference<>();
            preset.onObjectApply((level, layerID, levelX, levelY, object, objectRotation, blackboard) -> {
               if (object == WallSet.crypt.doorClosed || object == WallSet.crypt.doorOpen) {
                  doorTileRef.set(new Point(levelX, levelY));
               }
            });
            int placeDir = random.nextInt(4);

            try {
               preset = preset.rotate(PresetRotation.toRotationAngle(placeDir));
            } catch (PresetRotateException var35) {
            }

            final Preset finalPreset = preset;
            AreaFinder posFinder = new AreaFinder((int)centerTile.x, (int)centerTile.y, 5, true) {
               @Override
               public boolean checkPoint(int x, int y) {
                  return finalPreset.canApplyToLevelCentered(GraveyardIncursionLevel.this, x, y);
               }
            };
            posFinder.runFinder();
            if (posFinder.hasFound()) {
               Point tile = posFinder.getFirstFind();
               finalPreset.applyToLevelCentered(this, tile.x, tile.y);
               Point currentTile = doorTileRef.get();
               if (currentTile != null) {
                  int angle = 0;
                  int resolutionX;
                  int resolutionY;
                  int offsetX;
                  int offsetY;
                  switch (placeDir) {
                     case 0: {
                        angle = random.getIntOffset(270, 45);
                        boolean doubleDoor = houseWidth % 2 == 0;
                        resolutionX = doubleDoor ? 2 : 3;
                        resolutionY = 2;
                        offsetX = -1;
                        offsetY = -1;
                        break;
                     }
                     case 1: {
                        angle = random.getIntOffset(0, 45);
                        boolean doubleDoor = houseHeight % 2 == 0;
                        resolutionX = 2;
                        resolutionY = doubleDoor ? 2 : 3;
                        offsetX = 1;
                        offsetY = -1;
                        break;
                     }
                     case 2: {
                        angle = random.getIntOffset(90, 45);
                        boolean doubleDoor = houseWidth % 2 == 0;
                        resolutionX = doubleDoor ? 2 : 3;
                        resolutionY = 2;
                        offsetX = -1;
                        offsetY = 1;
                        break;
                     }
                     case 3: {
                        angle = random.getIntOffset(180, 45);
                        boolean doubleDoor = houseHeight % 2 == 0;
                        resolutionX = 2;
                        resolutionY = doubleDoor ? 2 : 3;
                        offsetX = -1;
                        offsetY = -1;
                        break;
                     }
                     default:
                        resolutionX = 0;
                        resolutionY = 0;
                        offsetX = 0;
                        offsetY = 0;
                  }

                  Float dir = GameMath.getAngleDir(angle);
                  LinesGeneration.pathTilesBreak(
                     new java.awt.geom.Line2D.Float(currentTile.x, currentTile.y, currentTile.x + dir.x * 30.0F, currentTile.y + dir.y * 30.0F),
                     true,
                     (from, next) -> {
                        boolean found = false;

                        for (int x = 0; x < resolutionX; x++) {
                           for (int y = 0; y < resolutionY; y++) {
                              int tileX = next.x + x + offsetX;
                              int tileY = next.y + y + offsetY;
                              if (this.isTileWithinBounds(tileX, tileY) && !this.getTile(tileX, tileY).isFloor) {
                                 this.setTile(tileX, tileY, cryptPathTileID);
                                 this.setObject(tileX, tileY, 0);
                                 found = true;
                              }
                           }
                        }

                        return found;
                     }
                  );
               }
            }
         }
      }

      PresetGeneration presets = new PresetGeneration(this);
      int spawnPadding = 40;
      int spawnMidX = random.getIntOffset(this.tileWidth / 2, this.tileWidth / 2 - spawnPadding * 2);
      int spawnMidY = random.getIntOffset(this.tileHeight / 2, this.tileHeight / 2 - spawnPadding * 2);
      TriangleLine midPoint = voronoiLines.stream().min(Comparator.comparingDouble(t -> t.p1.distance(spawnMidX, spawnMidY))).orElse(null);
      boolean hasBiggerArenaPerk = altarData.hasPerk(IncursionPerksRegistry.BIGGER_ARENA);
      if (midPoint != null) {
         IncursionBiome.generateEntrance(
            this, presets, random, (int)midPoint.p1.x + 1, (int)midPoint.p1.y + 1, 30, cryptAshID, "cryptpath", null, "cryptcolumn", hasBiggerArenaPerk
         );
      } else {
         IncursionBiome.generateEntrance(
            this, presets, random, this.tileWidth / 2, this.tileHeight / 2, 30, cryptAshID, "cryptpath", null, "cryptcolumn", hasBiggerArenaPerk
         );
      }

      this.generatePresetsBasedOnPerks(altarData, incursionData, presets, random, this.baseBiome);
      this.generateOresBasedOnPerks(altarData, null, this, this.baseBiome, random);
      if (incursionData instanceof BiomeExtractionIncursionData) {
         GameObject extractionObject = ObjectRegistry.getObject("cryptnightsteelorerocksmall");
         GenerationTools.generateGuaranteedRandomVeins(
            this,
            random,
            25,
            1,
            1,
            (level, tileX, tileY) -> level.getTileID(tileX, tileY) == cryptAshID && extractionObject.canPlace(level, tileX, tileY, 0, false) == null,
            (level, tileX, tileY) -> extractionObject.placeObject(level, tileX, tileY, 0, false)
         );
      }

      GameObject upgradeShardObject = ObjectRegistry.getObject(this.upgradeShardsRockID);
      GenerationTools.generateGuaranteedRandomVeins(
         this,
         random,
         23,
         1,
         1,
         (level, tileX, tileY) -> level.getTileID(tileX, tileY) == cryptAshID && upgradeShardObject.canPlace(level, tileX, tileY, 0, false) == null,
         (level, tileX, tileY) -> upgradeShardObject.placeObject(level, tileX, tileY, 0, false)
      );
      GameObject alchemyShardObject = ObjectRegistry.getObject(this.alchemyShardsRockID);
      GenerationTools.generateGuaranteedRandomVeins(
         this,
         random,
         22,
         1,
         1,
         (level, tileX, tileY) -> level.getTileID(tileX, tileY) == cryptAshID && alchemyShardObject.canPlace(level, tileX, tileY, 0, false) == null,
         (level, tileX, tileY) -> alchemyShardObject.placeObject(level, tileX, tileY, 0, false)
      );
      GenerationTools.checkValid(this);
   }

   @Override
   public LightManager constructLightManager() {
      return new LightManager(this) {
         @Override
         public void updateAmbientLight() {
            if (this.ambientLightOverride != null) {
               this.ambientLight = this.ambientLightOverride;
            } else if (Settings.alwaysLight) {
               this.ambientLight = this.newLight(150.0F);
            } else {
               this.ambientLight = GraveyardIncursionLevel.this.lightManager.newLight(GraveyardIncursionLevel.GRAVEYARD_AMBIENT_LIGHT);
            }
         }
      };
   }

   @Override
   public Stream<WallShadowVariables> getWallShadows() {
      Stream<WallShadowVariables> out = super.getWallShadows();
      float moonLight = this.getWorldEntity().getMoonLightFloat();
      if (moonLight <= 0.0F) {
         return out;
      }

      float moonProgress = this.getWorldEntity().getMoonProgress();
      return moonProgress <= 0.0F ? out : Stream.concat(out, Stream.of(WallShadowVariables.fromProgress(moonLight, moonProgress, 16.0F, 160.0F)));
   }

   protected static class OpenDirection extends Point {
      public int dir;
      public int depth;

      public OpenDirection(int x, int y, int dir, int depth) {
         super(x, y);
         this.dir = dir;
         this.depth = depth;
      }
   }
}
