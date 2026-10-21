package necesse.engine;

import necesse.engine.world.WorldSettings;

public class ProtectedDifficultyBasedGetter<T> {
   protected Object[] array = new Object[GameDifficulty.values().length];
   protected boolean[] set = new boolean[GameDifficulty.values().length];

   protected ProtectedDifficultyBasedGetter<T> set(GameDifficulty difficulty, T object) {
      int difficultyIndex = difficulty.ordinal();
      this.array[difficultyIndex] = object;
      this.set[difficultyIndex] = true;
      int midIndex = GameDifficulty.CLASSIC.ordinal();

      for (int i = 0; i < this.array.length; i++) {
         if (!this.set[i]) {
            if (this.array[i] == null) {
               this.array[i] = object;
            } else if (difficultyIndex < midIndex && i < difficultyIndex) {
               this.array[i] = object;
            } else if (difficultyIndex > midIndex && i > difficultyIndex) {
               this.array[i] = object;
            }
         }
      }

      return this;
   }

   protected T get(GameDifficulty difficulty) {
      return (T)this.array[difficulty.ordinal()];
   }

   protected T get(WorldSettingsGetter getter) {
      if (getter == null) {
         return this.get(GameDifficulty.CLASSIC);
      }

      WorldSettings worldSettings = getter.getWorldSettings();
      return worldSettings != null ? this.get(worldSettings.difficulty) : this.get(GameDifficulty.CLASSIC);
   }
}
