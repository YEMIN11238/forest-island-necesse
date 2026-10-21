package necesse.level.gameTile;

import java.awt.Color;
import java.awt.Point;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.inventory.lootTable.LootTable;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.TilePosition;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.level.maps.biomes.incursions.SpiderCastleBiome;
import necesse.level.maps.biomes.incursions.SwampDeepCaveBiome;
import necesse.level.maps.biomes.snow.SnowBiome;
import necesse.level.maps.biomes.swamp.SwampBiome;
import necesse.level.maps.regionSystem.SimulatePriorityList;

public class SpiderNestTile extends TerrainSplatterTile {
   public static MobSpawnTable nestSpawnTable = new MobSpawnTable().add(100, (level, client, tile) -> {
      Biome biome = level.getBiome(tile.x, tile.y);
      if (biome instanceof SpiderCastleBiome) {
         return null;
      }

      String variant = "giantcavespider";
      if (biome instanceof SnowBiome) {
         variant = "blackcavespider";
      } else if (biome instanceof SwampBiome) {
         if (level.getIdentifier().equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)) {
            variant = "smallswampcavespider";
         } else {
            variant = "swampcavespider";
         }
      } else if (biome instanceof SwampDeepCaveBiome) {
         variant = "smallswampcavespider";
      }

      return MobRegistry.getMob(variant, level);
   });
   public static double webGrowChance = GameMath.getAverageSuccessRuns(2500.0);
   private final GameRandom drawRandom;

   public SpiderNestTile() {
      super(false, "spidernest", "splattingmaskwide");
      this.mapColor = new Color(51, 51, 51);
      this.canBeMined = true;
      this.drawRandom = new GameRandom();
      this.isOrganic = true;
   }

   @Override
   public void addSimulateLogic(Level level, int x, int y, long ticks, SimulatePriorityList list, boolean sendChanges) {
      GrassTile.addSimulateGrow(
         level,
         x,
         y,
         webGrowChance,
         ticks,
         "cobweb",
         (object, l, tileX, tileY, rotation) -> object.canPlace(level, x, y, 0, false) == null
            && level.getObjectID(x, y - 1) != ObjectRegistry.getObjectID("royaleggobject"),
         list,
         sendChanges
      );
   }

   @Override
   public void tick(Level level, int x, int y) {
      if (level.isServer()) {
         if (level.getObjectID(x, y) == 0 && GameRandom.globalRandom.getChance(webGrowChance)) {
            GameObject cobweb = ObjectRegistry.getObject(ObjectRegistry.getObjectID("cobweb"));
            if (cobweb.canPlace(level, x, y, 0, false) == null && level.getObjectID(x, y - 1) != ObjectRegistry.getObjectID("royaleggobject")) {
               cobweb.placeObject(level, x, y, 0, false);
               level.sendObjectUpdatePacket(x, y);
            }
         }
      }
   }

   @Override
   public LootTable getLootTable(Level level, int tileX, int tileY) {
      return new LootTable();
   }

   @Override
   public Point getTerrainSprite(GameTextureSection terrainTexture, Level level, int tileX, int tileY) {
      int tile;
      synchronized (this.drawRandom) {
         tile = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(terrainTexture.getHeight() / 32);
      }

      return new Point(0, tile);
   }

   @Override
   public int getTerrainPriority() {
      return 200;
   }

   @Override
   public MobSpawnTable getMobSpawnTable(TilePosition pos, MobSpawnTable defaultTable) {
      if (!(pos.level instanceof IncursionLevel) && !pos.level.getIdentifier().isSurface()) {
         if (pos.objectID() == ObjectRegistry.cobWebID) {
            return nestSpawnTable;
         }

         int cobWeb = 0;
         Integer[] var4 = pos.level.getAdjacentObjectsInt(pos.tileX, pos.tileY);
         int var5 = var4.length;

         for (int var6 = 0; var6 < var5; var6++) {
            int objectID = var4[var6];
            if (objectID == ObjectRegistry.cobWebID) {
               cobWeb++;
            }
         }

         return cobWeb >= 2 ? nestSpawnTable : super.getMobSpawnTable(pos, defaultTable);
      } else {
         return super.getMobSpawnTable(pos, defaultTable);
      }
   }
}
