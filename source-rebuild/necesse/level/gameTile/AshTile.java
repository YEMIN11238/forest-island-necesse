package necesse.level.gameTile;

import java.awt.Color;
import java.awt.Point;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.particle.Particle;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.TilePosition;
import necesse.level.maps.biomes.MobSpawnTable;

public class AshTile extends TerrainSplatterTile {
   public static MobSpawnTable enemySpawnTable = new MobSpawnTable()
      .addLimited(200, "phoenix", 3, Mob.MOB_SPAWN_AREA.maxSpawnDistance * 2)
      .addLimited(200, "ashgolem", 6, Mob.MOB_SPAWN_AREA.maxSpawnDistance * 2);
   private final GameRandom drawRandom;

   public AshTile() {
      super(false, "ash", "splattingmaskwide");
      this.mapColor = new Color(44, 44, 45);
      this.canBeMined = true;
      this.drawRandom = new GameRandom();
      this.isOrganic = true;
   }

   @Override
   public void tickEffect(Level level, int x, int y) {
      super.tickEffect(level, x, y);
      if (GameRandom.globalRandom.getChance(0.025F) && !level.getObject(x, y).drawsFullTile() && level.getLightLevel(x, y).getLevel() > 0.0F) {
         int posX = x * 32 + GameRandom.globalRandom.nextInt(32);
         int posY = y * 32 + GameRandom.globalRandom.nextInt(32);
         boolean mirror = GameRandom.globalRandom.nextBoolean();
         int colorRange = GameRandom.globalRandom.getIntBetween(10, 100);
         level.entityManager
            .addParticle(posX, posY + 30, Particle.GType.COSMETIC)
            .fadesAlpha(0.4F, 0.4F)
            .size((options, lifeTime, timeAlive, lifePercent) -> {})
            .heightMoves(0.0F, GameRandom.globalRandom.getIntBetween(32, 64))
            .color(new Color(colorRange, colorRange, colorRange))
            .movesConstant(GameRandom.globalRandom.getFloatBetween(2.0F, 5.0F) * GameRandom.globalRandom.getFloatBetween(1.0F, -1.0F), 0.0F)
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
   public int getMobSpawnPositionTickets(Level level, int tileX, int tileY) {
      return 2;
   }

   @Override
   public MobSpawnTable getMobSpawnTable(TilePosition pos, MobSpawnTable defaultTable) {
      return enemySpawnTable;
   }
}
