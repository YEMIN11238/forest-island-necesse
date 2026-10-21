package necesse.inventory.item;

import java.awt.Color;
import java.util.Iterator;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameUtils;

public class ItemStatTipList extends ItemStatTip implements Iterable<ItemStatTip> {
   private GameLinkedList<ItemStatTipList.Element> list = new GameLinkedList<>();

   public void add(int priority, ItemStatTip tip) {
      if (this.list.isEmpty()) {
         this.list.add(new ItemStatTipList.Element(tip, priority));
      } else {
         int firstDelta = priority - this.list.getFirst().priority;
         int lastDelta = this.list.getLast().priority - priority;
         if (firstDelta < lastDelta) {
            for (GameLinkedList<ItemStatTipList.Element>.Element current = this.list.getFirstElement(); current != null; current = current.next()) {
               if (priority <= ((ItemStatTipList.Element)current.object).priority) {
                  current.insertBefore(new ItemStatTipList.Element(tip, priority));
                  return;
               }
            }

            this.list.addLast(new ItemStatTipList.Element(tip, priority));
         } else {
            for (GameLinkedList<ItemStatTipList.Element>.Element current = this.list.getLastElement(); current != null; current = current.prev()) {
               if (priority >= ((ItemStatTipList.Element)current.object).priority) {
                  current.insertAfter(new ItemStatTipList.Element(tip, priority));
                  return;
               }
            }

            this.list.addFirst(new ItemStatTipList.Element(tip, priority));
         }
      }
   }

   @Override
   public Iterator<ItemStatTip> iterator() {
      return GameUtils.mapIterator(this.list.iterator(), e -> e.tip);
   }

   @Override
   public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
      GameMessageBuilder builder = new GameMessageBuilder();
      boolean addNewLine = false;

      for (ItemStatTip tip : this) {
         if (addNewLine) {
            builder.append("\n");
         }

         builder.append(tip.toMessage(betterColor, worseColor, neutralColor, showDifference));
         addNewLine = true;
      }

      return builder;
   }

   private static class Element {
      public final ItemStatTip tip;
      public final int priority;

      public Element(ItemStatTip tip, int priority) {
         this.tip = tip;
         this.priority = priority;
      }
   }
}
