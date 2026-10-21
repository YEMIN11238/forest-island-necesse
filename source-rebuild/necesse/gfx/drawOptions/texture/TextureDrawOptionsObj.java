package necesse.gfx.drawOptions.texture;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.function.Consumer;
import necesse.engine.util.GameMath;
import necesse.gfx.ImpossibleDrawException;
import necesse.gfx.TextureBinder;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.shader.ShaderState;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

class TextureDrawOptionsObj {
   public final TextureBinder textureBinder;
   protected ArrayList<ShaderBind> shaderBinds = new ArrayList<>();
   protected ArrayList<ShaderSpriteAbstract> shaderSprites = new ArrayList<>();
   protected float drawDepth = 0.0F;
   protected int translateX;
   protected int translateY;
   protected float red;
   protected float green;
   protected float blue;
   protected float alpha;
   protected float[] advCol;
   protected float spriteX1;
   protected float spriteX2;
   protected float spriteX3;
   protected float spriteX4;
   protected float spriteY1;
   protected float spriteY2;
   protected float spriteY3;
   protected float spriteY4;
   protected float spriteWidth;
   protected float spriteHeight;
   protected int width;
   protected int height;
   protected boolean useRotation;
   protected int rotTranslateX;
   protected int rotTranslateY;
   protected float rotation;
   protected TextureDrawOptionsObj.PointRotate pointRotate;
   protected LinkedList<TextureDrawOptionsObj.PositionMod> positionMods = new LinkedList<>();
   protected boolean mirrorX;
   protected boolean mirrorY;
   protected boolean setBlend;
   protected int blendSourceRGB;
   protected int blendDestinationRGB;
   protected int blendSourceAlpha;
   protected int blendDestinationAlpha;
   protected LinkedList<ShaderState> shaderStates = new LinkedList<>();
   private float x1;
   private float y1;
   private float x2;
   private float y2;
   private float x3;
   private float y3;
   private float x4;
   private float y4;

   TextureDrawOptionsObj(TextureBinder textureBinder, int width, int height) {
      this.textureBinder = textureBinder;
      this.red = 1.0F;
      this.green = 1.0F;
      this.blue = 1.0F;
      this.alpha = 1.0F;
      this.advCol = null;
      this.spriteX1 = 0.0F;
      this.spriteX2 = 1.0F;
      this.spriteX3 = 1.0F;
      this.spriteX4 = 0.0F;
      this.spriteY1 = 0.0F;
      this.spriteY2 = 0.0F;
      this.spriteY3 = 1.0F;
      this.spriteY4 = 1.0F;
      this.spriteWidth = 1.0F;
      this.spriteHeight = 1.0F;
      this.width = width;
      this.height = height;
      this.useRotation = false;
      this.rotTranslateX = 0;
      this.rotTranslateY = 0;
      this.rotation = 0.0F;
      this.pointRotate = null;
      this.mirrorX = false;
      this.mirrorY = false;
      this.setBlend = false;
   }

   TextureDrawOptionsObj(GameTexture texture, int width, int height) {
      this((TextureBinder)texture, width, height);
   }

   TextureDrawOptionsObj(TextureDrawOptionsObj copy) {
      this.textureBinder = copy.textureBinder;
      this.shaderBinds.addAll(copy.shaderBinds);
      this.shaderSprites.addAll(copy.shaderSprites);
      this.drawDepth = copy.drawDepth;
      this.translateX = copy.translateX;
      this.translateY = copy.translateY;
      this.red = copy.red;
      this.green = copy.green;
      this.blue = copy.blue;
      this.alpha = copy.alpha;
      this.advCol = copy.advCol;
      this.spriteX1 = copy.spriteX1;
      this.spriteX2 = copy.spriteX2;
      this.spriteX3 = copy.spriteX3;
      this.spriteX4 = copy.spriteX4;
      this.spriteY1 = copy.spriteY1;
      this.spriteY2 = copy.spriteY2;
      this.spriteY3 = copy.spriteY3;
      this.spriteY4 = copy.spriteY4;
      this.spriteWidth = copy.spriteWidth;
      this.spriteHeight = copy.spriteHeight;
      this.width = copy.width;
      this.height = copy.height;
      this.useRotation = copy.useRotation;
      this.rotTranslateX = copy.rotTranslateX;
      this.rotTranslateY = copy.rotTranslateY;
      this.rotation = copy.rotation;
      if (copy.pointRotate != null) {
         this.pointRotate = new TextureDrawOptionsObj.PointRotate(copy.pointRotate);
      } else {
         this.pointRotate = null;
      }

      this.positionMods.addAll(copy.positionMods);
      this.mirrorX = copy.mirrorX;
      this.mirrorY = copy.mirrorY;
      this.setBlend = copy.setBlend;
      this.blendSourceRGB = copy.blendSourceRGB;
      this.blendDestinationRGB = copy.blendDestinationRGB;
      this.blendSourceAlpha = copy.blendSourceAlpha;
      this.blendDestinationAlpha = copy.blendDestinationAlpha;
      this.shaderStates.addAll(copy.shaderStates);
      this.x1 = copy.x1;
      this.y1 = copy.y1;
      this.x2 = copy.x2;
      this.y2 = copy.y2;
      this.x3 = copy.x3;
      this.y3 = copy.y3;
      this.x4 = copy.x4;
      this.y4 = copy.y4;
   }

   public void setRotation(float rotation, int centerX, int centerY) {
      this.pointRotate = new TextureDrawOptionsObj.PointRotate(rotation, centerX, centerY);
   }

   public void addRotation(float rotation, int centerX, int centerY) {
      this.positionMods.add(new TextureDrawOptionsObj.PointRotate(rotation, centerX, centerY));
   }

   public void addPositionMod(final Consumer<TextureDrawOptionsPositionMod> mod) {
      this.positionMods.add(new TextureDrawOptionsObj.PositionMod() {
         @Override
         public float apply(float drawX, float drawY, float lastRotation) {
            mod.accept(new TextureDrawOptionsPositionMod() {
               @Override
               public int getWidth() {
                  return TextureDrawOptionsObj.this.width;
               }

               @Override
               public int getHeight() {
                  return TextureDrawOptionsObj.this.height;
               }

               @Override
               public void addX1(float pixels) {
                  TextureDrawOptionsObj.this.x1 += pixels;
               }

               @Override
               public void addX2(float pixels) {
                  TextureDrawOptionsObj.this.x2 += pixels;
               }

               @Override
               public void addX3(float pixels) {
                  TextureDrawOptionsObj.this.x3 += pixels;
               }

               @Override
               public void addX4(float pixels) {
                  TextureDrawOptionsObj.this.x4 += pixels;
               }

               @Override
               public void addY1(float pixels) {
                  TextureDrawOptionsObj.this.y1 += pixels;
               }

               @Override
               public void addY2(float pixels) {
                  TextureDrawOptionsObj.this.y2 += pixels;
               }

               @Override
               public void addY3(float pixels) {
                  TextureDrawOptionsObj.this.y3 += pixels;
               }

               @Override
               public void addY4(float pixels) {
                  TextureDrawOptionsObj.this.y4 += pixels;
               }
            });
            return lastRotation;
         }
      });
   }

   public void pos(int drawX, int drawY) {
      drawX += this.translateX;
      drawY += this.translateY;
      this.x1 = drawX;
      this.y1 = drawY;
      this.x2 = this.x1 + this.width;
      this.y2 = this.y1;
      this.x3 = this.x2;
      this.y3 = this.y1 + this.height;
      this.x4 = this.x1;
      this.y4 = this.y3;
      float currentRotation = 0.0F;
      if (this.pointRotate != null) {
         currentRotation = this.pointRotate.apply(this.x1, this.y1, currentRotation);
      }

      for (TextureDrawOptionsObj.PositionMod change : this.positionMods) {
         currentRotation = change.apply(this.x1, this.y1, currentRotation);
      }
   }

   public void draw(boolean glBegin, boolean glEnd) {
      try {
         if (this.shaderBinds.isEmpty() && this.shaderSprites.isEmpty()) {
            this.drawSingleTexture(glBegin, glEnd);
         } else {
            this.drawMultiTexture(glBegin, glEnd);
         }
      } catch (Exception e) {
         ImpossibleDrawException.submitDrawError(e);
      }
   }

   protected void drawSingleTexture(boolean glBegin, boolean glEnd) {
      this.shaderStates.iterator().forEachRemaining(ShaderState::use);
      if (glBegin) {
         this.glBegin(false);
      }

      GL11.glColor4f(this.red, this.green, this.blue, this.alpha);
      float spriteX1 = this.spriteX1;
      float spriteX2 = this.spriteX2;
      float spriteX3 = this.spriteX3;
      float spriteX4 = this.spriteX4;
      float spriteY1 = this.spriteY1;
      float spriteY2 = this.spriteY2;
      float spriteY3 = this.spriteY3;
      float spriteY4 = this.spriteY4;
      if (this.mirrorX) {
         spriteX1 = this.spriteX2;
         spriteX2 = this.spriteX1;
         spriteX3 = this.spriteX4;
         spriteX4 = this.spriteX3;
      }

      if (this.mirrorY) {
         spriteY1 = this.spriteY4;
         spriteY2 = this.spriteY3;
         spriteY3 = this.spriteY2;
         spriteY4 = this.spriteY1;
      }

      if (this.advCol != null) {
         GL11.glColor4f(this.advCol[0], this.advCol[1], this.advCol[2], this.advCol[3]);
      }

      GL11.glTexCoord2f(spriteX1, spriteY1);
      GL11.glVertex3f(this.x1, this.y1, this.drawDepth);
      if (this.advCol != null) {
         GL11.glColor4f(this.advCol[4], this.advCol[5], this.advCol[6], this.advCol[7]);
      }

      GL11.glTexCoord2f(spriteX2, spriteY2);
      GL11.glVertex3f(this.x2, this.y2, this.drawDepth);
      if (this.advCol != null) {
         GL11.glColor4f(this.advCol[8], this.advCol[9], this.advCol[10], this.advCol[11]);
      }

      GL11.glTexCoord2f(spriteX3, spriteY3);
      GL11.glVertex3f(this.x3, this.y3, this.drawDepth);
      if (this.advCol != null) {
         GL11.glColor4f(this.advCol[12], this.advCol[13], this.advCol[14], this.advCol[15]);
      }

      GL11.glTexCoord2f(spriteX4, spriteY4);
      GL11.glVertex3f(this.x4, this.y4, this.drawDepth);
      if (glEnd) {
         this.glEnd();
      }

      this.shaderStates.descendingIterator().forEachRemaining(ShaderState::stop);
   }

   protected void drawMultiTexture(boolean glBegin, boolean glEnd) {
      this.shaderStates.iterator().forEachRemaining(ShaderState::use);
      if (glBegin) {
         this.glBegin(true);
      }

      if (this.setBlend) {
         GL14.glBlendFuncSeparate(this.blendSourceRGB, this.blendDestinationRGB, this.blendSourceAlpha, this.blendDestinationAlpha);
      }

      GL11.glColor4f(this.red, this.green, this.blue, this.alpha);
      float spriteX1 = this.spriteX1;
      float spriteX2 = this.spriteX2;
      float spriteX3 = this.spriteX3;
      float spriteX4 = this.spriteX4;
      float spriteY1 = this.spriteY1;
      float spriteY2 = this.spriteY2;
      float spriteY3 = this.spriteY3;
      float spriteY4 = this.spriteY4;
      if (this.mirrorX) {
         spriteX1 = this.spriteX2;
         spriteX2 = this.spriteX1;
         spriteX3 = this.spriteX4;
         spriteX4 = this.spriteX3;
      }

      if (this.mirrorY) {
         spriteY1 = this.spriteY4;
         spriteY2 = this.spriteY3;
         spriteY3 = this.spriteY2;
         spriteY4 = this.spriteY1;
      }

      if (this.advCol != null) {
         GL11.glColor4f(this.advCol[0], this.advCol[1], this.advCol[2], this.advCol[3]);
      }

      GL13.glMultiTexCoord2f(33984, spriteX1, spriteY1);
      this.shaderSprites.forEach(ShaderSpriteAbstract::startTopLeft);
      GL11.glVertex3f(this.x1, this.y1, this.drawDepth);
      if (this.advCol != null) {
         GL11.glColor4f(this.advCol[4], this.advCol[5], this.advCol[6], this.advCol[7]);
      }

      GL13.glMultiTexCoord2f(33984, spriteX2, spriteY2);
      this.shaderSprites.forEach(ShaderSpriteAbstract::startTopRight);
      GL11.glVertex3f(this.x2, this.y2, this.drawDepth);
      if (this.advCol != null) {
         GL11.glColor4f(this.advCol[8], this.advCol[9], this.advCol[10], this.advCol[11]);
      }

      GL13.glMultiTexCoord2f(33984, spriteX3, spriteY3);
      this.shaderSprites.forEach(ShaderSpriteAbstract::startBotRight);
      GL11.glVertex3f(this.x3, this.y3, this.drawDepth);
      if (this.advCol != null) {
         GL11.glColor4f(this.advCol[12], this.advCol[13], this.advCol[14], this.advCol[15]);
      }

      GL13.glMultiTexCoord2f(33984, spriteX4, spriteY4);
      this.shaderSprites.forEach(ShaderSpriteAbstract::startBotLeft);
      GL11.glVertex3f(this.x4, this.y4, this.drawDepth);
      if (glEnd) {
         this.glEnd();
      }

      this.shaderStates.descendingIterator().forEachRemaining(ShaderState::stop);
   }

   public void glBegin(boolean multi) {
      this.textureBinder.bindTexture();
      if (multi) {
         this.shaderBinds.forEach(ShaderBind::bind);
      }

      GL11.glLoadIdentity();
      if (this.useRotation) {
         GL11.glTranslatef(this.rotTranslateX + this.x1, this.rotTranslateY + this.y1, 0.0F);
         GL11.glRotatef(this.rotation, 0.0F, 0.0F, 1.0F);
         GL11.glTranslatef(-this.rotTranslateX - this.x1, -this.rotTranslateY - this.y1, 0.0F);
      }

      GL11.glBegin(7);
   }

   public void glEnd() {
      GL11.glEnd();
      if (this.useRotation) {
         GL11.glLoadIdentity();
      }

      if (this.setBlend) {
         GL14.glBlendFuncSeparate(770, 771, 1, 771);
      }
   }

   protected class PointRotate extends TextureDrawOptionsObj.PositionMod {
      protected final float pointRotate;
      protected final int pointRotateX;
      protected final int pointRotateY;

      public PointRotate(float pointRotate, int pointRotateX, int pointRotateY) {
         this.pointRotate = pointRotate;
         this.pointRotateX = pointRotateX;
         this.pointRotateY = pointRotateY;
      }

      public PointRotate(TextureDrawOptionsObj.PointRotate other) {
         this.pointRotate = other.pointRotate;
         this.pointRotateX = other.pointRotateX;
         this.pointRotateY = other.pointRotateY;
      }

      @Override
      public float apply(float drawX, float drawY, float lastRotation) {
         float nextRotation = this.pointRotate + lastRotation;
         if (this.pointRotate != 0.0F) {
            float cos = GameMath.cos(this.pointRotate);
            float sin = GameMath.sin(this.pointRotate);
            float centerX = drawX + this.pointRotateX;
            float centerY = drawY + this.pointRotateY;
            if (lastRotation != 0.0F) {
               float lastCos = GameMath.cos(lastRotation);
               float lastSin = GameMath.sin(lastRotation);
               float temp1 = this.pointRotateX * lastCos - this.pointRotateY * lastSin + drawX;
               centerY = this.pointRotateX * lastSin + this.pointRotateY * lastCos + drawY;
               centerX = temp1;
            }

            float temp1 = (TextureDrawOptionsObj.this.x1 - centerX) * cos - (TextureDrawOptionsObj.this.y1 - centerY) * sin + centerX;
            TextureDrawOptionsObj.this.y1 = (TextureDrawOptionsObj.this.x1 - centerX) * sin + (TextureDrawOptionsObj.this.y1 - centerY) * cos + centerY;
            TextureDrawOptionsObj.this.x1 = temp1;
            float temp2 = (TextureDrawOptionsObj.this.x2 - centerX) * cos - (TextureDrawOptionsObj.this.y2 - centerY) * sin + centerX;
            TextureDrawOptionsObj.this.y2 = (TextureDrawOptionsObj.this.x2 - centerX) * sin + (TextureDrawOptionsObj.this.y2 - centerY) * cos + centerY;
            TextureDrawOptionsObj.this.x2 = temp2;
            float temp3 = (TextureDrawOptionsObj.this.x3 - centerX) * cos - (TextureDrawOptionsObj.this.y3 - centerY) * sin + centerX;
            TextureDrawOptionsObj.this.y3 = (TextureDrawOptionsObj.this.x3 - centerX) * sin + (TextureDrawOptionsObj.this.y3 - centerY) * cos + centerY;
            TextureDrawOptionsObj.this.x3 = temp3;
            float temp4 = (TextureDrawOptionsObj.this.x4 - centerX) * cos - (TextureDrawOptionsObj.this.y4 - centerY) * sin + centerX;
            TextureDrawOptionsObj.this.y4 = (TextureDrawOptionsObj.this.x4 - centerX) * sin + (TextureDrawOptionsObj.this.y4 - centerY) * cos + centerY;
            TextureDrawOptionsObj.this.x4 = temp4;
         }

         return nextRotation;
      }
   }

   protected abstract static class PositionMod {
      public abstract float apply(float var1, float var2, float var3);
   }
}
