package necesse.gfx.fairType;

import java.awt.Color;
import java.util.Objects;
import java.util.function.Supplier;
import necesse.engine.input.InputEvent;
import necesse.engine.util.FloatDimension;

public class FairDefaultColorGlyph implements FairGlyph {
   public final FairGlyph glyph;
   public Supplier<Color> colorSupplier;

   public FairDefaultColorGlyph(FairGlyph glyph, Supplier<Color> colorSupplier) {
      Objects.requireNonNull(glyph);
      this.glyph = glyph;
      this.colorSupplier = colorSupplier;
   }

   @Override
   public FloatDimension getDimensions() {
      return this.glyph.getDimensions();
   }

   @Override
   public void updateDimensions() {
      this.glyph.updateDimensions();
   }

   @Override
   public void handleInputEvent(float drawX, float drawY, InputEvent event) {
      this.glyph.handleInputEvent(drawX, drawY, event);
   }

   @Override
   public void draw(float x, float y, Color defaultColor) {
      this.glyph.draw(x, y, this.colorSupplier == null ? defaultColor : this.colorSupplier.get());
   }

   @Override
   public void drawShadow(float x, float y) {
      this.glyph.drawShadow(x, y);
   }

   @Override
   public FairGlyph getTextBoxCharacter() {
      return new FairDefaultColorGlyph(this.glyph.getTextBoxCharacter(), this.colorSupplier);
   }

   @Override
   public boolean isWhiteSpaceGlyph() {
      return this.glyph.isWhiteSpaceGlyph();
   }

   @Override
   public boolean isNewLineGlyph() {
      return this.glyph.isNewLineGlyph();
   }

   @Override
   public Supplier<Supplier<Color>> getDefaultColor() {
      return this.glyph.getDefaultColor();
   }

   @Override
   public char getCharacter() {
      return this.glyph.getCharacter();
   }

   @Override
   public String getParseString() {
      return this.glyph.getParseString();
   }

   @Override
   public boolean canBeParsed() {
      return this.glyph.canBeParsed();
   }
}
