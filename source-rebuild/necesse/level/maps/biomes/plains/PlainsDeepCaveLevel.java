package necesse.level.maps.biomes.plains;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.GameEvents;
import necesse.engine.events.worldGeneration.GenerateCaveLayoutEvent;
import necesse.engine.events.worldGeneration.GenerateCaveMiniBiomesEvent;
import necesse.engine.events.worldGeneration.GenerateCaveOresEvent;
import necesse.engine.events.worldGeneration.GenerateCaveStructuresEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveLayoutEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveMiniBiomesEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveOresEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveStructuresEvent;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.entity.levelEvent.SpiritCorruptedLevelEvent;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.CaveGeneration;
import necesse.level.maps.generationModules.CellAutomaton;
import necesse.level.maps.generationModules.GenerationTools;
import necesse.level.maps.generationModules.LinesGeneration;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.generationModules.RiverGeneration;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.levelData.CursedCroneArenasLevelData;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.PresetUtils;
import necesse.level.maps.presets.RandomCaveChestRoom;
import necesse.level.maps.presets.TheCursedCroneArenaPreset;
import necesse.level.maps.presets.caveRooms.CaveRuins;
import necesse.level.maps.presets.modularPresets.runeboundTombPresets.RuneboundTombPreset;
import necesse.level.maps.presets.set.ChestRoomSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.WallSet;

public class PlainsDeepCaveLevel extends PlainsCaveLevel {
   public long nextSpiritCorruptedTime;

   public PlainsDeepCaveLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
   }

   public PlainsDeepCaveLevel(int islandX, int islandY, int dimension, WorldEntity worldEntity, Biome biome) {
      super(new LevelIdentifier(islandX, islandY, dimension), 300, 300, worldEntity);
      this.baseBiome = biome;
      this.isCave = true;
      this.generateLevel();
   }

   public static Point getArenaCenterTile(Level level) {
      GameRandom random = new GameRandom(level.getSeed()).nextSeeded(level.getIslandDimension()).nextSeeded(64);
      return new Point(random.getIntBetween(80, level.tileWidth - 80), random.getIntBetween(80, level.tileHeight - 80));
   }

   @Override
   public void generateLevel() {
      int deepRockTile = TileRegistry.getTileID("basaltrocktile");
      CaveGeneration cg = new CaveGeneration(this, "basaltrocktile", "basaltrock");
      GameEvents.triggerEvent(new GenerateCaveLayoutEvent(this, cg), e -> cg.generateLevel(0.39F, 4, 3, 6));
      GameEvents.triggerEvent(new GeneratedCaveLayoutEvent(this, cg));
      Point arenaCenterTile = getArenaCenterTile(this);
      GameEvents.triggerEvent(
         new GenerateCaveMiniBiomesEvent(this, cg),
         e -> {
            GenerationTools.generateRandomObjectVeinsOnTile(this, cg.random, 0.2F, 4, 8, deepRockTile, ObjectRegistry.getObjectID("wildcaveglow"), 0.2F, false);
            GenerationTools.generateRandomSmoothTileVeins(
               this, cg.random, 0.02F, 3, 4.0F, 10.0F, 4.0F, 7.0F, TileRegistry.getTileID("spiritwatertile"), 0.15F, true
            );
            GenerationTools.generateRandomSmoothTileVeins(this, cg.random, 0.07F, 2, 3.0F, 9.0F, 3.0F, 6.0F, TileRegistry.getTileID("lavatile"), 0.05F, true);
            int riverCount = cg.random.getIntBetween(6, 10);

            for (int i = 0; i < riverCount; i++) {
               RiverGeneration.generateOneSmallRiver(
                  cg.random, this, TileRegistry.spiritWaterID, "watergrass", r -> r.getOneOf(RiverGeneration.BASALT_BRIDGE, RiverGeneration.DRYAD_BRIDGE)
               );
            }

            LinesGeneration lg = new LinesGeneration(arenaCenterTile.x, arenaCenterTile.y, 15.0F);
            int[] riverAngles = new int[]{
               45,
               cg.random.getChance(0.5F) ? -1 : 45,
               135,
               cg.random.getChance(0.5F) ? -1 : 135,
               225,
               cg.random.getChance(0.5F) ? -1 : 225,
               270,
               cg.random.getChance(0.5F) ? -1 : 270,
               315,
               cg.random.getChance(0.5F) ? -1 : 315
            };

            for (int angle : riverAngles) {
               if (angle != -1) {
                  lg.addMultiArm(
                     cg.random,
                     angle,
                     20,
                     Math.max(this.tileWidth, this.tileHeight),
                     5.0F,
                     10.0F,
                     4.0F,
                     6.0F,
                     armLG -> armLG.x2 < 0 || armLG.x2 > this.tileWidth || armLG.y2 < 0 || armLG.y2 > this.tileHeight
                  );
               }
            }

            CellAutomaton ca = lg.doCellularAutomaton(cg.random);
            int waterGrassID = ObjectRegistry.getObjectID("watergrass");
            int waterLanternID = ObjectRegistry.getObjectID("waterlantern");
            ca.forEachTile(this, (level, tileX, tileY) -> {
               level.setTile(tileX, tileY, TileRegistry.spiritWaterID);
               if (cg.random.getChance(0.1F)) {
                  level.setObject(tileX, tileY, waterGrassID);
               } else if (cg.random.getChance(0.02F)) {
                  level.setObject(tileX, tileY, waterLanternID);
               }
            });
            this.liquidManager.calculateShores();
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("basaltcaverock"), 0.005F);
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("basaltcaverocksmall"), 0.01F);
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("dryadtree"), 0.02F);
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("cavebirchtree"), 0.02F);
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("cavemapletree"), 0.02F);
            System.out.println("hello");
            GameObject crystalClusterSmall = ObjectRegistry.getObject("topazclustersmall");
            GenerationTools.generateRandomSmoothVeinsL(
               this,
               cg.random,
               0.005F,
               4,
               4.0F,
               7.0F,
               4.0F,
               6.0F,
               lgx -> {
                  CellAutomaton cax = lgx.doCellularAutomaton(cg.random);
                  cax.streamAliveOrdered().forEachOrdered(tile -> {
                     cg.addIllegalCrateTile(tile.x, tile.y);
                     this.setTile(tile.x, tile.y, TileRegistry.getTileID("topazgravel"));
                     this.setObject(tile.x, tile.y, 0);
                  });
                  cax.streamAliveOrdered()
                     .forEachOrdered(
                        tile -> {
                           if (this.getObjectID(tile.x, tile.y) == 0
                              && this.getObjectID(tile.x - 1, tile.y) == 0
                              && this.getObjectID(tile.x + 1, tile.y) == 0
                              && this.getObjectID(tile.x, tile.y - 1) == 0
                              && this.getObjectID(tile.x, tile.y + 1) == 0
                              && cg.random.getChance(0.08F)) {
                              int rotation = cg.random.nextInt(4);
                              Point[] clearPoints = new Point[]{new Point(-1, -1), new Point(1, -1)};
                              if (this.getRelativeAnd(
                                 tile.x,
                                 tile.y,
                                 PresetUtils.getRotatedPoints(0, 0, rotation, clearPoints),
                                 (tileX, tileY) -> ca.isAlive(tileX, tileY) && this.getObjectID(tileX, tileY) == 0
                              )) {
                                 ObjectRegistry.getObject(ObjectRegistry.getObjectID("topazcluster")).placeObject(this, tile.x, tile.y, rotation, false);
                              }
                           }

                           if (cg.random.getChance(0.3F) && crystalClusterSmall.canPlace(this, tile.x, tile.y, 0, false) == null) {
                              crystalClusterSmall.placeObject(this, tile.x, tile.y, 0, false);
                           }
                        }
                     );
               }
            );
         }
      );
      GameEvents.triggerEvent(new GeneratedCaveMiniBiomesEvent(this, cg));
      GameEvents.triggerEvent(new GenerateCaveOresEvent(this, cg), e -> {
         cg.generateOreVeins(0.05F, 3, 6, ObjectRegistry.getObjectID("copperorebasaltrock"));
         cg.generateOreVeins(0.25F, 3, 6, ObjectRegistry.getObjectID("ironorebasaltrock"));
         cg.generateOreVeins(0.15F, 3, 6, ObjectRegistry.getObjectID("goldorebasaltrock"));
         cg.generateOreVeins(0.2F, 3, 6, ObjectRegistry.getObjectID("tungstenorebasaltrock"));
         cg.generateOreVeins(0.05F, 3, 6, ObjectRegistry.getObjectID("lifequartzbasaltrock"));
         cg.generateOreVeins(0.15F, 3, 6, ObjectRegistry.getObjectID("amberbasaltrock"));
      });
      GameEvents.triggerEvent(new GeneratedCaveOresEvent(this, cg));
      PresetGeneration presets = new PresetGeneration(this);
      GameEvents.triggerEvent(
         new GenerateCaveStructuresEvent(this, cg, presets),
         e -> {
            presets.addOccupiedSpace(new Rectangle(arenaCenterTile.x - 15, arenaCenterTile.y - 15, 30, 30));
            AtomicInteger chestRoomRotation = new AtomicInteger();
            int chestRoomAmount = cg.random.getIntBetween(13, 18);

            for (int i = 0; i < chestRoomAmount; i++) {
               Preset chestRoom = new RandomCaveChestRoom(cg.random, LootTablePresets.deepPlainsCaveChest, chestRoomRotation, ChestRoomSet.basalt);
               chestRoom.replaceTile(TileRegistry.basaltFloorID, cg.random.getOneOf(TileRegistry.basaltFloorID, TileRegistry.basaltPathID));
               presets.findRandomValidPositionAndApply(cg.random, 5, chestRoom, 10, true, true);
            }

            AtomicInteger caveRuinsRotation = new AtomicInteger();
            int caveRuinsCount = cg.random.getIntBetween(25, 35);

            for (int i = 0; i < caveRuinsCount; i++) {
               WallSet wallSet = WallSet.basalt;
               FurnitureSet furnitureSet = cg.random.getOneOf(FurnitureSet.birch, FurnitureSet.maple);
               String floorStringID = cg.random.getOneOf("basaltfloor", "basaltpath");
               Preset room = cg.random
                  .getOneOf(CaveRuins.caveRuinGetters)
                  .get(cg.random, wallSet, furnitureSet, floorStringID, LootTablePresets.plainsDeepCaveRuinsChest, caveRuinsRotation);
               presets.findRandomValidPositionAndApply(cg.random, 5, room, 10, true, true);
            }

            cg.generateRandomCrates(0.03F, ObjectRegistry.getObjectID("crate"));
         }
      );
      RuneboundTombPreset.generateRuneboundTombOnLevel(this, cg.random, presets);
      GameEvents.triggerEvent(new GeneratedCaveStructuresEvent(this, cg, presets));
      new TheCursedCroneArenaPreset().applyToLevelCentered(this, arenaCenterTile.x, arenaCenterTile.y);
      GenerationTools.checkValid(this);
   }

   @Override
   public boolean isDeepCaveLevel() {
      return true;
   }

   @Override
   public GameMessage getLocationMessage(int tileX, int tileY) {
      return new LocalMessage("biome", "deepcave", "biome", this.getBiome(tileX, tileY).getLocalization());
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.nextSpiritCorruptedTime == 0L) {
         this.nextSpiritCorruptedTime = this.getTime() + GameRandom.globalRandom.getIntBetween(25000, 40000);
      }

      if (this.nextSpiritCorruptedTime <= this.getTime()) {
         this.nextSpiritCorruptedTime = 0L;
         if (!this.buffManager.getModifier(LevelModifiers.SPIRIT_CORRUPTED)) {
            CursedCroneArenasLevelData arenas = CursedCroneArenasLevelData.getCursedCroneArenasData(this, false);
            if (arenas == null || !arenas.hasAnySpawnedCrone()) {
               return;
            }

            this.entityManager.events.add(new SpiritCorruptedLevelEvent(this.getTime() + 25000L));
         }
      }
   }
}
