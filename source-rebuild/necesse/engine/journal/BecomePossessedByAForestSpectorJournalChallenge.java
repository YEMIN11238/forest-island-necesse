package necesse.engine.journal;

import necesse.engine.journal.listeners.BuffGainedJournalChallengeListener;
import necesse.engine.journal.listeners.LevelChangedJournalChallengeListener;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffManager;
import necesse.level.maps.Level;

public class BecomePossessedByAForestSpectorJournalChallenge
   extends SimpleJournalChallenge
   implements BuffGainedJournalChallengeListener,
   LevelChangedJournalChallengeListener {
   @Override
   public void onChallengeRegistryClosed() {
      super.onChallengeRegistryClosed();
   }

   @Override
   public void onBuffGained(ServerClient serverClient, PlayerMob player, ActiveBuff activeBuff, boolean isOverride) {
      if (!this.isCompleted(serverClient) && this.isJournalEntryDiscovered(serverClient)) {
         Level level = player.getLevel();
         if (level.getIdentifier().equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)) {
            if (JournalChallengeUtils.isPlainsBiome(level.getBiome(player.getTileX(), player.getTileY()))) {
               this.checkBuffs(serverClient, player.buffManager);
            }
         }
      }
   }

   @Override
   public void onLevelChanged(ServerClient serverClient, Level oldLevel, Level newLevel) {
      if (!this.isCompleted(serverClient) && this.isJournalEntryDiscovered(serverClient)) {
         if (newLevel.getIdentifier().equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)) {
            if (JournalChallengeUtils.isPlainsBiome(newLevel.getBiome(serverClient.playerMob.getTileX(), serverClient.playerMob.getTileY()))) {
               this.checkBuffs(serverClient, serverClient.playerMob.buffManager);
            }
         }
      }
   }

   protected void checkBuffs(ServerClient serverClient, BuffManager buffManager) {
      for (ActiveBuff buff : buffManager.getArrayBuffs()) {
         if (buff.buff == BuffRegistry.Debuffs.SPIRIT_POSSESSED) {
            this.markCompleted(serverClient);
            serverClient.forceCombineNewStats();
            return;
         }
      }
   }
}
