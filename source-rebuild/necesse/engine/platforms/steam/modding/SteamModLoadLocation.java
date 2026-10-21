package necesse.engine.platforms.steam.modding;

import com.codedisaster.steamworks.SteamPublishedFileID;
import java.io.File;
import necesse.engine.modLoader.ModLoadLocation;

public class SteamModLoadLocation extends ModLoadLocation {
   public final SteamPublishedFileID workshopFileID;

   public SteamModLoadLocation(SteamModProvider modProvider, File path, SteamPublishedFileID workshopFileID) {
      super(modProvider, path);
      this.workshopFileID = workshopFileID;
   }
}
