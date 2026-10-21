package necesse.engine.world;

public interface GameClock {
   long getTime();

   long getWorldTime();

   long getLocalTime();

   static GameClock offsetClock(final GameClock clock, final long offset) {
      return new GameClock() {
         @Override
         public long getTime() {
            return clock.getTime() + offset;
         }

         @Override
         public long getWorldTime() {
            return clock.getWorldTime() + offset;
         }

         @Override
         public long getLocalTime() {
            return clock.getLocalTime() + offset;
         }
      };
   }

   static GameClock staticClock(final long time) {
      return new GameClock() {
         @Override
         public long getTime() {
            return time;
         }

         @Override
         public long getWorldTime() {
            return time;
         }

         @Override
         public long getLocalTime() {
            return time;
         }
      };
   }
}
