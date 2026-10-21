package necesse.engine.modLoader;

import necesse.engine.platforms.Platform;
import necesse.engine.platforms.PlatformManager;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;

public class ModSaveInfo {
   public final String id;
   public final String name;
   public final String version;
   public final LoadedMod.SaveType type;

   protected ModSaveInfo(LoadedMod mod) {
      this.id = mod.id;
      this.version = mod.version;
      this.name = mod.name;
      this.type = mod.getSaveType();
   }

   public ModSaveInfo(LoadedMod.SaveType type, LoadData save) {
      this.type = type;
      this.id = save.getUnsafeString("id", null, false);
      if (this.id == null) {
         throw new LoadDataException("Missing mod id");
      }

      this.name = save.getSafeString("name", null, false);
      if (this.name == null) {
         throw new LoadDataException("Missing mod name");
      }

      this.version = save.getUnsafeString("version", null, false);
      if (this.version == null) {
         throw new LoadDataException("Missing mod version");
      }
   }

   public static ModSaveInfo fromSave(LoadData save) {
      LoadedMod.SaveType type = save.getEnum(LoadedMod.SaveType.class, "type", null, false);
      if (type == null) {
         if (save.getLong("workshopID", -1L, false) == -1L) {
            type = LoadedMod.SaveType.FILE_MOD;
         } else {
            type = LoadedMod.SaveType.STEAM_MOD;
         }
      }

      ModSaveInfo modSaveInfo = PlatformManager.getPlatform().tryGetModSaveInfo(type, save);
      if (modSaveInfo == null) {
         throw new LoadDataException("Unexpected mod type value: " + type);
      } else {
         return modSaveInfo;
      }
   }

   public SaveData getSaveData() {
      SaveData save = new SaveData("");
      save.addUnsafeString("id", this.id);
      save.addSafeString("name", this.name);
      save.addUnsafeString("version", this.version);
      return save;
   }

   public ModProvider getModProvider() {
      for (ModProvider modProvider : Platform.getModProviders()) {
         if (this.type == LoadedMod.SaveType.FILE_MOD && modProvider instanceof ModsFolderModProvider) {
            return modProvider;
         }

         if (this.type == LoadedMod.SaveType.DEV_MOD && modProvider instanceof DevModProvider) {
            return modProvider;
         }
      }

      throw new RuntimeException("Could not find a ModProvider for mod of type " + this.type);
   }
}
