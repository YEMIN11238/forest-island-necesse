package necesse.engine.network.server;

public class ServerHostSettings {
   public String password = null;
   public boolean allowOutsideCharacters;
   public boolean forcedPvP;

   public void apply(Server server, boolean alreadyStarted) {
      if (this.password != null) {
         server.getSettings().password = this.password;
      }

      server.world.settings.allowOutsideCharacters = this.allowOutsideCharacters;
      server.world.settings.forcedPvP = this.forcedPvP;
      if (alreadyStarted) {
         server.world.settings.sendSettingsPacket();
      }
   }
}
