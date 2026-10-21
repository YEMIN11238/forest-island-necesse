package necesse.engine.platforms.steam.modding;

import com.codedisaster.steamworks.SteamNativeHandle;
import com.codedisaster.steamworks.SteamPublishedFileID;
import java.util.jar.JarFile;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModInfoFile;
import necesse.engine.modLoader.ModSaveInfo;

public class LoadedSteamMod extends LoadedMod {
   public final SteamPublishedFileID workshopFileID;

   public LoadedSteamMod(JarFile jarFile, ModInfoFile file, SteamModLoadLocation loadLocation) {
      super(jarFile, file, loadLocation);
      this.workshopFileID = loadLocation.workshopFileID;
   }

   @Override
   public String getReportInfo() {
      return super.getReportInfo() + " (" + SteamNativeHandle.getNativeHandle(this.workshopFileID) + ")";
   }

   @Override
   public LoadedMod.SaveType getSaveType() {
      return LoadedMod.SaveType.STEAM_MOD;
   }

   @Override
   public ModSaveInfo getModSaveInfo() {
      return new SteamModSaveInfo(this);
   }

   @Override
   public long getSteamWorkshopID() {
      return SteamNativeHandle.getNativeHandle(this.workshopFileID);
   }
}
