package necesse.entity.mobs.summon;

import java.awt.geom.Line2D.Float;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MinecartLine extends Float {
   public final int tileX;
   public final int tileY;
   public final int dir;
   public final float distance;
   public Supplier<MinecartLine> nextPositive;
   public Supplier<MinecartLine> nextNegative;

   protected MinecartLine(int tileX, int tileY, float x1, float y1, float x2, float y2, int dir, float distance) {
      super(tileX * 32 + x1, tileY * 32 + y1, tileX * 32 + x2, tileY * 32 + y2);
      this.tileX = tileX;
      this.tileY = tileY;
      this.dir = dir;
      this.distance = distance;
   }

   public static MinecartLine up(int tileX, int tileY) {
      return new MinecartLine(tileX, tileY, 16.0F, 0.0F, 16.0F, 16.0F, 0, 16.0F);
   }

   public static MinecartLine down(int tileX, int tileY) {
      return new MinecartLine(tileX, tileY, 16.0F, 16.0F, 16.0F, 32.0F, 2, 16.0F);
   }

   public static MinecartLine left(int tileX, int tileY) {
      return new MinecartLine(tileX, tileY, 0.0F, 16.0F, 16.0F, 16.0F, 3, 16.0F);
   }

   public static MinecartLine right(int tileX, int tileY) {
      return new MinecartLine(tileX, tileY, 16.0F, 16.0F, 32.0F, 16.0F, 1, 16.0F);
   }

   public static MinecartLine upEnd(int tileX, int tileY) {
      return new MinecartLine(tileX, tileY, 16.0F, 14.0F, 16.0F, 16.0F, 0, 2.0F);
   }

   public static MinecartLine downEnd(int tileX, int tileY) {
      return new MinecartLine(tileX, tileY, 16.0F, 16.0F, 16.0F, 26.0F, 2, 10.0F);
   }

   public static MinecartLine leftEnd(int tileX, int tileY) {
      return new MinecartLine(tileX, tileY, 10.0F, 16.0F, 16.0F, 16.0F, 3, 6.0F);
   }

   public static MinecartLine rightEnd(int tileX, int tileY) {
      return new MinecartLine(tileX, tileY, 16.0F, 16.0F, 22.0F, 16.0F, 1, 6.0F);
   }

   public MinecartLinePos progressLines(MinecartLine line, int moveDir, float currentDistanceAlong, float distanceToTravel, Consumer<MinecartLine> forEachLine) {
      while (true) {
         if (forEachLine != null) {
            forEachLine.accept(line);
         }

         if (moveDir == 0) {
            float missingDistance = currentDistanceAlong;
            if (!(distanceToTravel > missingDistance)) {
               if (line.dir != 1 && line.dir != 3) {
                  return new MinecartLinePos(line, line.x1, line.y1 + currentDistanceAlong - distanceToTravel, currentDistanceAlong - distanceToTravel, moveDir);
               }

               return new MinecartLinePos(line, line.x1 + currentDistanceAlong - distanceToTravel, line.y1, currentDistanceAlong - distanceToTravel, 1);
            }

            distanceToTravel -= missingDistance;
            currentDistanceAlong = 0.0F;
            if (line.nextNegative != null) {
               MinecartLine next = line.nextNegative.get();
               if (next != null) {
                  currentDistanceAlong = next.distance;
                  line = next;
                  if (next.dir == 1) {
                     moveDir = next.dir;
                     currentDistanceAlong = 0.0F;
                     continue;
                  }

                  if (next.dir == 3) {
                     moveDir = next.dir;
                     currentDistanceAlong = next.distance;
                  }
                  continue;
               }
            }
         } else if (moveDir == 1) {
            float missingDistance = line.distance - currentDistanceAlong;
            if (!(distanceToTravel > missingDistance)) {
               if (line.dir != 1 && line.dir != 3) {
                  return new MinecartLinePos(line, line.x1, line.y1 + currentDistanceAlong + distanceToTravel, currentDistanceAlong + distanceToTravel, 0);
               }

               return new MinecartLinePos(line, line.x1 + currentDistanceAlong + distanceToTravel, line.y1, currentDistanceAlong + distanceToTravel, moveDir);
            }

            distanceToTravel -= missingDistance;
            currentDistanceAlong = line.distance;
            if (line.nextPositive != null) {
               MinecartLine next = line.nextPositive.get();
               if (next != null) {
                  currentDistanceAlong = 0.0F;
                  line = next;
                  if (next.dir == 0) {
                     moveDir = next.dir;
                     currentDistanceAlong = next.distance;
                     continue;
                  }

                  if (next.dir == 2) {
                     moveDir = next.dir;
                     currentDistanceAlong = 0.0F;
                  }
                  continue;
               }
            }
         } else if (moveDir == 2) {
            float missingDistance = line.distance - currentDistanceAlong;
            if (!(distanceToTravel > missingDistance)) {
               if (line.dir != 1 && line.dir != 3) {
                  return new MinecartLinePos(line, line.x1, line.y1 + currentDistanceAlong + distanceToTravel, currentDistanceAlong + distanceToTravel, moveDir);
               }

               return new MinecartLinePos(line, line.x1 + currentDistanceAlong + distanceToTravel, line.y1, currentDistanceAlong + distanceToTravel, 3);
            }

            distanceToTravel -= missingDistance;
            currentDistanceAlong = line.distance;
            if (line.nextPositive != null) {
               MinecartLine next = line.nextPositive.get();
               if (next != null) {
                  currentDistanceAlong = 0.0F;
                  line = next;
                  if (next.dir == 1) {
                     moveDir = next.dir;
                     currentDistanceAlong = 0.0F;
                     continue;
                  }

                  if (next.dir == 3) {
                     moveDir = next.dir;
                     currentDistanceAlong = next.distance;
                  }
                  continue;
               }
            }
         } else {
            float missingDistance = currentDistanceAlong;
            if (!(distanceToTravel > missingDistance)) {
               if (line.dir != 1 && line.dir != 3) {
                  return new MinecartLinePos(line, line.x1, line.y1 + currentDistanceAlong - distanceToTravel, currentDistanceAlong - distanceToTravel, 2);
               }

               return new MinecartLinePos(line, line.x1 + currentDistanceAlong - distanceToTravel, line.y1, currentDistanceAlong - distanceToTravel, moveDir);
            }

            distanceToTravel -= missingDistance;
            currentDistanceAlong = 0.0F;
            if (line.nextNegative != null) {
               MinecartLine next = line.nextNegative.get();
               if (next != null) {
                  currentDistanceAlong = next.distance;
                  line = next;
                  if (next.dir == 0) {
                     moveDir = next.dir;
                     currentDistanceAlong = next.distance;
                     continue;
                  }

                  if (next.dir == 2) {
                     moveDir = next.dir;
                     currentDistanceAlong = 0.0F;
                  }
                  continue;
               }
            }
         }

         if (line.dir == 0) {
            return new MinecartLinePos(line, line.x1, line.y1 + 0.1F, 0.1F, moveDir, distanceToTravel);
         }

         if (line.dir == 1) {
            return new MinecartLinePos(line, line.x1 + line.distance - 0.1F, line.y1, line.distance - 0.1F, moveDir, distanceToTravel);
         }

         if (line.dir == 2) {
            return new MinecartLinePos(line, line.x1, line.y1 + line.distance - 0.1F, line.distance - 0.1F, moveDir, distanceToTravel);
         }

         return new MinecartLinePos(line, line.x1 + 0.1F, line.y1, 0.1F, moveDir, distanceToTravel);
      }
   }

   public MinecartLinePos progressLines(
      MinecartLine line, boolean positiveDirection, float currentDistanceAlong, float distanceToTravel, Consumer<MinecartLine> forEachLine
   ) {
      while (true) {
         if (forEachLine != null) {
            forEachLine.accept(line);
         }

         if (positiveDirection) {
            float missingDistance = line.distance - currentDistanceAlong;
            if (!(distanceToTravel > missingDistance)) {
               if (line.dir != 1 && line.dir != 3) {
                  return new MinecartLinePos(line, line.x1, line.y1 + currentDistanceAlong + distanceToTravel, currentDistanceAlong + distanceToTravel, 2);
               }

               return new MinecartLinePos(line, line.x1 + currentDistanceAlong + distanceToTravel, line.y1, currentDistanceAlong + distanceToTravel, 1);
            }

            distanceToTravel -= missingDistance;
         } else {
            float missingDistance = line.distance;
            if (!(distanceToTravel > missingDistance)) {
               if (line.dir != 1 && line.dir != 3) {
                  return new MinecartLinePos(line, line.x1, line.y1 + currentDistanceAlong - distanceToTravel, currentDistanceAlong - distanceToTravel, 0);
               }

               return new MinecartLinePos(line, line.x1 + currentDistanceAlong - distanceToTravel, line.y1, currentDistanceAlong - distanceToTravel, 3);
            }

            distanceToTravel -= missingDistance;
         }

         if (positiveDirection) {
            if (line.nextPositive != null) {
               MinecartLine next = line.nextPositive.get();
               if (next != null) {
                  if ((line.dir != 3 || next.dir != 0) && (line.dir != 0 || next.dir != 3)) {
                     currentDistanceAlong = 0.0F;
                  } else {
                     positiveDirection = false;
                     currentDistanceAlong = next.distance;
                  }

                  line = next;
                  continue;
               }
            }
         } else if (line.nextNegative != null) {
            MinecartLine next = line.nextNegative.get();
            if (next != null) {
               if ((line.dir != 1 || next.dir != 2) && (line.dir != 2 || next.dir != 1)) {
                  currentDistanceAlong = next.distance;
               } else {
                  positiveDirection = true;
                  currentDistanceAlong = 0.0F;
               }

               line = next;
               continue;
            }
         }

         if (positiveDirection) {
            if (line.dir != 1 && line.dir != 3) {
               return new MinecartLinePos(line, line.x1, line.y1 + line.distance, line.distance, 2, distanceToTravel);
            }

            return new MinecartLinePos(line, line.x1 + line.distance, line.y1, line.distance, 1, distanceToTravel);
         }

         if (line.dir != 1 && line.dir != 3) {
            return new MinecartLinePos(line, line.x1, line.y1, 0.0F, 0, distanceToTravel);
         }

         return new MinecartLinePos(line, line.x1, line.y1, 0.0F, 3, distanceToTravel);
      }
   }
}
