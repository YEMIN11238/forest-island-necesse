package necesse.engine.modLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.gfx.ui.HoverStateTextures;

public class ModsFolderModProvider extends ModProvider {
   private final GameMessage gameMessage = new LocalMessage("ui", "modfromfolder");

   private String modsPath() {
      return GlobalData.appDataPath() + "mods/";
   }

   @Override
   public List<ModLoadLocation> locateMods(boolean isServer) throws ModLoadException {
      ArrayList<ModLoadLocation> modLoadLocations = new ArrayList<>();
      File path = new File(this.modsPath());
      if (path.exists()) {
         File[] files = path.listFiles();
         if (files != null) {
            for (File file : files) {
               if (!file.getPath().endsWith("modlist.data")) {
                  modLoadLocations.add(new ModLoadLocation(this, file));
               }
            }
         }
      }

      return modLoadLocations;
   }

   @Override
   public boolean canHandleModType(LoadedMod.SaveType type) {
      return type == LoadedMod.SaveType.FILE_MOD;
   }

   @Override
   public HoverStateTextures getIcon() {
      return Settings.UI.folder_icon;
   }

   @Override
   public GameMessage getGameMessage() {
      return this.gameMessage;
   }

   @Override
   protected LoadedMod createLoadedMod(JarFile jarFile, ModInfoFile modInfoFile, ModLoadLocation loadLocation) {
      return new LoadedMod(jarFile, modInfoFile, loadLocation);
   }
}
