package necesse.engine.journal.listeners;

import necesse.engine.network.server.ServerClient;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;

public interface BiomeChangedJournalChallengeListener {
   void onBiomeChanged(ServerClient var1, Level var2, Biome var3, Biome var4);
}
