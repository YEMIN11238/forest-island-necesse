package necesse.gfx.ui.debug;

import java.util.Arrays;
import java.util.Comparator;
import necesse.engine.Settings;
import necesse.engine.input.InputEvent;
import necesse.engine.network.PacketManager;
import necesse.engine.network.SizePacket;
import necesse.engine.network.StatPacket;
import necesse.engine.network.client.Client;
import necesse.engine.registries.PacketRegistry;
import necesse.gfx.TableContentDraw;

public class DebugNetwork extends Debug {
   private boolean recent = true;
   private boolean sortByAmount = true;

   @Override
   protected void submitDebugInputEvent(InputEvent event, Client client) {
      if (event.state && event.getID() == 79) {
         this.recent = !this.recent;
         event.use();
      } else if (event.state && event.getID() == 80) {
         this.sortByAmount = !this.sortByAmount;
         event.use();
      }
   }

   @Override
   protected void drawDebug(Client client) {
      PacketManager manager = client.packetManager;
      if (Settings.serverPerspective && client.getLocalServer() != null) {
         manager = client.getLocalServer().packetManager;
      }

      StatPacket[] in = new StatPacket[PacketRegistry.getTotalRegistered()];
      StatPacket[] out = new StatPacket[PacketRegistry.getTotalRegistered()];
      if (this.recent) {
         Iterable<SizePacket> recentInPackets = manager.getRecentInPackets();
         synchronized (recentInPackets) {
            for (SizePacket p : recentInPackets) {
               if (p.type != -1) {
                  StatPacket stat = in[p.type];
                  if (stat == null) {
                     stat = new StatPacket(p.type);
                     in[p.type] = stat;
                  }

                  stat.amount++;
                  stat.bytes = stat.bytes + p.byteSize;
               }
            }
         }

         Iterable<SizePacket> recentOutPackets = manager.getRecentOutPackets();
         synchronized (recentOutPackets) {
            for (SizePacket p : recentOutPackets) {
               if (p.type != -1) {
                  StatPacket stat = out[p.type];
                  if (stat == null) {
                     stat = new StatPacket(p.type);
                     out[p.type] = stat;
                  }

                  stat.amount++;
                  stat.bytes = stat.bytes + p.byteSize;
               }
            }
         }
      } else {
         for (int i = 0; i < PacketRegistry.getTotalRegistered(); i++) {
            in[i] = manager.getTotalInStats(i);
            out[i] = manager.getTotalOutStats(i);
         }
      }

      if (this.sortByAmount) {
         Arrays.sort(in, Comparator.nullsLast(Comparator.comparingInt(p -> -p.amount)));
         Arrays.sort(out, Comparator.nullsLast(Comparator.comparingInt(p -> -p.amount)));
      } else {
         Arrays.sort(in, Comparator.nullsLast(Comparator.comparingInt(p -> -p.bytes)));
         Arrays.sort(out, Comparator.nullsLast(Comparator.comparingInt(p -> -p.bytes)));
      }

      if (this.recent) {
         this.drawString("Press 'O' to show total packets");
      } else {
         this.drawString("Press 'O' to show recent packets");
      }

      if (this.sortByAmount) {
         this.drawString("Press 'P' to sort by bytes");
      } else {
         this.drawString("Press 'P' to sort by count");
      }

      TableContentDraw tableDraw = new TableContentDraw();
      if (this.recent) {
         tableDraw.newRow()
            .addTextColumn("Recent packets in: ", bigFontOptions, 5, 5)
            .addTextColumn(manager.getAverageIn() + "/s", bigFontOptions, 5, 0)
            .addTextColumn("(" + manager.getAverageInPackets() + ")", bigFontOptions, 10, 0);
      } else {
         tableDraw.newRow()
            .addTextColumn("Total packets in: ", bigFontOptions, 5, 5)
            .addTextColumn(manager.getTotalIn(), bigFontOptions, 5, 0)
            .addTextColumn("(" + manager.getTotalInPackets() + ")", bigFontOptions, 10, 0);
      }

      for (int i = 0; i < Math.min(10, in.length); i++) {
         StatPacket c = in[i];
         TableContentDraw.TableRow row = tableDraw.newRow();
         if (c == null) {
            row.addTextColumn(" ", smallFontOptions);
         } else {
            float amountP = (int)(c.amount / (float)manager.getTotalInPackets() * 10000.0F) / 100.0F;
            float bytesP = (int)(c.bytes / (float)manager.getTotalInBytes() * 10000.0F) / 100.0F;
            String bytesS = bytesP + "% (" + c.getBytes() + ")";
            String amountS = amountP + "% (" + c.amount + ")";
            row.addTextColumn(PacketRegistry.getPacketSimpleName(c.type) + ":", smallFontOptions, 5, 2)
               .addTextColumn(this.sortByAmount ? amountS : bytesS, smallFontOptions, 10, 0)
               .addTextColumn(this.sortByAmount ? bytesS : amountS, smallFontOptions, 10, 0);
         }
      }

      tableDraw.newRow().addTextColumn("", smallFontOptions, 0, 2);
      if (this.recent) {
         tableDraw.newRow()
            .addTextColumn("Recent packets out: ", bigFontOptions, 5, 5)
            .addTextColumn(manager.getAverageOut() + "/s", bigFontOptions, 5, 0)
            .addTextColumn("(" + manager.getAverageOutPackets() + ")", bigFontOptions, 10, 0);
      } else {
         tableDraw.newRow()
            .addTextColumn("Total packets out: ", bigFontOptions, 5, 5)
            .addTextColumn(manager.getTotalOut(), bigFontOptions, 5, 0)
            .addTextColumn("(" + manager.getTotalOutPackets() + ")", bigFontOptions, 10, 0);
      }

      for (int i = 0; i < Math.min(10, out.length); i++) {
         StatPacket c = out[i];
         TableContentDraw.TableRow row = tableDraw.newRow();
         if (c == null) {
            row.addTextColumn(" ", smallFontOptions);
         } else {
            float amountP = (int)(c.amount / (float)manager.getTotalOutPackets() * 10000.0F) / 100.0F;
            float bytesP = (int)(c.bytes / (float)manager.getTotalOutBytes() * 10000.0F) / 100.0F;
            String bytesS = bytesP + "% (" + c.getBytes() + ")";
            String amountS = amountP + "% (" + c.amount + ")";
            row.addTextColumn(PacketRegistry.getPacketSimpleName(c.type) + ":", smallFontOptions, 5, 2)
               .addTextColumn(this.sortByAmount ? amountS : bytesS, smallFontOptions, 10, 0)
               .addTextColumn(this.sortByAmount ? bytesS : amountS, smallFontOptions, 10, 0);
         }
      }

      tableDraw.draw(10, this.skipY(tableDraw.getHeight() + 10));
   }
}
