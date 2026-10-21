package necesse.engine.platforms.steam.debug;

import com.codedisaster.steamworks.SteamAuthTicket;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUserCallback;
import com.codedisaster.steamworks.SteamAuth.AuthSessionResponse;

public class DebugSteamUserCallback extends DebugSteamCallback implements SteamUserCallback {
   @Override
   public void onValidateAuthTicket(SteamID steamID, AuthSessionResponse authSessionResponse, SteamID ownerSteamID) {
      this.print("onValidateAuthTicket", steamID, authSessionResponse, ownerSteamID);
   }

   @Override
   public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {
      this.print("onMicroTxnAuthorization", appID, orderID, authorized);
   }

   @Override
   public void onEncryptedAppTicket(SteamResult result) {
      this.print("onEncryptedAppTicket", result);
   }

   @Override
   public void onAuthSessionTicket(SteamAuthTicket authTicket, SteamResult result) {
      this.print("onAuthSessionTicket", authTicket, result);
   }
}
