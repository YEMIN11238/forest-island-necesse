package necesse.gfx.forms.components.chat;

import java.awt.Rectangle;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;

public class FormChatLog extends FormContentBox implements ChatMessageListener {
   public final ChatMessageList chat;
   private boolean active;
   private final LinkedList<FormChatLog.ChatMessageComponent> log = new LinkedList<>();
   private int nextY = 0;

   public FormChatLog(int x, int y, int width, int height, ChatMessageList chat) {
      super(x, y, width, height);
      this.chat = chat;

      for (ChatMessage m : chat) {
         this.addMessage(m);
      }

      this.drawVerticalOnLeft = true;
      this.shouldLimitDrawArea = true;
      this.hitboxFullSize = false;
   }

   @Override
   protected void init() {
      super.init();
      this.chat.addListener(this);
   }

   @Override
   public void onNewMessage(ChatMessage message) {
      this.addMessage(message);
   }

   @Override
   public void onRemoveMessage(ChatMessage message) {
      this.removeMessage(message);
   }

   @Override
   public boolean hasScrollbarX() {
      return false;
   }

   @Override
   public boolean hasScrollbarY() {
      return this.active && super.hasScrollbarY();
   }

   public synchronized void addMessage(ChatMessage message) {
      FormChatLog.ChatMessageComponent newComponent = this.addComponent(new FormChatLog.ChatMessageComponent(0, this.nextY, message));
      this.log.addLast(newComponent);
      if (this.log.size() > 250) {
         this.removeComponent(this.log.removeFirst());
      }

      this.nextY = this.nextY + newComponent.getBoundingBox().height;
      Rectangle newContent = this.getContentBoxToFitComponents();
      newContent.x = 0;
      newContent.width = this.getWidth();
      if (newContent.height < this.getHeight()) {
         newContent.y = newContent.height - this.getHeight();
         newContent.height = this.getHeight();
      }

      this.setContentBox(newContent);
      this.scrollToFitForced(newComponent.getBoundingBox());
   }

   public synchronized void removeMessage(ChatMessage message) {
      ListIterator<FormChatLog.ChatMessageComponent> li = this.log.listIterator();
      int moveUp = -1;

      while (li.hasNext()) {
         FormChatLog.ChatMessageComponent next = li.next();
         if (moveUp != -1) {
            next.setPosition(next.getX(), next.getY() - moveUp);
         } else if (next.message == message) {
            this.removeComponent(next);
            li.remove();
            moveUp = next.getBoundingBox().height;
            this.nextY -= moveUp;
         }
      }

      if (moveUp != -1) {
         Rectangle newContent = this.getContentBoxToFitComponents();
         newContent.x = 0;
         newContent.width = this.getWidth();
         if (newContent.height < this.getHeight()) {
            newContent.y = newContent.height - this.getHeight();
            newContent.height = this.getHeight();
         }

         this.setContentBox(newContent);
      }
   }

   public void refreshBoundingBoxes() {
      this.nextY = 0;

      for (FormChatLog.ChatMessageComponent msg : this.log) {
         msg.setPosition(0, this.nextY);
         this.nextY = this.nextY + msg.getBoundingBox().height;
      }

      Rectangle newContent = this.getContentBoxToFitComponents();
      newContent.x = 0;
      newContent.width = this.getWidth();
      if (newContent.height < this.getHeight()) {
         newContent.y = newContent.height - this.getHeight();
         newContent.height = this.getHeight();
      }

      this.setContentBox(newContent);
      if (!this.log.isEmpty()) {
         this.scrollToFitForced(this.log.getLast().getBoundingBox());
      } else {
         this.scrollY(0);
      }
   }

   public void setActive(boolean active) {
      this.active = active;
   }

   @Override
   public boolean isMouseOver(InputEvent event) {
      return false;
   }

   @Override
   public void dispose() {
      super.dispose();
      this.chat.removeListener(this);
   }

   public boolean isActive() {
      return this.active;
   }

   protected class ChatMessageComponent extends FormComponent implements FormPositionContainer {
      private FormPosition position;
      public final ChatMessage message;

      public ChatMessageComponent(int x, int y, ChatMessage message) {
         this.position = new FormFixedPosition(x, y);
         this.message = message;
      }

      @Override
      public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
         this.message.drawOptions.get().handleInputEvent(this.getX(), this.getY(), event);
      }

      @Override
      public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      }

      @Override
      public void addNextControllerFocus(
         List<ControllerFocus> list, int currentXOffset, int currentYOffset, ControllerNavigationHandler customNavigationHandler, Rectangle area, boolean draw
      ) {
      }

      @Override
      public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
         this.message.drawOptions.get().draw(this.getX(), this.getY());
      }

      @Override
      public boolean shouldDraw() {
         return FormChatLog.this.active || this.message.shouldDraw();
      }

      @Override
      public List<Rectangle> getHitboxes() {
         return singleBox(this.message.drawOptions.get().getBoundingBox(this.getX(), this.getY()));
      }

      @Override
      public FormPosition getPosition() {
         return this.position;
      }

      @Override
      public void setPosition(FormPosition position) {
         this.position = position;
      }
   }
}
