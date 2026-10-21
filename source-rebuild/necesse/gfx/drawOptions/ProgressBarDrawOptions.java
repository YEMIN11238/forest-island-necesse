package necesse.gfx.drawOptions;

import java.awt.Color;
import java.util.LinkedList;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.ui.ProgressBarTextures;

public class ProgressBarDrawOptions {
   public final GameTexture background;
   public final GameTexture fill;
   public final GameTexture indicator;
   public final int topPadding;
   public final int horizontalPadding;
   public final int width;
   protected LinkedList<ProgressBarDrawOptions.ProgressBarOptions> bars = new LinkedList<>();
   protected LinkedList<ProgressBarDrawOptions.ProgressIndicatorOptions> indicators = new LinkedList<>();
   protected Color backgroundColor = Color.WHITE;
   protected FontOptions fontOptions = new FontOptions(16);
   protected GameMessage text = null;
   protected int limitText = -1;
   protected int textAlign = 1;

   public ProgressBarDrawOptions(GameTexture background, GameTexture fill, GameTexture indicator, int topPadding, int horizontalPadding, int width) {
      this.background = background;
      this.fill = fill;
      this.indicator = indicator;
      this.topPadding = topPadding;
      this.horizontalPadding = horizontalPadding;
      this.width = width;
   }

   public ProgressBarDrawOptions(ProgressBarTextures textures, boolean inverted, int width) {
      this(
         inverted ? textures.fill : textures.background,
         inverted ? textures.background : textures.fill,
         textures.indicator,
         inverted ? -textures.topPadding : textures.topPadding,
         inverted ? -textures.horizontalPadding : textures.horizontalPadding,
         width
      );
   }

   public ProgressBarDrawOptions(ProgressBarTextures textures, int width) {
      this(textures, false, width);
   }

   public int getHeight() {
      return this.background.getHeight();
   }

   public ProgressBarDrawOptions color(Color color) {
      this.backgroundColor = color;
      return this;
   }

   public ProgressBarDrawOptions text(GameMessage text) {
      this.text = text;
      return this;
   }

   public ProgressBarDrawOptions text(String text) {
      return this.text(new StaticMessage(text));
   }

   public ProgressBarDrawOptions limitText(int padding) {
      this.limitText = padding;
      return this;
   }

   public ProgressBarDrawOptions textAlignLeft() {
      this.textAlign = 0;
      return this;
   }

   public ProgressBarDrawOptions textAlignCenter() {
      this.textAlign = 1;
      return this;
   }

   public ProgressBarDrawOptions textAlignRight() {
      this.textAlign = 2;
      return this;
   }

   public ProgressBarDrawOptions fontOptions(FontOptions fontOptions) {
      this.fontOptions = fontOptions;
      return this;
   }

   public ProgressBarDrawOptions.ProgressBarOptions addBar(float percent) {
      ProgressBarDrawOptions.ProgressBarOptions out = new ProgressBarDrawOptions.ProgressBarOptions(percent);
      this.bars.add(out);
      return out;
   }

   public ProgressBarDrawOptions addIndicator(float percent) {
      if (this.indicator == null) {
         throw new IllegalStateException("Progress bar does not support indicators");
      }

      this.indicators.add(new ProgressBarDrawOptions.ProgressIndicatorOptions(percent));
      return this;
   }

   public DrawOptions pos(int x, int y) {
      DrawOptionsList options = new DrawOptionsList();
      options.add(
         () -> FormComponent.drawWidthComponent(
            new GameSprite(this.background, 0, 0, this.background.getHeight()),
            new GameSprite(this.background, 1, 0, this.background.getHeight()),
            x,
            y,
            this.width,
            this.backgroundColor
         )
      );

      for (ProgressBarDrawOptions.ProgressBarOptions bar : this.bars) {
         options.add(bar.pos(x, y));
      }

      for (ProgressBarDrawOptions.ProgressIndicatorOptions indicator : this.indicators) {
         options.add(indicator.pos(x, y));
      }

      if (this.text != null) {
         int padding = Math.max(this.limitText, 0);
         String displayText = this.limitText >= 0
            ? GameUtils.maxString(this.text.translate(), this.fontOptions, this.width - padding * 2)
            : this.text.translate();
         int textWidth = FontManager.bit.getWidthCeil(displayText, this.fontOptions);
         int textHeight = this.fontOptions.getSize();
         int textY = y + this.topPadding + this.fill.getHeight() / 2 - textHeight / 2;
         int textX;
         switch (this.textAlign) {
            case 0:
               textX = x + padding;
               break;
            case 2:
               textX = x + this.width - textWidth - padding;
               break;
            default:
               textX = x + this.width / 2 - textWidth / 2;
         }

         options.add(() -> FontManager.bit.drawString(textX, textY, displayText, this.fontOptions));
      }

      return options;
   }

   public void draw(int x, int y) {
      this.pos(x, y).draw();
   }

   public class ProgressBarOptions {
      protected float percent;
      protected Color color = Color.WHITE;
      protected int minWidth;

      public ProgressBarOptions(float percent) {
         this.percent = percent;
      }

      public ProgressBarDrawOptions.ProgressBarOptions color(Color color) {
         this.color = color;
         return this;
      }

      public ProgressBarDrawOptions.ProgressBarOptions minWidth(int minWidth) {
         this.minWidth = minWidth;
         return this;
      }

      public ProgressBarDrawOptions end() {
         return ProgressBarDrawOptions.this;
      }

      protected DrawOptions pos(int x, int y) {
         float percent;
         if (this.minWidth > 0) {
            float minPercent = (float)this.minWidth / ProgressBarDrawOptions.this.width;
            percent = GameMath.lerp(this.percent, minPercent, 1.0F);
         } else {
            percent = this.percent;
         }

         int barWidth = (int)Math.ceil((ProgressBarDrawOptions.this.width - ProgressBarDrawOptions.this.horizontalPadding * 2) * percent);
         return barWidth > 0
            ? () -> FormComponent.drawWidthComponent(
               new GameSprite(ProgressBarDrawOptions.this.fill, 0, 0, ProgressBarDrawOptions.this.fill.getHeight()),
               new GameSprite(ProgressBarDrawOptions.this.fill, 1, 0, ProgressBarDrawOptions.this.fill.getHeight()),
               x + ProgressBarDrawOptions.this.topPadding,
               y + ProgressBarDrawOptions.this.topPadding,
               barWidth,
               this.color
            )
            : () -> {};
      }
   }

   public class ProgressIndicatorOptions {
      protected float percent;

      public ProgressIndicatorOptions(float percent) {
         this.percent = percent;
      }

      public ProgressBarDrawOptions end() {
         return ProgressBarDrawOptions.this;
      }

      protected DrawOptions pos(int x, int y) {
         int position = (int)Math.ceil((ProgressBarDrawOptions.this.width - ProgressBarDrawOptions.this.horizontalPadding * 2) * this.percent)
            - ProgressBarDrawOptions.this.indicator.getWidth() / 2;
         return ProgressBarDrawOptions.this.indicator
            .initDraw()
            .pos(x + ProgressBarDrawOptions.this.topPadding + position, y + ProgressBarDrawOptions.this.topPadding);
      }
   }
}
