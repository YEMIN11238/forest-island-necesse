package necesse.engine.modLoader;

import java.io.PrintStream;
import java.io.PrintWriter;

public class ModLoadException extends Exception {
   public final LoadedMod mod;

   public ModLoadException(LoadedMod mod, String message) {
      super(message);
      this.mod = mod;
   }

   public ModLoadException(LoadedMod mod, String message, Throwable cause) {
      super(message, cause);
      this.mod = mod;
   }

   @Override
   public void printStackTrace(PrintStream s) {
      if (this.getCause() != null) {
         this.getCause().printStackTrace(s);
      } else {
         super.printStackTrace(s);
      }
   }

   @Override
   public void printStackTrace(PrintWriter s) {
      if (this.getCause() != null) {
         this.getCause().printStackTrace(s);
      } else {
         super.printStackTrace(s);
      }
   }
}
