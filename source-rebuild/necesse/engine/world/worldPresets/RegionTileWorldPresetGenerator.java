package necesse.engine.world.worldPresets;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.function.Consumer;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

public class RegionTileWorldPresetGenerator {
   private final HashMap<Long, RegionTileWorldPresetGenerator.RegionPlace> placeFunctions = new HashMap<>();

   public void onRegionGenerated(int regionX, int regionY, RegionTileWorldPresetGenerator.RegionGeneratedFunction function) {
      long key = GameMath.getUniqueLongKey(regionX, regionY);
      this.placeFunctions.compute(key, (k, v) -> {
         if (v == null) {
            v = new RegionTileWorldPresetGenerator.RegionPlace();
         }

         return (RegionTileWorldPresetGenerator.RegionPlace)v;
      }).regionGeneratedFunctions.add(function);
   }

   public void onRegionsGenerated(
      int startRegionX, int startRegionY, int endRegionX, int endRegionY, RegionTileWorldPresetGenerator.RegionGeneratedFunction function
   ) {
      for (int regionX = startRegionX; regionX <= endRegionX; regionX++) {
         for (int regionY = startRegionY; regionY <= endRegionY; regionY++) {
            this.onRegionGenerated(regionX, regionY, function);
         }
      }
   }

   public void onRegionsGeneratedByTile(
      int startTileX, int startTileY, int endTileX, int endTileY, RegionTileWorldPresetGenerator.RegionGeneratedFunction function
   ) {
      int startRegionX = GameMath.getRegionCoordByTile(startTileX);
      int startRegionY = GameMath.getRegionCoordByTile(startTileY);
      int endRegionX = GameMath.getRegionCoordByTile(endTileX);
      int endRegionY = GameMath.getRegionCoordByTile(endTileY);
      this.onRegionsGenerated(startRegionX, startRegionY, endRegionX, endRegionY, function);
   }

   public void addTile(int tileX, int tileY, RegionTileWorldPresetGenerator.TilePlaceFunction placeFunction) {
      int regionX = GameMath.getRegionCoordByTile(tileX);
      int regionY = GameMath.getRegionCoordByTile(tileY);
      long key = GameMath.getUniqueLongKey(regionX, regionY);
      this.placeFunctions.compute(key, (k, v) -> {
         if (v == null) {
            v = new RegionTileWorldPresetGenerator.RegionPlace();
         }

         return (RegionTileWorldPresetGenerator.RegionPlace)v;
      }).tilePlaces.add(new RegionTileWorldPresetGenerator.TilePlace(tileX, tileY, placeFunction));
   }

   public void addToRegion(WorldPreset preset, LevelPresetsRegion presetsRegion, Consumer<LevelPresetsRegion.PlaceableWorldPreset> modifier) {
      for (Entry<Long, RegionTileWorldPresetGenerator.RegionPlace> entry : this.placeFunctions.entrySet()) {
         long key = entry.getKey();
         final int regionX = GameMath.getXFromUniqueLongKey(key);
         final int regionY = GameMath.getYFromUniqueLongKey(key);
         final RegionTileWorldPresetGenerator.RegionPlace value = entry.getValue();
         RegionPointsSet regionsSet = new RegionPointsSet();
         regionsSet.addRegion(regionX, regionY);
         LevelPresetsRegion.PlaceableWorldPreset placeableWorldPreset = presetsRegion.addPresetToRegions(
            preset, regionsSet, new LevelPresetsRegion.WorldPresetPlaceFunction() {
               @Override
               public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                  for (RegionTileWorldPresetGenerator.TilePlace placeFunctions : value.tilePlaces) {
                     placeFunctions.placeFunction.place(random, level, placeFunctions.tileX, placeFunctions.tileY, timer);
                  }

                  if (!value.regionGeneratedFunctions.isEmpty()) {
                     Region region = level.regionManager.getRegion(regionX, regionY, true);
                     if (region != null) {
                        for (RegionTileWorldPresetGenerator.RegionGeneratedFunction regionGeneratedFunction : value.regionGeneratedFunctions) {
                           regionGeneratedFunction.onRegionGenerated(random, level, region, timer);
                        }
                     }
                  }
               }
            }
         );
         if (modifier != null) {
            modifier.accept(placeableWorldPreset);
         }
      }
   }

   public void addToRegion(WorldPreset preset, LevelPresetsRegion presetsRegion) {
      this.addToRegion(preset, presetsRegion, null);
   }

   public void forEachRegion(RegionTileWorldPresetGenerator.ForEachFunction handler) {
      for (Entry<Long, RegionTileWorldPresetGenerator.RegionPlace> entry : this.placeFunctions.entrySet()) {
         long key = entry.getKey();
         final int regionX = GameMath.getXFromUniqueLongKey(key);
         final int regionY = GameMath.getYFromUniqueLongKey(key);
         final RegionTileWorldPresetGenerator.RegionPlace value = entry.getValue();
         handler.handle(regionX, regionY, new LevelPresetsRegion.WorldPresetPlaceFunction() {
            @Override
            public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
               for (RegionTileWorldPresetGenerator.TilePlace placeFunctions : value.tilePlaces) {
                  placeFunctions.placeFunction.place(random, level, placeFunctions.tileX, placeFunctions.tileY, timer);
               }

               if (!value.regionGeneratedFunctions.isEmpty()) {
                  Region region = level.regionManager.getRegion(regionX, regionY, true);
                  if (region != null) {
                     for (RegionTileWorldPresetGenerator.RegionGeneratedFunction regionGeneratedFunction : value.regionGeneratedFunctions) {
                        regionGeneratedFunction.onRegionGenerated(random, level, region, timer);
                     }
                  }
               }
            }
         });
      }
   }

   @FunctionalInterface
   public interface ForEachFunction {
      void handle(int var1, int var2, LevelPresetsRegion.WorldPresetPlaceFunction var3);
   }

   @FunctionalInterface
   public interface RegionGeneratedFunction {
      void onRegionGenerated(GameRandom var1, Level var2, Region var3, PerformanceTimerManager var4);
   }

   private static class RegionPlace {
      public LinkedList<RegionTileWorldPresetGenerator.RegionGeneratedFunction> regionGeneratedFunctions = new LinkedList<>();
      public LinkedList<RegionTileWorldPresetGenerator.TilePlace> tilePlaces = new LinkedList<>();

      private RegionPlace() {
      }
   }

   private static class TilePlace {
      public final int tileX;
      public final int tileY;
      public final RegionTileWorldPresetGenerator.TilePlaceFunction placeFunction;

      public TilePlace(int tileX, int tileY, RegionTileWorldPresetGenerator.TilePlaceFunction placeFunction) {
         this.tileX = tileX;
         this.tileY = tileY;
         this.placeFunction = placeFunction;
      }
   }

   @FunctionalInterface
   public interface TilePlaceFunction {
      void place(GameRandom var1, Level var2, int var3, int var4, PerformanceTimerManager var5);
   }
}
