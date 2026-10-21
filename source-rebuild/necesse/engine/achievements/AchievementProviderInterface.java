package necesse.engine.achievements;

public interface AchievementProviderInterface {
   AchievementProviderInterface.AchievementState getAchievementState(String var1);

   class AchievementState {
      public final boolean completed;
      public final Long completedTime;

      public AchievementState(boolean completed, Long completedTime) {
         this.completed = completed;
         this.completedTime = completedTime;
      }
   }
}
