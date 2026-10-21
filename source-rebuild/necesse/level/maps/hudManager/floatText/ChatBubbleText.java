package necesse.level.maps.hudManager.floatText;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.LinkedList;
import java.util.List;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.ChatBubbleDrawOptions;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.forms.components.chat.ChatMessage;
import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.hudManager.HudManager;

public class ChatBubbleText extends FloatText {
   static int maxWidth = 200;
   private final FontOptions fontOptions = new FontOptions(16);
   private int x;
   private int y;
   private Mob mob;
   private final String message;
   private FairTypeDrawOptions drawOptions;
   private int width;
   private int height;
   private long removeTime;

   public ChatBubbleText(int x, int y, String message) {
      this.x = x;
      this.y = y;
      this.message = message;
      this.mob = null;
   }

   public ChatBubbleText(Mob mob, String message) {
      this(mob.getX(), mob.getY(), message);
      this.mob = mob;
      this.x = mob.getX();
      this.y = mob.getY() - 50;
   }

   public void setMob(Mob mob) {
      this.mob = mob;
   }

   @Override
   public void init(HudManager manager) {
      super.init(manager);
      this.drawOptions = new FairType()
         .append(this.fontOptions, this.message)
         .applyParsers(ChatMessage.getParsers(this.fontOptions))
         .getDrawOptions(FairType.TextAlign.LEFT, maxWidth, true, true);
      Rectangle boundingBox = this.drawOptions.getBoundingBox();
      this.width = Math.max(32, boundingBox.x + boundingBox.width);
      this.height = boundingBox.y + boundingBox.height + 30;
      long stayTime = Math.max((boundingBox.width * this.drawOptions.getLineCount() + 200) / 100, 3) * 1000L;
      this.removeTime = this.getTime() + stayTime;
      if (this.mob != null) {
         manager.removeElements(element -> {
            if (element != this && element instanceof ChatBubbleText) {
               ChatBubbleText other = (ChatBubbleText)element;
               return other.mob == this.mob;
            } else {
               return false;
            }
         });
      }
   }

   @Override
   public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
      if (!this.isRemoved()) {
         if (camera.getBounds().intersects(this.getCollision())) {
            if (this.getTime() >= this.removeTime) {
               this.remove();
            } else {
               int drawXOffset = Math.min(this.width - 30, 20);
               if (this.mob != null) {
                  Point drawPos = this.mob.getDrawPos();
                  this.x = drawPos.x - drawXOffset;
                  this.y = drawPos.y - 40;
               }

               int drawX = camera.getDrawX(this.x) - 32;
               int drawY = camera.getDrawY(this.y);
               final List<DrawOptions> options = new LinkedList<>();
               Rectangle boundingBox = this.drawOptions.getBoundingBox();
               int textHeight = boundingBox.y + boundingBox.height;
               int textOffset = boundingBox.width < this.width - 10 ? this.width / 2 - boundingBox.width / 2 - 2 : 0;
               options.add(
                  new ChatBubbleDrawOptions(this.width + 16, textHeight + 20).setBotIndicator(24 + drawXOffset, false).pos(drawX - 8, drawY - this.height - 5)
               );
               options.add(() -> this.drawOptions.draw(drawX + textOffset + 2, drawY - this.height + 4, Settings.UI.chatBubbleActiveTextColor));
               list.add(new SortedDrawable() {
                  @Override
                  public int getPriority() {
                     return 1000;
                  }

                  @Override
                  public void draw(TickManager tickManager) {
                     options.forEach(DrawOptions::draw);
                  }
               });
            }
         }
      }
   }

   @Override
   public int getX() {
      return this.x;
   }

   @Override
   public int getY() {
      return this.y;
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @Override
   public Rectangle getCollision() {
      return new Rectangle(this.x - 38, this.y - this.height - 2, this.width + 16, this.height);
   }
}
