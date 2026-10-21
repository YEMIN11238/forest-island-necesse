package necesse.entity.trails;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.entity.mobs.Mob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.Level;

public class RopeTrail extends Trail {
   private final Mob ropedMob;
   private final Mob roperMob;

   public RopeTrail(Level level, Mob ropedMob, Mob roperMob, float ropedHeightOffset, float roperHeightOffset, Color color) {
      super(new TrailVector(ropedMob.x, ropedMob.y, ropedMob.x - roperMob.x, ropedMob.y - roperMob.y, 20.0F, 0.0F), level, color, 5000);
      this.ropedMob = ropedMob;
      this.roperMob = roperMob;
      this.sprite = new GameSprite(GameResources.chains, 5, 0, 32);
      this.update(ropedHeightOffset, roperHeightOffset);
   }

   public RopeTrail(Level level, Mob ropedMob, Mob roperMob, float ropedHeightOffset, float roperHeightOffset) {
      this(level, ropedMob, roperMob, ropedHeightOffset, roperHeightOffset, new Color(71, 39, 25));
   }

   public synchronized void update(float ropedHeightOffset, float roperHeightOffset) {
      int startX = this.ropedMob.getDrawX();
      int startY = this.ropedMob.getDrawY();
      float startHeight = 0.0F;
      startHeight += ropedHeightOffset;
      float endHeight = 0.0F;
      int endX;
      int endY;
      switch (this.roperMob.getDir()) {
         case 0:
            endX = this.roperMob.getDrawX() + 4;
            endY = this.roperMob.getDrawY() + 6;
            break;
         case 1:
            endX = this.roperMob.getDrawX() + 2;
            endY = this.roperMob.getDrawY() + 12;
            break;
         case 2:
            endX = this.roperMob.getDrawX() - 4;
            endY = this.roperMob.getDrawY() + 12;
            break;
         case 3:
            endX = this.roperMob.getDrawX() - 2;
            endY = this.roperMob.getDrawY() + 12;
            break;
         default:
            endX = this.roperMob.getDrawX();
            endY = this.roperMob.getDrawY();
      }

      endHeight += roperHeightOffset;
      startHeight -= this.ropedMob.getCurrentAttackDrawYOffset();
      endHeight -= this.roperMob.getCurrentAttackDrawYOffset();
      int yOffset = 16;
      startY += yOffset;
      endY += yOffset;
      startHeight += yOffset;
      endHeight += yOffset;
      this.reset(new TrailVector(startX, startY, endX - startX, endY - startY, this.thickness, startHeight));
      float smooth = (float)new Float(startX, startY).distance(endX, endY);
      float midX = (startX - endX) / 2.0F + endX;
      float midY = (startY - endY) / 2.0F + endY;
      float midHeight = (startHeight - endHeight) / 2.0F + endHeight - smooth / 10.0F;
      this.addPoints(
         3,
         new TrailVector(midX, midY, midX - startX, midY - startY, this.thickness, midHeight),
         new TrailVector(endX, endY, endX - midX, endY - midY, this.thickness, endHeight)
      );
   }

   @Override
   protected synchronized DrawOptions getDrawSection(TrailDrawSection s, GameCamera camera) {
      return s.getSpriteTrailsDraw(this.sprite, camera, TrailDrawSection.lightColorSetter(this.level, this.col));
   }

   @Override
   protected synchronized DrawOptions getDrawNextSection(TrailDrawSection s, TrailPointList list, float alpha, GameCamera camera) {
      return TrailDrawSection.getSpriteTrailsDraw(this.sprite, list, 0, list.size() - 1, camera, TrailDrawSection.lightColorSetter(this.level, this.col));
   }
}
