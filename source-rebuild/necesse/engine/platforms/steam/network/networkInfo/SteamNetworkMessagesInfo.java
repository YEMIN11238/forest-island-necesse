package necesse.engine.platforms.steam.network.networkInfo;

import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamNetworkingMessages;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamNetworkingMessages.SendFlag;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Objects;
import necesse.engine.GameLog;
import necesse.engine.network.networkInfo.NetworkInfo;

public class SteamNetworkMessagesInfo extends NetworkInfo {
   public static SendFlag sendFlag = SendFlag.Unreliable;
   public static final int defaultChannel = 0;
   public static final HashMap<Long, HashMap<SteamResult, SteamNetworkMessagesInfo.MessageWarnings>> messageWarnings = new HashMap<>();
   public static int messageWarningCooldown = 10000;
   public final SteamNetworkingMessages networking;
   public final SteamID remoteID;

   public SteamNetworkMessagesInfo(SteamNetworkingMessages networking, SteamID remoteID) {
      this.networking = networking;
      this.remoteID = remoteID;
   }

   @Override
   public void send(byte[] data) throws IOException {
      ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
      buffer.put(data);
      buffer.flip();

      try {
         SteamResult steamResult = this.networking.sendMessageToUser(this.remoteID, buffer, sendFlag, 0);
         if (steamResult != SteamResult.OK) {
            if (steamResult != SteamResult.ConnectFailed
               && steamResult != SteamResult.NoConnection
               && steamResult != SteamResult.InvalidParam
               && steamResult != SteamResult.InvalidState) {
               synchronized (messageWarnings) {
                  HashMap<SteamResult, SteamNetworkMessagesInfo.MessageWarnings> remoteIDWarnings = messageWarnings.compute(
                     SteamID.getNativeHandle(this.remoteID), (key, last) -> last == null ? new HashMap<>() : last
                  );
                  SteamNetworkMessagesInfo.MessageWarnings warnings = remoteIDWarnings.compute(
                     steamResult, (key, last) -> last == null ? new SteamNetworkMessagesInfo.MessageWarnings() : last
                  );
                  warnings.submit("Could not send packet to " + SteamID.getNativeHandle(this.remoteID) + " with length " + data.length + ": " + steamResult);
               }
            } else {
               this.networking.closeSessionWithUser(this.remoteID);
               GameLog.warn.println("Closed Steam session with " + SteamID.getNativeHandle(this.remoteID) + " because " + steamResult.name());
            }
         }
      } catch (SteamException e) {
         throw new IOException(e);
      }
   }

   @Override
   public String getDisplayName() {
      return "STEAM:" + SteamID.getNativeHandle(this.remoteID);
   }

   @Override
   public void closeConnection() {
   }

   @Override
   public void resetConnection() {
      System.out.println("RESET CONNECTION " + this.remoteID);
      this.networking.closeSessionWithUser(this.remoteID);
      this.networking.acceptSessionWithUser(this.remoteID);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof SteamNetworkMessagesInfo) {
         SteamNetworkMessagesInfo other = (SteamNetworkMessagesInfo)obj;
         return Objects.equals(this.remoteID, other.remoteID);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return (int)SteamID.getNativeHandle(this.remoteID);
   }

   protected static class MessageWarnings {
      public long lastPrintTime;
      public int warningsSinceLastPrint;

      public void submit(String warning) {
         long timeSinceLastPrint = System.currentTimeMillis() - this.lastPrintTime;
         if (timeSinceLastPrint >= SteamNetworkMessagesInfo.messageWarningCooldown) {
            if (this.warningsSinceLastPrint > 1) {
               warning = warning + " (" + this.warningsSinceLastPrint + ")";
            }

            GameLog.warn.println(warning);
            this.lastPrintTime = System.currentTimeMillis();
            this.warningsSinceLastPrint = 0;
         }

         this.warningsSinceLastPrint++;
      }
   }
}
