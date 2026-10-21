package necesse.engine.screenHudManager;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.function.Predicate;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;

public class ScreenFloatTextFade extends ScreenFloatText {
   public int riseTime;
   public int hoverTime;
   public int fadeTime;
   private long spawnTime;
   private String text;
   private final FontOptions fontOptions;
   private int x;
   private int y;
   private int width;
   private int height;
   protected int heightIncrease;
   public boolean avoidOtherText;
   public Predicate<ScreenFloatTextFade> avoidOtherTextFilter;
   public int expandTime;

   public ScreenFloatTextFade(int x, int y, FontOptions fontOptions) {
      this.fontOptions = fontOptions;
      this.x = x;
      this.y = y;
      this.expandTime = 150;
      this.riseTime = 500;
      this.hoverTime = 0;
      this.fadeTime = 1000;
      this.heightIncrease = 50;
   }

   public ScreenFloatTextFade(int x, int y, String text, FontOptions fontOptions) {
      this(x, y, fontOptions);
      this.setText(text);
   }

   public void resetSpawnTime() {
      this.spawnTime = this.getTime();
   }

   public boolean isAlive() {
      return this.getTime() - this.spawnTime <= this.riseTime + this.hoverTime + this.fadeTime;
   }

   public float getLifeProgress() {
      return (float)(this.getTime() - this.spawnTime) / (this.riseTime + this.hoverTime + this.fadeTime);
   }

   public String getText() {
      return this.text;
   }

   public void setText(String text) {
      this.text = text;
      this.width = FontManager.bit.getWidthCeil(text, this.fontOptions);
      this.height = FontManager.bit.getHeightCeil(text, this.fontOptions);
   }

   @Override
   public void draw(TickManager tickManager) {
      if (!this.isRemoved()) {
         if (!this.isAlive()) {
            this.remove();
         } else {
            long timeAlive = this.getTime() - this.spawnTime;
            int drawX = this.x;
            int drawY = this.getY();
            float alpha = 1.0F;
            if (timeAlive >= this.riseTime + this.hoverTime) {
               alpha = Math.abs((float)(timeAlive - (this.riseTime + this.hoverTime)) / this.fadeTime - 1.0F);
            }

            float size = 1.0F;
            if (timeAlive < this.expandTime) {
               size = GameMath.limit((float)timeAlive / this.expandTime, 0.0F, 1.0F);
            }

            FontOptions fontOptions = new FontOptions(this.fontOptions).size((int)(this.fontOptions.getSize() * size)).alphaf(alpha);
            FontManager.bit.drawString(drawX - FontManager.bit.getWidthCeil(this.text, fontOptions) / 2, drawY, this.text, fontOptions);
         }
      }
   }

   @Override
   public int getX() {
      return this.x - this.width / 2;
   }

   public int getRealX() {
      return this.x;
   }

   @Override
   public int getY() {
      return this.y - this.getCurrentHeightIncrease();
   }

   public int getRealY() {
      return this.y;
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
      return new Rectangle(this.x, this.y - this.height, this.getWidth(), this.getHeight());
   }

   @Override
   public void addThis(ArrayList<ScreenHudElement> elements) {
      super.addThis(elements);
      this.resetSpawnTime();
      if (this.avoidOtherText) {
         while (this.checkCollision(elements)) {
         }
      }
   }

   public boolean checkCollision(ArrayList<ScreenHudElement> elements) {
      for (ScreenHudElement e : elements) {
         if (!e.isRemoved() && e != this && e instanceof ScreenFloatTextFade) {
            ScreenFloatTextFade other = (ScreenFloatTextFade)e;
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
