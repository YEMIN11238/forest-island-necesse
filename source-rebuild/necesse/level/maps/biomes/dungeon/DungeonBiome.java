package necesse.level.maps.biomes.dungeon;

import necesse.engine.AbstractMusicList;
import necesse.engine.MusicList;
import necesse.engine.network.server.Server;
import necesse.engine.registries.MusicRegistry;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.level.maps.biomes.forest.ForestBiome;

public class DungeonBiome extends ForestBiome {
   public static MobSpawnTable defaultDungeonMobs = new MobSpawnTable()
      .add(80, "enchantedzombie")
      .add(60, "enchantedzombiearcher")
      .add(15, "enchantedcrawlingzombie")
      .add(30, "voidapprentice");

   @Override
   public Level getNewSurfaceLevel(int islandX, int islandY, float islandSize, Server server, WorldEntity worldEntity) {
      return new DungeonSurfaceLevel(islandX, islandY, islandSize, server, worldEntity, this);
   }

   @Override
   public MobSpawnTable getMobSpawnTable(Level level) {
      return level instanceof DungeonArenaLevel ? new MobSpawnTable() : defaultDungeonMobs;
   }

   @Override
   public MobSpawnTable getCritterSpawnTable(Level level) {
      return defaultCaveCritters;
   }

   @Override
   public float getSpawnRateMod(Level level) {
      return super.getSpawnRateMod(level) * 0.9F;
   }

   @Override
   public AbstractMusicList getLevelMusic(Level level, PlayerMob perspective) {
      return new MusicList(MusicRegistry.VoidsEmbrace);
   }
}
