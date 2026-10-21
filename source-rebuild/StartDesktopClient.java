import necesse.StartPlatformClient;
import necesse.engine.platforms.desktop.DesktopPlatform;

public class StartDesktopClient {
   public static void main(String[] args) {
      StartPlatformClient.start(args, new DesktopPlatform(false));
   }
}
