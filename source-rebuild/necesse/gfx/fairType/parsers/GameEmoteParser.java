package necesse.gfx.fairType.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import necesse.gfx.fairType.FairGameEmoteGlyph;
import necesse.gfx.fairType.FairGlyph;
import necesse.gfx.gameIcon.GameEmote;
import necesse.gfx.gameIcon.GameEmoteRegistry;

public class GameEmoteParser extends TypeParser<GameEmoteParser.GameIconParserResult> {
   public static final Pattern EMOTE_PATTERN = Pattern.compile(":(\\w+):");
   public final int size;

   public GameEmoteParser(int size) {
      this.size = size;
   }

   public GameEmoteParser.GameIconParserResult getMatchResult(FairGlyph[] glyphs, int startIndex) {
      StringBuilder sBuilder = new StringBuilder();

      for (FairGlyph glyph : glyphs) {
         sBuilder.append(glyph.getCharacter());
      }

      Matcher m = EMOTE_PATTERN.matcher(sBuilder.toString());
      if (m.find(startIndex)) {
         String stringID = m.group(1);
         int end = m.end(1) + 1;

         for (GameEmoteRegistry.CustomGameEmoteHandler customHandler : GameEmoteRegistry.customHandlers) {
            GameEmote icon = customHandler.handle(stringID);
            if (icon != null) {
               return new GameEmoteParser.GameIconParserResult(m.start(), end, icon);
            }
         }

         GameEmote emote = GameEmoteRegistry.getIcon(stringID);
         return emote == null ? null : new GameEmoteParser.GameIconParserResult(m.start(), end, emote);
      } else {
         return null;
      }
   }

   public FairGlyph[] parse(GameEmoteParser.GameIconParserResult result, FairGlyph[] oldGlyphs) {
      return result == null ? oldGlyphs : new FairGlyph[]{new FairGameEmoteGlyph(result.emote, this.size)};
   }

   public static class GameIconParserResult extends TypeParserResult {
      public final GameEmote emote;

      public GameIconParserResult(int start, int end, GameEmote emote) {
         super(start, end);
         this.emote = emote;
      }
   }
}
