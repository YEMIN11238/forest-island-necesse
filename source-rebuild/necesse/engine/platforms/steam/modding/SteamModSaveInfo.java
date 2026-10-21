package necesse.engine.platforms.steam.modding;

import com.codedisaster.steamworks.SteamNativeHandle;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModProvider;
import necesse.engine.modLoader.ModSaveInfo;
import necesse.engine.platforms.Platform;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;

public class SteamModSaveInfo extends ModSaveInfo {
   public final long workshopID;

   protected SteamModSaveInfo(LoadedSteamMod mod) {
      super(mod);
      this.workshopID = SteamNativeHandle.getNativeHandle(mod.workshopFileID);
   }

   public SteamModSaveInfo(LoadedMod.SaveType type, LoadData save) {
      super(type, save);
      this.workshopID = save.getLong("workshopID", -1L);
      if (this.workshopID == -1L) {
         throw new LoadDataException("Missing workshopID for Steam mod.");
      }
   }

   @Override
   public SaveData getSaveData() {
      SaveData save = super.getSaveData();
      save.addLong("workshopID", this.workshopID);
      return save;
   }

   @Override
   public ModProvider getModProvider() {
      for (ModProvider modProvider : Platform.getModProviders()) {
         if (modProvider instanceof SteamModProvider) {
            return modProvider;
         }
      }

      throw new RuntimeException("Could not find a ModProvider for mod of type " + this.type);
   }
}
