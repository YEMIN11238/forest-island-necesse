package necesse.gfx.forms.presets.debug.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.Function;
import necesse.engine.input.InputEvent;
import necesse.gfx.forms.presets.debug.DebugForm;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.ListGameTooltips;

public abstract class MouseDebugGameTool extends DebugGameTool {
   protected Function<InputEvent, Boolean> scroll;
   protected Function<InputEvent, Boolean> mouseMove;
   protected String scrollUsage;
   protected HashMap<Integer, Function<InputEvent, Boolean>> keyEvents = new HashMap<>();
   protected HashMap<Integer, String> keyUsages = new HashMap<>();

   public MouseDebugGameTool(DebugForm parent, String name) {
      super(parent, name);
   }

   public void setKeyUsage(int key, String usage) {
      this.keyUsages.put(key, usage);
   }

   public void onKeyEvent(int key, Function<InputEvent, Boolean> event, String usage) {
      this.keyEvents.put(key, event);
      this.setKeyUsage(key, usage);
   }

   public void onKeyClick(int key, Function<InputEvent, Boolean> event, String usage) {
      this.onKeyEvent(key, e -> e.state ? true : event.apply(e), usage);
   }

   public void setLeftUsage(String usage) {
      this.setKeyUsage(-100, usage);
   }

   public void onLeftEvent(Function<InputEvent, Boolean> event, String usage) {
      this.onKeyEvent(-100, event, usage);
   }

   public void onLeftClick(Function<InputEvent, Boolean> event, String usage) {
      this.onKeyClick(-100, event, usage);
   }

   public void setRightUsage(String usage) {
      this.setKeyUsage(-99, usage);
   }

   public void onRightEvent(Function<InputEvent, Boolean> event, String usage) {
      this.onKeyEvent(-99, event, usage);
   }

   public void onRightClick(Function<InputEvent, Boolean> event, String usage) {
      this.onKeyClick(-99, event, usage);
   }

   public void setScrollUsage(String usage) {
      this.scrollUsage = usage;
   }

   public void onScroll(Function<InputEvent, Boolean> event, String usage) {
      this.scroll = event;
      this.setScrollUsage(usage);
   }

   public void onMouseMove(Function<InputEvent, Boolean> event) {
      this.mouseMove = event;
   }

   @Override
   public boolean inputEvent(InputEvent event) {
      if (this.parent.mainGame.formManager.isMouseOver(event)) {
         return false;
      }

      try {
         if (event.isMouseMoveEvent()) {
            if (this.mouseMove != null) {
               return this.mouseMove.apply(event);
            }
         } else if (event.isMouseWheelEvent()) {
            if (this.scroll != null) {
               return !event.state || this.scroll.apply(event);
            }
         } else {
            Function<InputEvent, Boolean> listener = this.keyEvents.get(event.getID());
            if (listener != null) {
               return listener.apply(event);
            }
         }

         return false;
      } catch (Exception ex) {
         System.err.println(this.name + " debug tool error:");
         ex.printStackTrace();
         return true;
      }
   }

   @Override
   public GameTooltips getTooltips() {
      ListGameTooltips list = new ListGameTooltips(super.getTooltips());
      ArrayList<Entry<Integer, String>> usages = new ArrayList<>();

      for (Entry<Integer, String> usage : this.keyUsages.entrySet()) {
         if (usage.getValue() != null && !usage.getValue().isEmpty()) {
            usages.add(usage);
         }
      }

      if (this.scrollUsage != null && !this.scrollUsage.isEmpty()) {
         usages.add(new Entry<Integer, String>() {
            public Integer getKey() {
               return -98;
            }

            public String getValue() {
               return MouseDebugGameTool.this.scrollUsage;
            }

            public String setValue(String value) {
               return value;
            }
         });
      }

      usages.sort(Comparator.comparingInt(Entry::getKey));

      for (Entry<Integer, String> usage : usages) {
         list.add(new InputTooltip(usage.getKey(), usage.getValue()));
      }

      return list;
   }
}
