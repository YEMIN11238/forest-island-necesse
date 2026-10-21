package necesse.engine;

public class MusicOptionsOffset {
   public final MusicOptions options;
   public final int index;
   public final long offset;

   public MusicOptionsOffset(MusicOptions options, int index, long offset) {
      this.options = options;
      this.index = index;
      this.offset = offset;
   }
}
