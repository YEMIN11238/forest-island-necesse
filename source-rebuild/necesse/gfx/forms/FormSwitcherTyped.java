package necesse.gfx.forms;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerNavigationHandler;

public class FormSwitcherTyped<C extends FormComponent> extends FormComponent {
   private final LinkedList<FormSwitcherTyped.SwitchComponent> components = new LinkedList<>();
   private FormSwitcherTyped.SwitchComponent currentComponent = null;
   public boolean useInactiveHitBoxes = false;

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      if (this.currentComponent != null) {
         this.currentComponent.component.handleInputEvent(event, tickManager, perspective);
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      if (this.currentComponent != null) {
         this.currentComponent.component.handleControllerEvent(event, tickManager, perspective);
      }
   }

   @Override
   public void addNextControllerFocus(
      List<ControllerFocus> list, int currentXOffset, int currentYOffset, ControllerNavigationHandler customNavigationHandler, Rectangle area, boolean draw
   ) {
      if (this.currentComponent != null) {
         this.currentComponent.component.addNextControllerFocus(list, currentXOffset, currentYOffset, customNavigationHandler, area, draw);
      }
   }

   @Override
   protected void init() {
      super.init();
      this.components.forEach(c -> {
         c.component.setManager(this.getManager(), this);
         c.component.inheritStyle(this);
      });
   }

   public <T extends C> T addComponent(T component, BiConsumer<T, Boolean> onSwitch) {
      this.components.add(new FormSwitcherTyped.SwitchComponent(component, onSwitch));
      component.setManager(this.getManager(), this);
      component.inheritStyle(this);
      return component;
   }

   public <T extends C> T addComponent(T component) {
      return this.addComponent(component, null);
   }

   public boolean hasComponent(C component) {
      return this.components.stream().anyMatch(c -> c.component == component);
   }

   public void removeComponent(C component) {
      if (this.hasComponent(component)) {
         component.dispose();
         this.components.removeIf(c -> c.component == component);
      }
   }

   public <T extends C> FormSwitcherTyped<C>.SwitchComponent<T> getSwitch(T component) {
      FormSwitcherTyped<C>.SwitchComponent<T> switcher = (FormSwitcherTyped<C>.SwitchComponent<T>)this.components
         .stream()
         .filter(c -> c.component == component)
         .findFirst()
         .orElse(null);
      if (switcher == null) {
         throw new IllegalArgumentException("Component not added to switcher");
      } else {
         return switcher;
      }
   }

   public void clearCurrent() {
      if (this.currentComponent != null) {
         if (this.currentComponent.onSwitch != null) {
            this.currentComponent.onSwitch.accept(this.currentComponent.component, false);
         }

         if (this.currentComponent.component instanceof FormSwitchedComponent) {
            ((FormSwitchedComponent)this.currentComponent.component).onSwitched(false);
         }
      }

      this.currentComponent = null;
      WindowManager.getWindow().submitNextMoveEvent();
   }

   public void makeCurrent(C component) {
      this.getSwitch(component).makeCurrent();
   }

   public <T extends C> T addAndMakeCurrentTemporary(T component) {
      return this.addAndMakeCurrentTemporary(component, null);
   }

   public <T extends C> T addAndMakeCurrentTemporary(T component, Runnable onRemoved) {
      this.addComponent(component, (c, active) -> {
         if (!active) {
            this.removeComponent((C)c);
            if (onRemoved != null) {
               onRemoved.run();
            }
         }
      });
      this.makeCurrent((C)component);
      return component;
   }

   public boolean isCurrent(C component) {
      return this.getSwitch(component).isCurrent();
   }

   public C getCurrent() {
      return (C)(this.currentComponent == null ? null : this.currentComponent.component);
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      if (this.currentComponent != null) {
         this.currentComponent.component.draw(tickManager, perspective, renderBox);
      }
   }

   @Override
   public List<Rectangle> getHitboxes() {
      if (this.useInactiveHitBoxes) {
         return this.components.stream().flatMap(c -> c.component.getHitboxes().stream()).collect(Collectors.toList());
      } else {
         return this.currentComponent == null ? Collections.emptyList() : this.currentComponent.component.getHitboxes();
      }
   }

   @Override
   public boolean isMouseOver(InputEvent event) {
      if (this.useInactiveHitBoxes) {
         return this.components.stream().anyMatch(c -> c.component.isMouseOver(event));
      } else {
         return this.currentComponent == null ? false : this.currentComponent.component.isMouseOver(event);
      }
   }

   @Override
   public boolean shouldDraw() {
      return this.currentComponent != null && this.currentComponent.component.shouldDraw();
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      this.components.stream().map(c -> c.component).forEach(formComponent -> formComponent.onWindowResized(window));
   }

   @Override
   public void dispose() {
      super.dispose();
      this.components.stream().map(c -> c.component).forEach(FormComponent::dispose);
   }

   public class SwitchComponent<T extends C> {
      public final FormComponent component;
      private final BiConsumer<T, Boolean> onSwitch;

      private SwitchComponent(T component, BiConsumer<T, Boolean> onSwitch) {
         this.component = component;
         this.onSwitch = onSwitch;
      }

      public boolean isCurrent() {
         return FormSwitcherTyped.this.currentComponent == this;
      }

      public void makeCurrent() {
         if (FormSwitcherTyped.this.currentComponent != this) {
            FormSwitcherTyped.SwitchComponent prev = FormSwitcherTyped.this.currentComponent;
            if (prev != null) {
               if (prev.onSwitch != null) {
                  prev.onSwitch.accept((T)prev.component, false);
               }

               if (prev.component instanceof FormSwitchedComponent) {
                  ((FormSwitchedComponent)prev.component).onSwitched(false);
               }
            }

            FormSwitcherTyped.this.currentComponent = this;
            if (this.onSwitch != null) {
               this.onSwitch.accept((T)this.component, true);
            }

            if (this.component instanceof FormSwitchedComponent) {
               ((FormSwitchedComponent)this.component).onSwitched(true);
            }

            WindowManager.getWindow().submitNextMoveEvent();
         }
      }
   }
}
