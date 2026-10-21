package necesse.engine.platforms.steam.stats;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUserStats;
import com.codedisaster.steamworks.SteamUserStatsCallback;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.achievements.AchievementManager;
import necesse.engine.platforms.steam.SteamAchievementProvider;
import necesse.engine.playerStats.GameGlobalStats;
import necesse.engine.playerStats.GameStats;
import necesse.engine.playerStats.PlayerStats;
import necesse.engine.playerStats.StatsProvider;

public class SteamStatsProvider extends StatsProvider {
   private static SteamUserStats stats;
   private static GameGlobalStats globalStats = new GameGlobalStats(null);
   private static final long storeStatsTimeout = 5000L;
   private static final long storeStatsTimeLimit = 60000L;
   private static long nextStoreStatsTime = 0L;
   private static boolean shouldStoreStats = false;
   private static boolean waitForStoreStats;

   @Override
   public void initialize() {
      final StatsProvider self = this;
      stats = new SteamUserStats(
         new SteamUserStatsCallback() {
            @Override
            public void onUserStatsStored(long gameId, SteamResult result) {
               if (gameId == 1169040L) {
                  GameLog.debug.println("Stored Steam user stats! " + result);
                  SteamStatsProvider.nextStoreStatsTime = System.currentTimeMillis() + 60000L;
                  SteamStatsProvider.shouldStoreStats = false;
                  SteamStatsProvider.waitForStoreStats = false;
               }
            }

            @Override
            public void onUserAchievementStored(long gameId, boolean isGroupAchievement, String achievementName, int curProgress, int maxProgress) {
               if (gameId == 1169040L) {
                  GameLog.debug
                     .println("Stored Steam user achievement " + achievementName + (isGroupAchievement ? "*" : "") + ": " + curProgress + "/" + maxProgress);
                  SteamStatsProvider.nextStoreStatsTime = System.currentTimeMillis() + 60000L;
                  SteamStatsProvider.shouldStoreStats = false;
               }
            }

            @Override
            public void onGlobalStatsReceived(long gameId, SteamResult result) {
               if (gameId == 1169040L) {
                  SteamStatsProvider.globalStats = new GameGlobalStats(self);
               }
            }
         }
      );
      this.updateGlobalStats();
   }

   @Override
   public void dispose() {
      if (stats.storeStats()) {
         waitForStoreStats = true;
         long timeout = System.currentTimeMillis() + 5000L;

         while (waitForStoreStats) {
            if (timeout < System.currentTimeMillis()) {
               GameLog.warn.println("Timed out storing Steam stats before dispose");
               break;
            }

            SteamAPI.runCallbacks();

            try {
               Thread.sleep(500L);
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         }
      } else {
         GameLog.warn.println("Could not store Steam stats before dispose");
      }

      stats.dispose();
   }

   @Override
   public void tick() {
      super.tick();
      if (shouldStoreStats && nextStoreStatsTime < System.currentTimeMillis()) {
         if (!stats.storeStats()) {
            GameLog.warn.println("Could not store Steam stats!");
         }

         nextStoreStatsTime = System.currentTimeMillis() + 5000L;
      }
   }

   @Override
   public void resetStatsAndAchievements(boolean achievementsToo) {
      if (!stats.resetAllStats(achievementsToo)) {
         GameLog.warn.println("Could not reset Steam stats" + (achievementsToo ? " and achievements" : ""));
      }
   }

   @Override
   public void forceStoreStatsAndAchievements() {
      if (!stats.storeStats()) {
         GameLog.warn.println("Could not store Steam stats!");
      }
   }

   @Override
   public void storeStatsAndAchievements() {
      shouldStoreStats = true;
   }

   @Override
   public void updateGlobalStats() {
      stats.requestGlobalStats(0);
   }

   @Override
   public void updateStats() {
      GameStats steamGameStats = new GameStats(this);
      PlayerStats playerStats = GlobalData.stats();
      if (playerStats != null) {
         playerStats.loadStatsFromPlatform(steamGameStats);
      }
   }

   @Override
   public void updateAchievements() {
      SteamAchievementProvider steamAchievementProvider = new SteamAchievementProvider(stats);
      AchievementManager achievements = GlobalData.achievements();
      if (achievements != null) {
         achievements.loadFromPlatform(steamAchievementProvider);
      }
   }

   @Override
   public void setStat(String apiName, int value, boolean printWarning) {
      if (!stats.setStatI(apiName, value) && printWarning) {
         GameLog.warn.println("Could not set Steam stat " + apiName + " to " + value);
      }
   }

   @Override
   public void setStat(String apiName, int value) {
      this.setStat(apiName, value, true);
   }

   @Override
   public long getGlobalStat(String name, long defaultValue) {
      return stats.getGlobalStat(name, defaultValue);
   }

   @Override
   public double getGlobalStat(String name, double defaultValue) {
      return stats.getGlobalStat(name, defaultValue);
   }

   @Override
   public void setAchievement(String apiName) {
      if (!stats.setAchievement(apiName)) {
         GameLog.warn.println("Could not set Steam achievement " + apiName);
      }
   }

   @Override
   public GameGlobalStats getGlobalStats() {
      return globalStats;
   }

   @Override
   public int getStat(String name, int defaultValue) {
      return stats.getStatI(name, defaultValue);
   }
}
