package necesse.entity.projectile.pathProjectile;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.geom.Path2D.Double;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameMath;
import necesse.engine.util.tween.Easings;
import necesse.entity.levelEvent.ShowSurvivorWhipAttackEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.trails.Trail;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.Level;

public class SurvivorWhipProjectile extends PathProjectile {
   public Float dir;
   public boolean isUpgraded;
   public float frontAttackRange;
   public float behindAttackRange;

   public SurvivorWhipProjectile() {
   }

   public SurvivorWhipProjectile(
      Mob owner, int targetX, int targetY, int animTime, float frontAttackRange, float behindAttackRange, GameDamage damage, int knockback
   ) {
      this.x = owner.x;
      this.y = owner.y;
      this.dir = GameMath.normalize(targetX - owner.x, targetY - owner.y);
      int distance = 1000;
      this.speed = Projectile.getTravelSpeedForMillis(animTime, distance);
      this.setDistance(distance);
      this.frontAttackRange = frontAttackRange;
      this.behindAttackRange = behindAttackRange;
      this.setDamage(damage);
      this.knockback = knockback;
      this.setOwner(owner);
   }

   public SurvivorWhipProjectile(
      Mob owner, int targetX, int targetY, int animTime, float frontAttackRange, float behindAttackRange, GameDamage damage, int knockback, boolean isUpgraded
   ) {
      this(owner, targetX, targetY, animTime, frontAttackRange, behindAttackRange, damage, knockback);
      this.isUpgraded = isUpgraded;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.frontAttackRange);
      writer.putNextFloat(this.behindAttackRange);
      writer.putNextFloat(this.dir.x);
      writer.putNextFloat(this.dir.y);
      writer.putNextBoolean(this.isUpgraded);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.frontAttackRange = reader.getNextFloat();
      this.behindAttackRange = reader.getNextFloat();
      float dirX = reader.getNextFloat();
      float dirY = reader.getNextFloat();
      this.dir = new Float(dirX, dirY);
      this.isUpgraded = reader.getNextBoolean();
   }

   @Override
   public void init() {
      super.init();
      this.isSolid = false;
      this.maxMovePerTick = 12;
      this.height = 0.0F;
      this.piercing = 1000;
      this.setWidth(this.isUpgraded ? 40.0F : 20.0F);
      if (!this.isServer()) {
         this.getLevel().entityManager.events.addHidden(new ShowSurvivorWhipAttackEvent(this, 100));
      }
   }

   @Override
   public Trail getTrail() {
      return null;
   }

   @Override
   public Float getPosition(double dist) {
      Mob owner = this.getOwner();
      if (owner != null && this.dir != null) {
         float currentAttackProgress = this.traveledDistance / this.distance;
         return getWhipPositionFinal(owner.x, owner.y, this.dir, this.frontAttackRange, this.behindAttackRange, currentAttackProgress);
      } else {
         return new Float(this.x, this.y);
      }
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
   }

   public static Float getWhipPositionFloat(float progress) {
      float startX = progress;
      float firstPart = 0.66F;
      if (progress <= firstPart) {
         progress *= 1.0F / firstPart;
         progress = Easings.CubicIn.ease(progress);
         return new Float(progress, Easings.QuartIn.ease(progress) / 2.0F - startX);
      } else {
         progress = (progress - firstPart) * (1.0F / (1.0F - firstPart));
         progress = Easings.CubicOut.ease(progress);
         return new Float(1.0F - progress / 2.0F, 1.0F - Easings.QuartIn.ease(1.0F - progress) / 2.0F - startX);
      }
   }

   public static SurvivorWhipProjectile.WhipPosition getWhipPositionFinal(
      float startX, float startY, Float attackDir, float frontAttackRange, float behindAttackRange, float progress
   ) {
      boolean isSecondPart = false;
      Float originalAttackDir = attackDir;
      float originalProgress = progress;
      float attackRange = frontAttackRange;
      if (progress < 0.5) {
         progress = 1.0F - progress * 2.0F;
      } else {
         progress = 1.0F - (progress - 0.5F) * 2.0F;
         attackDir = new Float(-attackDir.x, -attackDir.y);
         attackRange = behindAttackRange;
         isSecondPart = true;
      }

      Float currentPos = getWhipPositionFloat(progress);
      float forwardRange = GameMath.lerp(progress, attackRange, Math.max(1.0F, attackRange - attackRange / 4.0F));
      float perpRange = GameMath.lerp(progress, Math.max(1.0F, attackRange - attackRange / 5.0F), attackRange) / 3.0F;
      float currentX = startX + currentPos.x * attackDir.x * forwardRange;
      float currentY = startY + currentPos.x * attackDir.y * forwardRange;
      Float perpDir = GameMath.getPerpendicularDir(attackDir);
      currentX += currentPos.y * perpDir.x * perpRange * (originalAttackDir.x < 0.0F ? 1 : -1);
      currentY += currentPos.y * perpDir.y * perpRange * (originalAttackDir.x < 0.0F ? 1 : -1);
      return new SurvivorWhipProjectile.WhipPosition(currentX, currentY, attackDir, originalAttackDir, progress, originalProgress, isSecondPart);
   }

   public static void drawWhipPositionFloat(float percent, int steps, float posX, float posY, float size, Color color) {
      Path2D path = new Double();
      path.moveTo(0.0, size);

      for (int i = 0; i <= steps; i++) {
         Float p = getWhipPositionFloat((float)i / steps);
         double x = p.x;
         double y = 1.0 - p.y;
         path.lineTo(x * size, y * size);
      }

      for (int i = steps; i >= 0; i--) {
         Float p = getWhipPositionFloat((float)i / steps);
         double x = p.x;
         double y = 1.0 - p.y;
         path.lineTo(x * size, y * size);
      }

      Float p = getWhipPositionFloat(percent);
      Renderer.drawShape(path, posX, posY, false, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
      Renderer.drawCircle((int)(posX + p.x * size), (int)(posY + (1.0F - p.y) * size), GameMath.max(2, (int)size / 20), 12, color.darker(), false);
      Renderer.drawLine((int)posX, (int)(posY + size), (int)(posX + size), (int)(posY + size), color);
      Renderer.drawLine((int)posX, (int)posY, (int)posX, (int)(posY + size), color);
      FontManager.bit.drawString(posX, posY + size + 5.0F, "Value in:    " + String.format("%.2f", percent), new FontOptions(10));
      FontManager.bit
         .drawString(posX, posY + size + 15.0F, "Value out:  " + String.format("%.2f", p.x) + "x" + String.format("%.2f", p.y), new FontOptions(10));
   }

   public static class WhipPosition extends Float {
      public Float currentAttackDir;
      public Float originalAttackDir;
      public float currentAttackProgress;
      public float originalAttackProgress;
      public boolean isSecondPart;

      public WhipPosition(
         float x, float y, Float currentAttackDir, Float originalAttackDir, float currentAttackProgress, float originalAttackProgress, boolean isSecondPart
      ) {
         super(x, y);
         this.currentAttackDir = currentAttackDir;
         this.originalAttackDir = originalAttackDir;
         this.currentAttackProgress = currentAttackProgress;
         this.originalAttackProgress = originalAttackProgress;
         this.isSecondPart = isSecondPart;
      }
   }
}
