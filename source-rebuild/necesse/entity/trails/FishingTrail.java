package necesse.entity.trails;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.entity.Entity;
import necesse.entity.mobs.Mob;
import necesse.entity.projectile.FishingHookProjectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.inventory.item.placeableItem.fishingRodItem.FishingRodItem;
import necesse.level.maps.Level;

public class FishingTrail extends Trail {
   private Entity hook;
   private boolean isHookProjectile;
   private Mob mob;
   private FishingRodItem fishingRod;

   public FishingTrail(Mob owner, Level level, Entity hook, FishingRodItem fishingRod) {
      super(new TrailVector(owner.x, owner.y, owner.x - hook.x, owner.y - hook.y, 16.0F, 0.0F), level, new Color(255, 255, 255), 5000);
      this.mob = owner;
      this.hook = hook;
      this.fishingRod = fishingRod;
      this.sprite = fishingRod.getTrailSprite();
      this.update();
   }

   public FishingTrail(Mob owner, Level level, FishingHookProjectile hook, FishingRodItem fishingRod) {
      this(owner, level, (Entity)hook, fishingRod);
      this.isHookProjectile = true;
   }

   public synchronized void update() {
      Point tipPos = this.fishingRod.getTipPos(this.mob);
      int tipHeight = this.fishingRod.getTipHeight(this.mob);
      float startX = tipPos.x;
      float startY = tipPos.y;
      float endX = this.hook.x;
      float endY = this.hook.y;
      float hookHeight = 0.0F;
      if (this.isHookProjectile) {
         hookHeight = ((FishingHookProjectile)this.hook).getHeight();
      }

      this.reset(new TrailVector(startX, startY, startX - endX, startY - endY, this.thickness, tipHeight));
      float smooth = (float)new Float(startX, startY).distance(endX, endY);
      float midX = (startX - endX) / 2.0F + endX;
      float midY = (startY - endY) / 2.0F + endY;
      float midHeight = (tipHeight - hookHeight) / 2.0F + hookHeight - smooth / 10.0F;
      this.addPoints(
         3,
         new TrailVector(midX, midY, startX - midX, startY - midY, this.thickness, midHeight),
         new TrailVector(endX, endY, endX - midX, endY - midY, this.thickness, hookHeight)
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
