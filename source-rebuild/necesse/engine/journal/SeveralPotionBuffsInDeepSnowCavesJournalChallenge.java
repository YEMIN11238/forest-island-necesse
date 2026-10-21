package necesse.engine.journal;

import java.util.ArrayList;
import necesse.engine.journal.listeners.BuffGainedJournalChallengeListener;
import necesse.engine.journal.listeners.LevelChangedJournalChallengeListener;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffManager;
import necesse.level.maps.Level;

public class SeveralPotionBuffsInDeepSnowCavesJournalChallenge
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
         if (level.isCave && JournalChallengeUtils.isSnowBiome(level.getBiome(player.getTileX(), player.getTileY()))) {
            if (this.isValidBuff(activeBuff)) {
               this.checkBuffs(serverClient, player.buffManager);
            }
         }
      }
   }

   @Override
   public void onLevelChanged(ServerClient serverClient, Level oldLevel, Level newLevel) {
      if (!this.isCompleted(serverClient) && this.isJournalEntryDiscovered(serverClient)) {
         if (newLevel.isCave && JournalChallengeUtils.isSnowBiome(newLevel.getBiome(serverClient.playerMob.getTileX(), serverClient.playerMob.getTileY()))) {
            this.checkBuffs(serverClient, serverClient.playerMob.buffManager);
         }
      }
   }

   protected void checkBuffs(ServerClient serverClient, BuffManager buffManager) {
      ArrayList<ActiveBuff> buffs = buffManager.getArrayBuffs();
      int validFoundBuffs = 0;

      for (ActiveBuff buff : buffs) {
         if (this.isValidBuff(buff)) {
            if (++validFoundBuffs >= 5) {
               this.markCompleted(serverClient);
               serverClient.forceCombineNewStats();
               return;
            }
         }
      }
   }

   protected boolean isValidBuff(ActiveBuff buff) {
      return buff.buff.getStringID().contains("potion");
   }
}
