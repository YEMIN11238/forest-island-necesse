package necesse.inventory.container.logicGate;

import java.awt.Point;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.PlaceableItemInterface;
import necesse.inventory.container.Container;
import necesse.level.gameLogicGate.entities.TFlipFlopLogicGateEntity;

public class TFlipFlopLogicGateContainer extends Container {
   public TFlipFlopLogicGateEntity entity;
   public final WireSelectCustomAction setInputs;
   public final WireSelectCustomAction setOutputs1;
   public final WireSelectCustomAction setOutputs2;

   public TFlipFlopLogicGateContainer(final NetworkClient client, int uniqueSeed, final TFlipFlopLogicGateEntity entity) {
      super(client, uniqueSeed);
      this.entity = entity;
      this.setInputs = this.registerAction(new WireSelectCustomAction() {
         @Override
         protected void run(boolean[] wires) {
            entity.wireInputs = wires;
            if (client.isServer()) {
               entity.updateOutputs(false);
               entity.sendUpdatePacket();
            }
         }
      });
      this.setOutputs1 = this.registerAction(new WireSelectCustomAction() {
         @Override
         protected void run(boolean[] wires) {
            entity.wireOutputs1 = wires;
            if (client.isServer()) {
               entity.updateOutputs(false);
               entity.sendUpdatePacket();
            }
         }
      });
      this.setOutputs2 = this.registerAction(new WireSelectCustomAction() {
         @Override
         protected void run(boolean[] wires) {
            entity.wireOutputs2 = wires;
            if (client.isServer()) {
               entity.updateOutputs(false);
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
