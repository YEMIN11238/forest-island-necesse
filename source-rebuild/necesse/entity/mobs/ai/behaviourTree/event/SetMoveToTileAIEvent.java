package necesse.entity.mobs.ai.behaviourTree.event;

import necesse.entity.mobs.friendly.human.MoveToTile;

public class SetMoveToTileAIEvent extends AIEvent {
   public MoveToTile moveToTile;
   public boolean clearWhenArrived;

   public SetMoveToTileAIEvent(MoveToTile moveToTile, boolean clearWhenArrived) {
      this.moveToTile = moveToTile;
      this.clearWhenArrived = clearWhenArrived;
   }
}
