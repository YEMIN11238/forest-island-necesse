package necesse.inventory.container.mobCommands;

import necesse.engine.network.server.ServerClient;
import necesse.inventory.container.Container;
import necesse.inventory.container.settlement.SettlementDependantContainer;

public abstract class MobCommandsHandler {
   public final Container container;
   public final SettlementDependantContainer settlementContainer;
   public final CommandFollowMeAction commandFollowMeAction;
   public final CommandClearOrdersAction commandClearOrdersAction;
   public final CommandGuardAction commandGuardAction;
   public final CommandAttackAction commandAttackAction;
   public final CommandGroupAddOrRemoveAction commandGroupAddOrRemoveAction;

   public MobCommandsHandler(Container container) {
      this.container = container;
      if (container instanceof SettlementDependantContainer) {
         this.settlementContainer = (SettlementDependantContainer)container;
      } else {
         this.settlementContainer = null;
      }

      this.commandFollowMeAction = container.registerAction(new CommandFollowMeAction(this));
      this.commandClearOrdersAction = container.registerAction(new CommandClearOrdersAction(this));
      this.commandGuardAction = container.registerAction(new CommandGuardAction(this));
      this.commandAttackAction = container.registerAction(new CommandAttackAction(this));
      if (this.settlementContainer != null) {
         this.commandGroupAddOrRemoveAction = container.registerAction(new CommandGroupAddOrRemoveAction(this));
      } else {
         this.commandGroupAddOrRemoveAction = null;
      }
   }

   public abstract void sendClientOutOfSyncPacket(ServerClient var1);
}
