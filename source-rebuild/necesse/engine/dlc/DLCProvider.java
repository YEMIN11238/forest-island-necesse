package necesse.engine.dlc;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import necesse.engine.util.Event;

public abstract class DLCProvider {
   public static DLCProvider instance;
   public static final HashMap<DLC, DLCProvider.DLCStatus> DLCStatuses = new HashMap<>();
   public static final Event<DLC> onDLCInstalled = new Event<>();

   public DLCProvider() {
      instance = this;
      DLC.DLCs.forEach(dlc -> DLCStatuses.put(dlc, DLCProvider.DLCStatus.NOT_OWNED));
   }

   public void checkForNewlyInstalledDLCs() {
      DLCStatuses.forEach((DLC, value) -> {
         if (value != DLCProvider.DLCStatus.INSTALLED) {
            DLCProvider.DLCStatus newStatus = this.checkDLCStatus(DLC);
            DLCStatuses.put(DLC, newStatus);
            if (newStatus == DLCProvider.DLCStatus.INSTALLED) {
               DLC.onDLCInstalledClient();
               onDLCInstalled.invoke(DLC);
            }
         }
      });
   }

   public static List<DLC> getInstalledDLCs() {
      return DLCStatuses.entrySet()
         .stream()
         .filter(entry -> entry.getValue() == DLCProvider.DLCStatus.INSTALLED)
         .map(Entry::getKey)
         .collect(Collectors.toList());
   }

   protected abstract DLCProvider.DLCStatus checkDLCStatus(DLC var1);

   public enum DLCStatus {
      NOT_OWNED,
      OWNED,
      INSTALLED;
   }
}
