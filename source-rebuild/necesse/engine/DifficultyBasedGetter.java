package necesse.engine;

public class DifficultyBasedGetter<T> extends ProtectedDifficultyBasedGetter<T> {
   public DifficultyBasedGetter<T> set(GameDifficulty difficulty, T object) {
      super.set(difficulty, object);
      return this;
   }

   @Override
   public T get(GameDifficulty difficulty) {
      return super.get(difficulty);
   }

   @Override
   public T get(WorldSettingsGetter getter) {
      return super.get(getter);
   }
}
