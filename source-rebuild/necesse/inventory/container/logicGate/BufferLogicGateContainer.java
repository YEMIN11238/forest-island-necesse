package necesse.inventory.container.logicGate;

import java.awt.Point;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.inventory.PlaceableItemInterface;
import necesse.inventory.container.Container;
import necesse.inventory.container.customAction.IntCustomAction;
import necesse.level.gameLogicGate.entities.BufferLogicGateEntity;

public class BufferLogicGateContainer extends Container {
   public BufferLogicGateEntity entity;
   public final WireSelectCustomAction setInputs;
   public final WireSelectCustomAction setOutputs;
   public final IntCustomAction setDelay;

   public BufferLogicGateContainer(final NetworkClient client, int uniqueSeed, final BufferLogicGateEntity entity) {
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
      this.setOutputs = this.registerAction(new WireSelectCustomAction() {
         @Override
         protected void run(boolean[] wires) {
            entity.wireOutputs = wires;
            if (client.isServer()) {
               entity.updateOutputs(false);
               entity.sendUpdatePacket();
            }
         }
      });
      this.setDelay = this.registerAction(new IntCustomAction() {
         @Override
         protected void run(int value) {
            entity.delayTicks = GameMath.limit(value, 1, 200);
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
