package necesse.engine.platforms.desktop.server;

import java.util.Scanner;
import necesse.engine.loading.ServerLoader;

public class ServerScanThread extends Thread {
   private final ServerLoader serverLoader;

   public ServerScanThread(ServerLoader serverLoader) {
      super("Command scanner");
      this.serverLoader = serverLoader;
   }

   @Override
   public void run() {
      Scanner scanner = new Scanner(System.in);

      while (!this.isInterrupted()) {
         if (scanner.hasNextLine()) {
            String command = scanner.nextLine();
            this.serverLoader.handleCommand(command);
            if (command.isEmpty()) {
               this.sleep(10);
            }
         } else {
            this.sleep(10);
         }
      }

      scanner.close();
   }

   private void sleep(int millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException var3) {
      }
   }
}
