package necesse.level.maps.biomes.incursions;

import necesse.engine.AbstractMusicList;
import necesse.engine.MusicList;
import necesse.engine.registries.MusicRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.MobSpawnTable;

public class ScrapyardBiome extends Biome {
   public static MobSpawnTable mobs = new MobSpawnTable().add(50, "magechanic").add(20, "furnacegolem").add(100, "haywireshredder").add(100, "mutant");

   @Override
   public MobSpawnTable getCritterSpawnTable(Level level) {
      return new MobSpawnTable();
   }

   @Override
   public MobSpawnTable getMobSpawnTable(Level level) {
      return mobs;
   }

   @Override
   public AbstractMusicList getLevelMusic(Level level, PlayerMob perspective) {
      return new MusicList(MusicRegistry.AScrewLoose);
   }
}
