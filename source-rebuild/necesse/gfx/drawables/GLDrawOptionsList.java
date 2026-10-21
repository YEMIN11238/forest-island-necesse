package necesse.gfx.drawables;

import java.util.ArrayList;
import necesse.gfx.TextureBinder;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import org.lwjgl.opengl.GL11;

public class GLDrawOptionsList {
   private final ArrayList<float[]> coords;
   private final ArrayList<float[]> colors;

   public GLDrawOptionsList(int initialCapacity) {
      this.coords = new ArrayList<>(initialCapacity);
      this.colors = new ArrayList<>(initialCapacity);
   }

   public GLDrawOptionsList() {
      this(0);
   }

   public synchronized GLDrawOptionsList add(float[] coords, float[] colors) {
      this.coords.add(coords);
      this.colors.add(colors);
      return this;
   }

   public synchronized GLDrawOptionsList add(float xCoord, float yCoord, float[] color) {
      return this.add(new float[]{xCoord, yCoord}, color);
   }

   public synchronized GLDrawOptionsList add(float xCoord, float yCoord, float red, float green, float blue, float alpha) {
      return this.add(new float[]{xCoord, yCoord}, new float[]{red, green, blue, alpha});
   }

   public int size() {
      return this.coords.size();
   }

   public synchronized DrawOptions pos(int drawMode) {
      return () -> {
         GameTexture.unbindTexture();
         GL11.glLoadIdentity();
         GL11.glBegin(drawMode);

         for (int i = 0; i < this.coords.size(); i++) {
            float[] coord = this.coords.get(i);
            float[] color = this.colors.get(i);
            GL11.glColor4f(color[0], color[1], color[2], color[3]);
            GL11.glVertex2f(coord[0], coord[1]);
         }

         GL11.glEnd();
      };
   }

   public synchronized void draw(int drawMode) {
      GameTexture.unbindTexture();
      GL11.glLoadIdentity();
      GL11.glBegin(drawMode);

      for (int i = 0; i < this.coords.size(); i++) {
         float[] coord = this.coords.get(i);
         float[] color = this.colors.get(i);
         GL11.glColor4f(color[0], color[1], color[2], color[3]);
         GL11.glVertex2f(coord[0], coord[1]);
      }

      GL11.glEnd();
   }

   public synchronized DrawOptions pos(int drawMode, int coordsPerElement) {
      return this.pos(drawMode, coordsPerElement, SharedTextureDrawOptions.MAX_VERTEX_CALLS_PER_DRAW_CALL);
   }

   public synchronized DrawOptions pos(int drawMode, int coordsPerElement, int maxElementsPerCall) {
      return () -> {
         GameTexture.unbindTexture();
         int drawCounter = 0;
         GL11.glLoadIdentity();
         GL11.glBegin(drawMode);

         for (int i = 0; i < this.coords.size(); i++) {
            float[] coord = this.coords.get(i);
            float[] color = this.colors.get(i);
            if (drawCounter >= maxElementsPerCall) {
               GL11.glEnd();
               GL11.glBegin(drawMode);
               drawCounter = 0;
            }

            for (int j = 0; j < coordsPerElement; j++) {
               int coordIndex = j * 2;
               int colorIndex = j * 4;
               GL11.glColor4f(color[colorIndex], color[colorIndex + 1], color[colorIndex + 2], color[colorIndex + 3]);
               GL11.glVertex2f(coord[coordIndex], coord[coordIndex + 1]);
            }

            drawCounter++;
         }

         GL11.glEnd();
      };
   }

   public synchronized void draw(int drawMode, int coordsPerElement) {
      this.draw(drawMode, coordsPerElement, SharedTextureDrawOptions.MAX_VERTEX_CALLS_PER_DRAW_CALL);
   }

   public synchronized void draw(int drawMode, int coordsPerElement, int maxElementsPerCall) {
      this.draw(TextureBinder.NO_TEXTURE, drawMode, coordsPerElement, maxElementsPerCall);
   }

   public synchronized void draw(TextureBinder binder, int drawMode, int coordsPerElement, int maxElementsPerCall) {
      binder.bindTexture();
      int drawCounter = 0;
      GL11.glLoadIdentity();
      GL11.glBegin(drawMode);

      for (int i = 0; i < this.coords.size(); i++) {
         float[] coord = this.coords.get(i);
         float[] color = this.colors.get(i);
         if (drawCounter >= maxElementsPerCall) {
            GL11.glEnd();
            GL11.glLoadIdentity();
            GL11.glBegin(drawMode);
            drawCounter = 0;
         }

         for (int j = 0; j < coordsPerElement; j++) {
            int coordIndex = j * 2;
            int colorIndex = j * 4;
            GL11.glColor4f(color[colorIndex], color[colorIndex + 1], color[colorIndex + 2], color[colorIndex + 3]);
            GL11.glVertex2f(coord[coordIndex], coord[coordIndex + 1]);
         }

         drawCounter++;
      }

      GL11.glEnd();
   }
}
