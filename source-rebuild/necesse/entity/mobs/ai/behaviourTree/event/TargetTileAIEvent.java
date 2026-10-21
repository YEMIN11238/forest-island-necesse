package necesse.entity.mobs.ai.behaviourTree.event;

public class TargetTileAIEvent extends AIEvent {
   public final int tileX;
   public final int tileY;

   public TargetTileAIEvent(int tileX, int tileY) {
      this.tileX = tileX;
      this.tileY = tileY;
   }
}
