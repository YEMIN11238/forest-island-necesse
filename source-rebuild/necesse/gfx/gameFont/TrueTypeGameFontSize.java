package necesse.gfx.gameFont;

public class TrueTypeGameFontSize {
   public final TrueTypeGameFontInfo info;
   public final int size;
   public final float fontSize;
   public final float lineGap;

   public TrueTypeGameFontSize(TrueTypeGameFontInfo info, int size) {
      this.info = info;
      this.size = size;
      this.fontSize = info.getFontSize(size);
      this.lineGap = info.getFontSize(size);
   }
}
