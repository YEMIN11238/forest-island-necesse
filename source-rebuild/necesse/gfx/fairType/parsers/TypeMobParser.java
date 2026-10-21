package necesse.gfx.fairType.parsers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.SaveComponent;
import necesse.engine.save.SaveSyntaxException;
import necesse.gfx.fairType.FairGlyph;
import necesse.gfx.fairType.FairMobGlyph;

public class TypeMobParser extends TypeParser<TypeMobParser.MobParserResult> {
   public static final Pattern MOB_PATTERN = Pattern.compile("\\[mob=(\\w+)]");
   public static final Pattern MOBS_PATTERN = Pattern.compile("\\[mobs=((?:\\w+|,)+)]");
   public final int size;
   public final Function<FairMobGlyph, FairMobGlyph> modder;

   public TypeMobParser(int size, Function<FairMobGlyph, FairMobGlyph> modder) {
      this.size = size;
      this.modder = modder;
   }

   public TypeMobParser.MobParserResult getMatchResult(FairGlyph[] glyphs, int startIndex) {
      StringBuilder sBuilder = new StringBuilder();

      for (FairGlyph glyph : glyphs) {
         sBuilder.append(glyph.getCharacter());
      }

      Matcher multiMatcher = MOBS_PATTERN.matcher(sBuilder.toString());
      Matcher singleMatcher = MOB_PATTERN.matcher(sBuilder.toString());
      boolean multiMatchFound = multiMatcher.find(startIndex);
      boolean singleMatchFound = singleMatcher.find(startIndex);
      if (multiMatchFound && singleMatchFound && singleMatcher.start(0) < multiMatcher.start(0)) {
         multiMatchFound = false;
      }

      if (multiMatchFound) {
         String mobsData = multiMatcher.group(1);
         int end = multiMatcher.end(1) + 1;

         try {
            String fullMatch = multiMatcher.group(0);
            int sectionStop = SaveComponent.getSectionStop(fullMatch, '[', ']', 0);
            mobsData = fullMatch.substring(multiMatcher.start(1) - multiMatcher.start(0), sectionStop);
            end = multiMatcher.start(0) + sectionStop + 1;
         } catch (SaveSyntaxException var18) {
         }

         ArrayList<Integer> mobIDs = new ArrayList<>();
         String[] split = mobsData.split(",");

         for (String mobStringID : split) {
            int mobID = MobRegistry.getMobID(mobStringID);
            if (mobID != -1) {
               mobIDs.add(mobID);
            }
         }

         return new TypeMobParser.MobParserResult(multiMatcher.start(), end, mobIDs);
      } else if (singleMatchFound) {
         String stringID = singleMatcher.group(1);
         int end = singleMatcher.end(1) + 1;

         try {
            String fullMatch = singleMatcher.group(0);
            int sectionStop = SaveComponent.getSectionStop(fullMatch, '[', ']', 0);
            stringID = fullMatch.substring(singleMatcher.start(1) - singleMatcher.start(0), sectionStop);
            end = singleMatcher.start(0) + sectionStop + 1;
         } catch (SaveSyntaxException var19) {
         }

         int mobID = MobRegistry.getMobID(stringID);
         return mobID == -1 ? null : new TypeMobParser.MobParserResult(singleMatcher.start(), end, mobID);
      } else {
         return null;
      }
   }

   public FairGlyph[] parse(TypeMobParser.MobParserResult result, FairGlyph[] oldGlyphs) {
      if (result.mobIDs.isEmpty()) {
         return oldGlyphs;
      }

      FairMobGlyph glyph = new FairMobGlyph(this.size, result.mobIDs);
      if (this.modder != null) {
         glyph = this.modder.apply(glyph);
      }

      return new FairGlyph[]{glyph};
   }

   public static class MobParserResult extends TypeParserResult {
      public final List<Integer> mobIDs;

      public MobParserResult(int start, int end, List<Integer> mobIDs) {
         super(start, end);
         this.mobIDs = mobIDs;
      }

      public MobParserResult(int start, int end, int mobID) {
         this(start, end, mobID == -1 ? Collections.emptyList() : Collections.singletonList(mobID));
      }
   }
}
