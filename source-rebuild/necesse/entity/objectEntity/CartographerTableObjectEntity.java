package necesse.entity.objectEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Function;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameUtils;
import necesse.engine.util.ObjectValue;
import necesse.entity.events.IntEntityEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.container.object.CartographersTableContainer;
import necesse.level.maps.Level;

public class CartographerTableObjectEntity extends ObjectEntity {
   protected HashSet<Integer> currentSlotsUsing = new HashSet<>();
   protected IntEntityEvent addSlotEvent = this.registerEvent(new IntEntityEvent() {
      @Override
      protected void run(int value) {
         CartographerTableObjectEntity.this.currentSlotsUsing.add(value);
      }
   });
   protected IntEntityEvent removeSlotEvent = this.registerEvent(new IntEntityEvent() {
      @Override
      protected void run(int value) {
         CartographerTableObjectEntity.this.currentSlotsUsing.remove(value);
      }
   });

   public CartographerTableObjectEntity(Level level, String type, int x, int y) {
      super(level, type, x, y);
   }

   @Override
   public void setupContentPacket(PacketWriter writer) {
      super.setupContentPacket(writer);
      writer.putNextShortUnsigned(this.currentSlotsUsing.size());

      for (int slot : this.currentSlotsUsing) {
         writer.putNextShortUnsigned(slot);
      }
   }

   @Override
   public void applyContentPacket(PacketReader reader) {
      super.applyContentPacket(reader);
      int size = reader.getNextShortUnsigned();
      this.currentSlotsUsing.clear();

      for (int i = 0; i < size; i++) {
         this.currentSlotsUsing.add(reader.getNextShortUnsigned());
      }
   }

   public void addUsingClient(NetworkClient client) {
      if (client.isServer()) {
         this.addSlotEvent.runAndSend(client.slot);
      } else {
         this.currentSlotsUsing.add(client.slot);
      }
   }

   public void removeUsingClient(NetworkClient client) {
      if (client.isServer()) {
         this.removeSlotEvent.runAndSend(client.slot);
      } else {
         this.currentSlotsUsing.remove(client.slot);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.getLevel().tickManager().isGameTickInSecond(8)) {
         this.cleanUpUsingClients();
      }
   }

   public void cleanUpUsingClients() {
      Server server = this.getServer();
      if (server != null) {
         ArrayList<Integer> toRemove = new ArrayList<>();

         for (int slot : this.currentSlotsUsing) {
            ServerClient client = server.getClient(slot);
            if (client == null) {
               toRemove.add(slot);
            } else if (client.getContainer() instanceof CartographersTableContainer) {
               CartographersTableContainer container = (CartographersTableContainer)client.getContainer();
               if (!container.tableObjectEntity.getLevel().isSamePlace(this.getLevel())
                  || container.tableObjectEntity.tileX != this.tileX
                  || container.tableObjectEntity.tileY != this.tileY) {
                  toRemove.add(slot);
               }
            } else {
               toRemove.add(slot);
            }
         }

         for (int slot : toRemove) {
            this.removeSlotEvent.runAndSend(slot);
         }
      }
   }

   @Override
   public void onMouseHover(PlayerMob perspective, boolean debug) {
      super.onMouseHover(perspective, debug);
      StringTooltips tooltips = new StringTooltips();
      tooltips.add(this.getObject().getDisplayName());
      GameTooltipManager.addTooltip(tooltips, TooltipLocation.INTERACT_FOCUS);
   }

   public boolean hasSameUsingSlots(HashSet<Integer> slots) {
      if (slots.size() != this.currentSlotsUsing.size()) {
         return false;
      }

      for (int slot : slots) {
         if (!this.currentSlotsUsing.contains(slot)) {
            return false;
         }
      }

      return true;
   }

   public boolean hasClientUsing(int slot) {
      return this.currentSlotsUsing.contains(slot);
   }

   public Iterable<ObjectValue<Integer, NetworkClient>> getOptionalUsingClients() {
      Function<Integer, ObjectValue<Integer, NetworkClient>> slotToClient;
      if (this.isServer()) {
         Server server = this.getServer();
         slotToClient = slot -> new ObjectValue<>(slot, server.getClient(slot));
      } else if (this.isClient()) {
         Client client = this.getClient();
         slotToClient = slot -> new ObjectValue<>(slot, client.getClient(slot));
      } else {
         slotToClient = slot -> new ObjectValue<>(slot, null);
      }

      return GameUtils.mapIterable(this.currentSlotsUsing.iterator(), slotToClient);
   }
}
