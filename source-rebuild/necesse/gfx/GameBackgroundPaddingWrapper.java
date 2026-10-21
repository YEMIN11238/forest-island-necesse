package necesse.gfx;

public class GameBackgroundPaddingWrapper extends GameBackgroundWrapper {
   public int padding;

   public GameBackgroundPaddingWrapper(GameBackground background, int padding) {
      super(background);
      this.padding = padding;
   }

   @Override
   public int getContentPadding() {
      return super.getContentPadding() + this.padding;
   }
}
