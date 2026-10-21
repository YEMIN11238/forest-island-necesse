package necesse.engine.journal;

import necesse.engine.journal.listeners.AdventurePartyChangedJournalChallengeListener;
import necesse.engine.journal.listeners.BiomeChangedJournalChallengeListener;
import necesse.engine.journal.listeners.LevelChangedJournalChallengeListener;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;

public class PartyInSwampCavesJournalChallenge
   extends SimpleJournalChallenge
   implements LevelChangedJournalChallengeListener,
   BiomeChangedJournalChallengeListener,
   AdventurePartyChangedJournalChallengeListener {
   @Override
   public void onLevelChanged(ServerClient serverClient, Level oldLevel, Level newLevel) {
      if (!this.isCompleted(serverClient) && this.isJournalEntryDiscovered(serverClient)) {
         this.checkCompletion(serverClient, newLevel);
      }
   }

   @Override
   public void onBiomeChanged(ServerClient serverClient, Level level, Biome oldBiome, Biome newBiome) {
      if (!this.isCompleted(serverClient) && this.isJournalEntryDiscovered(serverClient)) {
         this.checkCompletion(serverClient, level, newBiome);
      }
   }

   @Override
   public void onPartyMemberAdded(ServerClient serverClient, HumanMob mob) {
      if (!this.isCompleted(serverClient) && this.isJournalEntryDiscovered(serverClient)) {
         this.checkCompletion(serverClient, serverClient.getLevel());
      }
   }

   @Override
   public void onPartyMemberRemoved(ServerClient serverClient, HumanMob mob, boolean becauseInvalid, boolean mobRemove, boolean isDeath) {
   }

   protected void checkCompletion(ServerClient serverClient, Level level) {
      this.checkCompletion(serverClient, level, level.getBiome(serverClient.playerMob.getTileX(), serverClient.playerMob.getTileY()));
   }

   protected void checkCompletion(ServerClient serverClient, Level level, Biome biome) {
      if (level.isCave && JournalChallengeUtils.isSwampBiome(biome)) {
         if (serverClient.adventureParty.getSize() >= 2) {
            this.markCompleted(serverClient);
            serverClient.forceCombineNewStats();
         }
      }
   }
}
