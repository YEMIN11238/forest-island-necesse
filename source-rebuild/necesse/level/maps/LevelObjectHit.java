package necesse.level.maps;

import java.awt.Point;
import java.awt.Rectangle;
import necesse.level.gameObject.GameObject;

public class LevelObjectHit extends Rectangle {
   public final Level level;
   public final int tileX;
   public final int tileY;

   public LevelObjectHit(Rectangle r, Level level, int tileX, int tileY) {
      super(r);
      this.level = level;
      this.tileX = tileX;
      this.tileY = tileY;
   }

   public LevelObjectHit(Rectangle r, Level level) {
      this(r, level, Integer.MIN_VALUE, Integer.MIN_VALUE);
   }

   public Point getPoint() {
      return new Point(this.tileX, this.tileY);
   }

   public GameObject getObject() {
      return this.level.getObject(this.tileX, this.tileY);
   }

   public LevelObject getLevelObject() {
      return this.level.getLevelObject(this.tileX, this.tileY);
   }

   public boolean invalidPos() {
      return this.tileX == Integer.MIN_VALUE || this.tileY == Integer.MIN_VALUE;
   }
}
