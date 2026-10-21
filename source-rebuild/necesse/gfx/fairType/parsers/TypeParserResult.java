package necesse.gfx.fairType.parsers;

public class TypeParserResult {
   public final int start;
   public final int end;

   public TypeParserResult(int start, int end) {
      this.start = start;
      this.end = end;
   }

   @Override
   public String toString() {
      return super.toString() + "{start=" + this.start + ",end=" + this.end + "}";
   }
}
