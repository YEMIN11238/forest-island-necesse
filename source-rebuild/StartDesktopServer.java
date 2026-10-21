import necesse.StartPlatformServer;
import necesse.engine.platforms.desktop.DesktopPlatform;

public class StartDesktopServer {
   public static void main(String[] args) {
      StartPlatformServer.start(args, new DesktopPlatform(true));
   }
}
