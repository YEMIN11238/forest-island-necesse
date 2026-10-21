package necesse.engine.network.client;

import java.util.HashMap;
import java.util.UUID;
import necesse.engine.GameCache;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameUtils;

public class ClientIslandNotes {
   private final long worldUniqueID;
   private HashMap<String, String> notes = new HashMap<>();

   public ClientIslandNotes(long worldUniqueID) {
      this.worldUniqueID = worldUniqueID;
      this.loadNotes();
   }

   private String getCachePath() {
      byte[] bytes = GameUtils.getStringBytes(this.worldUniqueID + "IslandNotes");
      return "/client/" + UUID.nameUUIDFromBytes(bytes);
   }

   private void saveNotes() {
      SaveData save = new SaveData("IslandNotes");

      for (String key : this.notes.keySet()) {
         String note = this.notes.get(key);
         if (note != null && note.length() != 0) {
            save.addSafeString(key, note);
         }
      }

      GameCache.cacheSave(save, this.getCachePath());
   }

   private void loadNotes() {
      LoadData save = GameCache.getSave(this.getCachePath());
      if (save != null) {
         this.notes.clear();

         for (LoadData data : save.getLoadData()) {
            if (data.isData()) {
               this.notes.put(data.getName(), LoadData.getSafeString(data));
            }
         }
      }
   }

   public void set(int islandX, int islandY, String notes) {
      this.notes.put(islandX + "x" + islandY, notes);
      this.saveNotes();
   }

   public String get(int islandX, int islandY) {
      return this.notes.get(islandX + "x" + islandY);
   }
}
