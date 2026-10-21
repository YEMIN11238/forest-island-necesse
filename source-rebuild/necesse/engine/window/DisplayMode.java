package necesse.engine.window;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;

public enum DisplayMode {
   Windowed(new LocalMessage("settingsui", "displaywindowed"), true),
   Borderless(new LocalMessage("settingsui", "displayborderless"), false),
   Fullscreen(new LocalMessage("settingsui", "displayfullscreen"), true);

   public final GameMessage displayName;
   public final boolean canSelectSize;

   DisplayMode(GameMessage displayName, boolean canSelectSize) {
      this.displayName = displayName;
      this.canSelectSize = canSelectSize;
   }
}
