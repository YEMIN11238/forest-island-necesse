package necesse.engine.commands.clientCommands;

import necesse.engine.Settings;
import necesse.engine.commands.PermissionLevel;

public class ToggleHeatDistortionClientCommand extends BoolClientCommand {
   public ToggleHeatDistortionClientCommand() {
      super("heatdistortion", "Toggles the Heat Distortion graphical effect", PermissionLevel.USER, (client, logs, result) -> {
         Settings.heatDistortionEffects = result.result(Settings.heatDistortionEffects);
         Settings.saveClientSettings();
         logs.add("heatdistortion: " + Settings.heatDistortionEffects);
      });
   }
}
