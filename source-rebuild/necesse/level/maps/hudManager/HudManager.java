package necesse.level.maps.hudManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.SortedDrawable;
import necesse.level.maps.Level;

public class HudManager {
   private final Level level;
   private final ArrayList<HudManager.StoredElement> list;
   private final GameLinkedList<HudDrawOnMap> mapList;

   public HudManager(Level level) {
      this.level = level;
      this.list = new ArrayList<>();
      this.mapList = new GameLinkedList<>();
   }

   public void tick() {
      for (int i = 0; i < this.list.size(); i++) {
         HudManager.StoredElement e = this.list.get(i);
         if (e.hudDrawElement.isRemoved()) {
            if (e.mapDrawElement != null) {
               e.mapDrawElement.remove();
               e.mapDrawElement = null;
            }

            this.list.remove(i);
            i--;
            e.hudDrawElement.onRemove();
         }
      }
   }

   public Iterable<HudDrawElement> getElements() {
      return GameUtils.mapIterable(this.list.iterator(), e -> e.hudDrawElement);
   }

   public Stream<HudDrawElement> streamElements() {
      return this.list.stream().map(e -> e.hudDrawElement);
   }

   public boolean removeOneElements(Predicate<HudDrawElement> predicate) {
      return this.removeElements(predicate, 1) > 0;
   }

   public int removeElements(Predicate<HudDrawElement> predicate) {
      return this.removeElements(predicate, Integer.MAX_VALUE);
   }

   public int removeElements(Predicate<HudDrawElement> predicate, int limit) {
      if (limit <= 0) {
         return 0;
      }

      int removed = 0;

      for (int i = 0; i < this.list.size(); i++) {
         HudManager.StoredElement e = this.list.get(i);
         if (predicate.test(e.hudDrawElement)) {
            removed++;
            if (e.mapDrawElement != null) {
               e.mapDrawElement.remove();
               e.mapDrawElement = null;
            }

            this.list.remove(i);
            i--;
            e.hudDrawElement.onRemove();
            if (removed >= limit) {
               break;
            }
         }
      }

      return removed;
   }

   public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
      for (HudManager.StoredElement e : this.list.toArray(new HudManager.StoredElement[0])) {
         if (e != null && e.hudDrawElement != null && !e.hudDrawElement.isRemoved()) {
            e.hudDrawElement.addDrawables(list, camera, perspective);
         }
      }
   }

   public Iterable<HudDrawOnMap> getMapDraws() {
      return this.mapList;
   }

   public <T extends HudDrawElement> T addElement(T element) {
      if (element != null && !this.level.isServer()) {
         HudManager.StoredElement storedElement = new HudManager.StoredElement(element);
         this.list.add(storedElement);
         if (element instanceof HudDrawOnMap) {
            storedElement.mapDrawElement = this.mapList.addLast((HudDrawOnMap)element);
         }

         element.setLevel(this.level);
         element.init(this);
         return element;
      } else {
         return null;
      }
   }

   private static class StoredElement {
      public HudDrawElement hudDrawElement;
      public GameLinkedList<HudDrawOnMap>.Element mapDrawElement;

      public StoredElement(HudDrawElement hudDrawElement) {
         this.hudDrawElement = hudDrawElement;
      }
   }
}
