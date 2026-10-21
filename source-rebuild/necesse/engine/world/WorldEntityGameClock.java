package necesse.engine.world;

public interface WorldEntityGameClock extends GameClock {
   WorldEntity getWorldEntity();

   @Override
   default long getTime() {
      WorldEntity worldEntity = this.getWorldEntity();
      return worldEntity == null ? 0L : worldEntity.getTime();
   }

   @Override
   default long getWorldTime() {
      WorldEntity worldEntity = this.getWorldEntity();
      return worldEntity == null ? 0L : worldEntity.getWorldTime();
   }

   @Override
   default long getLocalTime() {
      WorldEntity worldEntity = this.getWorldEntity();
      return worldEntity == null ? 0L : worldEntity.getLocalTime();
   }
}
