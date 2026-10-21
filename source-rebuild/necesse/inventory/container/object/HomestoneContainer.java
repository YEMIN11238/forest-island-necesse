package necesse.inventory.container.object;

import java.util.ArrayList;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.TeleportResult;
import necesse.entity.levelEvent.TeleportEvent;
import necesse.inventory.container.customAction.ContainerCustomAction;
import necesse.inventory.container.customAction.IntBooleanCustomAction;
import necesse.inventory.container.customAction.IntCustomAction;
import necesse.inventory.container.settlement.SettlementDependantContainer;
import necesse.inventory.container.settlement.events.SettlementDataEvent;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.Waystone;

public class HomestoneContainer extends SettlementDependantContainer {
   public final LevelObject levelObject;
   public int maxWaystones;
   public ArrayList<Waystone> waystones;
   public IntCustomAction useWaystone;
   public IntBooleanCustomAction moveWaystoneUp;
   public IntBooleanCustomAction moveWaystoneDown;
   public HomestoneContainer.WaystoneRenameCustomAction renameWaystone;

   public HomestoneContainer(final NetworkClient var1, int var2, SettlementDataEvent var3, LevelObject var4, Packet var5) {
      super(var1, var2, var3, false);
      this.levelObject = var4;
      this.update(var5);
      this.subscribeEvent(HomestoneUpdateEvent.class, var0 -> true, () -> true);
      this.onEvent(HomestoneUpdateEvent.class, var1x -> this.update(var1x.content));
      this.useWaystone = this.registerAction(new IntCustomAction() {
         @Override
         protected void run(int var1x) {
            if (var1.isServer()) {
               ServerClient var2 = var1.getServerClient();
               Server var3 = var2.getServer();
               Level var4 = var2.getLevel();
               ServerSettlementData var5 = HomestoneContainer.this.getServerData();
               if (var5 != null) {
                  ArrayList<Waystone> var6 = var5.getWaystones();
                  if (var1x >= 0 && var1x < var6.size()) {
                     Waystone var7 = var6.get(var1x);
                     var4.entityManager.events.addHidden(new TeleportEvent(var2, 0, var7.destination, 0.0F, null, var6x -> {
                        if (var7.checkIsValid(var3, var5.uniqueID)) {
                           var2.closeContainer(true);
                           var2.newStats.homestones_used.increment(1);
                           return new TeleportResult(true, var7.findTeleportLocation(var3, var1.playerMob));
                        } else {
                           var6.remove(var7);
                           var2.sendChatMessage(new LocalMessage("ui", "waystoneinvalid"));
                           var5.sendEvent(HomestoneUpdateEvent.class);
                           return new TeleportResult(false, null);
                        }
                     }));
                  } else {
                     new HomestoneUpdateEvent(var5).applyAndSendToClient(var2);
                  }
               }
            }
         }
      });
      this.moveWaystoneUp = this.registerAction(new IntBooleanCustomAction() {
         @Override
         protected void run(int var1x, boolean var2x) {
            if (var1x > 0 && var1x <= HomestoneContainer.this.waystones.size() - 1) {
               Waystone var3 = HomestoneContainer.this.waystones.remove(var1x);
               if (var2x) {
                  HomestoneContainer.this.waystones.add(0, var3);
               } else {
                  HomestoneContainer.this.waystones.add(var1x - 1, var3);
               }

               if (var1.isServer()) {
                  ServerClient var4 = var1.getServerClient();
                  Level var5 = var4.getLevel();
                  ServerSettlementData var6 = HomestoneContainer.this.getServerData();
                  if (var6 != null) {
                     ArrayList<Waystone> var7 = var6.getWaystones();
                     if (var1x > var7.size() - 1) {
                        return;
                     }

                     Waystone var8 = var7.remove(var1x);
                     if (var2x) {
                        var7.add(0, var8);
                     } else {
                        var7.add(var1x - 1, var8);
                     }

                     var6.sendEvent(HomestoneUpdateEvent.class);
                  }
               }
            }
         }
      });
      this.moveWaystoneDown = this.registerAction(new IntBooleanCustomAction() {
         @Override
         protected void run(int var1x, boolean var2) {
            if (var1x >= 0 && var1x <= HomestoneContainer.this.waystones.size() - 2) {
               Waystone var3 = HomestoneContainer.this.waystones.remove(var1x);
               if (var2) {
                  HomestoneContainer.this.waystones.add(var3);
               } else {
                  HomestoneContainer.this.waystones.add(var1x + 1, var3);
               }

               if (var1.isServer()) {
                  ServerSettlementData var4 = HomestoneContainer.this.getServerData();
                  if (var4 != null) {
                     ArrayList<Waystone> var5 = var4.getWaystones();
                     if (var1x > var5.size() - 2) {
                        return;
                     }

                     Waystone var6 = var5.remove(var1x);
                     if (var2) {
                        var5.add(var6);
                     } else {
                        var5.add(var1x + 1, var6);
                     }

                     var4.sendEvent(HomestoneUpdateEvent.class);
                  }
               }
            }
         }
      });
      this.renameWaystone = this.registerAction(new HomestoneContainer.WaystoneRenameCustomAction());
   }

   public void update(Packet var1) {
      PacketReader var2 = new PacketReader(var1);
      this.maxWaystones = var2.getNextShortUnsigned();
      int var3 = var2.getNextShortUnsigned();
      this.waystones = new ArrayList<>(var3);

      for (int var4 = 0; var4 < var3; var4++) {
         this.waystones.add(new Waystone(var2));
      }
   }

   @Override
   public boolean isValid(ServerClient var1) {
      return !super.isValid(var1) ? false : !this.levelObject.hasChanged() && this.levelObject.isInInteractRange(var1.playerMob);
   }

   public static Packet getContainerContent(ServerSettlementData var0) {
      Packet var1 = new Packet();
      PacketWriter var2 = new PacketWriter(var1);
      var2.putNextShortUnsigned(var0.getMaxWaystones());
      ArrayList<Waystone> var3 = var0.getWaystones();
      var2.putNextShortUnsigned(var3.size());

      for (Waystone var5 : var3) {
         var5.writePacket(var2);
      }

      return var1;
   }

   public class WaystoneRenameCustomAction extends ContainerCustomAction {
      protected WaystoneRenameCustomAction() {
      }

      public void runAndSend(int var1, String var2) {
         Packet var3 = new Packet();
         PacketWriter var4 = new PacketWriter(var3);
         var4.putNextInt(var1);
         var4.putNextString(var2);
         this.runAndSendAction(var3);
      }

      @Override
      public void executePacket(PacketReader var1) {
         int var2 = var1.getNextInt();
         String var3 = var1.getNextString();
         if (HomestoneContainer.this.client.isServer()) {
            ServerClient var4 = HomestoneContainer.this.client.getServerClient();
            ServerSettlementData var5 = HomestoneContainer.this.getServerData();
            if (var5 != null) {
               ArrayList<Waystone> var6 = var5.getWaystones();
               if (var2 >= 0 && var2 < var6.size()) {
                  Waystone var7 = var6.get(var2);
                  var7.name = var3;
                  var5.sendEvent(HomestoneUpdateEvent.class);
               } else {
                  new HomestoneUpdateEvent(var5).applyAndSendToClient(var4);
               }
            }
         }
      }
   }
}
