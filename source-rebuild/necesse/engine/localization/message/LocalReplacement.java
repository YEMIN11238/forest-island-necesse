package necesse.engine.localization.message;

import java.util.HashMap;
import java.util.function.Function;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public class LocalReplacement {
   private static GameMessage NULL_MESSAGE = new StaticMessage("Null");
   public final String key;
   private final Function<LocalMessage, GameMessage> replacement;
   private GameMessage lastReplacement;

   public LocalReplacement(PacketReader reader) {
      this.key = reader.getNextString();
      GameMessage replacement = GameMessage.fromContentPacket(reader.getNextContentPacket());
      this.replacement = l -> replacement;
   }

   public void writeContentPacket(PacketWriter writer, LocalMessage lm) {
      GameMessage replacement = this.getReplacement(lm);
      writer.putNextString(this.key);
      writer.putNextContentPacket(replacement.getContentPacket());
   }

   public LocalReplacement(LoadData save) {
      this.key = save.getUnsafeString("key");
      GameMessage replacement = GameMessage.loadSave(save.getFirstLoadDataByName("replace"));
      this.replacement = lm -> replacement;
   }

   public SaveData getSaveData(String saveName, LocalMessage lm) {
      SaveData save = new SaveData(saveName);
      save.addUnsafeString("key", this.key);
      save.addSaveData(this.getReplacement(lm).getSaveData("replace"));
      return save;
   }

   public LocalReplacement(String key, Function<LocalMessage, GameMessage> replacement) {
      this.key = key;
      this.replacement = replacement;
   }

   public LocalReplacement(String key, GameMessage replacement) {
      this(key, l -> replacement);
   }

   public LocalReplacement(String key, String replacement) {
      this.key = key;
      StaticMessage message = new StaticMessage(replacement);
      this.replacement = l -> message;
   }

   public GameMessage getReplacement(LocalMessage lm) {
      GameMessage replacement = this.replacement.apply(lm);
      return replacement == null ? NULL_MESSAGE : replacement;
   }

   public String setSteamRichPresence(HashMap<String, String> map, LocalMessage lm, String key) throws DuplicateRichPresenceKeyException {
      String out = this.getReplacement(lm).setSteamRichPresence(map, null);
      if (key != null) {
         if (map.containsKey(key)) {
            throw new DuplicateRichPresenceKeyException("Duplicate rich presence key: " + key + ". Before: " + map.get(key) + ", now: " + out);
         }

         map.put(key, out);
      }

      return out;
   }

   public boolean hasUpdated(LocalMessage lm) {
      GameMessage replacement = this.getReplacement(lm);
      return this.lastReplacement == null || replacement.hasUpdated();
   }

   public boolean isSame(LocalMessage thisLM, LocalMessage otherLM, LocalReplacement other) {
      return other.key.equals(this.key) && other.getReplacement(otherLM).isSame(this.getReplacement(thisLM));
   }
}
