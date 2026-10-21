package necesse.entity.manager;

import java.util.LinkedList;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GroundPillar;
import necesse.engine.util.GroundPillarList;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelDrawUtils;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.level.maps.Level;

public abstract class GroundPillarHandler<T extends GroundPillar> {
   protected Level level;
   private final GroundPillarList<T> pillars;

   public GroundPillarHandler(GroundPillarList<T> pillars) {
      this.pillars = pillars;
   }

   public boolean tickAndShouldRemove() {
      if (this.canRemove()) {
         synchronized (this.pillars) {
            if (this.pillars.isEmpty()) {
               return true;
            }
         }
      }

      return false;
   }

   protected abstract boolean canRemove();

   public abstract double getCurrentDistanceMoved();

   public long getCurrentTime() {
      return this.level.getWorldEntity().getLocalTime();
   }

   public void addDrawables(
      List<LevelSortedDrawable> list, LevelDrawUtils.DrawArea drawArea, Level level, TickManager tickManager, GameCamera camera, PlayerMob perspective
   ) {
      long currentTime = this.getCurrentTime();
      double distanceMoved = this.getCurrentDistanceMoved();
      LinkedList<T> pillars;
      synchronized (this.pillars) {
         this.pillars.clean(currentTime, distanceMoved);
         pillars = this.pillars.copyPillars();
      }

      for (final T pillar : pillars) {
         if (drawArea.isInPos(pillar.x, pillar.y)) {
            final DrawOptions drawOptions = this.getDrawOptions(pillar, currentTime, distanceMoved, camera, perspective);
            if (drawOptions != null) {
               list.add(new LevelSortedDrawable(this) {
                  @Override
                  public int getSortY() {
                     return pillar.y;
                  }

                  @Override
                  public void draw(TickManager tickManager) {
                     drawOptions.draw();
                  }
               });
            }
         }
      }
   }

   public DrawOptions getDrawOptions(T pillar, long currentTime, double distanceMoved, GameCamera camera, PlayerMob perspective) {
      return pillar.getDrawOptions(this.level, currentTime, distanceMoved, camera, perspective);
   }
}
