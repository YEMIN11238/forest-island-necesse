package necesse.engine.platforms.desktop.server;

import java.io.IOException;
import java.util.ArrayList;
import necesse.engine.GameLaunch;
import necesse.engine.GameLog;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.loading.ServerLoader;
import necesse.engine.network.server.Server;
import necesse.engine.server.ServerWindow;
import necesse.engine.util.GameUtils;

public class DesktopServerWindow extends ServerWindow {
   private ServerJFrame serverForm;
   private ServerScanThread scanThread = null;
   private Server server;

   @Override
   public void init(ServerLoader serverLoader) {
      if (GameLaunch.launchOptions.containsKey("nogui")) {
         this.serverForm = null;
      } else {
         this.serverForm = new ServerJFrame(serverLoader);
      }

      if (this.serverForm != null) {
         GameLog.setServerWrite(this.serverForm::writeConsole);
      }

      if (GameLaunch.launchOptions.containsKey("help")) {
         System.out.println("Available parameters:");
         System.out.println("-help - Shows this help menu");
         System.out.println("-nogui - Runs the server in terminal instead of opening the GUI");
         System.out.println("-settings <file> - Settings file path to load server settings from");
         System.out.println("Parameters not given will be loaded from server settings file");
         System.out.println("-world <name> - World to load instead of being asked which to load");
         System.out.println("-port <port> - Port to host at");
         System.out.println("-slots <slots> - Amount of player slots");
         System.out.println("-owner <name> - Anyone that connects with this name, will get owner permissions");
         System.out.println("-motd <message> - Sets the message of the day. Use \\n for new line");
         System.out.println("-password <password> - The password for the server, blank for no password");
         System.out.println("-pausewhenempty <1/0> - Pauses the world when there are no players in server, defaults 0");
         System.out
            .println(
               "-giveclientspower <1/0> - If the server should check client actions, a kind of anti-cheat. When off it will give a much smoother experience for clients. Defaults off."
            );
         System.out.println("-logging <1/0> - If on the server will generate a log file for each session, defaults 1");
         System.out.println("-logs <folder> - What folder to place the logs, if logging is enabled");
         System.out.println("-zipsaves <1/0> - If saves should be compressed, defaults to 1");
         System.out.println("-language <language> - Sets the language of the server, only used for occasional messages in log");
         System.out.println("-ip <address> - Binds the server IP to the address");
         System.out
            .println(
               "-datadir <path> - Sets the path where cache, latest log, saves etc. are stored. Defaults to Necesse folder in appdata on different platforms"
            );
         System.out.println("-localdir - Same as -datadir, but uses the local directory the server is launched from");
         this.showExit();
      } else {
         if (this.serverForm == null) {
            this.scanThread = new ServerScanThread(serverLoader);
            this.scanThread.setDaemon(true);
            this.scanThread.start();
         }
      }
   }

   @Override
   public void showExit() {
      if (this.serverForm == null) {
         new Thread(() -> {
            try {
               System.in.read();
            } catch (IOException var1) {
            }
         }).start();
         System.out.println();
         System.out.println("You can now exit");
      }
   }

   @Override
   public void clearConsole() {
      if (this.serverForm != null) {
         this.serverForm.clearConsole();
      }
   }

   @Override
   public void setServer(Server server) {
      this.server = server;
      if (this.serverForm != null) {
         this.serverForm.setServer(server);
      }
   }

   @Override
   public void updateGUI(TickManager tickManager) {
      if (this.serverForm != null) {
         if (tickManager != null && this.server != null) {
            ArrayList<String> lines = new ArrayList<>();
            lines.add("World: " + this.server.world.filePath.getName());
            lines.add("Players: " + this.server.getPlayersOnline() + "/" + this.server.getSlots());
            lines.add("Loaded levels: " + this.server.world.levelManager.getLoadedLevelsNum());
            lines.add("World time: " + this.server.world.worldEntity.getDayTimeInt() + ", day " + this.server.world.worldEntity.getDay());
            lines.add("Game time: " + this.server.world.worldEntity.getTime() / 1000L);
            lines.add("");
            lines.add("TickTime: " + GameUtils.getTimeStringNano(this.server.serverThread.gameLoop.getTickTimeAverage()));
            lines.add("TPS: " + tickManager.getTPS() + ", " + tickManager.getFPS());
            lines.add("TotalTicks: " + tickManager.getTotalTicks());
            lines.add("SkippedTicks: " + tickManager.getSkippedTicks());
            lines.add("");
            lines.add(
               "Received: "
                  + this.server.packetManager.getAverageIn()
                  + "/s ("
                  + this.server.packetManager.getAverageInPackets()
                  + "), Total: "
                  + this.server.packetManager.getTotalIn()
                  + " ("
                  + this.server.packetManager.getTotalInPackets()
                  + ")"
            );
            lines.add(
               "Sent: "
                  + this.server.packetManager.getAverageOut()
                  + "/s ("
                  + this.server.packetManager.getAverageOutPackets()
                  + "), Total: "
                  + this.server.packetManager.getTotalOut()
                  + " ("
                  + this.server.packetManager.getTotalOutPackets()
                  + ")"
            );
            lines.add("");
            long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            float usedPercent = (float)usedMemory / (float)Runtime.getRuntime().totalMemory();
            lines.add("Memory: " + GameUtils.getByteString(usedMemory) + " (" + (int)(usedPercent * 10000.0F) / 100.0F + " %)");
            lines.add("Max: " + GameUtils.getByteString(Runtime.getRuntime().maxMemory()));
            this.serverForm.setInfoLines(lines);
         }
      }
   }

   @Override
   public void dispose() {
      if (this.serverForm != null) {
         this.serverForm.dispose();
      }

      if (this.scanThread != null) {
         System.exit(0);
      }
   }
}
