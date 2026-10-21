package necesse.level.maps.incursion;

import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.GameEvents;
import necesse.engine.events.worldGeneration.GenerateCaveLayoutEvent;
import necesse.engine.events.worldGeneration.GenerateCaveMiniBiomesEvent;
import necesse.engine.events.worldGeneration.GenerateCaveStructuresEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveLayoutEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveMiniBiomesEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveOresEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveStructuresEvent;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.Mob;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.generationModules.CaveGeneration;
import necesse.level.maps.generationModules.GenerationTools;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.presets.ScrapyardArenaPreset;

public class ScrapyardIncursionLevel extends IncursionLevel {
   public Point bossSpawnTile;
   public Point arenaEntranceTile;
   public Point arenaExitTile;
   public ArrayList<Point> flameTrapTiles = new ArrayList<>();
   public ArrayList<Point> explosiveBarrelTrapTiles = new ArrayList<>();

   public ScrapyardIncursionLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
      this.lightManager.ambientLightOverride = this.lightManager.newLight(90.0F, 0.2F, 75.0F);
   }

   public ScrapyardIncursionLevel(LevelIdentifier identifier, BiomeMissionIncursionData incursion, WorldEntity worldEntity, AltarData altarData) {
      super(identifier, 100, 250, incursion, worldEntity);
      this.baseBiome = BiomeRegistry.SCRAPYARD;
      this.isCave = false;
      this.upgradeShardsRockID = "upgradeshardscraprock";
      this.alchemyShardsRockID = "alchemyshardscraprock";
      this.essenceRockID = "radiatedessencescraprock";
      this.generateLevel(incursion, altarData);
      this.lightManager.ambientLightOverride = this.lightManager.newLight(90.0F, 0.2F, 75.0F);
   }

   public void generateLevel(BiomeMissionIncursionData incursionData, AltarData altarData) {
      CaveGeneration cg = new CaveGeneration(this, "rustgravel", "scraprock");
      int rustGravel = TileRegistry.getTileID("rustgravel");
      int grassTile = TileRegistry.grassID;
      cg.random.setSeed(incursionData.getUniqueID());
      GameEvents.triggerEvent(new GenerateCaveLayoutEvent(this, cg), e -> cg.generateLevel(0.34F, 4, 3, 6));
      GameEvents.triggerEvent(new GeneratedCaveLayoutEvent(this, cg));
      GameEvents.triggerEvent(
         new GenerateCaveMiniBiomesEvent(this, cg),
         e -> {
            GenerationTools.generateRandomSmoothTileVeins(this, cg.random, 0.05F, 2, 7.0F, 20.0F, 2.0F, 4.0F, TileRegistry.getTileID("watertile"), 1.0F, true);
            GenerationTools.generateRandomSmoothTileVeins(
               this, cg.random, 0.05F, 2, 15.0F, 50.0F, 2.0F, 4.0F, TileRegistry.getTileID("liquidoozetile"), 1.0F, true
            );
            GenerationTools.generateRandomSmoothTileVeins(this, cg.random, 0.05F, 2, 7.0F, 20.0F, 2.0F, 4.0F, TileRegistry.getTileID("junkfloor"), 1.0F, true);
            this.liquidManager.calculateShores();
            GameObject grassObject = ObjectRegistry.getObject("grass");
            GenerationTools.generateRandomVeins(this, cg.random, 0.15F, 12, 20, (level, tileX, tileY) -> {
               if (cg.random.getChance(0.7F) && this.getTileID(tileX, tileY) == rustGravel) {
                  level.setTile(tileX, tileY, grassTile);
                  if (cg.random.getChance(0.4F) && grassObject.canPlace(level, tileX, tileY, 0, false) == null) {
                     grassObject.placeObject(level, tileX, tileY, 0, false);
                  }
               }
            });
            GameObject vendingMachine = ObjectRegistry.getObject("vendingmachine");
            int rotation = cg.random.nextInt(4);
            GenerationTools.generateGuaranteedRandomVeins(
               this,
               cg.random,
               10,
               1,
               1,
               (level, tileX, tileY) -> vendingMachine.canPlace(level, tileX, tileY, rotation, false) == null,
               (level, tileX, tileY) -> {
                  vendingMachine.placeObject(level, tileX, tileY, rotation, false);
                  LootTablePresets.vendingMachine
                     .applyToLevel(cg.random, level.buffManager.getModifier(LevelModifiers.LOOT), level, tileX, tileY, level, rotation);
               }
            );
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("scrapheap"), 0.005F);
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("explosivebarrel"), 0.005F);
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("scraplamp"), 0.001F);
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("scrapheapbig"), 0.01F);
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("electrictower"), 0.005F);
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("coolingtower"), 0.005F);
            GameObject witheredGrass = ObjectRegistry.getObject("witheredgrass");
            GenerationTools.iterateLevel(
               this,
               (x, y) -> this.getTileID(x, y) == rustGravel && this.getObjectID(x, y) == 0 && cg.random.getChance(0.3F),
               (x, y) -> witheredGrass.placeObject(this, x, y, 0, false)
            );
         }
      );
      GameEvents.triggerEvent(new GeneratedCaveMiniBiomesEvent(this, cg));
      PresetGeneration presets = new PresetGeneration(this);
      GameEvents.triggerEvent(new GenerateCaveStructuresEvent(this, cg, presets), e -> cg.generateRandomCrates(0.03F, ObjectRegistry.getObjectID("crate")));
      GameEvents.triggerEvent(new GeneratedCaveStructuresEvent(this, cg, presets));
      PresetGeneration entranceAndPerkPresets = new PresetGeneration(this);
      int xOffset = cg.random.getIntBetween(-20, 20);
      int yOffset = cg.random.getIntBetween(-10, 10);
      IncursionBiome.generateEntrance(
         this,
         this.tileWidth / 2 + xOffset,
         this.tileHeight - 30 + yOffset,
         entranceAndPerkPresets,
         cg.random,
         20,
         cg.rockTile,
         "junkfloor",
         "scrapfloor",
         "factorywall",
         false
      );
      ScrapyardArenaPreset arenaPreset = new ScrapyardArenaPreset();
      entranceAndPerkPresets.applyPreset(arenaPreset, this.tileWidth / 2 - arenaPreset.width / 2 - 1, 0);
      this.generatePresetsBasedOnPerks(altarData, incursionData, entranceAndPerkPresets, cg.random, this.baseBiome);
      GenerationTools.checkValid(this);
      GameObject extractionObject = ObjectRegistry.getObject("fuelskullencasing");
      if (incursionData instanceof BiomeExtractionIncursionData) {
         GenerationTools.generateGuaranteedRandomVeins(
            this,
            cg.random,
            25,
            1,
            1,
            (level, tileX, tileY) -> level.getTileID(tileX, tileY) == cg.rockTile && extractionObject.canPlace(level, tileX, tileY, 0, false) == null,
            (level, tileX, tileY) -> extractionObject.placeObject(level, tileX, tileY, 0, false)
         );
      }

      cg.generateGuaranteedOreVeins(75, 6, 12, ObjectRegistry.getObjectID(this.upgradeShardsRockID));
      cg.generateGuaranteedOreVeins(75, 6, 12, ObjectRegistry.getObjectID(this.alchemyShardsRockID));
      this.generateOresBasedOnPerks(altarData, cg, this, this.baseBiome, cg.random);
      GameEvents.triggerEvent(new GeneratedCaveOresEvent(this, cg));
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (this.bossSpawnTile != null) {
         save.addPoint("bossSpawnTile", this.bossSpawnTile);
      }

      if (this.arenaEntranceTile != null) {
         save.addPoint("arenaEntranceTile", this.arenaEntranceTile);
      }

      if (this.arenaExitTile != null) {
         save.addPoint("arenaExitTile", this.arenaExitTile);
      }

      if (this.flameTrapTiles != null && !this.flameTrapTiles.isEmpty()) {
         save.addPointCollection("flameTrapTiles", this.flameTrapTiles);
      }

      if (this.explosiveBarrelTrapTiles != null && !this.explosiveBarrelTrapTiles.isEmpty()) {
         save.addPointCollection("explosiveBarrelTrapTiles", this.explosiveBarrelTrapTiles);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.bossSpawnTile = save.getPoint("bossSpawnTile", null, false);
      this.arenaEntranceTile = save.getPoint("arenaEntranceTile", null, false);
      this.arenaExitTile = save.getPoint("arenaExitTile", null, false);
      this.flameTrapTiles = save.getPointCollection("flameTrapTiles", new ArrayList<>(), false);
      this.explosiveBarrelTrapTiles = save.getPointCollection("explosiveBarrelTrapTiles", new ArrayList<>(), false);
   }

   @Override
   public Point getBossPortalCenterPosition() {
      return this.bossSpawnTile != null
         ? new Point(GameMath.getLevelCoordinate(this.bossSpawnTile.x) + 16, GameMath.getLevelCoordinate(this.bossSpawnTile.y) + 16)
         : super.getBossPortalCenterPosition();
   }

   @Override
   public int getBossPortalCenterOffset() {
      return this.bossSpawnTile != null ? 128 : super.getBossPortalCenterOffset();
   }

   @Override
   public GameMessage getBossPortalSpawnedMessage() {
      return new LocalMessage("misc", "bossportalnorth");
   }

   @Override
   public void updateBossOffsetPosition(Mob mob, int spawnLevelX, int spawnLevelY) {
      mob.setPos(spawnLevelX, spawnLevelY + 20, true);
   }

   @Override
   public Point getBossSpawnPosition() {
      return this.bossSpawnTile != null
         ? new Point(GameMath.getLevelCoordinate(this.bossSpawnTile.x) + 16, GameMath.getLevelCoordinate(this.bossSpawnTile.y) + 16)
         : super.getBossSpawnPosition();
   }
}
