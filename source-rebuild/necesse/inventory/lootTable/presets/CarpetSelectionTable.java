package necesse.inventory.lootTable.presets;

import java.util.HashMap;
import java.util.Map.Entry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;

public class CarpetSelectionTable {
   public static final HashMap<String, Integer> carpets = new HashMap<>();

   public static String getRandomCarpetID(GameRandom random) {
      if (carpets.isEmpty()) {
         return "woolcarpet";
      }

      TicketSystemList<String> ticketList = new TicketSystemList<>();

      for (Entry<String, Integer> entry : carpets.entrySet()) {
         ticketList.addObject(entry.getValue(), entry.getKey());
      }

      return ticketList.getRandomObject(random);
   }

   public static String getRandomCarpetIDFromSpecificHashmap(GameRandom random, HashMap<String, Integer> hashMap) {
      if (hashMap.isEmpty()) {
         return "velourcarpet";
      }

      TicketSystemList<String> ticketList = new TicketSystemList<>();

      for (Entry<String, Integer> entry : hashMap.entrySet()) {
         ticketList.addObject(entry.getValue(), entry.getKey());
      }

      return ticketList.getRandomObject(random);
   }

   static {
      carpets.put("woolcarpet", 100);
      carpets.put("leathercarpet", 100);
      carpets.put("brownbearcarpet", 100);
      carpets.put("bluecarpet", 100);
      carpets.put("goldgridcarpet", 100);
      carpets.put("greencarpet", 100);
      carpets.put("steelgreycarpet", 100);
      carpets.put("purplecarpet", 80);
      carpets.put("velourcarpet", 80);
      carpets.put("heartcarpet", 80);
      carpets.put("redyarncarpet", 60);
   }
}
