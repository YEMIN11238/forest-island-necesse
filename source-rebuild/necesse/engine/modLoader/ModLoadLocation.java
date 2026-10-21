package necesse.engine.modLoader;

import java.io.File;

public class ModLoadLocation {
   public final ModProvider modProvider;
   public final File path;

   public ModLoadLocation(ModProvider modProvider, File path) {
      this.modProvider = modProvider;
      this.path = path;
   }
}
