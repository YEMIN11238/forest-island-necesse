package necesse.entity.mobs.ai.behaviourTree.event;

public class WanderingNowAIEvent extends AIEvent {
   public int targetTileX;
   public int targetTileY;
   public boolean isRunningFromAttackers;

   public WanderingNowAIEvent(int targetTileX, int targetTileY, boolean isRunningFromAttackers) {
      this.targetTileX = targetTileX;
      this.targetTileY = targetTileY;
      this.isRunningFromAttackers = isRunningFromAttackers;
   }
}
