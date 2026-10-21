package necesse.engine.networkAction;

import java.util.ArrayList;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;

public abstract class NetworkActionRegistry<R, T extends NetworkAction<R>> {
   public final R registrar;
   public final String actionCallName;
   private boolean registryOpen = true;
   private int maxActions;
   private ArrayList<T> actions = new ArrayList<>();

   public NetworkActionRegistry(R registrar, String actionCallName, int maxActions) {
      if (actionCallName == null) {
         actionCallName = "NetworkAction";
      }

      this.registrar = registrar;
      this.actionCallName = actionCallName;
      this.maxActions = maxActions;
   }

   public NetworkActionRegistry(R registrar, String actionCallName) {
      this(registrar, actionCallName, 32767);
   }

   public void closeRegistry() {
      this.registryOpen = false;
   }

   public final void runAction(int id, PacketReader reader) {
      if (id >= 0 && id < this.actions.size()) {
         this.actions.get(id).executePacket(reader);
      } else {
         System.err.println("Could not find and run " + this.actionCallName + " " + id);
      }
   }

   public abstract void runAndSendAction(NetworkAction<R> var1, Packet var2);

   public final <C extends T> C register(C action) {
      if (!this.registryOpen) {
         throw new IllegalStateException("Cannot register " + this.actionCallName + " after initialization, must be done in constructor.");
      }

      if (this.actions.size() >= this.maxActions) {
         throw new IllegalStateException("Cannot register any more " + this.actionCallName);
      }

      this.actions.add((T)action);
      action.onRegister(this, this.actions.size() - 1);
      return action;
   }

   public boolean isEmpty() {
      return this.actions.isEmpty();
   }

   public Iterable<T> getActions() {
      return this.actions;
   }
}
