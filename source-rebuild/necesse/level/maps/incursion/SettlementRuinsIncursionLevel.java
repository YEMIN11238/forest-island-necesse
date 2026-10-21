package necesse.level.maps.incursion;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import necesse.engine.GameRandomNoise;
import necesse.engine.events.loot.ObjectLootTableDropsEvent;
import necesse.engine.events.loot.TileLootTableDropsEvent;
import necesse.engine.postProcessing.PostProcessingEffects;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.TicketSystemList;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.AscendedPylonObjectEntity;
import necesse.level.gameObject.FruitBushObject;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.SeedObject;
import necesse.level.gameObject.SingleRockObject;
import necesse.level.gameObject.SingleRockSmall;
import necesse.level.gameTile.GameTile;
import necesse.level.gameTile.PathTiledTile;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.generationModules.GenerationTools;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.presets.AscendedPylonAreaPreset;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.regionSystem.RegionBoundsExecutor;

public class SettlementRuinsIncursionLevel extends IncursionLevel {
   public static int WALL_PADDING = 5;
   public HashSet<Integer> ignoredDamagedObjectIDs = new HashSet<>();
   public TicketSystemList<Integer> floorTileList;
   public TicketSystemList<Integer> corruptFloorTileList;
   protected FurnitureSet[] oldFurnitureSets;
   protected FurnitureSet targetFurnitureSet;
   protected HashSet<String> objectBlacklist;

   public SettlementRuinsIncursionLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
      this.ignoredDamagedObjectIDs.add(ObjectRegistry.getObjectID("ascendedwall"));
      this.floorTileList = new TicketSystemList<>();
      this.floorTileList.addObject(100, TileRegistry.cryptAshID);
      this.floorTileList.addObject(100, TileRegistry.basaltRockID);
      this.corruptFloorTileList = new TicketSystemList<>();
      this.corruptFloorTileList.addObject(20, TileRegistry.cryptAshID);
      this.corruptFloorTileList.addObject(20, TileRegistry.basaltRockID);
      this.corruptFloorTileList.addObject(100, TileRegistry.ascendedCorruptionID);
      this.oldFurnitureSets = new FurnitureSet[]{
         FurnitureSet.oak, FurnitureSet.spruce, FurnitureSet.maple, FurnitureSet.birch, FurnitureSet.pine, FurnitureSet.palm, FurnitureSet.dungeon
      };
      this.targetFurnitureSet = FurnitureSet.deadwood;
      this.objectBlacklist = new HashSet<>();
      this.objectBlacklist.add("fallenaltar");
      this.objectBlacklist.add("homestone");
      this.objectBlacklist.add("waystone");
      this.lightManager.ambientLightOverride = this.lightManager.newLight(300.0F, 0.2F, 75.0F);
   }

   public SettlementRuinsIncursionLevel(
      Level copyLevel,
      Rectangle tileRectangle,
      int entranceTileX,
      int entranceTileY,
      LevelIdentifier identifier,
      BiomeMissionIncursionData incursion,
      WorldEntity worldEntity
   ) {
      super(identifier, WALL_PADDING * 2 + tileRectangle.width, WALL_PADDING * 2 + tileRectangle.height, incursion, worldEntity);
      this.ignoredDamagedObjectIDs.add(ObjectRegistry.getObjectID("ascendedwall"));
      this.floorTileList = new TicketSystemList<>();
      this.floorTileList.addObject(100, TileRegistry.cryptAshID);
      this.floorTileList.addObject(100, TileRegistry.basaltRockID);
      this.corruptFloorTileList = new TicketSystemList<>();
      this.corruptFloorTileList.addObject(20, TileRegistry.cryptAshID);
      this.corruptFloorTileList.addObject(20, TileRegistry.basaltRockID);
      this.corruptFloorTileList.addObject(100, TileRegistry.ascendedCorruptionID);
      this.oldFurnitureSets = new FurnitureSet[]{
         FurnitureSet.oak, FurnitureSet.spruce, FurnitureSet.maple, FurnitureSet.birch, FurnitureSet.pine, FurnitureSet.palm, FurnitureSet.dungeon
      };
      this.targetFurnitureSet = FurnitureSet.deadwood;
      this.objectBlacklist = new HashSet<>();
      this.objectBlacklist.add("fallenaltar");
      this.objectBlacklist.add("homestone");
      this.objectBlacklist.add("waystone");
      this.baseBiome = BiomeRegistry.SETTLEMENT_RUINS;
      this.isCave = true;
      this.generateLevel(copyLevel, tileRectangle, entranceTileX, entranceTileY, incursion);
      this.lightManager.ambientLightOverride = this.lightManager.newLight(300.0F, 0.2F, 75.0F);
   }

   public static Color getShade(Level level) {
      float time = (float)level.getTime() / 5000.0F;
      float v = time - (float)Math.floor(time);
      return Color.getHSBColor(v, 0.5F, 1.0F);
   }

   public static void updateSceneShade(Level level) {
      Color shade = getShade(level);
      float red = shade.getRed() / 255.0F;
      float green = shade.getGreen() / 255.0F;
      float blue = shade.getBlue() / 255.0F;
      float mod = 1.0F / GameMath.min(red, green, blue);
      PostProcessingEffects.setSceneShade(red * mod, green * mod, blue * mod);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      updateSceneShade(this);
   }

   @Override
   public int getAppearedObjectDamage(GameObject object, int layerID, int tileX, int tileY) {
      if (this.objectLayer.isPlayerPlaced(layerID, tileX, tileY)) {
         return 0;
      }

      if (this.ignoredDamagedObjectIDs.contains(object.getID())) {
         return 0;
      }

      long seed = GameTile.getTileSeed(tileX, tileY, layerID + 15);
      return new GameRandom(seed).getIntBetween(0, object.objectHealth);
   }

   @Override
   public void onTileLootTableDropped(TileLootTableDropsEvent event) {
      super.onTileLootTableDropped(event);
      if (!this.tileLayer.isPlayerPlaced(event.tileX, event.tileY)) {
         event.dropPos = null;
      }
   }

   @Override
   public void onObjectLootTableDropped(ObjectLootTableDropsEvent event) {
      super.onObjectLootTableDropped(event);
      if (!this.objectLayer.isPlayerPlaced(event.tileX, event.tileY)) {
         event.objectDrops = null;
      }
   }

   public void generateLevel(Level copyLevel, Rectangle tileRectangle, int entranceTileX, int entranceTileY, BiomeMissionIncursionData incursionData) {
      RegionBoundsExecutor copyLevelExecutor = new RegionBoundsExecutor(
         copyLevel.regionManager, tileRectangle.x, tileRectangle.y, tileRectangle.x + tileRectangle.width - 1, tileRectangle.y + tileRectangle.height - 1, true
      );
      GameRandom random = new GameRandom(incursionData.getUniqueID()).nextSeeded(673);

      for (int newTileX = WALL_PADDING; newTileX < tileRectangle.width + WALL_PADDING; newTileX++) {
         for (int newTileY = WALL_PADDING; newTileY < tileRectangle.height + WALL_PADDING; newTileY++) {
            this.biomeLayer.setBiome(newTileX, newTileY, this.baseBiome.getID());
         }
      }

      for (int newTileX = WALL_PADDING; newTileX < tileRectangle.width + WALL_PADDING; newTileX++) {
         for (int newTileY = WALL_PADDING; newTileY < tileRectangle.height + WALL_PADDING; newTileY++) {
            int lastTileX = tileRectangle.x + newTileX - WALL_PADDING;
            int lastTileY = tileRectangle.y + newTileY - WALL_PADDING;
            GameTile tile = copyLevelExecutor.getOnTile(
               lastTileX, lastTileY, (region, regionTileX, regionTileY) -> region.tileLayer.getTileByRegion(regionTileX, regionTileY), null
            );
            tile = this.changeTile(random, newTileX, newTileY, tile, copyLevel, lastTileX, lastTileY);
            if (tile == null) {
               tile = TileRegistry.getTile(TileRegistry.grassID);
            }

            this.tileLayer.setTile(newTileX, newTileY, tile.getID());
         }
      }

      for (int newTileX = WALL_PADDING; newTileX < tileRectangle.width + WALL_PADDING; newTileX++) {
         for (int newTileY = WALL_PADDING; newTileY < tileRectangle.height + WALL_PADDING; newTileY++) {
            int lastTileX = tileRectangle.x + newTileX - WALL_PADDING;
            int lastTileY = tileRectangle.y + newTileY - WALL_PADDING;
            GameObject object = copyLevelExecutor.getOnTile(
               lastTileX, lastTileY, (region, regionTileX, regionTileY) -> region.objectLayer.getObjectByRegion(0, regionTileX, regionTileY), null
            );
            if (object.isMultiTileMaster()) {
               object = this.changeObject(random, 0, newTileX, newTileY, copyLevel, object, lastTileX, lastTileY);
               if (object != null) {
                  boolean isObjectPlayerPlaced = copyLevel.objectLayer.isPlayerPlaced(0, lastTileX, lastTileY);
                  byte objectRotation = copyLevel.objectLayer.getObjectRotation(0, lastTileX, lastTileY);
                  if (object.canPlace(this, newTileX, newTileY, objectRotation, isObjectPlayerPlaced) == null) {
                     object.placeObject(this, newTileX, newTileY, objectRotation, false);
                  }
               }
            }
         }
      }

      for (int newTileX = WALL_PADDING; newTileX < tileRectangle.width + WALL_PADDING; newTileX++) {
         for (int newTileY = WALL_PADDING; newTileY < tileRectangle.height + WALL_PADDING; newTileY++) {
            int lastTileX = tileRectangle.x + newTileX - WALL_PADDING;
            int lastTileY = tileRectangle.y + newTileY - WALL_PADDING;

            for (int objectLayerID = 1; objectLayerID < ObjectLayerRegistry.getTotalLayers(); objectLayerID++) {
               int finalObjectLayerID = objectLayerID;
               GameObject object = copyLevelExecutor.getOnTile(
                  lastTileX,
                  lastTileY,
                  (region, regionTileX, regionTileY) -> region.objectLayer.getObjectByRegion(finalObjectLayerID, regionTileX, regionTileY),
                  null
               );
               object = this.changeObject(random, objectLayerID, newTileX, newTileY, copyLevel, object, lastTileX, lastTileY);
               if (object != null) {
                  this.objectLayer.setObject(objectLayerID, newTileX, newTileY, object.getID());
                  this.objectLayer
                     .setObjectRotation(objectLayerID, newTileX, newTileY, copyLevel.objectLayer.getObjectRotation(objectLayerID, lastTileX, lastTileY));
               }
            }
         }
      }

      for (int newTileX = WALL_PADDING; newTileX < tileRectangle.width + WALL_PADDING; newTileX++) {
         for (int newTileY = WALL_PADDING; newTileY < tileRectangle.height + WALL_PADDING; newTileY++) {
            if (random.getChance(0.005F)) {
               this.objectLayer.setObject(0, newTileX, newTileY, ObjectRegistry.getObjectID("deadwoodcandles"));
            } else if (random.getChance(0.005F)) {
               this.objectLayer.setObject(0, newTileX, newTileY, ObjectRegistry.getObjectID("deadwoodcandelabra"));
            } else if (random.getChance(0.01F)) {
               this.objectLayer.setObject(0, newTileX, newTileY, ObjectRegistry.getObjectID("cryptgravestone1"));
               this.objectLayer.setObjectRotation(0, newTileX, newTileY, random.nextInt(4));
            } else if (random.getChance(0.01F)) {
               this.objectLayer.setObject(0, newTileX, newTileY, ObjectRegistry.getObjectID("cryptgravestone2"));
               this.objectLayer.setObjectRotation(0, newTileX, newTileY, random.nextInt(4));
            } else if (random.getChance(0.2F)) {
               this.objectLayer.setObject(0, newTileX, newTileY, ObjectRegistry.getObjectID("cryptgrass"));
            }
         }
      }

      GameRandomNoise noise = new GameRandomNoise(random.nextInt());
      float spreadModifier = 1.2F;
      float size = 0.02F;
      int scale = 64;

      for (int newTileX = WALL_PADDING; newTileX < tileRectangle.width + WALL_PADDING; newTileX++) {
         for (int newTileY = WALL_PADDING; newTileY < tileRectangle.height + WALL_PADDING; newTileY++) {
            double value = noise.perlin2Fractal((double)newTileX / scale * spreadModifier, (double)newTileY / scale * spreadModifier, 4, 0.5);
            double abs = Math.abs(value);
            float maxValue = size * spreadModifier;
            if (abs < maxValue) {
               this.tileLayer.setTile(newTileX, newTileY, this.corruptFloorTileList.getRandomObject(random));

               for (int objectLayerID = 0; objectLayerID < ObjectLayerRegistry.getTotalLayers(); objectLayerID++) {
                  this.objectLayer.setObject(objectLayerID, newTileX, newTileY, 0);
               }
            }
         }
      }

      int newEntranceTileX = entranceTileX - tileRectangle.x + WALL_PADDING;
      int newEntranceTileY = entranceTileY - tileRectangle.y + WALL_PADDING;
      PresetGeneration presets = new PresetGeneration(this);
      presets.addOccupiedSpace(newEntranceTileX - 20, newEntranceTileY - 20, 40, 40);
      ArrayList<Integer> remainingAttackIndexes = AscendedPylonObjectEntity.getNewDefaultAttackIndexesList();
      Collections.shuffle(remainingAttackIndexes, random);

      for (int i = 0; i < 3; i++) {
         ArrayList<Integer> currentAttackIndexes = new ArrayList<>();
         if (!remainingAttackIndexes.isEmpty()) {
            int pylonsLeft = 3 - i;
            int attacksToRemove = Math.max(remainingAttackIndexes.size() / pylonsLeft, 1);

            for (int j = 0; j < attacksToRemove; j++) {
               currentAttackIndexes.add(remainingAttackIndexes.remove(0));
            }
         }

         AscendedPylonAreaPreset pylonPreset = new AscendedPylonAreaPreset(23, random, currentAttackIndexes);
         Point spawnTile = presets.findRandomValidPositionAndApply(random, 200, pylonPreset, WALL_PADDING + 5, false, false, false, false);
         if (spawnTile != null) {
            presets.addOccupiedSpace(spawnTile.x - 20, spawnTile.y - 20, pylonPreset.width + 40, pylonPreset.height + 40);
         }
      }

      int paddingWallID = ObjectRegistry.getObjectID("cryptwall");

      for (int tileX = 0; tileX < this.tileWidth; tileX++) {
         for (int tileY = 0; tileY < WALL_PADDING; tileY++) {
            this.tileLayer.setTile(tileX, tileY, TileRegistry.cryptAshID);
            this.regionManager.setTileProtected(tileX, tileY, true);
            if (tileY < WALL_PADDING - 1) {
               this.objectLayer.setObject(0, tileX, tileY, paddingWallID);
            }
         }

         for (int tileY = this.tileHeight - WALL_PADDING; tileY < this.tileHeight; tileY++) {
            this.tileLayer.setTile(tileX, tileY, TileRegistry.cryptAshID);
            this.regionManager.setTileProtected(tileX, tileY, true);
            if (tileY >= this.tileHeight - WALL_PADDING + 1) {
               this.objectLayer.setObject(0, tileX, tileY, paddingWallID);
            }
         }
      }

      for (int tileY = WALL_PADDING - 1; tileY < this.tileHeight - WALL_PADDING + 1; tileY++) {
         for (int tileX = 0; tileX < WALL_PADDING; tileX++) {
            this.tileLayer.setTile(tileX, tileY, TileRegistry.cryptAshID);
            this.regionManager.setTileProtected(tileX, tileY, true);
            if (tileX < WALL_PADDING - 1) {
               this.objectLayer.setObject(0, tileX, tileY, paddingWallID);
            }
         }

         for (int tileX = this.tileWidth - WALL_PADDING; tileX < this.tileWidth; tileX++) {
            this.tileLayer.setTile(tileX, tileY, TileRegistry.cryptAshID);
            this.regionManager.setTileProtected(tileX, tileY, true);
            if (tileX >= this.tileWidth - WALL_PADDING + 1) {
               this.objectLayer.setObject(0, tileX, tileY, paddingWallID);
            }
         }
      }

      for (int layer = 0; layer < ObjectLayerRegistry.getTotalLayers(); layer++) {
         this.objectLayer.setObject(layer, newEntranceTileX, newEntranceTileY, 0);
         this.objectLayer.setObjectRotation(layer, newEntranceTileX, newEntranceTileY, 0);
         this.objectLayer.setIsPlayerPlaced(layer, newEntranceTileX, newEntranceTileY, false);
      }

      IncursionBiome.addReturnPortalOnTile(this, newEntranceTileX, newEntranceTileY);
      GenerationTools.checkValid(this);
   }

   protected GameTile changeTile(GameRandom random, int newTileX, int newTileY, GameTile lastTile, Level lastLevel, int lastTileX, int lastTileY) {
      if (lastTile.getID() == TileRegistry.getTileID("farmland")) {
         return lastTile;
      }

      if (!lastTile.isFloor && !lastTile.isLiquid) {
         return TileRegistry.getTile(this.floorTileList.getRandomObject(random));
      }

      if (!lastTile.isLiquid && random.getChance(0.25F)) {
         return TileRegistry.getTile(this.floorTileList.getRandomObject(random));
      }

      if (lastTile.isFloor) {
         if (lastTile instanceof PathTiledTile) {
            return TileRegistry.getTile(random.getOneOf(TileRegistry.getTileID("cryptpath"), TileRegistry.basaltPathID));
         }

         if (random.getChance(0.25F)) {
            return TileRegistry.getTile(random.getOneOf(TileRegistry.basaltFloorID, TileRegistry.deadWoodFloorID));
         }
      }

      return lastTile;
   }

   protected GameObject changeObject(
      GameRandom random, int layerID, int newTileX, int newTileY, Level lastLevel, GameObject lastObject, int lastTileX, int lastTileY
   ) {
      if (lastObject.isGrass) {
         return null;
      }

      if (random.getChance(0.15F)) {
         return null;
      }

      if (this.objectBlacklist.contains(lastObject.getStringID())) {
         return null;
      }

      if (lastObject.getLightLevel(lastLevel, layerID, lastTileX, lastTileY) > 0) {
         return null;
      }

      if (lastObject.shouldReturnOnDeletedLevels(lastLevel, layerID, lastTileX, lastTileY)) {
         return null;
      }

      if (lastObject.isTree) {
         return ObjectRegistry.getObject("deadwoodtree");
      }

      if (lastObject instanceof FruitBushObject) {
         return null;
      }

      if (lastObject instanceof SeedObject) {
         return null;
      }

      if (lastObject instanceof SingleRockSmall) {
         return ObjectRegistry.getObject("basaltcaverocksmall");
      }

      if (lastObject instanceof SingleRockObject) {
         return ObjectRegistry.getObject("basaltcaverock");
      }

      for (FurnitureSet oldFurnitureSet : this.oldFurnitureSets) {
         int nextID = this.targetFurnitureSet.replaceSingleObjectRandomly(random, oldFurnitureSet, lastObject.getID());
         if (nextID != lastObject.getID()) {
            return ObjectRegistry.getObject(nextID);
         }
      }

      return lastObject;
   }

   @Override
   public boolean shouldLimitCameraWithinBounds(PlayerMob perspective) {
      return false;
   }
}
