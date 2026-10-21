package necesse.entity.chains;

import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.Drawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import org.lwjgl.opengl.GL11;

public class Chain {
   public ChainLocation pos1;
   public ChainLocation pos2;
   public float height;
   private boolean removed;
   private int drawStart;
   private float m;
   private boolean calculatedM;
   public boolean drawOnTop;
   public int drawOnTopOrder = -10000;
   public GameSprite sprite = new GameSprite(GameResources.chains, 1, 0, 32);

   public Chain(ChainLocation pos1, ChainLocation pos2) {
      this.pos1 = pos1;
      this.pos2 = pos2;
   }

   public Chain(int x1, int y1, int x2, int y2) {
      this(new StaticChainLocation(x1, y1), new StaticChainLocation(x2, y2));
   }

   public int getMaxY() {
      return Math.max(this.pos1.getY(), this.pos2.getY());
   }

   public int getMinY() {
      return Math.min(this.pos1.getY(), this.pos2.getY());
   }

   public float getXPos(float y) {
      if (!(y < this.getMinY()) && !(y > this.getMaxY())) {
         if (!this.calculatedM) {
            float xDif = this.pos2.getX() - this.pos1.getX();
            float yDif = this.pos2.getY() - this.pos1.getY();
            if (xDif == 0.0F) {
               this.m = 0.0F;
            } else {
               this.m = yDif / xDif;
            }

            this.calculatedM = true;
         }

         return this.m == 0.0F ? this.pos1.getX() : (y - this.pos1.getY() + this.m * this.pos1.getX()) / this.m;
      } else {
         return -1.0F;
      }
   }

   public void addDrawables(OrderableDrawables list, int startTileY, int endTileY, Level level, TickManager tickManager, GameCamera camera) {
      final ArrayList<LevelSortedDrawable> sortedDrawables = new ArrayList<>();
      this.addDrawables(sortedDrawables, startTileY, endTileY, level, tickManager, camera);
      sortedDrawables.sort(null);
      list.add(this.drawOnTopOrder, new Drawable() {
         @Override
         public void draw(TickManager tickManager) {
            sortedDrawables.forEach(d -> d.draw(tickManager));
         }
      });
   }

   public void addDrawables(List<LevelSortedDrawable> list, int startTileY, int endTileY, Level level, TickManager tickManager, GameCamera camera) {
      this.resetDraw();

      for (int i = startTileY; i < endTileY; i++) {
         int startY = i * 32;
         int endY = startY + 32;
         if ((startY >= this.getMinY() || endY >= this.getMinY()) && (startY <= this.getMaxY() || endY <= this.getMaxY())) {
            final int sortY = startY + this.getDrawY();
            final DrawOptions options = this.getDrawSection(startY, endY, level, camera);
            list.add(new LevelSortedDrawable(this) {
               @Override
               public int getSortY() {
                  return sortY;
               }

               @Override
               public void draw(TickManager tickManager) {
                  Performance.record(tickManager, "chainDraw", options::draw);
               }
            });
         }
      }
   }

   private DrawOptions getDrawSection(int startY, int endY, Level level, GameCamera camera) {
      int startX = (int)this.getXPos(startY);
      int endX = (int)this.getXPos(endY);
      if (endX == -1) {
         endY = this.getMaxY();
         endX = (int)this.getXPos(endY);
      }

      if (startX == -1) {
         startY = this.getMinY();
         startX = (int)this.getXPos(startY);
      }

      if (this.pos1.getY() == this.pos2.getY()) {
         startX = this.pos1.getX();
         endX = this.pos2.getX();
      }

      DrawOptionsList options = new DrawOptionsList();
      this.drawStart = this.addSpriteChainLightStartTextLevelDrawOptions(
         options, this.sprite, startX, startY, endX, endY, this.drawStart, this.height, level, camera
      );
      return options;
   }

   protected int addSpriteChainLightStartTextLevelDrawOptions(
      List<DrawOptions> options, GameSprite sprite, int x1, int y1, int x2, int y2, int start, float chainHeight, Level level, GameCamera camera
   ) {
      Float tempPoint = new Float(x2 - x1, y2 - y1);
      float dist = (float)tempPoint.distance(0.0, 0.0);
      float normX = tempPoint.x / dist;
      float normY = tempPoint.y / dist;
      float halfWidth = sprite.width / 2.0F;
      int height = sprite.height;
      float i = -start;
      float posX = x1;
      float posY = y1;

      while (i < dist) {
         if (i > dist - sprite.height) {
            height = Math.abs((int)(i - dist));
         }

         i += sprite.height;
         int size = Math.abs(start - height);
         float nextPosX = posX + normX * size;
         float nextPosY = posY + normY * size;
         float middlePosX = posX + normX * size / 2.0F;
         float halfNextPosY = posY + normY * size / 2.0F;
         GameLight light = new GameLight(150.0F);
         if (level != null) {
            light = level.getLightLevel(GameMath.getTileCoordinate(middlePosX), GameMath.getTileCoordinate(halfNextPosY));
         }

         Runnable glColorSetter = light.getGLColorSetter(1.0F, 1.0F, 1.0F, 1.0F);
         Float drawPos1 = new Float(camera.getDrawX(posX), camera.getDrawY(posY) - chainHeight);
         Float drawPos2 = new Float(camera.getDrawX(nextPosX), camera.getDrawY(nextPosY) - chainHeight);
         Float quad1 = GameMath.getPerpendicularPoint(drawPos1, halfWidth, normX, normY);
         Float quad2 = GameMath.getPerpendicularPoint(drawPos1, -halfWidth, normX, normY);
         Float quad3 = GameMath.getPerpendicularPoint(drawPos2, -halfWidth, normX, normY);
         Float quad4 = GameMath.getPerpendicularPoint(drawPos2, halfWidth, normX, normY);
         float spriteX1 = TextureDrawOptions.pixel(sprite.spriteX, sprite.spriteWidth, sprite.texture.getWidth());
         float spriteX2 = TextureDrawOptions.pixel(sprite.spriteX + 1, sprite.spriteWidth, sprite.texture.getWidth());
         float spriteY1 = TextureDrawOptions.pixel(sprite.spriteY, start, sprite.spriteHeight, sprite.texture.getHeight());
         float spriteY2 = TextureDrawOptions.pixel(sprite.spriteY, height, sprite.spriteHeight, sprite.texture.getHeight());
         options.add(() -> {
            sprite.texture.bindTexture();
            glColorSetter.run();
            GL11.glBegin(7);
            GL11.glTexCoord2f(spriteX1, spriteY1);
            GL11.glVertex2f(quad1.x, quad1.y - 1.0F);
            GL11.glTexCoord2f(spriteX2, spriteY1);
            GL11.glVertex2f(quad2.x, quad2.y - 1.0F);
            GL11.glTexCoord2f(spriteX2, spriteY2);
            GL11.glVertex2f(quad3.x, quad3.y);
            GL11.glTexCoord2f(spriteX1, spriteY2);
            GL11.glVertex2f(quad4.x, quad4.y);
            GL11.glEnd();
         });
         posX = nextPosX;
         posY = nextPosY;
         start = 0;
      }

      return height;
   }

   public void remove() {
      this.removed = true;
   }

   public boolean isRemoved() {
      return this.pos1.removed() || this.pos2.removed() || this.removed;
   }

   public int getDrawY() {
      return (int)this.height;
   }

   public void resetDraw() {
      this.drawStart = 0;
      this.calculatedM = false;
   }
}
