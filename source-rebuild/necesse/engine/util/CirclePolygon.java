package necesse.engine.util;

import java.awt.Polygon;

public class CirclePolygon extends Polygon {
   public CirclePolygon(int centerX, int centerY, int xRadius, int yRadius, int numSides) {
      super(getXCoords(centerX, xRadius, numSides), getYCoords(centerY, yRadius, numSides), numSides);
   }

   private static int[] getXCoords(int centerX, int radius, int numSides) {
      int[] xCoords = new int[numSides];

      for (int i = 0; i < numSides; i++) {
         double angle = (Math.PI * 2) * i / numSides;
         xCoords[i] = centerX + (int)(radius * Math.cos(angle));
      }

      return xCoords;
   }

   private static int[] getYCoords(int centerY, int radius, int numSides) {
      int[] yCoords = new int[numSides];

      for (int i = 0; i < numSides; i++) {
         double angle = (Math.PI * 2) * i / numSides;
         yCoords[i] = centerY + (int)(radius * Math.sin(angle));
      }

      return yCoords;
   }
}
