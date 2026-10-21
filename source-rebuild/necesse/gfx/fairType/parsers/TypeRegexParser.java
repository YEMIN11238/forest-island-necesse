package necesse.gfx.fairType.parsers;

import java.util.regex.Pattern;
import necesse.gfx.fairType.FairGlyph;

public abstract class TypeRegexParser extends TypeParser<TypeParserMatcherResult> {
   public final Pattern regex;

   public TypeRegexParser(Pattern regex) {
      this.regex = regex;
   }

   public TypeRegexParser(String regex) {
      this(Pattern.compile(regex));
   }

   public TypeParserMatcherResult getMatchResult(FairGlyph[] glyphs, int startIndex) {
      return TypeParserMatcherResult.regexResult(glyphs, startIndex, this.regex);
   }
}
