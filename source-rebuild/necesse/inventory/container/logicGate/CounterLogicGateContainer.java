package necesse.inventory.container.logicGate;

import java.awt.Point;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.PlaceableItemInterface;
import necesse.inventory.container.Container;
import necesse.inventory.container.customAction.IntCustomAction;
import necesse.level.gameLogicGate.entities.CounterLogicGateEntity;

public class CounterLogicGateContainer extends Container {
   public CounterLogicGateEntity entity;
   public final WireSelectCustomAction setIncInputs;
   public final WireSelectCustomAction setDecInputs;
   public final WireSelectCustomAction setResetInputs;
   public final WireSelectCustomAction setOutputs;
   public final IntCustomAction setMaxValue;

   public CounterLogicGateContainer(final NetworkClient client, int uniqueSeed, final CounterLogicGateEntity entity) {
      super(client, uniqueSeed);
      this.entity = entity;
      this.setIncInputs = this.registerAction(new WireSelectCustomAction() {
         @Override
         protected void run(boolean[] wires) {
            entity.incInputs = wires;
            if (client.isServer()) {
               entity.updateOutputs(false);
               entity.sendUpdatePacket();
            }
         }
      });
      this.setDecInputs = this.registerAction(new WireSelectCustomAction() {
         @Override
         protected void run(boolean[] wires) {
            entity.decInputs = wires;
            if (client.isServer()) {
               entity.updateOutputs(false);
               entity.sendUpdatePacket();
            }
         }
      });
      this.setResetInputs = this.registerAction(new WireSelectCustomAction() {
         @Override
         protected void run(boolean[] wires) {
            entity.resetInputs = wires;
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
      this.setMaxValue = this.registerAction(new IntCustomAction() {
         @Override
         protected void run(int value) {
            entity.setMaxValue(value);
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
