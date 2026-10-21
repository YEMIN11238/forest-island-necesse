package necesse.engine.util;

import java.nio.Buffer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

public class PointerList {
   private List<Runnable> list;

   public PointerList(Pointer... pointers) {
      this.list = Arrays.stream(pointers).map(p -> (Runnable)() -> MemoryUtil.nmemFree(p.address())).collect(Collectors.toList());
   }

   public <T extends Pointer> T add(T pointer) {
      this.list.add(() -> MemoryUtil.nmemFree(pointer.address()));
      return pointer;
   }

   public <T extends Buffer> T add(T buffer) {
      this.list.add(() -> MemoryUtil.memFree(buffer));
      return buffer;
   }

   public PointerBuffer add(PointerBuffer buffer) {
      this.list.add(() -> MemoryUtil.memFree(buffer));
      return buffer;
   }

   public void addFreeAction(Runnable action) {
      this.list.add(action);
   }

   public void freeAll() {
      for (Runnable action : this.list) {
         action.run();
      }
   }
}
