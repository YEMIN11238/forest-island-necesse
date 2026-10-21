package necesse.engine.util;

import java.awt.Dimension;
import java.awt.geom.Dimension2D;

public class FloatDimension extends Dimension2D {
   public float width;
   public float height;

   public FloatDimension() {
      this(0.0F, 0.0F);
   }

   public FloatDimension(FloatDimension d) {
      this(d.width, d.height);
   }

   public FloatDimension(float width, float height) {
      this.width = width;
      this.height = height;
   }

   @Override
   public double getWidth() {
      return this.width;
   }

   @Override
   public double getHeight() {
      return this.height;
   }

   public Dimension toInt() {
      return new Dimension(GameMath.ceil(this.width), GameMath.ceil(this.height));
   }

   @Override
   public void setSize(double width, double height) {
      this.setSize((float)width, (float)height);
   }

   public void setSize(float width, float height) {
      this.width = width;
      this.height = height;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof FloatDimension)) {
         return false;
      }

      FloatDimension d = (FloatDimension)obj;
      return this.width == d.width && this.height == d.height;
   }

   @Override
   public int hashCode() {
      float sum = this.width + this.height;
      return (int)(sum * (sum + 1.0F) / 2.0F + this.width);
   }

   @Override
   public String toString() {
      return this.getClass().getName() + "[width=" + this.width + ",height=" + this.height + "]";
   }
}
