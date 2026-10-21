package necesse.engine.platforms.steam.debug;

import com.codedisaster.steamworks.SteamID;
import java.util.Objects;
import necesse.engine.GameLog;

public class DebugSteamCallback {
   public void print(boolean printStackTrace, String methodName, Object... args) {
      StringBuilder builder = new StringBuilder("Steam: ");
      if (methodName != null) {
         builder.append(methodName).append(": ");
      }

      for (int i = 0; i < args.length; i++) {
         builder.append("Arg ").append(i).append(": ").append(this.argsToString(args[i]));
         if (i < args.length - 1) {
            builder.append(", ");
         }
      }

      GameLog.debug.println(builder);
      if (printStackTrace) {
         new Throwable().printStackTrace(GameLog.debug);
      }
   }

   public void print(String methodName, Object... args) {
      this.print(false, methodName, args);
   }

   public String argsToString(Object arg) {
      return arg instanceof SteamID ? "STEAM64-" + SteamID.getNativeHandle((SteamID)arg) : Objects.toString(arg);
   }
}
