package necesse.engine.platforms.desktop.stats;

import necesse.engine.playerStats.GameGlobalStats;
import necesse.engine.playerStats.StatsProvider;

public class DesktopStatsProvider extends StatsProvider {
   private static final boolean statsLoaded = false;
   private static final GameGlobalStats globalStats = new GameGlobalStats(null);
   private static final long storeStatsTimeout = 5000L;
   private static final long storeStatsTimeLimit = 60000L;
   private static final long nextStoreStatsTime = 0L;
   private static final boolean shouldStoreStats = false;
   private static boolean waitForStoreStats;

   @Override
   public void initialize() {
   }

   @Override
   public void dispose() {
   }

   @Override
   public void resetStatsAndAchievements(boolean achievementsToo) {
   }

   @Override
   public void forceStoreStatsAndAchievements() {
   }

   @Override
   public void storeStatsAndAchievements() {
   }

   @Override
   public void updateGlobalStats() {
   }

   @Override
   public void updateStats() {
   }

   @Override
   public void updateAchievements() {
   }

   @Override
   public void setStat(String apiName, int value, boolean printWarning) {
   }

   @Override
   public void setStat(String apiName, int value) {
   }

   @Override
   public int getStat(String name, int defaultValue) {
      return -1;
   }

   @Override
   public long getGlobalStat(String name, long defaultValue) {
      return -1L;
   }

   @Override
   public double getGlobalStat(String name, double defaultValue) {
      return -1.0;
   }

   @Override
   public void setAchievement(String apiName) {
   }

   @Override
   public GameGlobalStats getGlobalStats() {
      return globalStats;
   }
}
