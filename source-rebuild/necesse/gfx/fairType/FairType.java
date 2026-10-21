package necesse.gfx.fairType;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import necesse.engine.Settings;
import necesse.engine.input.InputEvent;
import necesse.engine.util.FloatDimension;
import necesse.engine.util.GameMath;
import necesse.gfx.fairType.parsers.TypeParser;
import necesse.gfx.fairType.parsers.TypeParserResult;
import necesse.gfx.gameFont.FontOptions;

public class FairType {
   private final ArrayList<FairGlyph> glyphs;

   public FairType() {
      this.glyphs = new ArrayList<>();
   }

   public FairType(ArrayList<FairGlyph> glyphs) {
      this.glyphs = glyphs;
   }

   public FairType(FairGlyph... glyphs) {
      this(new ArrayList<>(Arrays.asList(glyphs)));
   }

   public FairType(FairType copy) {
      this.glyphs = new ArrayList<>(copy.glyphs);
   }

   public FairType append(String string, Function<Character, FairCharacterGlyph> constructor) {
      return this.append(FairCharacterGlyph.fromString(string, constructor));
   }

   public FairType append(FontOptions fontOptions, String string) {
      return this.append(FairCharacterGlyph.fromString(fontOptions, string));
   }

   public FairType append(FairGlyph... glyphs) {
      Objects.requireNonNull(glyphs);
      this.glyphs.addAll(Arrays.asList(glyphs));
      return this;
   }

   public FairType insert(int index, FairGlyph... glyphs) {
      Objects.requireNonNull(glyphs);
      this.glyphs.addAll(index, Arrays.asList(glyphs));
      return this;
   }

   public FairGlyph remove(int index) {
      return this.glyphs.remove(index);
   }

   public boolean remove(FairGlyph glyph) {
      return this.glyphs.remove(glyph);
   }

   public int getLength() {
      return this.glyphs.size();
   }

   public FairGlyph get(int index) {
      return this.glyphs.get(index);
   }

   public FairGlyph[] getGlyphsArray() {
      return this.glyphs.toArray(new FairGlyph[0]);
   }

   public int indexOf(FairGlyph glyph) {
      return this.glyphs.indexOf(glyph);
   }

   public int indexOf(String str) {
      return this.getCharacterString().indexOf(str);
   }

   public FairType getTextBoxCopy() {
      ArrayList<FairGlyph> glyphs = new ArrayList<>(this.glyphs.size());

      for (FairGlyph glyph : this.glyphs) {
         glyphs.add(glyph.getTextBoxCharacter());
      }

      return new FairType(glyphs);
   }

   public String getParseString() {
      StringBuilder builder = new StringBuilder();

      for (FairGlyph glyph : this.glyphs) {
         builder.append(glyph.getParseString());
      }

      return builder.toString();
   }

   public String getCharString() {
      char[] ar = new char[this.getLength()];

      for (int i = 0; i < this.glyphs.size(); i++) {
         ar[i] = this.glyphs.get(i).getCharacter();
      }

      return new String(ar);
   }

   public boolean matchesSearch(String search) {
      if (this.glyphs.isEmpty() && search.isEmpty()) {
         return true;
      }

      int counter = 0;

      for (int i = 0; i < this.glyphs.size(); i++) {
         char c = Character.toLowerCase(this.glyphs.get(i).getCharacter());
         if (c != '\ufffe') {
            if (c == search.charAt(counter)) {
               counter++;
            } else {
               if (counter > 0) {
                  i--;
               }

               counter = 0;
            }

            if (counter == search.length()) {
               return true;
            }
         }
      }

      return false;
   }

   public FairType applyParsers(Function<FairType.Parse, Boolean> shouldParse, Consumer<FairType.Parse> onParse, TypeParser... parsers) {
      for (TypeParser parser : parsers) {
         int startIndex = 0;

         while (startIndex <= this.getLength()) {
            TypeParserResult result = parser.getMatchResult(this.glyphs.toArray(new FairGlyph[0]), startIndex);
            if (result == null) {
               break;
            }

            FairGlyph[] oldGlyphs = this.glyphs.subList(result.start, result.end).toArray(new FairGlyph[0]);
            FairGlyph[] append = parser.parse(result, oldGlyphs);
            FairType.Parse parse = new FairType.Parse(parser, oldGlyphs, result.start, result.end, append);
            if (append != null && append != oldGlyphs && (shouldParse == null || shouldParse.apply(parse))) {
               int toRemove = result.end - result.start;

               for (int i = 0; i < toRemove; i++) {
                  this.glyphs.remove(result.start);
               }

               if (append != null) {
                  this.glyphs.addAll(result.start, Arrays.asList(append));
                  startIndex = result.start + append.length;
               }

               if (onParse != null) {
                  onParse.accept(parse);
               }
            } else {
               startIndex = result.end + 1;
            }
         }
      }

      return this;
   }

   public FairType applyParsers(TypeParser... parsers) {
      return this.applyParsers(null, null, parsers);
   }

   public FairType replaceAll(String match, FairGlyph... replace) {
      this.applyParsers(TypeParsers.replaceParser(match, replace));
      return this;
   }

   private String getCharacterString() {
      char[] chars = new char[this.glyphs.size()];

      for (int i = 0; i < chars.length; i++) {
         chars[i] = this.glyphs.get(i).getCharacter();
      }

      return new String(chars);
   }

   public void updateGlyphDimensions() {
      for (FairGlyph glyph : this.glyphs) {
         glyph.updateDimensions();
      }
   }

   public FairTypeDrawOptions getDrawOptions(FairType.TextAlign align) {
      return this.getDrawOptions(align, -1, false);
   }

   public FairTypeDrawOptions getDrawOptions(FairType.TextAlign align, int maxWidth, boolean forceMaxWidth) {
      return this.getDrawOptions(align, maxWidth, forceMaxWidth, true);
   }

   public FairTypeDrawOptions getDrawOptions(FairType.TextAlign align, int maxWidth, boolean forceMaxWidth, boolean removeWhitespaceBounding) {
      return this.getDrawOptions(align, maxWidth, forceMaxWidth, 0, -1, false, null, removeWhitespaceBounding, removeWhitespaceBounding);
   }

   public FairTypeDrawOptions getDrawOptions(FairType.TextAlign align, int maxWidth, int lineSpacing, boolean forceMaxWidth, boolean removeWhitespaceBounding) {
      return this.getDrawOptions(align, maxWidth, forceMaxWidth, lineSpacing, -1, false, null, removeWhitespaceBounding, removeWhitespaceBounding);
   }

   public FairTypeDrawOptions getDrawOptions(
      FairType.TextAlign align,
      int maxWidth,
      boolean forceMaxWidth,
      int lineSpacing,
      int maxLines,
      boolean cutLastLineWord,
      FontOptions ellipsisFO,
      boolean removeWhitespaceBounding
   ) {
      return this.getDrawOptions(
         align, maxWidth, forceMaxWidth, lineSpacing, maxLines, cutLastLineWord, ellipsisFO, removeWhitespaceBounding, removeWhitespaceBounding
      );
   }

   public FairTypeDrawOptions getDrawOptions(
      FairType.TextAlign textAlign,
      int maxWidth,
      boolean forceMaxWidth,
      int lineSpacing,
      int maxLines,
      boolean cutLastLineWord,
      FontOptions ellipsisFO,
      boolean removeWhitespaceLeft,
      boolean removeWhiteSpaceRight
   ) {
      FairType.DrawVars drawVars = new FairType.DrawVars();
      boolean displaysEverything = true;

      for (int i = 0; i < this.glyphs.size(); i++) {
         FairGlyph glyph = this.glyphs.get(i);
         if (glyph.isNewLineGlyph()) {
            drawVars.submitLine();
            if (maxLines > 0 && drawVars.lines.size() >= maxLines) {
               if (i < this.glyphs.size() - 1) {
                  displaysEverything = false;
               }
               break;
            }
         }

         FloatDimension dim = glyph.getDimensions();
         if (maxWidth > 0 && forceMaxWidth && drawVars.currentLine.currentWord.width + dim.width > maxWidth) {
            drawVars.currentLine.submitWord();
         }

         drawVars.currentLine.currentWord.glyphs.add(new FairType.GlyphIndex(i, glyph));
         drawVars.currentLine.currentWord.width = drawVars.currentLine.currentWord.width + dim.width;
         drawVars.currentLine.currentWord.height = Math.max(drawVars.currentLine.currentWord.height, dim.height);
         if (glyph.isWhiteSpaceGlyph() && !glyph.isNewLineGlyph()) {
            drawVars.currentLine.submitWord();
         }
      }

      if (maxLines <= 0 || drawVars.lines.size() < maxLines) {
         drawVars.submitLine();
      }

      if (maxWidth > 0) {
         for (int i = 0; i < drawVars.lines.size(); i++) {
            FairType.DrawLine currentLine = drawVars.lines.get(i);
            if (currentLine.width > maxWidth) {
               LinkedList<FairType.DrawWord> lineWords = currentLine.words;
               currentLine.words = new LinkedList<>();
               currentLine.width = 0.0F;
               currentLine.height = 0.0F;

               for (FairType.DrawWord word : lineWords) {
                  if (!currentLine.words.isEmpty() && currentLine.width + word.width > maxWidth) {
                     if (maxLines > 0 && i >= maxLines - 1 && cutLastLineWord) {
                        FairType.DrawWord cutWord = new FairType.DrawWord();

                        while (!word.glyphs.isEmpty()) {
                           FairType.GlyphIndex glyph = word.glyphs.getFirst();
                           FloatDimension dim = glyph.glyph.getDimensions();
                           if (!(currentLine.width + cutWord.width + dim.width <= maxWidth)) {
                              break;
                           }

                           cutWord.glyphs.add(glyph);
                           cutWord.width = cutWord.width + dim.width;
                           cutWord.height = Math.max(cutWord.height, dim.height);
                           word.glyphs.removeFirst();
                        }

                        if (!cutWord.glyphs.isEmpty()) {
                           word.updateDimensions();
                           currentLine.words.add(cutWord);
                           currentLine.width = currentLine.width + cutWord.width;
                           currentLine.height = Math.max(currentLine.height, cutWord.height);
                        }
                     }

                     drawVars.lines.add(i + 1, currentLine = new FairType.DrawLine());
                     if (maxLines > 0 && i >= maxLines - 1) {
                        if (i < drawVars.lines.size() - 1) {
                           displaysEverything = false;
                        }
                        break;
                     }

                     i++;
                  }

                  currentLine.words.add(word);
                  currentLine.width = currentLine.width + word.width;
                  currentLine.height = Math.max(currentLine.height, word.height);
               }

               if (maxLines > 0 && i >= maxLines) {
                  break;
               }
            }
         }
      }

      if (maxLines > 0) {
         while (drawVars.lines.size() > maxLines) {
            displaysEverything = false;
            drawVars.lines.remove(drawVars.lines.size() - 1);
         }
      }

      FairGlyph[] ellipsisGlyphs = null;
      if (!displaysEverything && ellipsisFO != null) {
         ellipsisGlyphs = FairCharacterGlyph.fromString(ellipsisFO, "...");
         if (maxWidth > 0) {
            FairType.DrawLine lastLine = drawVars.lines.get(drawVars.lines.size() - 1);
            float ellipsisWidth = Arrays.stream(ellipsisGlyphs).reduce(0.0F, (last, fairGlyph) -> last + fairGlyph.getDimensions().width, Float::sum);

            while (!lastLine.words.isEmpty() && !(lastLine.width + ellipsisWidth <= maxWidth)) {
               FairType.DrawWord lastWord = lastLine.words.getLast();
               if (!lastWord.glyphs.isEmpty()) {
                  lastWord.glyphs.removeLast();
                  lastWord.updateDimensions();
                  lastLine.updateDimensions();
               }

               if (lastWord.glyphs.isEmpty()) {
                  lastLine.words.removeLast();
                  lastLine.updateDimensions();
               }
            }
         }
      }

      drawVars.lines.forEach(l -> {
         l.drawWidth = l.width;
         if (removeWhitespaceLeft) {
            l.stripWhitespaceLeft();
         }

         if (removeWhiteSpaceRight) {
            l.stripWhitespaceRight();
         }
      });
      Supplier<Color> currentColor = null;
      final LinkedList<GlyphContainer> drawGlyphs = new LinkedList<>();
      float[] lineHeights = new float[drawVars.lines.size()];
      float boundingX = 0.0F;
      float boundingWidth = 0.0F;
      float boundingHeight = 0.0F;
      float currentX = 0.0F;
      float currentY = 0.0F;
      final LinkedList<GlyphContainer> ellipsisGlyphsDraw = new LinkedList<>();

      for (int lineNum = 0; lineNum < drawVars.lines.size(); lineNum++) {
         FairType.DrawLine line = drawVars.lines.get(lineNum);
         lineHeights[lineNum] = line.height;
         float offsetX = 0.0F;
         if (textAlign == FairType.TextAlign.CENTER) {
            offsetX = -line.drawWidth / 2.0F;
         } else if (textAlign == FairType.TextAlign.RIGHT) {
            offsetX = -line.drawWidth;
         }

         boundingX = Math.min(boundingX == 0.0F ? 2.1474836E9F : boundingX, offsetX);
         boundingWidth = Math.max(boundingWidth, line.drawWidth);
         boundingHeight += line.height;
         if (line.height > 0.0F) {
            boundingHeight += lineSpacing;
         }

         for (FairType.DrawWord word : line.words) {
            for (FairType.GlyphIndex e : word.glyphs) {
               Supplier<Supplier<Color>> newCurrentColor = e.glyph.getDefaultColor();
               if (newCurrentColor != null) {
                  currentColor = newCurrentColor.get();
               }

               drawGlyphs.add(
                  new GlyphContainer(e.glyph, e.index, lineNum, line.height, currentX + offsetX + line.drawOffsetX, currentY + line.height, currentColor)
               );
               currentX += e.glyph.getDimensions().width;
            }
         }

         if (lineNum == drawVars.lines.size() - 1 && ellipsisGlyphs != null) {
            float currentEllipsisWidth = 0.0F;

            for (FairGlyph ellipsisGlyph : ellipsisGlyphs) {
               ellipsisGlyphsDraw.add(
                  new GlyphContainer(ellipsisGlyph, -1, lineNum, line.height, currentX + currentEllipsisWidth, currentY + line.height, currentColor)
               );
               currentEllipsisWidth += ellipsisGlyph.getDimensions().width;
            }

            boundingWidth = Math.max(boundingWidth, line.drawWidth + currentEllipsisWidth);
         }

         currentY += line.height;
         if (line.height > 0.0F) {
            currentY += lineSpacing;
         }

         currentX = 0.0F;
      }

      final FairType copy = new FairType(this);
      final Rectangle bounding = new Rectangle((int)boundingX, 0, GameMath.ceil(boundingWidth), GameMath.ceil(boundingHeight));
      final boolean lastPixelFont = Settings.pixelFont;
      final boolean finalDisplaysEverything = displaysEverything;
      return new FairTypeDrawOptions(
         textAlign, lineSpacing, lineHeights, maxWidth, forceMaxWidth, maxLines, cutLastLineWord, ellipsisFO, removeWhitespaceLeft, removeWhiteSpaceRight
      ) {
         @Override
         public void handleInputEvent(int drawX, int drawY, InputEvent event) {
            for (GlyphContainer drawGlyph : drawGlyphs) {
               if (event.isUsed()) {
                  break;
               }

               drawGlyph.handleInputEvent(drawX, drawY, event);
            }
         }

         @Override
         public Rectangle getBoundingBox(int drawX, int drawY) {
            return new Rectangle(bounding.x + drawX, bounding.y + drawY, bounding.width, bounding.height);
         }

         @Override
         public void drawCharacters(int drawX, int drawY, Color defaultColor) {
            drawGlyphs.forEach(gc -> gc.draw(drawX, drawY, defaultColor));
            ellipsisGlyphsDraw.forEach(gc -> gc.draw(drawX, drawY, defaultColor));
         }

         @Override
         public void drawShadows(int drawX, int drawY) {
            drawGlyphs.forEach(gc -> gc.drawShadow(drawX, drawY));
            ellipsisGlyphsDraw.forEach(gc -> gc.drawShadow(drawX, drawY));
         }

         @Override
         public LinkedList<GlyphContainer> getDrawList() {
            return drawGlyphs;
         }

         @Override
         public boolean displaysEverything() {
            return finalDisplaysEverything;
         }

         @Override
         public FairType getType() {
            return copy;
         }

         @Override
         public boolean shouldUpdate() {
            return lastPixelFont != Settings.pixelFont;
         }
      };
   }

   private class DrawLine {
      public LinkedList<FairType.DrawWord> words = new LinkedList<>();
      public FairType.DrawWord currentWord = FairType.this.new DrawWord();
      public float drawWidth;
      public float drawOffsetX;
      public float width;
      public float height;

      private DrawLine() {
      }

      public void submitWord() {
         this.words.add(this.currentWord);
         this.width = this.width + this.currentWord.width;
         this.height = Math.max(this.height, this.currentWord.height);
         this.currentWord = FairType.this.new DrawWord();
      }

      public void stripWhitespaceLeft() {
         if (!this.words.isEmpty()) {
            float leftWhiteSpace = this.whiteSpaceSize(this.words.iterator(), w -> w.glyphs.iterator());
            this.drawOffsetX -= leftWhiteSpace;
            this.drawWidth -= leftWhiteSpace;
         }
      }

      public void stripWhitespaceRight() {
         if (!this.words.isEmpty()) {
            this.drawWidth = this.drawWidth - this.whiteSpaceSize(this.words.descendingIterator(), w -> w.glyphs.descendingIterator());
         }
      }

      public void updateDimensions() {
         this.width = 0.0F;
         this.height = 0.0F;

         for (FairType.DrawWord word : this.words) {
            this.width = this.width + word.width;
            this.height = Math.max(this.height, word.height);
         }
      }

      public float whiteSpaceSize(Iterator<FairType.DrawWord> wordIt, Function<FairType.DrawWord, Iterator<FairType.GlyphIndex>> glyphItSupplier) {
         float width = 0.0F;

         while (wordIt.hasNext()) {
            Iterator<FairType.GlyphIndex> glyphIt = glyphItSupplier.apply(wordIt.next());

            while (glyphIt.hasNext()) {
               FairType.GlyphIndex gi = glyphIt.next();
               if (!gi.glyph.isWhiteSpaceGlyph()) {
                  return width;
               }

               width += gi.glyph.getDimensions().width;
            }
         }

         return width;
      }
   }

   private class DrawVars {
      public ArrayList<FairType.DrawLine> lines = new ArrayList<>();
      public FairType.DrawLine currentLine = FairType.this.new DrawLine();

      private DrawVars() {
      }

      public void submitLine() {
         this.currentLine.submitWord();
         this.lines.add(this.currentLine);
         this.currentLine = FairType.this.new DrawLine();
      }
   }

   private class DrawWord {
      public LinkedList<FairType.GlyphIndex> glyphs = new LinkedList<>();
      public float width;
      public float height;

      private DrawWord() {
      }

      public void updateDimensions() {
         this.width = 0.0F;
         this.height = 0.0F;

         for (FairType.GlyphIndex e : this.glyphs) {
            FloatDimension dim = e.glyph.getDimensions();
            this.width = this.width + dim.width;
            this.height = Math.max(this.height, dim.height);
         }
      }

      @Override
      public String toString() {
         return Arrays.toString(this.glyphs.toArray());
      }
   }

   private static class GlyphIndex {
      public final int index;
      public final FairGlyph glyph;

      public GlyphIndex(int index, FairGlyph glyph) {
         this.index = index;
         this.glyph = glyph;
      }

      @Override
      public String toString() {
         return this.index + ":" + this.glyph.toString();
      }
   }

   public static class Parse {
      public final TypeParser<?> parser;
      public final FairGlyph[] oldGlyphs;
      public final int start;
      public final int end;
      public final FairGlyph[] newGlyphs;

      private Parse(TypeParser<?> parser, FairGlyph[] oldGlyphs, int start, int end, FairGlyph[] newGlyphs) {
         this.parser = parser;
         this.oldGlyphs = oldGlyphs;
         this.start = start;
         this.end = end;
         this.newGlyphs = newGlyphs;
      }
   }

   public enum TextAlign {
      LEFT,
      CENTER,
      RIGHT;
   }
}
