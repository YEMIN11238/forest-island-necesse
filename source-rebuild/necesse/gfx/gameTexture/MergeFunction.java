package necesse.gfx.gameTexture;

import java.awt.Color;
import necesse.engine.util.GameMath;

@FunctionalInterface
public interface MergeFunction {
   MergeFunction GLBLEND = (curCol, mergeCol) -> {
      int red = (int)(curCol.getRed() / 255.0F * (mergeCol.getRed() / 255.0F) * 255.0F);
      int green = (int)(curCol.getGreen() / 255.0F * (mergeCol.getGreen() / 255.0F) * 255.0F);
      int blue = (int)(curCol.getBlue() / 255.0F * (mergeCol.getBlue() / 255.0F) * 255.0F);
      return new Color(red, green, blue, curCol.getAlpha());
   };
   MergeFunction NORMAL = (curCol, mergeCol) -> {
      float cRed = curCol.getRed() / 255.0F;
      float cGreen = curCol.getGreen() / 255.0F;
      float cBlue = curCol.getBlue() / 255.0F;
      float cAlpha = curCol.getAlpha() / 255.0F;
      float mRed = mergeCol.getRed() / 255.0F;
      float mGreen = mergeCol.getGreen() / 255.0F;
      float mBlue = mergeCol.getBlue() / 255.0F;
      float mAlpha = mergeCol.getAlpha() / 255.0F;
      float ai = cAlpha * (1.0F - mAlpha);
      float ao = mAlpha + ai;
      int red = (int)((mRed * mAlpha + cRed * ai) / ao * 255.0F);
      int green = (int)((mGreen * mAlpha + cGreen * ai) / ao * 255.0F);
      int blue = (int)((mBlue * mAlpha + cBlue * ai) / ao * 255.0F);
      int alpha = (int)(ao * 255.0F);
      return new Color(red, green, blue, alpha);
   };
   MergeFunction ALPHA_OVERRIDE = (curCol, mergeCol) -> new Color(curCol.getRed(), curCol.getGreen(), curCol.getBlue(), mergeCol.getAlpha());
   MergeFunction MULTIPLY = (curCol, mergeCol) -> {
      float cRed = curCol.getRed() / 255.0F;
      float cGreen = curCol.getGreen() / 255.0F;
      float cBlue = curCol.getBlue() / 255.0F;
      float cAlpha = curCol.getAlpha() / 255.0F;
      float mRed = mergeCol.getRed() / 255.0F;
      float mGreen = mergeCol.getGreen() / 255.0F;
      float mBlue = mergeCol.getBlue() / 255.0F;
      float mAlpha = mergeCol.getAlpha() / 255.0F;
      return new Color(cRed * mRed, cGreen * mGreen, cBlue * mBlue, cAlpha * mAlpha);
   };
   MergeFunction ALPHA_MASK = (curCol, mergeCol) -> {
      float brightness = GameMath.max(mergeCol.getRed(), mergeCol.getGreen(), mergeCol.getBlue()) / 255.0F;
      return new Color(curCol.getRed(), curCol.getGreen(), curCol.getBlue(), (int)(curCol.getAlpha() * brightness));
   };

   Color merge(Color var1, Color var2);
}
