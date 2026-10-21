package necesse.level.maps.biomes.temple;

import necesse.engine.AbstractMusicList;
import necesse.engine.MusicList;
import necesse.engine.registries.MusicRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.level.maps.biomes.forest.ForestBiome;

public class TempleBiome extends ForestBiome {
   public static MobSpawnTable templeMobs = new MobSpawnTable()
      .add(50, "ancientskeleton")
      .add(50, "ancientarmoredskeleton")
      .add(40, "ancientskeletonthrower")
      .add(30, "ancientskeletonmage");

   @Override
   public float getSpawnRateMod(Level level) {
      return super.getSpawnRateMod(level) * 0.75F;
   }

   @Override
   public float getSpawnCapMod(Level level) {
      return super.getSpawnCapMod(level) * 0.75F;
   }

   @Override
   public MobSpawnTable getMobSpawnTable(Level level) {
      return level instanceof TempleArenaLevel ? new MobSpawnTable() : templeMobs;
   }

   @Override
   public MobSpawnTable getCritterSpawnTable(Level level) {
      return level.isCave ? defaultCaveCritters : defaultSurfaceCritters;
   }

   @Override
   public AbstractMusicList getLevelMusic(Level level, PlayerMob perspective) {
      return new MusicList(MusicRegistry.LostTemple);
   }
}
