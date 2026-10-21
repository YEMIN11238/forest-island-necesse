package necesse.inventory.container.logicGate;

import java.awt.Point;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.PlaceableItemInterface;
import necesse.inventory.container.Container;
import necesse.inventory.container.customAction.IntCustomAction;
import necesse.level.gameLogicGate.entities.SoundLogicGateEntity;

public class SoundLogicGateContainer extends Container {
   public SoundLogicGateEntity entity;
   public final IntCustomAction setSound;
   public final IntCustomAction setSemitone;

   public SoundLogicGateContainer(final NetworkClient client, int uniqueSeed, final SoundLogicGateEntity entity) {
      super(client, uniqueSeed);
      this.entity = entity;
      this.setSound = this.registerAction(new IntCustomAction() {
         @Override
         protected void run(int value) {
            entity.sound = value;
            if (client.isServer()) {
               entity.sendUpdatePacket();
            }
         }
      });
      this.setSemitone = this.registerAction(new IntCustomAction() {
         @Override
         protected void run(int value) {
            entity.semitone = value;
            if (client.isServer()) {
               entity.sendUpdatePacket();
            }
         }
      });
   }

   @Override
   public boolean isValid(ServerClient client) {
      return !super.isValid(client)
         ? false
         : !this.entity.isRemoved()
            && new Point(this.entity.tileX * 32 + 16, this.entity.tileY * 32 + 16).distance(client.playerMob.getX(), client.playerMob.getY())
               <= PlaceableItemInterface.getPlaceRange(client.playerMob);
   }
}
