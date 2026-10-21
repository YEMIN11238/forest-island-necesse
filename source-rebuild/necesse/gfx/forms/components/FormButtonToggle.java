package necesse.gfx.forms.components;

import java.util.Objects;
import necesse.engine.MouseDraggingElement;
import necesse.engine.input.Input;
import necesse.engine.input.InputEvent;
import necesse.gfx.Renderer;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormEventsHandler;
import necesse.gfx.forms.events.FormInputEvent;
import necesse.gfx.ui.ButtonState;

public abstract class FormButtonToggle extends FormButton implements ToggleButton<FormButtonToggle> {
   private FormEventsHandler<FormInputEvent<FormButtonToggle>> onToggle;
   private boolean toggled;

   public FormButtonToggle(boolean toggled) {
      this.handleClicksIfNoEventHandlers = true;
      this.onToggle = new FormEventsHandler<>();
      this.toggled = toggled;
   }

   public FormButtonToggle() {
      this(false);
      this.onToggle = new FormEventsHandler<>();
   }

   public FormButtonToggle onToggled(FormEventListener<FormInputEvent<FormButtonToggle>> listener) {
      this.onToggle.addListener(listener);
      return this;
   }

   public void setupDragToOtherButtons(Object sameButtonObject, boolean submitOnToggleEvent) {
      this.onDragStarted(e -> {
         final int id = e.draggingStartedEvent.isUsed() ? e.draggingStartedEvent.getLastID() : e.draggingStartedEvent.getID();
         Renderer.setMouseDraggingElement(new FormButtonToggle.FormButtonToggleDraggingElement(this, sameButtonObject) {
            @Override
            public boolean isKeyDown(Input input) {
               return input.isKeyDown(id);
            }
         });
         this.setToggled(!this.isToggled());
         if (submitOnToggleEvent) {
            this.onToggle.onEvent(new FormInputEvent<>(this, e.event));
            this.playTickSound();
         }
      });
      this.onChangedHover(
         e -> {
            if (this.isHovering() && this.isActive() && !this.isOnCooldown()) {
               MouseDraggingElement draggingElement = Renderer.getMouseDraggingElement();
               if (draggingElement instanceof FormButtonToggle.FormButtonToggleDraggingElement) {
                  FormButtonToggle.FormButtonToggleDraggingElement thisElement = (FormButtonToggle.FormButtonToggleDraggingElement)draggingElement;
                  if (thisElement.component != this
                     && this.isToggled() != thisElement.component.isToggled()
                     && Objects.equals(thisElement.sameObject, sameButtonObject)) {
                     this.setToggled(thisElement.component.isToggled());
                     if (submitOnToggleEvent) {
                        this.onToggle.onEvent(new FormInputEvent<>(this, e.event));
                     }

                     this.playTickSound();
                  }
               }
            }
         }
      );
   }

   public void setupDragToOtherButtons(Object sameButtonObject) {
      this.setupDragToOtherButtons(sameButtonObject, true);
   }

   protected boolean isDraggingThis() {
      MouseDraggingElement draggingElement = Renderer.getMouseDraggingElement();
      if (draggingElement instanceof FormButtonToggle.FormButtonToggleDraggingElement) {
         FormButtonToggle.FormButtonToggleDraggingElement thisElement = (FormButtonToggle.FormButtonToggleDraggingElement)draggingElement;
         return thisElement.component == this;
      } else {
         return false;
      }
   }

   @Override
   public ButtonState getButtonState() {
      if (!this.isActive() || this.isOnCooldown()) {
         return ButtonState.INACTIVE;
      } else {
         return !this.isToggled() && !this.isHovering() ? ButtonState.ACTIVE : ButtonState.HIGHLIGHTED;
      }
   }

   @Override
   protected void pressed(InputEvent event) {
      if (!this.isDraggingThis()) {
         this.toggled = !this.toggled;
         super.pressed(event);
         this.onToggle.onEvent(new FormInputEvent<>(this, event));
      }
   }

   @Override
   public boolean isToggled() {
      return this.toggled;
   }

   @Override
   public void setToggled(boolean value) {
      this.toggled = value;
   }

   @Override
   public void reset() {
      this.toggled = false;
   }

   @Override
   public void drawDraggingElement(int mouseX, int mouseY) {
   }

   protected static class FormButtonToggleDraggingElement implements MouseDraggingElement {
      public final FormButtonToggle component;
      public final Object sameObject;

      public FormButtonToggleDraggingElement(FormButtonToggle component, Object sameObject) {
         this.component = component;
         this.sameObject = sameObject;
      }

      @Override
      public boolean draw(int mouseX, int mouseY) {
         this.component.drawDraggingElement(mouseX, mouseY);
         return true;
      }
   }
}
