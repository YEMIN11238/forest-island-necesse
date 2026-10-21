package necesse.level.maps.hudManager.floatText;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.StringDrawOptions;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.hudManager.HudDrawElement;
import necesse.level.maps.hudManager.HudManager;

public class FloatTextFade extends FloatText {
   public int riseTime;
   public int hoverTime;
   public int fadeOutTime;
   public int fadeInTime;
   private long spawnTime;
   private String[] lines;
   protected FontOptions fontOptions;
   private int x;
   private int y;
   private int width;
   private int height;
   public int heightIncrease;
   public boolean avoidOtherText;
   public Predicate<FloatTextFade> avoidOtherTextFilter;
   public int expandTime;
   public int typeWriterExpandTime = 0;
   public boolean typeWriterExpandFromCenter = false;

   public FloatTextFade(int x, int y, FontOptions fontOptions) {
      this.fontOptions = fontOptions;
      this.x = x;
      this.y = y;
      this.expandTime = 150;
      this.riseTime = 500;
      this.hoverTime = 0;
      this.fadeOutTime = 1000;
      this.fadeInTime = 0;
      this.heightIncrease = 50;
   }

   public FloatTextFade(int x, int y, String text, FontOptions fontOptions) {
      this(x, y, fontOptions);
      this.setText(text);
   }

   @Override
   public void init(HudManager manager) {
      super.init(manager);
      this.resetSpawnTime();
      if (this.avoidOtherText) {
         while (this.checkCollision(manager.getElements())) {
         }
      }
   }

   public void resetSpawnTime() {
      this.spawnTime = this.getTime();
   }

   public boolean isAlive() {
      return (float)(this.getTime() - this.spawnTime) <= this.getHighestFadeInOrRiseTime() + this.hoverTime + this.fadeOutTime;
   }

   public float getLifeProgress() {
      return (float)(this.getTime() - this.spawnTime) / (this.getHighestFadeInOrRiseTime() + this.hoverTime + this.fadeOutTime);
   }

   public float getFullLifeTime() {
      return this.getHighestFadeInOrRiseTime() + this.hoverTime + this.fadeOutTime;
   }

   private float getHighestFadeInOrRiseTime() {
      return Math.max(this.riseTime, this.fadeInTime);
   }

   public String[] getLines() {
      return this.lines;
   }

   public String getText() {
      return GameUtils.join(this.lines, "\n");
   }

   public void setText(String text) {
      this.lines = text.split("\\n");
      this.width = 0;
      this.height = 0;

      for (String line : this.lines) {
         this.width = Math.max(this.width, FontManager.bit.getWidthCeil(line, this.fontOptions));
         this.height = this.height + FontManager.bit.getHeightCeil(line, this.fontOptions);
      }
   }

   @Override
   public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
      if (!this.isRemoved()) {
         if (this.getLevel() != null) {
            if (camera.getBounds().intersects(this.getCollision())) {
               if (!this.isAlive()) {
                  this.remove();
               } else {
                  long timeAlive = this.getTime() - this.spawnTime;
                  int drawX = camera.getDrawX(this.getAnchorX());
                  int drawY = camera.getDrawY(this.getY() - this.getHeight());
                  float alpha = 1.0F;
                  if ((float)timeAlive >= this.getHighestFadeInOrRiseTime() + this.hoverTime) {
                     alpha = Math.abs(((float)timeAlive - (this.getHighestFadeInOrRiseTime() + this.hoverTime)) / this.fadeOutTime - 1.0F);
                  }

                  if (timeAlive < this.fadeInTime) {
                     alpha = GameMath.limit((float)timeAlive / this.fadeInTime, 0.0F, 1.0F);
                  }

                  float size = 1.0F;
                  if (timeAlive < this.expandTime) {
                     size = GameMath.limit((float)timeAlive / this.expandTime, 0.0F, 1.0F);
                  }

                  FontOptions fontOptions = new FontOptions(this.fontOptions).size((int)(this.fontOptions.getSize() * size)).alphaf(alpha);
                  final DrawOptionsList drawOptions = new DrawOptionsList();
                  int totalCharactersToShow = Integer.MAX_VALUE;
                  if (this.typeWriterExpandTime > 0) {
                     int totalCharacters = Arrays.stream(this.lines).mapToInt(String::length).sum();
                     totalCharactersToShow = (int)((float)timeAlive / this.typeWriterExpandTime * totalCharacters);
                  }

                  int totalCharactersShown = 0;

                  for (int i = 0; i < this.lines.length; i++) {
                     String line = this.lines[i];
                     int charactersToShowLeft = totalCharactersToShow - totalCharactersShown;
                     if (charactersToShowLeft <= 0) {
                        break;
                     }

                     totalCharactersShown += line.length();
                     if (line.length() > charactersToShowLeft) {
                        line = line.substring(0, charactersToShowLeft);
                     }

                     if (this.typeWriterExpandFromCenter) {
                        drawOptions.add(new StringDrawOptions(fontOptions, line).posCenterX(drawX, drawY));
                     } else {
                        int halfWidth = FontManager.bit.getWidthCeil(this.lines[i], fontOptions) / 2;
                        drawOptions.add(new StringDrawOptions(fontOptions, line).pos(drawX - halfWidth, drawY));
                     }

                     if (i < this.lines.length - 1) {
                        drawY += FontManager.bit.getHeightCeil(line, fontOptions);
                     }
                  }

                  list.add(new SortedDrawable() {
                     @Override
                     public int getPriority() {
                        return 0;
                     }

                     @Override
                     public void draw(TickManager tickManager) {
                        drawOptions.draw();
                     }
                  });
               }
            }
         }
      }
   }

   public int getAnchorX() {
      return this.x;
   }

   public int getAnchorY() {
      return this.y;
   }

   @Override
   public int getX() {
      return this.getAnchorX() - this.width / 2;
   }

   @Override
   public int getY() {
      return this.getAnchorY() - this.getCurrentHeightIncrease();
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   public void setX(int x) {
      this.x = x + this.width / 2;
   }

   public void setY(int y) {
      this.y = y + this.getCurrentHeightIncrease();
   }

   private int getCurrentHeightIncrease() {
      float lifeProgress = Math.min(1.0F, (float)(this.getTime() - this.spawnTime) / this.riseTime);
      return (int)(this.heightIncrease * GameMath.sin(lifeProgress * 90.0F));
   }

   public void setPos(int x, int y) {
      this.setX(x);
      this.setY(y);
   }

   @Override
   public Rectangle getCollision() {
      return new Rectangle(this.getAnchorX(), this.getAnchorY() - this.getHeight(), this.getWidth(), this.getHeight());
   }

   public boolean checkCollision(Iterable<HudDrawElement> elements) {
      for (HudDrawElement e : elements) {
         if (!e.isRemoved() && e != this && e instanceof FloatTextFade) {
            FloatTextFade other = (FloatTextFade)e;
            if ((this.avoidOtherTextFilter == null || this.avoidOtherTextFilter.test(other)) && other.collidesWith(this)) {
               int increase = this.getHeight() + 2 - (other.y - this.y);
               increase = Math.max(increase, 2);
               this.setY(this.getY() - increase);
               return true;
            }
         }
      }

      return false;
   }
}
