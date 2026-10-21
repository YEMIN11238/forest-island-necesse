package necesse.engine.localization.message;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import necesse.engine.localization.AbstractTranslationElement;
import necesse.engine.localization.Language;
import necesse.engine.localization.Localization;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public class LocalMessage extends GameMessage {
   public String category;
   public String key;
   private HashMap<String, LocalReplacement> replacements;
   private String lastTranslation;
   private int lastLanguageUnique;
   private boolean updated;
   private boolean hasCleanedReplacements;

   public LocalMessage() {
      this.category = "null";
      this.key = "null";
      this.replacements = new HashMap<>();
   }

   public LocalMessage(String category, String key) {
      Objects.requireNonNull(category);
      Objects.requireNonNull(key);
      this.category = category;
      this.key = key;
      this.replacements = new HashMap<>();
      this.updated = true;
   }

   public LocalMessage(String category, String key, String replaceKey, String replacement) {
      this(category, key);
      this.addReplacement(replaceKey, replacement);
   }

   public LocalMessage(String category, String key, String replaceKey, GameMessage replacement) {
      this(category, key);
      this.addReplacement(replaceKey, replacement);
   }

   public LocalMessage(String category, String key, String replaceKey, Function<LocalMessage, GameMessage> replacement) {
      this(category, key);
      this.addReplacement(replaceKey, replacement);
   }

   public LocalMessage(String category, String key, String... replacements) {
      this(category, key);
      if (replacements.length % 2 != 0) {
         throw new IllegalArgumentException("Replacements must have an even amount of parameters");
      }

      for (int i = 0; i < replacements.length; i += 2) {
         String replaceKey = replacements[i];
         String replacement = replacements[i + 1];
         this.addReplacement(replaceKey, replacement);
      }
   }

   public LocalMessage(String category, String key, Object... replacements) {
      this(category, key);
      if (replacements.length % 2 != 0) {
         throw new IllegalArgumentException("Replacements must have an even amount of parameters");
      }

      for (int i = 0; i < replacements.length; i += 2) {
         String replaceKey = replacements[i].toString();
         if (replacements[i + 1] instanceof GameMessage) {
            this.addReplacement(replaceKey, (GameMessage)replacements[i + 1]);
         } else {
            this.addReplacement(replaceKey, replacements[i + 1].toString());
         }
      }
   }

   @Override
   protected void addPacketContent(PacketWriter writer) {
      writer.putNextString(this.category);
      writer.putNextString(this.key);
      if (!this.hasCleanedReplacements) {
         this.cleanReplacements();
      }

      writer.putNextShortUnsigned(this.replacements.size());

      for (LocalReplacement lr : this.replacements.values()) {
         lr.writeContentPacket(writer, this);
      }
   }

   @Override
   protected void applyPacketContent(PacketReader reader) {
      this.category = reader.getNextString();
      this.key = reader.getNextString();
      int size = reader.getNextShortUnsigned();
      this.replacements = new HashMap<>();

      for (int i = 0; i < size; i++) {
         LocalReplacement replacement = new LocalReplacement(reader);
         this.replacements.put(replacement.key, replacement);
      }
   }

   @Override
   public void addSaveData(SaveData save) {
      save.addUnsafeString("category", this.category);
      save.addUnsafeString("key", this.key);
      if (!this.replacements.isEmpty()) {
         SaveData replaces = new SaveData("REPLACES");

         for (LocalReplacement replacement : this.replacements.values()) {
            replaces.addSaveData(replacement.getSaveData("", this));
         }

         save.addSaveData(replaces);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      this.category = save.getUnsafeString("category");
      this.key = save.getUnsafeString("key");
      this.replacements = new HashMap<>();
      LoadData replaces = save.getFirstLoadDataByName("REPLACES");
      if (replaces != null) {
         for (LoadData replaceData : replaces.getLoadData()) {
            LocalReplacement localReplacement = new LocalReplacement(replaceData);
            this.replacements.put(localReplacement.key, localReplacement);
         }
      }
   }

   @Override
   public boolean isEmpty() {
      return false;
   }

   @Override
   public boolean hasUpdated() {
      if (this.updated) {
         return true;
      }

      if (this.lastLanguageUnique != Localization.getCurrentLang().updateUnique) {
         return true;
      }

      for (LocalReplacement lr : this.replacements.values()) {
         if (lr.hasUpdated(this)) {
            return true;
         }
      }

      return false;
   }

   protected void cleanReplacements(AbstractTranslationElement translation, boolean debug) {
      if (debug) {
         int last = this.replacements.size();
         Set<String> lastKeys = new HashSet<>(this.replacements.keySet());
         if (this.replacements.keySet().removeIf(key -> !translation.hasReplacementKey(key))) {
            int next = this.replacements.size();
            System.out
               .println(
                  "LOCAL MESSAGE REPLACEMENTS: "
                     + this.category
                     + "."
                     + this.key
                     + " went from "
                     + last
                     + " to "
                     + next
                     + " replacements - "
                     + Arrays.toString(lastKeys.toArray())
                     + " -> "
                     + Arrays.toString(this.replacements.keySet().toArray())
               );
         }
      } else {
         this.replacements.keySet().removeIf(key -> !translation.hasReplacementKey(key));
      }

      this.hasCleanedReplacements = true;
   }

   protected void cleanReplacements() {
      AbstractTranslationElement translation = Localization.getTranslation(Localization.getCurrentLang(), this.category, this.key, true);
      this.cleanReplacements(translation, false);
   }

   protected void update() {
      Language currentLang = Localization.getCurrentLang();
      AbstractTranslationElement translation = Localization.getTranslation(currentLang, this.category, this.key, true);
      if (this.replacements.isEmpty()) {
         this.lastTranslation = translation.translation;
      } else {
         this.cleanReplacements(translation, false);
         this.lastTranslation = translation.getReplacedTranslation(key -> {
            LocalReplacement replacement = this.replacements.get(key);
            return replacement == null ? null : replacement.getReplacement(this).translate();
         });
      }

      this.lastLanguageUnique = currentLang.updateUnique;
      this.updated = false;
   }

   @Override
   public String translate() {
      if (this.hasUpdated()) {
         this.update();
      }

      return this.lastTranslation;
   }

   @Override
   public String setSteamRichPresence(HashMap<String, String> map, String key) throws DuplicateRichPresenceKeyException {
      String out = "#" + this.category + "_" + this.key;

      for (LocalReplacement value : this.replacements.values()) {
         value.setSteamRichPresence(map, this, value.key);
      }

      if (key != null) {
         if (map.containsKey(key)) {
            throw new DuplicateRichPresenceKeyException("Duplicate rich presence key: " + key + ". Before: " + map.get(key) + ", now: " + out);
         }

         map.put(key, out);
      }

      return out;
   }

   @Override
   public boolean isMissingKey(Language language) {
      if (language.isMissing(this.category, this.key)) {
         return true;
      }

      for (LocalReplacement replacement : this.replacements.values()) {
         GameMessage message = replacement.getReplacement(this);
         if (message.isMissingKey(language)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean isSameAsEnglish(Language language) {
      if (language.isSameAsEnglish(this.key, this.category)) {
         return true;
      }

      for (LocalReplacement replacement : this.replacements.values()) {
         GameMessage message = replacement.getReplacement(this);
         if (message.isSameAsEnglish(language)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public String translateDebug(Language language) {
      AbstractTranslationElement translation = Localization.getTranslation(language, this.category, this.key, true);
      return this.replacements.isEmpty() ? translation.translation : translation.getReplacedTranslation(key -> {
         LocalReplacement replacement = this.replacements.get(key);
         return replacement == null ? null : replacement.getReplacement(this).translateDebug(language);
      });
   }

   @Override
   public boolean isSame(GameMessage message) {
      if (message.getID() != this.getID()) {
         return false;
      }

      LocalMessage o = (LocalMessage)message;
      if (o.category.equals(this.category) && o.key.equals(this.key) && o.replacements.size() == this.replacements.size()) {
         for (Entry<String, LocalReplacement> entry : this.replacements.entrySet()) {
            LocalReplacement otherReplacement = o.replacements.get(entry.getKey());
            if (otherReplacement == null) {
               return false;
            }

            if (!entry.getValue().isSame(this, o, otherReplacement)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public Collection<LocalReplacement> getReplacements() {
      return this.replacements.values();
   }

   public LocalMessage addReplacement(String key, String replacement) {
      LocalReplacement localReplacement = new LocalReplacement(key, replacement);
      this.replacements.put(key, localReplacement);
      this.hasCleanedReplacements = false;
      this.updated = true;
      return this;
   }

   public LocalMessage addReplacement(String key, GameMessage replacement) {
      LocalReplacement localReplacement = new LocalReplacement(key, replacement);
      this.replacements.put(key, localReplacement);
      this.hasCleanedReplacements = false;
      this.updated = true;
      return this;
   }

   public LocalMessage addReplacement(String key, Function<LocalMessage, GameMessage> replacement) {
      LocalReplacement localReplacement = new LocalReplacement(key, replacement);
      this.replacements.put(key, localReplacement);
      this.hasCleanedReplacements = false;
      this.updated = true;
      return this;
   }

   @Override
   public String toString() {
      StringBuilder replacements = new StringBuilder();

      for (LocalReplacement lr : this.replacements.values()) {
         replacements.append(", ").append(lr.key).append(": ").append(lr.getReplacement(this).toString());
      }

      return this.getClass().getSimpleName() + "@" + Integer.toHexString(this.hashCode()) + "[" + this.category + "." + this.key + replacements + "]";
   }
}
