package necesse.engine.util;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Point2D.Float;

public final class GameMath {
   private static final int cosSinePrecision = 3600;
   private static final double cosSineDiv = 0.1;
   private static final float[] cos = new float[3600];
   private static final float[] sin = new float[3600];
   public static final double diagonalDistance;
   private static final long LONG_KEY_BIT_MASK;

   private GameMath() {
      throw new IllegalStateException("GameMath cannot be instantiated");
   }

   public static float cos(float angle) {
      return cos[(int)(fixAngle(angle) / 0.1)];
   }

   public static float sin(float angle) {
      return sin[(int)(fixAngle(angle) / 0.1)];
   }

   public static int limit(int value, int min, int max) {
      return min(max(value, min), max);
   }

   public static long limit(long value, long min, long max) {
      return min(max(value, min), max);
   }

   public static float limit(float value, float min, float max) {
      return min(max(value, min), max);
   }

   public static double limit(double value, double min, double max) {
      return min(max(value, min), max);
   }

   public static int max(int... values) {
      if (values.length == 0) {
         throw new IllegalArgumentException("Must give at least 1 value");
      }

      int out = values[0];

      for (int i = 1; i < values.length; i++) {
         out = Math.max(out, values[i]);
      }

      return out;
   }

   public static int min(int... values) {
      if (values.length == 0) {
         throw new IllegalArgumentException("Must give at least 1 value");
      }

      int out = values[0];

      for (int i = 1; i < values.length; i++) {
         out = Math.min(out, values[i]);
      }

      return out;
   }

   public static long max(long... values) {
      if (values.length == 0) {
         throw new IllegalArgumentException("Must give at least 1 value");
      }

      long out = values[0];

      for (int i = 1; i < values.length; i++) {
         out = Math.max(out, values[i]);
      }

      return out;
   }

   public static long min(long... values) {
      if (values.length == 0) {
         throw new IllegalArgumentException("Must give at least 1 value");
      }

      long out = values[0];

      for (int i = 1; i < values.length; i++) {
         out = Math.min(out, values[i]);
      }

      return out;
   }

   public static float max(float... values) {
      if (values.length == 0) {
         throw new IllegalArgumentException("Must give at least 1 value");
      }

      float out = values[0];

      for (int i = 1; i < values.length; i++) {
         out = Math.max(out, values[i]);
      }

      return out;
   }

   public static float min(float... values) {
      if (values.length == 0) {
         throw new IllegalArgumentException("Must give at least 1 value");
      }

      float out = values[0];

      for (int i = 1; i < values.length; i++) {
         out = Math.min(out, values[i]);
      }

      return out;
   }

   public static double max(double... values) {
      if (values.length == 0) {
         throw new IllegalArgumentException("Must give at least 1 value");
      }

      double out = values[0];

      for (int i = 1; i < values.length; i++) {
         out = Math.max(out, values[i]);
      }

      return out;
   }

   public static double min(double... values) {
      if (values.length == 0) {
         throw new IllegalArgumentException("Must give at least 1 value");
      }

      double out = values[0];

      for (int i = 1; i < values.length; i++) {
         out = Math.min(out, values[i]);
      }

      return out;
   }

   public static int floor(double value) {
      return (int)Math.floor(value);
   }

   public static int ceil(double value) {
      return (int)Math.ceil(value);
   }

   public static double floorMod(double value, double mod) {
      value %= mod;
      if (value < 0.0) {
         value += mod;
      }

      return value;
   }

   public static double floorMod(float value, float mod) {
      value %= mod;
      if (value < 0.0F) {
         value += mod;
      }

      return value;
   }

   public static float getPercentageBetweenTwoNumbers(float value, float min, float max) {
      return clamp(value, min, max);
   }

   public static float clamp(float value, float min, float max) {
      return (value - min) / (max - min);
   }

   public static double clamp(double value, double min, double max) {
      return (value - min) / (max - min);
   }

   public static float lerp(float percent, float min, float max) {
      return min + (max - min) * percent;
   }

   public static float lerpExp(float percent, float exponent, float min, float max) {
      return lerp((float)Math.pow(percent, exponent), min, max);
   }

   public static float lerpExpSmooth(float percent, float smooth, float exponent, float min, float max) {
      if (smooth == 0.0F) {
         return lerpExp(percent, exponent, min, max);
      }

      float expPercent = (float)Math.pow(percent, exponent);
      float smoothPercent = (expPercent + percent / smooth) / (1.0F + 1.0F / smooth);
      return lerp(smoothPercent, min, max);
   }

   public static float exp(float percent, float exponent) {
      return (float)Math.pow(percent, exponent);
   }

   public static float expSmooth(float percent, float smooth, float exponent) {
      float expPercent = exp(percent, exponent);
      return smooth == 0.0F ? expPercent : (expPercent + percent / smooth) / (1.0F + 1.0F / smooth);
   }

   public static int lerp(float percent, int min, int max) {
      return (int)(min + (max - min) * percent);
   }

   public static int lerpExp(float percent, float exponent, int min, int max) {
      return lerp((float)Math.pow(percent, exponent), min, max);
   }

   public static int lerpExpSmooth(float percent, float smooth, float exponent, int min, int max) {
      if (smooth == 0.0F) {
         return lerpExp(percent, exponent, min, max);
      }

      float expPercent = (float)Math.pow(percent, exponent);
      float smoothPercent = (expPercent + percent / smooth) / (1.0F + 1.0F / smooth);
      return lerp(smoothPercent, min, max);
   }

   public static double lerp(double percent, double min, double max) {
      return min + (max - min) * percent;
   }

   public static long lerp(double percent, long min, long max) {
      return (long)(min + (max - min) * percent);
   }

   public static long lerpExp(double percent, double exponent, long min, long max) {
      return lerp(Math.pow(percent, exponent), min, max);
   }

   public static long lerpExpSmooth(double percent, double smooth, double exponent, long min, long max) {
      if (smooth == 0.0) {
         return lerpExp(percent, exponent, min, max);
      }

      double expPercent = Math.pow(percent, exponent);
      double smoothPercent = (expPercent + percent / smooth) / (1.0 + 1.0 / smooth);
      return lerp(smoothPercent, min, max);
   }

   public static float map(float value, float valueMin, float valueMax, float targetMin, float targetMax) {
      value -= valueMin;
      float valueDelta = valueMax - valueMin;
      return lerp(value / valueDelta, targetMin, targetMax);
   }

   public static double map(double value, double valueMin, double valueMax, double targetMin, double targetMax) {
      value -= valueMin;
      double valueDelta = valueMax - valueMin;
      return lerp(value / valueDelta, targetMin, targetMax);
   }

   public static int roundToNearest(float value, int delimiter) {
      return Math.round(value / delimiter) * delimiter;
   }

   public static int floorToNearest(float value, int delimiter) {
      return (int)Math.floor(value / delimiter) * delimiter;
   }

   public static int ceilToNearest(float value, int delimiter) {
      return (int)Math.ceil(value / delimiter) * delimiter;
   }

   public static float toDecimals(float value, int decimals) {
      if (decimals < 0) {
         throw new IllegalArgumentException("Decimals must be equal or larger than 0");
      }

      float mult = (float)Math.pow(10.0, decimals);
      return Math.round(value * mult) / mult;
   }

   public static double toDecimals(double value, int decimals) {
      if (decimals < 0) {
         throw new IllegalArgumentException("Decimals must be equal or larger than 0");
      }

      double mult = Math.pow(10.0, decimals);
      return Math.round(value * mult) / mult;
   }

   public static String removeDecimalIfZero(float value) {
      return (int)value == value ? "" + (int)value : "" + value;
   }

   public static String removeDecimalIfZero(double value) {
      return (int)value == value ? "" + (int)value : "" + value;
   }

   public static double nthRoot(double value, double n) {
      return Math.pow(value, 1.0 / n);
   }

   public static float nthRoot(float value, float n) {
      return (float)Math.pow(value, 1.0 / n);
   }

   public static int nthRoot(int value, int n) {
      return (int)Math.pow(value, 1.0 / n);
   }

   public static float pixelsToCentimeters(float pixels) {
      return pixels * 3.86F;
   }

   public static float pixelsToMeters(float pixels) {
      return pixelsToCentimeters(pixels) / 100.0F;
   }

   public static int metersToPixels(float meters) {
      return (int)(meters / pixelsToMeters(1.0F));
   }

   public static float getAngleDifference(float a, float b) {
      a = fixAngle(a);
      b = fixAngle(b);
      float d = Math.abs(a - b) % 360.0F;
      float r = d > 180.0F ? 360.0F - d : d;
      int sign = (!(a - b >= 0.0F) || !(a - b <= 180.0F)) && (!(a - b <= -180.0F) || !(a - b >= -360.0F)) ? -1 : 1;
      return r * sign;
   }

   public static float fixAngle(float angle) {
      float out = angle % 360.0F;
      if (angle < 0.0F) {
         float v = 360.0F + out;
         return v == 360.0F ? 0.0F : v;
      } else {
         return out;
      }
   }

   public static Float getAngleDir(float angle) {
      float dx = cos(angle);
      float dy = sin(angle);
      return new Float(dx, dy);
   }

   public static float getAngle(Float dir) {
      return (float)Math.toDegrees(Math.atan2(dir.y, dir.x));
   }

   public static double getAngle(Double dir) {
      return Math.toDegrees(Math.atan2(dir.y, dir.x));
   }

   public static double getExactDistance(double x1, double y1, double x2, double y2) {
      x1 -= x2;
      y1 -= y2;
      return Math.sqrt(x1 * x1 + y1 * y1);
   }

   public static float getExactDistance(float x1, float y1, float x2, float y2) {
      x1 -= x2;
      y1 -= y2;
      return (float)Math.sqrt(x1 * x1 + y1 * y1);
   }

   public static double diagonalMoveDistance(int x1, int y1, int x2, int y2) {
      int dx = Math.abs(x1 - x2);
      int dy = Math.abs(y1 - y2);
      if (dx < dy) {
         int difference = dy - dx;
         return dx * diagonalDistance + difference;
      } else if (dy < dx) {
         int difference = dx - dy;
         return dy * diagonalDistance + difference;
      } else {
         return dx * diagonalDistance;
      }
   }

   public static double diagonalMoveDistance(Point p1, Point p2) {
      return diagonalMoveDistance(p1.x, p1.y, p2.x, p2.y);
   }

   public static double diagonalMoveDistance(double dx, double dy) {
      if (dx < dy) {
         double difference = dy - dx;
         return dx * diagonalDistance + difference;
      } else if (dy < dx) {
         double difference = dx - dy;
         return dy * diagonalDistance + difference;
      } else {
         return dx * diagonalDistance;
      }
   }

   public static double diagonalMoveDistance(double x1, double y1, double x2, double y2) {
      return diagonalMoveDistance(Math.abs(x1 - x2), Math.abs(y1 - y2));
   }

   public static float squareDistance(float x1, float y1, float x2, float y2) {
      return max(Math.abs(x1 - x2), Math.abs(y1 - y2));
   }

   public static float diamondDistance(float x1, float y1, float x2, float y2) {
      return Math.abs(x1 - x2) + Math.abs(y1 - y2);
   }

   public static double diamondDistance(double x1, double y1, double x2, double y2) {
      return Math.abs(x1 - x2) + Math.abs(y1 - y2);
   }

   public static float preciseDistance(float x1, float y1, float x2, float y2) {
      double px = x1 - x2;
      double py = y1 - y2;
      return (float)Math.sqrt(px * px + py * py);
   }

   public static float getAverage(float... values) {
      float sum = 0.0F;

      for (float value : values) {
         sum += value;
      }

      return sum / values.length;
   }

   public static Float normalize(float dx, float dy) {
      Float tempPoint = new Float(dx, dy);
      float dist = (float)tempPoint.distance(0.0, 0.0);
      float normX = dist == 0.0F ? 0.0F : tempPoint.x / dist;
      float normY = dist == 0.0F ? 0.0F : tempPoint.y / dist;
      return new Float(normX, normY);
   }

   public static Double normalize(double dx, double dy) {
      Double tempPoint = new Double(dx, dy);
      double dist = tempPoint.distance(0.0, 0.0);
      double normX = dist == 0.0 ? 0.0 : tempPoint.x / dist;
      double normY = dist == 0.0 ? 0.0 : tempPoint.y / dist;
      return new Double(normX, normY);
   }

   public static <T> IntersectionPoint<T> getIntersectionPoint(T target, Line2D l, Rectangle2D r, boolean checkInsideRect) {
      double x1 = l.getX1();
      double y1 = l.getY1();
      java.awt.geom.Line2D.Double topLine = new java.awt.geom.Line2D.Double(r.getX() - 1.0, r.getY(), r.getX() + r.getWidth() + 1.0, r.getY());
      java.awt.geom.Line2D.Double leftLine = new java.awt.geom.Line2D.Double(r.getX(), r.getY() - 1.0, r.getX(), r.getY() + r.getHeight() + 1.0);
      java.awt.geom.Line2D.Double botLine = new java.awt.geom.Line2D.Double(
         r.getX() - 1.0, r.getY() + r.getHeight(), r.getX() + r.getWidth() + 1.0, r.getY() + r.getHeight()
      );
      java.awt.geom.Line2D.Double rightLine = new java.awt.geom.Line2D.Double(
         r.getX() + r.getWidth(), r.getY() - 1.0, r.getX() + r.getWidth(), r.getY() + r.getHeight() + 1.0
      );
      IntersectionPoint<T> ip = null;
      if (y1 >= botLine.y1) {
         ip = toIP(getIntersectionPoint(l, botLine), target, IntersectionPoint.Dir.UP);
      }

      if (ip == null && x1 <= leftLine.x1) {
         ip = toIP(getIntersectionPoint(l, leftLine), target, IntersectionPoint.Dir.RIGHT);
      }

      if (ip == null && y1 <= topLine.y1) {
         ip = toIP(getIntersectionPoint(l, topLine), target, IntersectionPoint.Dir.DOWN);
      }

      if (ip == null && x1 >= rightLine.x1) {
         ip = toIP(getIntersectionPoint(l, rightLine), target, IntersectionPoint.Dir.LEFT);
      }

      if (checkInsideRect) {
         if (ip == null && y1 >= topLine.y1) {
            ip = toIP(getIntersectionPoint(l, topLine), target, IntersectionPoint.Dir.UP);
         }

         if (ip == null && x1 <= rightLine.x1) {
            ip = toIP(getIntersectionPoint(l, rightLine), target, IntersectionPoint.Dir.RIGHT);
         }

         if (ip == null && y1 <= botLine.y1) {
            ip = toIP(getIntersectionPoint(l, botLine), target, IntersectionPoint.Dir.DOWN);
         }

         if (ip == null && x1 >= leftLine.x1) {
            ip = toIP(getIntersectionPoint(l, leftLine), target, IntersectionPoint.Dir.LEFT);
         }
      }

      return ip;
   }

   private static <T> IntersectionPoint<T> toIP(Point2D point, T target, IntersectionPoint.Dir dir) {
      return point == null ? null : new IntersectionPoint<>(point.getX(), point.getY(), target, dir);
   }

   public static Point2D getIntersectionPoint(Line2D l1, Line2D l2) {
      return getIntersectionPoint(l1, l2, false);
   }

   public static Point2D getIntersectionPoint(Line2D l1, Line2D l2, boolean infiniteLines) {
      if (!infiniteLines && !l1.intersectsLine(l2)) {
         return null;
      } else {
         double x1 = l1.getX1();
         double y1 = l1.getY1();
         double x2 = l1.getX2();
         double y2 = l1.getY2();
         double x3 = l2.getX1();
         double y3 = l2.getY1();
         double x4 = l2.getX2();
         double y4 = l2.getY2();
         double l1deltaX = x1 - x2;
         double l1deltaY = y1 - y2;
         double l2deltaX = x3 - x4;
         double l2deltaY = y3 - y4;
         double d = l1deltaX * l2deltaY - l1deltaY * l2deltaX;
         if (d != 0.0) {
            double xi = ((x1 * y2 - y1 * x2) * l2deltaX - l1deltaX * (x3 * y4 - y3 * x4)) / d;
            double yi = ((x1 * y2 - y1 * x2) * l2deltaY - l1deltaY * (x3 * y4 - y3 * x4)) / d;
            return new Double(xi, yi);
         } else {
            return null;
         }
      }
   }

   public static double dot(Point2D p1, Point2D p2) {
      return p1.getX() * p2.getX() + p1.getY() * p2.getY();
   }

   public static float dot(Float p1, Float p2) {
      return p1.x * p2.x + p1.y * p2.y;
   }

   public static Point2D getClosestPointOnLine(Line2D line, Point2D point, boolean infiniteLine) {
      double dist = line.getP1().distance(line.getP2());
      if (dist == 0.0) {
         return line.getP1();
      }

      Point2D dir = new Double((line.getX2() - line.getX1()) / dist, (line.getY2() - line.getY1()) / dist);
      Point2D lhs = new Double(point.getX() - line.getX1(), point.getY() - line.getY1());
      double dotP = dot(lhs, dir);
      if (!infiniteLine) {
         dotP = limit(dotP, 0.0, dist);
      }

      return new Double(line.getX1() + dir.getX() * dotP, line.getY1() + dir.getY() * dotP);
   }

   public static Double getPerpendicularDir(double xDir, double yDir) {
      return new Double(-yDir, xDir);
   }

   public static Double getPerpendicularDir(Point2D dir) {
      return getPerpendicularDir(dir.getX(), dir.getY());
   }

   public static Float getPerpendicularDir(float xDir, float yDir) {
      return new Float(-yDir, xDir);
   }

   public static Float getPerpendicularDir(Float dir) {
      return getPerpendicularDir(dir.x, dir.y);
   }

   public static Double getPerpendicularPoint(double startX, double startY, float length, double xDir, double yDir) {
      Double perpDir = getPerpendicularDir(xDir, yDir);
      return new Double(startX - perpDir.x * length, startY - perpDir.y * length);
   }

   public static Double getPerpendicularPoint(double startX, double startY, float length, Double dir) {
      return getPerpendicularPoint(startX, startY, length, dir.x, dir.y);
   }

   public static Double getPerpendicularPoint(double startX, double startY, float length, Point2D dir) {
      return getPerpendicularPoint(startX, startY, length, dir.getX(), dir.getY());
   }

   public static Double getPerpendicularPoint(Double start, float length, double xDir, double yDir) {
      return getPerpendicularPoint(start.x, start.y, length, xDir, yDir);
   }

   public static Double getPerpendicularPoint(Point2D start, float length, double xDir, double yDir) {
      return getPerpendicularPoint(start.getX(), start.getY(), length, xDir, yDir);
   }

   public static Double getPerpendicularPoint(Double start, float length, Double dir) {
      return getPerpendicularPoint(start.x, start.y, length, dir.x, dir.y);
   }

   public static Double getPerpendicularPoint(Point2D start, float length, Double dir) {
      return getPerpendicularPoint(start.getX(), start.getY(), length, dir.x, dir.y);
   }

   public static Double getPerpendicularPoint(Double start, float length, Point2D dir) {
      return getPerpendicularPoint(start.x, start.y, length, dir.getX(), dir.getY());
   }

   public static Double getPerpendicularPoint(Point2D start, float length, Point2D dir) {
      return getPerpendicularPoint(start.getX(), start.getY(), length, dir.getX(), dir.getY());
   }

   public static Float getPerpendicularPoint(float startX, float startY, float length, float xDir, float yDir) {
      Float perpDir = getPerpendicularDir(xDir, yDir);
      return new Float(startX - perpDir.x * length, startY - perpDir.y * length);
   }

   public static Float getPerpendicularPoint(float startX, float startY, float length, Float dir) {
      return getPerpendicularPoint(startX, startY, length, dir.x, dir.y);
   }

   public static Float getPerpendicularPoint(Float start, float length, float xDir, float yDir) {
      return getPerpendicularPoint(start.x, start.y, length, xDir, yDir);
   }

   public static Float getPerpendicularPoint(Float start, float length, Float dir) {
      return getPerpendicularPoint(start.x, start.y, length, dir.x, dir.y);
   }

   public static Line2D getPerpendicularLine(Line2D line, float offset) {
      Double dir = normalize(line.getX1() - line.getX2(), line.getY1() - line.getY2());
      Double perpDir = getPerpendicularDir(dir.x, dir.y);
      return new java.awt.geom.Line2D.Double(
         line.getX1() + perpDir.x * offset, line.getY1() + perpDir.y * offset, line.getX2() + perpDir.x * offset, line.getY2() + perpDir.y * offset
      );
   }

   public static boolean getBit(long value, int bit) {
      return (value >> bit & 1L) == 1L;
   }

   public static long setBit(long value, int bit, boolean set) {
      return set ? value | 1L << bit : value & ~(1L << bit);
   }

   public static byte getByte(long value, int byteIndex) {
      return (byte)(value >> byteIndex * 8);
   }

   public static long setByte(long value, int byteIndex, byte set) {
      long cleanValue = 255L << byteIndex * 8;
      long setValue = (long)set << byteIndex * 8 & cleanValue;
      return value & ~cleanValue | setValue;
   }

   public static int setBit(int value, int bit, boolean set) {
      return set ? value | 1 << bit : value & ~(1 << bit);
   }

   public static byte getByte(int value, int byteIndex) {
      return (byte)(value >> byteIndex * 8);
   }

   public static int setByte(int value, int byteIndex, byte set) {
      int cleanValue = 255 << byteIndex * 8;
      int setValue = set << byteIndex * 8 & cleanValue;
      return value & ~cleanValue | setValue;
   }

   public static short setBit(short value, int bit, boolean set) {
      return set ? (short)(value | 1 << bit) : (short)(value & ~(1 << bit));
   }

   public static byte getByte(short value, int byteIndex) {
      return (byte)(value >> byteIndex * 8);
   }

   public static short setByte(short value, int byteIndex, byte set) {
      int cleanValue = 255 << byteIndex * 8;
      int setValue = set << byteIndex * 8 & cleanValue;
      return (short)(value & ~cleanValue | setValue);
   }

   public static byte setBit(byte value, int bit, boolean set) {
      return set ? (byte)(value | 1 << bit) : (byte)(value & ~(1 << bit));
   }

   public static double getSuccessAfterRuns(double chance, long runs) {
      return 1.0 - Math.pow(1.0 - chance, runs);
   }

   public static double getRunsForSuccess(double chance, double targetChance) {
      return Math.log(1.0 - targetChance) / Math.log(1.0 - chance);
   }

   public static double getChanceAtRuns(double targetChance, double runs) {
      double d = Math.pow(1.0 - targetChance, 1.0 / runs);
      return 1.0 - d;
   }

   public static double getAverageSuccessRuns(double runs) {
      return getChanceAtRuns(0.5, runs * 0.705);
   }

   public static double getAverageRunsForSuccess(double chance, double targetChance) {
      return getRunsForSuccess(chance, targetChance) * 1.418;
   }

   public static double getAverageRunsForSuccess(double chance) {
      return getAverageRunsForSuccess(chance, 0.5);
   }

   public static boolean isPowerOf2(long value) {
      return (value & -value) == value;
   }

   public static long getBitMask(int bits) {
      return bits >= 64 ? -1L : (1L << bits) - 1L;
   }

   public static long getUniqueLongKey(int x, int y) {
      return (y & LONG_KEY_BIT_MASK) << 32 | x & LONG_KEY_BIT_MASK;
   }

   public static int getXFromUniqueLongKey(long key) {
      return (int)key;
   }

   public static int getYFromUniqueLongKey(long key) {
      return (int)(key >> 32);
   }

   public static int getUniqueIntKey(int x, int y) {
      return (y & 65535) << 16 | x & 65535;
   }

   public static int getXFromUniqueIntKey(int key) {
      return key & 65535;
   }

   public static int getYFromUniqueIntKey(int key) {
      return key >> 16;
   }

   public static int divideByPowerOf2(int value, int power) {
      return value >= 0 ? value >> power : value + (1 << power) - 1 >> power;
   }

   public static long divideByPowerOf2(long value, int power) {
      return value >= 0L ? value >> power : value + (1L << power) - 1L >> power;
   }

   public static int divideByPowerOf2RoundedDown(int value, int power) {
      return value >> power;
   }

   public static long divideByPowerOf2RoundedDown(long value, int power) {
      return value >> power;
   }

   public static int divideByPowerOf2RoundedUp(int value, int power) {
      return (value >> power) + ((value & (1 << power) - 1) == 0 ? 0 : 1);
   }

   public static long divideByPowerOf2RoundedUp(long value, int power) {
      return (value >> power) + ((value & (1L << power) - 1L) == 0L ? 0 : 1);
   }

   public static int multiplyByPowerOf2(int value, int power) {
      return value << power;
   }

   public static long multiplyByPowerOf2(long value, int power) {
      return value << power;
   }

   public static int getTileCoordinate(int levelCoordinate) {
      return levelCoordinate >> 5;
   }

   public static int getTileCoordinate(float levelCoordinate) {
      return levelCoordinate < 0.0F ? getTileCoordinate((int)Math.floor(levelCoordinate)) : getTileCoordinate((int)levelCoordinate);
   }

   public static int getTileCoordinate(double levelCoordinate) {
      return levelCoordinate < 0.0 ? getTileCoordinate((int)Math.floor(levelCoordinate)) : getTileCoordinate((int)levelCoordinate);
   }

   public static float getTileFloatCoordinate(float levelCoordinate) {
      return levelCoordinate / 32.0F;
   }

   public static int getLevelCoordinate(int tileCoordinate) {
      return tileCoordinate << 5;
   }

   public static int getRegionCoordByTile(int tileCoordinate) {
      return divideByPowerOf2RoundedDown(tileCoordinate, 4);
   }

   public static int getTileCoordByRegion(int regionCoordinate) {
      return multiplyByPowerOf2(regionCoordinate, 4);
   }

   public static byte[] toByteArray(int[] ints) {
      byte[] bytes = new byte[ints.length << 2];

      for (int i = 0; i < ints.length; i++) {
         int value = ints[i];
         bytes[i << 2] = (byte)(value >> 24);
         bytes[(i << 2) + 1] = (byte)(value >> 16);
         bytes[(i << 2) + 2] = (byte)(value >> 8);
         bytes[(i << 2) + 3] = (byte)value;
      }

      return bytes;
   }

   public static int[] toIntArray(byte[] bytes) {
      int[] ints = new int[bytes.length >> 2];

      for (int i = 0; i < ints.length; i++) {
         byte b1 = bytes[i << 2];
         byte b2 = bytes[(i << 2) + 1];
         byte b3 = bytes[(i << 2) + 2];
         byte b4 = bytes[(i << 2) + 3];
         ints[i] = (b1 & 255) << 24 | (b2 & 255) << 16 | (b3 & 255) << 8 | b4 & 255;
      }

      return ints;
   }

   public static byte[] toByteArray(short[] shorts) {
      byte[] bytes = new byte[shorts.length << 1];

      for (int i = 0; i < shorts.length; i++) {
         short value = shorts[i];
         bytes[i << 1] = (byte)(value >> 8);
         bytes[(i << 1) + 1] = (byte)value;
      }

      return bytes;
   }

   public static short[] toShortArray(byte[] bytes) {
      short[] shorts = new short[bytes.length >> 1];

      for (int i = 0; i < shorts.length; i++) {
         byte b1 = bytes[i << 1];
         byte b2 = bytes[(i << 1) + 1];
         shorts[i] = (short)((b1 & 255) << 8 | b2 & 0xFF);
      }

      return shorts;
   }

   public static byte[] toByteArray(long[] longs) {
      byte[] bytes = new byte[longs.length << 3];

      for (int i = 0; i < longs.length; i++) {
         long value = longs[i];
         bytes[i << 3] = (byte)(value >> 56);
         bytes[(i << 3) + 1] = (byte)(value >> 48);
         bytes[(i << 3) + 2] = (byte)(value >> 40);
         bytes[(i << 3) + 3] = (byte)(value >> 32);
         bytes[(i << 3) + 4] = (byte)(value >> 24);
         bytes[(i << 3) + 5] = (byte)(value >> 16);
         bytes[(i << 3) + 6] = (byte)(value >> 8);
         bytes[(i << 3) + 7] = (byte)value;
      }

      return bytes;
   }

   public static long[] toLongArray(byte[] bytes) {
      long[] longs = new long[bytes.length >> 3];

      for (int i = 0; i < longs.length; i++) {
         byte b1 = bytes[i << 3];
         byte b2 = bytes[(i << 3) + 1];
         byte b3 = bytes[(i << 3) + 2];
         byte b4 = bytes[(i << 3) + 3];
         byte b5 = bytes[(i << 3) + 4];
         byte b6 = bytes[(i << 3) + 5];
         byte b7 = bytes[(i << 3) + 6];
         byte b8 = bytes[(i << 3) + 7];
         longs[i] = (long)(b1 & 0xFF) << 56
            | (long)(b2 & 0xFF) << 48
            | (long)(b3 & 0xFF) << 40
            | (long)(b4 & 0xFF) << 32
            | (long)(b5 & 0xFF) << 24
            | (long)(b6 & 0xFF) << 16
            | (long)(b7 & 0xFF) << 8
            | b8 & 0xFF;
      }

      return longs;
   }

   public static byte[] toByteArray(float[] floats) {
      byte[] bytes = new byte[floats.length << 2];

      for (int i = 0; i < floats.length; i++) {
         int value = java.lang.Float.floatToIntBits(floats[i]);
         bytes[i << 2] = (byte)(value >> 24);
         bytes[(i << 2) + 1] = (byte)(value >> 16);
         bytes[(i << 2) + 2] = (byte)(value >> 8);
         bytes[(i << 2) + 3] = (byte)value;
      }

      return bytes;
   }

   public static float[] toFloatArray(byte[] bytes) {
      float[] floats = new float[bytes.length >> 2];

      for (int i = 0; i < floats.length; i++) {
         byte b1 = bytes[i << 2];
         byte b2 = bytes[(i << 2) + 1];
         byte b3 = bytes[(i << 2) + 2];
         byte b4 = bytes[(i << 2) + 3];
         int value = (b1 & 255) << 24 | (b2 & 255) << 16 | (b3 & 255) << 8 | b4 & 255;
         floats[i] = java.lang.Float.intBitsToFloat(value);
      }

      return floats;
   }

   public static byte[] toByteArray(double[] doubles) {
      byte[] bytes = new byte[doubles.length << 3];

      for (int i = 0; i < doubles.length; i++) {
         long value = java.lang.Double.doubleToLongBits(doubles[i]);
         bytes[i << 3] = (byte)(value >> 56);
         bytes[(i << 3) + 1] = (byte)(value >> 48);
         bytes[(i << 3) + 2] = (byte)(value >> 40);
         bytes[(i << 3) + 3] = (byte)(value >> 32);
         bytes[(i << 3) + 4] = (byte)(value >> 24);
         bytes[(i << 3) + 5] = (byte)(value >> 16);
         bytes[(i << 3) + 6] = (byte)(value >> 8);
         bytes[(i << 3) + 7] = (byte)value;
      }

      return bytes;
   }

   public static double[] toDoubleArray(byte[] bytes) {
      double[] doubles = new double[bytes.length >> 3];

      for (int i = 0; i < doubles.length; i++) {
         byte b1 = bytes[i << 3];
         byte b2 = bytes[(i << 3) + 1];
         byte b3 = bytes[(i << 3) + 2];
         byte b4 = bytes[(i << 3) + 3];
         byte b5 = bytes[(i << 3) + 4];
         byte b6 = bytes[(i << 3) + 5];
         byte b7 = bytes[(i << 3) + 6];
         byte b8 = bytes[(i << 3) + 7];
         long value = (long)(b1 & 0xFF) << 56
            | (long)(b2 & 0xFF) << 48
            | (long)(b3 & 0xFF) << 40
            | (long)(b4 & 0xFF) << 32
            | (long)(b5 & 0xFF) << 24
            | (long)(b6 & 0xFF) << 16
            | (long)(b7 & 0xFF) << 8
            | b8 & 0xFF;
         doubles[i] = java.lang.Double.longBitsToDouble(value);
      }

      return doubles;
   }

   public static byte[] toByteArray(boolean[] booleans) {
      byte[] bytes = new byte[4 + (booleans.length + 7 >> 3)];
      bytes[0] = (byte)(booleans.length >> 24 & 0xFF);
      bytes[1] = (byte)(booleans.length >> 16 & 0xFF);
      bytes[2] = (byte)(booleans.length >> 8 & 0xFF);
      bytes[3] = (byte)(booleans.length & 0xFF);

      for (int i = 0; i < booleans.length; i++) {
         if (booleans[i]) {
            bytes[4 + (i >> 3)] = (byte)(bytes[4 + (i >> 3)] | 1 << (i & 7));
         }
      }

      return bytes;
   }

   public static boolean[] toBooleanArray(byte[] bytes) {
      byte b1 = bytes[0];
      byte b2 = bytes[1];
      byte b3 = bytes[2];
      byte b4 = bytes[3];
      int length = (b1 & 255) << 24 | (b2 & 255) << 16 | (b3 & 255) << 8 | b4 & 255;
      boolean[] booleans = new boolean[length];

      for (int i = 0; i < booleans.length; i++) {
         booleans[i] = (bytes[4 + (i >> 3)] & 1 << (i & 7)) != 0;
      }

      return booleans;
   }

   static {
      for (int i = 0; i < 3600; i++) {
         float angle = 0.1F * i;
         cos[i] = (float)Math.cos(Math.toRadians(angle));
         sin[i] = (float)Math.sin(Math.toRadians(angle));
      }

      diagonalDistance = Math.sqrt(2.0);
      LONG_KEY_BIT_MASK = getBitMask(32);
   }
}
