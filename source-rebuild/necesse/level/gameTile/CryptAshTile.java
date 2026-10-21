package necesse.level.gameTile;

import java.awt.Color;
import java.awt.Point;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.inventory.lootTable.LootTable;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.TilePosition;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.level.maps.regionSystem.SimulatePriorityList;

public class CryptAshTile extends TerrainSplatterTile {
   public static double growChance = GameMath.getAverageSuccessRuns(12000.0);
   public static MobSpawnTable cryptSpawnTable = new MobSpawnTable().add(100, "vampire");
   private final GameRandom drawRandom;

   public CryptAshTile() {
      super(false, "cryptash", "splattingmaskwide");
      this.mapColor = new Color(44, 44, 45);
      this.canBeMined = true;
      this.drawRandom = new GameRandom();
      this.isOrganic = true;
   }

   @Override
   public void addSimulateLogic(Level level, int x, int y, long ticks, SimulatePriorityList list, boolean sendChanges) {
      GrassTile.addSimulateGrow(level, x, y, growChance, ticks, "cryptgrass", list, sendChanges);
   }

   @Override
   public void tick(Level level, int x, int y) {
      if (level.isServer()) {
         if (level.getObjectID(x, y) == 0 && GameRandom.globalRandom.getChance(growChance)) {
            GameObject grass = ObjectRegistry.getObject(ObjectRegistry.getObjectID("cryptgrass"));
            if (grass.canPlace(level, x, y, 0, false) == null) {
               grass.placeObject(level, x, y, 0, false);
               level.sendObjectUpdatePacket(x, y);
            }
         }
      }
   }

   @Override
   public void tickEffect(Level level, int x, int y) {
      super.tickEffect(level, x, y);
      if (GameRandom.globalRandom.getChance(0.025F) && !level.getObject(x, y).drawsFullTile() && level.getLightLevel(x, y).getLevel() > 0.0F) {
         int posX = x * 32 + GameRandom.globalRandom.nextInt(32);
         int posY = y * 32 + GameRandom.globalRandom.nextInt(32);
         boolean mirror = GameRandom.globalRandom.nextBoolean();
         level.entityManager
            .addParticle(posX, posY + 30, Particle.GType.COSMETIC)
            .sprite(GameResources.fogParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 32, 16))
            .fadesAlpha(0.4F, 0.4F)
            .size((options, lifeTime, timeAlive, lifePercent) -> {})
            .height(30.0F)
            .dontRotate()
            .movesConstant(GameRandom.globalRandom.getFloatBetween(2.0F, 5.0F) * GameRandom.globalRandom.getOneOf(1.0F, -1.0F), 0.0F)
            .modify((options, lifeTime, timeAlive, lifePercent) -> options.mirror(mirror, false))
            .lifeTime(3000);
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
      return !(pos.level instanceof IncursionLevel) && !pos.level.getIdentifier().isSurface() ? cryptSpawnTable : super.getMobSpawnTable(pos, defaultTable);
   }

   @Override
   public int getMobSpawnPositionTickets(Level level, int tileX, int tileY) {
      return 500;
   }
}
