package necesse.engine.modLoader;

public class ModNextListData {
   public final LoadedMod mod;
   public final boolean enabled;

   public ModNextListData(LoadedMod mod, boolean enabled) {
      this.mod = mod;
      this.enabled = enabled;
   }
}
