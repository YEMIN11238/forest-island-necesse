package necesse.engine.platforms.steam.debug;

import com.codedisaster.steamworks.SteamUtilsCallback;

public class DebugSteamUtilsCallback extends DebugSteamCallback implements SteamUtilsCallback {
   @Override
   public void onSteamShutdown() {
      this.print("onSteamShutdown");
   }
}
