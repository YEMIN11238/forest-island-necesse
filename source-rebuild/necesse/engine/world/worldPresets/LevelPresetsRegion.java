package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.function.Predicate;
import java.util.stream.Stream;
import necesse.engine.GameLog;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.gameLoop.tickManager.PerformanceTimerUtils;
import necesse.engine.registries.WorldPresetRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.HashMapArrayList;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.PointHashMap;
import necesse.engine.util.PointHashSet;
import necesse.engine.util.TicketSystemList;
import necesse.engine.world.SpawnTileFinder;
import necesse.engine.world.World;
import necesse.engine.world.WorldFile;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

public class LevelPresetsRegion {
   public final WorldPresetsRegion worldRegion;
   public final LevelIdentifier identifier;
   protected HashMap<String, RegionOccupiedRectangles> occupiedRectanglesBoards = new HashMap<>();
   protected HashMap<Integer, LevelPresetsRegion.GenerationRegion> generationRegions = new HashMap<>();
   protected PointHashMap<Integer> generationRegionUniqueIDAtRegion = new PointHashMap<>();
   protected int generationRegionUniqueIDCounter = 1;
   protected ArrayList<LevelPresetsRegion.PlaceableWorldPreset> presets = new ArrayList<>();
   protected HashMapArrayList<Integer, Long> biomeRegions = new HashMapArrayList<>();
   protected HashMap<Integer, Integer> biomeIDWeights = new HashMap<>();
   protected int totalBiomeWeight = 0;
   protected HashSet<Integer> currentGenerationRuns = new HashSet<>();
   protected ArrayList<LevelPresetsRegion.GeneratedPresetData> generatedPresets = new ArrayList<>();
   public int unloadBuffer = 0;

   public LevelPresetsRegion(WorldPresetsRegion worldRegion, LevelIdentifier identifier, LevelPresetsRegion existingRegion, PerformanceTimerManager timer) {
      this.worldRegion = worldRegion;
      this.identifier = identifier;
      if (existingRegion != null) {
         Performance.record(timer, "copyBiomes", () -> {
            this.biomeRegions = existingRegion.biomeRegions;
            this.biomeIDWeights = existingRegion.biomeIDWeights;
            this.totalBiomeWeight = existingRegion.totalBiomeWeight;
         });
      } else {
         Performance.record(timer, "getBiomes", () -> {
            int startRegionX = GameMath.getRegionCoordByTile(worldRegion.startTileX);
            int startRegionY = GameMath.getRegionCoordByTile(worldRegion.startTileY);
            int endRegionX = GameMath.getRegionCoordByTile(worldRegion.startTileX + worldRegion.tileWidth - 1);
            int endRegionY = GameMath.getRegionCoordByTile(worldRegion.startTileY + worldRegion.tileHeight - 1);
            BiomeGeneratorStack biomeStack = worldRegion.worldEntity.getGeneratorStack();

            for (int regionX = startRegionX; regionX <= endRegionX; regionX++) {
               for (int regionY = startRegionY; regionY <= endRegionY; regionY++) {
                  int startTileX = GameMath.getTileCoordByRegion(regionX);
                  int startTileY = GameMath.getTileCoordByRegion(regionY);
                  int lastBiomeIDCount = 0;
                  int biomeID = biomeStack.getLazyBiomeID(startTileX, startTileY);
                  int lastBiomeID = biomeID;
                  lastBiomeIDCount++;
                  this.biomeRegions.add(biomeID, GameMath.getUniqueLongKey(regionX, regionY));
                  biomeID = biomeStack.getLazyBiomeID(startTileX + 16 - 1, startTileY);
                  if (biomeID != lastBiomeID) {
                     int finalLastBiomeIDCount = lastBiomeIDCount;
                     this.biomeIDWeights.compute(biomeID, (id, weight) -> weight == null ? finalLastBiomeIDCount : weight + finalLastBiomeIDCount);
                     lastBiomeID = biomeID;
                     lastBiomeIDCount = 1;
                  } else {
                     lastBiomeIDCount++;
                  }

                  this.biomeRegions.add(biomeID, GameMath.getUniqueLongKey(regionX, regionY));
                  biomeID = biomeStack.getLazyBiomeID(startTileX, startTileY + 16 - 1);
                  if (biomeID != lastBiomeID) {
                     int finalLastBiomeIDCount = lastBiomeIDCount;
                     this.biomeIDWeights.compute(biomeID, (id, weight) -> weight == null ? finalLastBiomeIDCount : weight + finalLastBiomeIDCount);
                     lastBiomeID = biomeID;
                     lastBiomeIDCount = 1;
                  } else {
                     lastBiomeIDCount++;
                  }

                  this.biomeRegions.add(biomeID, GameMath.getUniqueLongKey(regionX, regionY));
                  biomeID = biomeStack.getLazyBiomeID(startTileX + 16 - 1, startTileY + 16 - 1);
                  if (biomeID != lastBiomeID) {
                     int finalLastBiomeIDCount = lastBiomeIDCount;
                     this.biomeIDWeights.compute(biomeID, (id, weight) -> weight == null ? finalLastBiomeIDCount : weight + finalLastBiomeIDCount);
                     lastBiomeID = biomeID;
                     lastBiomeIDCount = 1;
                  } else {
                     lastBiomeIDCount++;
                  }

                  this.biomeRegions.add(biomeID, GameMath.getUniqueLongKey(regionX, regionY));
                  final int finalBiomeWeight = lastBiomeIDCount;
                  this.biomeIDWeights.compute(lastBiomeID, (id, weight) -> weight == null ? finalBiomeWeight : weight + finalBiomeWeight);
                  this.totalBiomeWeight += 4;
               }
            }
         });
      }
   }

   public synchronized void addOccupiedSpaceBoard(String board, Rectangle tileRectangle) {
      this.occupiedRectanglesBoards.compute(board, (key, last) -> {
         if (last == null) {
            last = new RegionOccupiedRectangles();
         }

         return (RegionOccupiedRectangles)last;
      }).add(tileRectangle);
   }

   public synchronized LevelPresetsRegion.TiledPlaceableWorldPreset addPreset(
      WorldPreset preset,
      Iterable<Rectangle> occupiedTileRectangles,
      String[] addToOccupiedSpaceBoards,
      LevelPresetsRegion.WorldPresetPlaceFunction placeFunction
   ) {
      RegionPointsSet regions = new RegionPointsSet();

      for (Rectangle occupiedTileRectangle : occupiedTileRectangles) {
         regions.addTileRectangle(occupiedTileRectangle);
         if (addToOccupiedSpaceBoards != null) {
            for (String board : addToOccupiedSpaceBoards) {
               this.addOccupiedSpaceBoard(board, occupiedTileRectangle);
            }
         }
      }

      LevelPresetsRegion.TiledPlaceableWorldPreset worldPreset = new LevelPresetsRegion.TiledPlaceableWorldPreset(
         this, preset, this.presets.size(), regions, occupiedTileRectangles, placeFunction
      );
      this.addPresetToGenerationRegion(worldPreset);
      this.presets.add(worldPreset);
      return worldPreset;
   }

   public LevelPresetsRegion.TiledPlaceableWorldPreset addPreset(
      WorldPreset preset, final Rectangle occupiedRectangle, String[] addToOccupiedSpaceBoards, LevelPresetsRegion.WorldPresetPlaceFunction placeFunction
   ) {
      LinkedList<Rectangle> iterable = new LinkedList<Rectangle>() {
         {
            this.add(occupiedRectangle);
         }
      };
      return this.addPreset(preset, iterable, addToOccupiedSpaceBoards, placeFunction);
   }

   public LevelPresetsRegion.TiledPlaceableWorldPreset addPreset(
      WorldPreset preset,
      int tileX,
      int tileY,
      Dimension dimension,
      String[] addToOccupiedSpaceBoards,
      LevelPresetsRegion.WorldPresetPlaceFunction placeFunction
   ) {
      return this.addPreset(preset, new Rectangle(tileX, tileY, dimension.width, dimension.height), addToOccupiedSpaceBoards, placeFunction);
   }

   public LevelPresetsRegion.TiledPlaceableWorldPreset addPreset(
      WorldPreset preset, Iterable<Rectangle> occupiedTileRectangles, String addToOccupiedSpaceBoard, LevelPresetsRegion.WorldPresetPlaceFunction placeFunction
   ) {
      return this.addPreset(preset, occupiedTileRectangles, addToOccupiedSpaceBoard == null ? null : new String[]{addToOccupiedSpaceBoard}, placeFunction);
   }

   public LevelPresetsRegion.TiledPlaceableWorldPreset addPreset(
      WorldPreset preset, Rectangle occupiedRectangle, String addToOccupiedSpaceBoard, LevelPresetsRegion.WorldPresetPlaceFunction placeFunction
   ) {
      return this.addPreset(preset, occupiedRectangle, addToOccupiedSpaceBoard == null ? null : new String[]{addToOccupiedSpaceBoard}, placeFunction);
   }

   public LevelPresetsRegion.TiledPlaceableWorldPreset addPreset(
      WorldPreset preset, int tileX, int tileY, Dimension dimension, String addToOccupiedSpaceBoard, LevelPresetsRegion.WorldPresetPlaceFunction placeFunction
   ) {
      return this.addPreset(preset, tileX, tileY, dimension, addToOccupiedSpaceBoard == null ? null : new String[]{addToOccupiedSpaceBoard}, placeFunction);
   }

   public synchronized LevelPresetsRegion.PlaceableWorldPreset addPresetToRegions(
      WorldPreset preset, RegionPointsSet regions, LevelPresetsRegion.WorldPresetPlaceFunction placeFunction
   ) {
      LevelPresetsRegion.PlaceableWorldPreset worldPreset = new LevelPresetsRegion.RegionPlaceableWorldPreset(
         this, preset, this.presets.size(), regions, placeFunction
      );
      this.addPresetToGenerationRegion(worldPreset);
      this.presets.add(worldPreset);
      return worldPreset;
   }

   private void addPresetToGenerationRegion(LevelPresetsRegion.PlaceableWorldPreset worldPreset) {
      LevelPresetsRegion.GenerationRegion generationRegion = null;

      for (Point region : worldPreset.occupiedRegions) {
         int regionUniqueID = this.generationRegionUniqueIDAtRegion.getOrDefault(region.x, region.y, -1);
         if (regionUniqueID == -1) {
            if (generationRegion == null) {
               generationRegion = new LevelPresetsRegion.GenerationRegion(this.generationRegionUniqueIDCounter++);
               this.generationRegionUniqueIDAtRegion.put(region.x, region.y, generationRegion.uniqueID);
               this.generationRegions.put(generationRegion.uniqueID, generationRegion);
            } else {
               this.generationRegionUniqueIDAtRegion.put(region.x, region.y, generationRegion.uniqueID);
            }
         } else {
            LevelPresetsRegion.GenerationRegion existingGenerationRegion = this.generationRegions.get(regionUniqueID);
            if (existingGenerationRegion != generationRegion && generationRegion != null) {
               existingGenerationRegion.presets.addAll(generationRegion.presets);
               existingGenerationRegion.regions.addAll(generationRegion.regions);
               this.generationRegions.remove(generationRegion.uniqueID);

               for (Point point : generationRegion.regions) {
                  this.generationRegionUniqueIDAtRegion.put(point.x, point.y, existingGenerationRegion.uniqueID);
               }

               for (LevelPresetsRegion.PlaceableWorldPreset preset : generationRegion.presets) {
                  preset.generationRegionUniqueID = existingGenerationRegion.uniqueID;
               }
            }

            generationRegion = existingGenerationRegion;
         }

         generationRegion.regions.add(region.x, region.y);
      }

      if (generationRegion == null) {
         throw new IllegalStateException("Attempted to add world preset without any occupied regions: " + worldPreset.preset.getStringID());
      }

      worldPreset.generationRegionUniqueID = generationRegion.uniqueID;
      generationRegion.presets.add(worldPreset);
   }

   public boolean isRectangleOccupied(String board, Rectangle tileRectangle) {
      RegionOccupiedRectangles occupiedRectangles = this.occupiedRectanglesBoards.get(board);
      return occupiedRectangles != null && occupiedRectangles.isOccupied(tileRectangle);
   }

   public boolean isRectangleOccupied(String board, int tileX, int tileY, int width, int height) {
      return this.isRectangleOccupied(board, new Rectangle(tileX, tileY, width, height));
   }

   public boolean isRectangleOccupied(String[] boards, Rectangle tileRectangle) {
      return Arrays.stream(boards).anyMatch(board -> this.isRectangleOccupied(board, tileRectangle));
   }

   public boolean isRectangleOccupied(String[] boards, int tileX, int tileY, int width, int height) {
      return this.isRectangleOccupied(boards, new Rectangle(tileX, tileY, width, height));
   }

   public boolean hasAnyOfBiome(int biomeID) {
      return !this.biomeRegions.isEmpty(biomeID);
   }

   public float getBiomeWeight(int biomeID) {
      Integer weight = this.biomeIDWeights.get(biomeID);
      return weight == null ? 0.0F : (float)weight.intValue() / this.totalBiomeWeight;
   }

   public Iterable<Point> getBiomeRegions(int biomeID) {
      ArrayList<Long> regionUniqueIDs = this.biomeRegions.get(biomeID);
      return GameUtils.mapIterable(regionUniqueIDs.iterator(), uniqueKey -> {
         int regionX = GameMath.getXFromUniqueLongKey(uniqueKey);
         int regionY = GameMath.getYFromUniqueLongKey(uniqueKey);
         return new Point(regionX, regionY);
      });
   }

   public boolean isRegionBiome(int regionX, int regionY, int biomeID) {
      return this.biomeRegions.contains(biomeID, GameMath.getUniqueLongKey(regionX, regionY));
   }

   public Point getRandomBiomeRegion(GameRandom random, int biomeID) {
      ArrayList<Long> regionUniqueIDs = this.biomeRegions.get(biomeID);
      if (regionUniqueIDs.isEmpty()) {
         return null;
      }

      long regionUniqueID = random.getOneOf(regionUniqueIDs);
      int regionX = GameMath.getXFromUniqueLongKey(regionUniqueID);
      int regionY = GameMath.getYFromUniqueLongKey(regionUniqueID);
      return new Point(regionX, regionY);
   }

   public Point getRandomBiomeRegion(GameRandom random, Iterable<Integer> biomeIDs) {
      TicketSystemList<ArrayList<Long>> biomeRegionsList = new TicketSystemList<>();

      for (int biomeID : biomeIDs) {
         ArrayList<Long> regionUniqueIDs = this.biomeRegions.get(biomeID);
         if (!regionUniqueIDs.isEmpty()) {
            biomeRegionsList.addObject(regionUniqueIDs.size(), regionUniqueIDs);
         }
      }

      ArrayList<Long> regionUniqueIDs = biomeRegionsList.getRandomObject(random);
      if (regionUniqueIDs != null && !regionUniqueIDs.isEmpty()) {
         long regionUniqueID = random.getOneOf(regionUniqueIDs);
         int regionX = GameMath.getXFromUniqueLongKey(regionUniqueID);
         int regionY = GameMath.getYFromUniqueLongKey(regionUniqueID);
         return new Point(regionX, regionY);
      } else {
         return null;
      }
   }

   public void finalizeRegionSetup() {
      for (LevelPresetsRegion.GenerationRegion generationRegion : this.generationRegions.values()) {
         Comparator<LevelPresetsRegion.PlaceableWorldPreset> presetsComparator = Comparator.comparingInt(preset -> -preset.preset.getPriority());
         presetsComparator = presetsComparator.thenComparingInt(preset -> preset.index);
         generationRegion.presets.sort(presetsComparator);
      }
   }

   protected LevelPresetsRegion.GenerationRegion getGenerationRegionAtRegion(int regionX, int regionY) {
      int generationRegionUniqueID = this.generationRegionUniqueIDAtRegion.getOrDefault(regionX, regionY, -1);
      return generationRegionUniqueID == -1 ? null : this.generationRegions.get(generationRegionUniqueID);
   }

   public synchronized int startGenerateRegion(Region region) {
      LevelPresetsRegion.GenerationRegion generationRegion = this.getGenerationRegionAtRegion(region.regionX, region.regionY);
      if (generationRegion == null) {
         return -1;
      }

      if (this.currentGenerationRuns.contains(generationRegion.uniqueID)) {
         return -1;
      }

      if (WorldPresetsRegion.DEBUG_PLACING_PRESETS && !this.currentGenerationRuns.isEmpty()) {
         String[] otherGenerationRuns = this.currentGenerationRuns.stream().map(id -> this.generationRegions.get(id)).map(r -> {
            String regionString = r.regions.stream().findFirst().map(pos -> pos.x + "x" + pos.y).orElse(null);
            return r.uniqueID + " (" + regionString + ")";
         }).toArray(String[]::new);
         String regionString = generationRegion.regions.stream().findFirst().map(pos -> pos.x + "x" + pos.y).orElse(null);
         System.out
            .println(
               "ALREADY RUNNING ANOTHER GENERATION RUN OTHER THAN "
                  + generationRegion.uniqueID
                  + " ("
                  + regionString
                  + ")?? "
                  + Arrays.toString(otherGenerationRuns)
            );
      }

      this.currentGenerationRuns.add(generationRegion.uniqueID);

      for (LevelPresetsRegion.PlaceableWorldPreset preset : generationRegion.presets) {
         for (Point regionPos : preset.occupiedRegions) {
            if ((regionPos.x != region.regionX || regionPos.y != region.regionY)
               && region.manager.level.regionManager.isRegionGenerated(regionPos.x, regionPos.y)) {
               preset.hasAlreadyGeneratedRegion = true;
               if (WorldPresetsRegion.DEBUG_PLACING_PRESETS) {
                  System.out
                     .println(
                        "ALREADY GENERATED "
                           + preset.getDebugName()
                           + ", REGION: "
                           + regionPos.x
                           + "x"
                           + regionPos.y
                           + ", STARTED FROM: "
                           + region.regionX
                           + "x"
                           + region.regionY
                           + ", RUN: "
                           + generationRegion.uniqueID
                     );
               }
               break;
            }
         }
      }

      return generationRegion.uniqueID;
   }

   public synchronized void runGenerateRegion(int generationUniqueID, Region region, int customSeed) {
      if (generationUniqueID != -1) {
         LevelPresetsRegion.GenerationRegion generationRegion = this.generationRegions.get(generationUniqueID);

         try {
            Level level = region.manager.level;
            boolean debugTimer = WorldPresetsRegion.DEBUG_PLACING_PRESETS;
            PerformanceTimerManager timer;
            if (debugTimer) {
               timer = new PerformanceTimerManager(false);
            } else if (this.worldRegion.worldEntity.isServer()) {
               timer = this.worldRegion.worldEntity.getServer().tickManager();
            } else if (this.worldRegion.worldEntity.isClient()) {
               timer = this.worldRegion.worldEntity.getClient().tickManager();
            } else {
               timer = null;
            }

            long time = System.nanoTime();

            for (Point regionPos : generationRegion.regions) {
               if ((regionPos.x != region.regionX || regionPos.y != region.regionY) && this.worldRegion.isRegionWithinBounds(regionPos.x, regionPos.y)) {
                  Region newRegion = region.getRegion(regionPos.x, regionPos.y, true);
                  if (this.worldRegion.worldEntity.keepPresetGeneratedRegionsLoaded) {
                     newRegion.unloadRegionBuffer = region.unloadRegionBuffer;
                  }
               }
            }

            Performance.addRecordedTime(timer, "generateRegions", System.nanoTime() - time);
            boolean placeAnyPresets = Performance.record(timer, "placePresets", () -> {
               boolean placedAnyPresets = false;

               for (LevelPresetsRegion.PlaceableWorldPreset worldPresetx : generationRegion.presets) {
                  if (!worldPresetx.hasAlreadyGeneratedRegionOrBlockedBySpawn()) {
                     placedAnyPresets = true;
                     Performance.record(timer, worldPresetx.preset.getStringID(), () -> {
                        GameRandom random = customSeed == 0 ? this.worldRegion.worldEntity.getNewWorldRandom() : new GameRandom(customSeed);
                        random = random.nextSeeded(worldPresetx.firstRegion.x).nextSeeded(worldPresetx.firstRegion.y).nextSeeded(worldPresetx.index);

                        try {
                           worldPresetx.place(random, level, timer);
                           if (worldPresetx instanceof LevelPresetsRegion.FoundPresetData && ((LevelPresetsRegion.FoundPresetData)worldPresetx).shouldSave()) {
                              LevelPresetsRegion.FoundPresetData presetData = (LevelPresetsRegion.FoundPresetData)worldPresetx;
                              if (presetData.getTileX() != Integer.MIN_VALUE && presetData.getTileY() != Integer.MIN_VALUE) {
                                 this.generatedPresets.add(new LevelPresetsRegion.GeneratedPresetData(presetData));
                              }
                           }
                        } catch (Exception e) {
                           e.printStackTrace();
                        }
                     });
                  }
               }

               return placedAnyPresets;
            });

            for (Point regionPos : generationRegion.regions) {
               if (this.worldRegion.isRegionWithinBounds(regionPos.x, regionPos.y)) {
                  level.removeDirtyRegion(regionPos.x, regionPos.y);
               }
            }

            for (LevelPresetsRegion.PlaceableWorldPreset worldPreset : generationRegion.presets) {
               if (!worldPreset.hasAlreadyGeneratedRegionOrBlockedBySpawn()) {
                  Performance.record(timer, "validateTiles", () -> {
                     for (Rectangle tileRectangle : worldPreset.getOccupiedTileRectangles()) {
                        Region.checkTilesGenerationValid(level, tileRectangle);
                     }
                  });
               }
            }

            if (debugTimer && placeAnyPresets) {
               String regionString = generationRegion.regions.stream().findFirst().map(pos -> pos.x + "x" + pos.y).orElse(null);
               System.out.println("PERFORMANCE RESULTS FOR PLACING PRESETS GENERATION " + generationRegion.uniqueID + " (" + regionString + "):");
               System.out.println("TOTAL GENERATED REGIONS: " + generationRegion.regions.size());
               PerformanceTimerUtils.printPerformanceTimer(timer.getCurrentRootPerformanceTimer());
            }
         } finally {
            this.currentGenerationRuns.remove(generationRegion.uniqueID);
            region.manager.level.runDirtyRegionSync();
         }
      }
   }

   public Stream<LevelPresetsRegion.FoundPresetData> streamPresets(Predicate<LevelPresetsRegion.PlaceableWorldPreset> filter) {
      return Stream.concat(
         this.generatedPresets.stream(),
         this.presets
            .stream()
            .filter(preset -> preset instanceof LevelPresetsRegion.FoundPresetData)
            .filter(filter)
            .map(preset -> (LevelPresetsRegion.FoundPresetData)preset)
      );
   }

   public void loadGeneratedPresetsFile() {
      World world = this.worldRegion.worldEntity.serverWorld;
      if (world != null) {
         WorldFile file = world.fileSystem.getPresetRegionFile(this.identifier, this.worldRegion.worldPresetRegionX, this.worldRegion.worldPresetRegionY);

         try {
            if (file.exists()) {
               LoadData save = new LoadData(file);
               this.generatedPresets.clear();

               for (LoadData presetSave : save.getLoadDataByName("PRESET")) {
                  try {
                     this.generatedPresets.add(new LevelPresetsRegion.GeneratedPresetData(presetSave));
                  } catch (LoadDataException e) {
                     GameLog.warn.println("Error loading generated preset: " + e.getMessage());
                  } catch (Exception e) {
                     e.printStackTrace();
                  }
               }
            }
         } catch (Exception e) {
            System.err
               .println(
                  "Could not load "
                     + this.identifier
                     + " generated presets from file at "
                     + this.worldRegion.startLevelRegionX
                     + "x"
                     + this.worldRegion.startLevelRegionY
                     + " ("
                     + this.worldRegion.worldPresetRegionX
                     + "x"
                     + this.worldRegion.worldPresetRegionY
                     + ")"
               );
            e.printStackTrace();
         }
      }
   }

   public void saveGeneratedPresetsFile() {
      World world = this.worldRegion.worldEntity.serverWorld;
      if (world != null && !this.generatedPresets.isEmpty()) {
         WorldFile file = world.fileSystem.getPresetRegionFile(this.identifier, this.worldRegion.worldPresetRegionX, this.worldRegion.worldPresetRegionY);
         SaveData save = new SaveData("");

         for (LevelPresetsRegion.GeneratedPresetData presetData : this.generatedPresets) {
            save.addSaveData(presetData.getSaveData("PRESET"));
         }

         save.saveScript(file);
      }
   }

   public Iterable<LevelPresetsRegion.PresetDebugData> getDebugData() {
      return GameUtils.mapIterable(this.presets.iterator(), preset -> preset);
   }

   public Iterable<LevelPresetsRegion.GenerationRegionDebugData> getDebugDataByGenerationRegions() {
      return GameUtils.mapIterable(this.generationRegions.values().iterator(), generationRegion -> generationRegion);
   }

   public void markPresetsToNotGenerate(Predicate<LevelPresetsRegion.PlaceableWorldPreset> filter) {
      for (LevelPresetsRegion.PlaceableWorldPreset preset : this.presets) {
         if (filter.test(preset)) {
            preset.presetBlockedBySpawn = true;
         }
      }
   }

   public interface FoundPresetData {
      int getTileX();

      int getTileY();

      void overrideTilePosition(int var1, int var2);

      boolean shouldSave();

      void setShouldSave(boolean var1);

      WorldPreset getPreset();

      String getFoundIdentifier();

      boolean isGenerated();
   }

   protected static class GeneratedPresetData implements LevelPresetsRegion.FoundPresetData {
      public final WorldPreset preset;
      public final int tileX;
      public final int tileY;
      public final String foundIdentifier;

      protected GeneratedPresetData(LevelPresetsRegion.FoundPresetData presetData) {
         this.preset = presetData.getPreset();
         this.tileX = presetData.getTileX();
         this.tileY = presetData.getTileY();
         this.foundIdentifier = presetData.getFoundIdentifier();
      }

      protected GeneratedPresetData(LoadData save) throws LoadDataException {
         String presetStringID = save.getUnsafeString("presetStringID", null, false);
         if (presetStringID == null) {
            throw new LoadDataException("Missing generated presetStringID");
         }

         this.preset = WorldPresetRegistry.getPreset(presetStringID);
         if (this.preset == null) {
            throw new LoadDataException("Invalid generated presetStringID: " + presetStringID);
         }

         this.tileX = save.getInt("tileX", Integer.MIN_VALUE, false);
         this.tileY = save.getInt("tileY", Integer.MIN_VALUE, false);
         if (this.tileX != Integer.MIN_VALUE && this.tileY != Integer.MIN_VALUE) {
            this.foundIdentifier = save.getSafeString("foundIdentifier", null, false);
         } else {
            throw new LoadDataException("Missing generated preset tile");
         }
      }

      public SaveData getSaveData(String name) {
         SaveData save = new SaveData(name);
         save.addUnsafeString("presetStringID", this.preset.getStringID());
         save.addInt("tileX", this.tileX);
         save.addInt("tileY", this.tileY);
         if (this.foundIdentifier != null) {
            save.addSafeString("foundIdentifier", this.foundIdentifier);
         }

         return save;
      }

      @Override
      public int getTileX() {
         return this.tileX;
      }

      @Override
      public int getTileY() {
         return this.tileY;
      }

      @Override
      public void overrideTilePosition(int tileX, int tileY) {
         throw new IllegalStateException("Cannot override tile position of generated preset");
      }

      @Override
      public void setShouldSave(boolean shouldSave) {
         throw new IllegalStateException("Cannot change shouldSave of generated preset");
      }

      @Override
      public boolean shouldSave() {
         return true;
      }

      @Override
      public WorldPreset getPreset() {
         return this.preset;
      }

      @Override
      public String getFoundIdentifier() {
         return this.foundIdentifier;
      }

      @Override
      public boolean isGenerated() {
         return true;
      }
   }

   protected static class GenerationRegion implements LevelPresetsRegion.GenerationRegionDebugData {
      public final int uniqueID;
      public final PointHashSet regions = new PointHashSet();
      public final ArrayList<LevelPresetsRegion.PlaceableWorldPreset> presets = new ArrayList<>();

      public GenerationRegion(int uniqueID) {
         this.uniqueID = uniqueID;
      }

      @Override
      public int getUniqueID() {
         return this.uniqueID;
      }

      @Override
      public Iterable<LevelPresetsRegion.PresetDebugData> getPresetDebugData() {
         return GameUtils.mapIterable(this.presets.iterator(), preset -> preset);
      }

      @Override
      public Iterable<Point> getRegions() {
         return this.regions;
      }
   }

   public interface GenerationRegionDebugData {
      int getUniqueID();

      Iterable<LevelPresetsRegion.PresetDebugData> getPresetDebugData();

      Iterable<Point> getRegions();
   }

   public abstract static class PlaceableWorldPreset implements LevelPresetsRegion.PresetDebugData {
      public final WorldPreset preset;
      protected int generationRegionUniqueID = -1;
      public final int index;
      public final RegionPointsSet occupiedRegions;
      public final Point firstRegion;
      protected String debugName;
      protected boolean hasAlreadyGeneratedRegion = false;
      protected boolean presetBlockedBySpawn = false;
      protected int removeIfWithinSpawnRegionRange;

      protected PlaceableWorldPreset(LevelPresetsRegion presetsRegion, WorldPreset worldPreset, int index, RegionPointsSet occupiedRegions) {
         this.preset = worldPreset;
         this.index = index;
         this.occupiedRegions = occupiedRegions;
         this.removeIfWithinSpawnRegionRange = presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER)
            ? SpawnTileFinder.CLEAR_SPAWN_REGION_RANGE
            : -1;
         Comparator<Point> comparator = Comparator.comparingInt(e -> e.x);
         comparator = comparator.thenComparingInt(e -> e.y);
         this.firstRegion = occupiedRegions.streamRegions().min(comparator).orElse(null);
         if (this.firstRegion == null) {
            throw new IllegalStateException("WorldPreset must have at least one occupied region, but none were found for preset: " + worldPreset.getStringID());
         }
      }

      public LevelPresetsRegion.PlaceableWorldPreset setDebugName(String debugName) {
         this.debugName = debugName;
         return this;
      }

      public LevelPresetsRegion.PlaceableWorldPreset setDontRemoveIfNearbySpawn() {
         this.removeIfWithinSpawnRegionRange = 0;
         return this;
      }

      public LevelPresetsRegion.PlaceableWorldPreset setRemoveIfWithinSpawnRegionRange(int regionRange) {
         this.removeIfWithinSpawnRegionRange = regionRange;
         return this;
      }

      public int getRemoveIfWithinSpawnRegionRange() {
         return this.removeIfWithinSpawnRegionRange;
      }

      public abstract void place(GameRandom var1, Level var2, PerformanceTimerManager var3);

      @Override
      public String getDebugName() {
         return this.preset.getStringID()
            + ":"
            + this.index
            + "@"
            + Integer.toHexString(this.hashCode())
            + (this.debugName == null ? "" : "\n" + this.debugName);
      }

      @Override
      public Iterable<Point> getOccupiedRegions() {
         return this.occupiedRegions;
      }

      @Override
      public int getGenerationRegionUniqueID() {
         return this.generationRegionUniqueID;
      }

      public boolean hasAlreadyGeneratedRegionOrBlockedBySpawn() {
         return this.hasAlreadyGeneratedRegion || this.presetBlockedBySpawn;
      }
   }

   public interface PresetDebugData {
      String getDebugName();

      Iterable<Point> getOccupiedRegions();

      Iterable<Rectangle> getOccupiedTileRectangles();

      int getGenerationRegionUniqueID();
   }

   protected static class RegionPlaceableWorldPreset extends LevelPresetsRegion.PlaceableWorldPreset {
      public final LevelPresetsRegion.WorldPresetPlaceFunction placeFunction;

      public RegionPlaceableWorldPreset(
         LevelPresetsRegion presetsRegion,
         WorldPreset worldPreset,
         int index,
         RegionPointsSet occupiedRegions,
         LevelPresetsRegion.WorldPresetPlaceFunction placeFunction
      ) {
         super(presetsRegion, worldPreset, index, occupiedRegions);
         this.placeFunction = placeFunction;
      }

      @Override
      public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
         this.placeFunction.place(random, level, timer);
      }

      @Override
      public Iterable<Rectangle> getOccupiedTileRectangles() {
         return Collections.emptyList();
      }
   }

   public static class TiledPlaceableWorldPreset extends LevelPresetsRegion.PlaceableWorldPreset implements LevelPresetsRegion.FoundPresetData {
      public final Iterable<Rectangle> occupiedTileRectangles;
      private int averageTileX;
      private int averageTileY;
      public final LevelPresetsRegion.WorldPresetPlaceFunction placeFunction;
      protected boolean shouldSave;
      public String foundIdentifier;

      public TiledPlaceableWorldPreset(
         LevelPresetsRegion presetsRegion,
         WorldPreset worldPreset,
         int index,
         RegionPointsSet occupiedRegions,
         Iterable<Rectangle> occupiedTileRectangles,
         LevelPresetsRegion.WorldPresetPlaceFunction placeFunction
      ) {
         super(presetsRegion, worldPreset, index, occupiedRegions);
         this.shouldSave = worldPreset.shouldSaveGenerated;
         this.occupiedTileRectangles = occupiedTileRectangles;
         int totalTileX = 0;
         int totalTileY = 0;
         int count = 0;

         for (Rectangle tileRectangle : occupiedTileRectangles) {
            totalTileX += tileRectangle.x + tileRectangle.width / 2;
            totalTileY += tileRectangle.y + tileRectangle.height / 2;
            count++;
         }

         this.averageTileX = count > 0 ? totalTileX / count : Integer.MIN_VALUE;
         this.averageTileY = count > 0 ? totalTileY / count : Integer.MIN_VALUE;
         this.placeFunction = placeFunction;
      }

      @Override
      public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
         this.placeFunction.place(random, level, timer);
      }

      @Override
      public Iterable<Rectangle> getOccupiedTileRectangles() {
         return this.occupiedTileRectangles;
      }

      @Override
      public int getTileX() {
         return this.averageTileX;
      }

      @Override
      public int getTileY() {
         return this.averageTileY;
      }

      @Override
      public void overrideTilePosition(int tileX, int tileY) {
         this.averageTileX = tileX;
         this.averageTileY = tileY;
      }

      @Override
      public void setShouldSave(boolean shouldSave) {
         this.shouldSave = shouldSave;
      }

      @Override
      public boolean shouldSave() {
         return this.shouldSave;
      }

      @Override
      public WorldPreset getPreset() {
         return this.preset;
      }

      @Override
      public String getFoundIdentifier() {
         return this.foundIdentifier;
      }

      @Override
      public boolean isGenerated() {
         return false;
      }
   }

   public interface WorldPresetPlaceFunction {
      void place(GameRandom var1, Level var2, PerformanceTimerManager var3);
   }
}
