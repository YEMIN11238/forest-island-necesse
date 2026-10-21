package necesse.level.maps.hudManager.floatText.thoughtBubble;

import java.awt.Rectangle;
import java.util.List;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.hudManager.HudManager;
import necesse.level.maps.hudManager.floatText.FloatText;

public abstract class ThoughtBubble extends FloatText {
   public static int FADE_IN_ANIMATION_TIME = 100;
   private final Mob mob;
   private final int stayTime;
   private long spawnTime;

   public ThoughtBubble(Mob mob, int stayTime) {
      this.mob = mob;
      this.stayTime = stayTime;
   }

   @Override
   public void init(HudManager manager) {
      super.init(manager);
      manager.removeElements(element -> {
         if (element != this && element instanceof ThoughtBubble) {
            ThoughtBubble other = (ThoughtBubble)element;
            return this.mob.getUniqueID() == other.mob.getUniqueID();
         } else {
            return false;
         }
      });
      this.spawnTime = this.getTime();
   }

   @Override
   public int getX() {
      Rectangle selectBox = this.mob.getSelectBox(this.mob.getDrawX(), this.mob.getDrawY());
      return selectBox.x + selectBox.width / 2 - 24;
   }

   @Override
   public int getY() {
      Rectangle selectBox = this.mob.getSelectBox(this.mob.getDrawX(), this.mob.getDrawY());
      return selectBox.y - (this.mob.isHealthBarVisible() ? 5 : 0) - 48;
   }

   @Override
   public int getWidth() {
      return 48;
   }

   @Override
   public int getHeight() {
      return 48;
   }

   @Override
   public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
      if (!this.isRemoved() && !this.mob.removed()) {
         if (this.getTime() >= this.spawnTime + this.stayTime) {
            this.remove();
         } else if (camera.getBounds().intersects(this.getCollision())) {
            int drawX = camera.getDrawX(this.getX());
            int drawY = camera.getDrawY(this.getY());
            int timeSinceSpawned = (int)(this.getTime() - this.spawnTime);
            float fadeInProgress = GameMath.limit((float)timeSinceSpawned / FADE_IN_ANIMATION_TIME, 0.0F, 1.0F);
            GameTexture background = Settings.UI.settler_thought_bubble;
            int fadeInWidth = (int)(background.getWidth() * fadeInProgress);
            int fadeInHeight = (int)(background.getHeight() * fadeInProgress);
            drawX += (background.getWidth() - fadeInWidth) / 2;
            drawY += background.getHeight() - fadeInHeight;
            final TextureDrawOptionsEnd drawOptions = background.initDraw().size(fadeInWidth, fadeInHeight).pos(drawX, drawY);
            int fullContentSize = this.getThoughtContentSize();
            int currentContentSize = (int)(fullContentSize * fadeInProgress);
            final DrawOptions thoughtContent;
            if (currentContentSize > 0) {
               int drawOffset = (int)(16.0F * fadeInProgress - (fullContentSize - 16) * fadeInProgress / 2.0F);
               thoughtContent = this.getThoughtContent(drawX + drawOffset, drawY + drawOffset, currentContentSize, fadeInProgress, perspective);
            } else {
               thoughtContent = null;
            }

            list.add(new SortedDrawable() {
               @Override
               public int getPriority() {
                  return 1000;
               }

               @Override
               public void draw(TickManager tickManager) {
                  drawOptions.draw();
                  if (thoughtContent != null) {
                     thoughtContent.draw();
                  }
               }
            });
         }
      }
   }

   public int getThoughtContentSize() {
      return 16;
   }

   public abstract DrawOptions getThoughtContent(int var1, int var2, int var3, float var4, PlayerMob var5);
}
