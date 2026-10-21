package necesse.engine.platforms.steam.dlc;

import com.codedisaster.steamworks.SteamApps;
import necesse.engine.dlc.DLC;
import necesse.engine.dlc.DLCProvider;
import necesse.engine.platforms.steam.SteamData;

public class SteamDLCRegistry extends DLCProvider {
   @Override
   protected DLCProvider.DLCStatus checkDLCStatus(DLC dlc) {
      SteamApps apps = SteamData.getApps();
      apps.installDLC(dlc.steamAppID);
      return apps.isDlcInstalled(dlc.steamAppID) ? DLCProvider.DLCStatus.INSTALLED : DLCProvider.DLCStatus.NOT_OWNED;
   }
}
