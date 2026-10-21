package necesse.entity.mobs;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import necesse.engine.GameLog;
import necesse.engine.Settings;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketQuestGiverUpdate;
import necesse.engine.network.server.ServerClient;
import necesse.engine.quest.Quest;
import necesse.engine.quest.QuestManager;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.light.GameLight;

public interface QuestGiver {
   QuestGiver.QuestGiverObject getQuestGiverObject();

   List<Quest> getGivenQuests(ServerClient var1);

   default void applyQuestGiverUpdatePacket(PacketQuestGiverUpdate packet, Client client) {
      QuestGiver.QuestGiverObject obj = this.getQuestGiverObject();
      synchronized (obj.questLock) {
         obj.clientQuests.clear();

         for (int questUniqueID : packet.questUniqueIDs) {
            obj.clientQuests.add(new QuestGiver.GivenQuest(questUniqueID, client.quests));
         }

         obj.clientUpdateTicker = 0;
      }
   }

   static DrawOptions getMarkerDrawOptions(char icon, Color color, int x, int y, GameLight light, GameCamera camera, int xOffset, int yOffset) {
      return getMarkerDrawOptions(Character.toString(icon), color, x, y, light, camera, xOffset, yOffset);
   }

   static DrawOptions getMarkerDrawOptions(String icons, Color color, int x, int y, GameLight light, GameCamera camera, int xOffset, int yOffset) {
      if (!Settings.showQuestMarkers) {
         return () -> {};
      }

      float floatLight = light.getFloatLevel();
      float alpha = 1.0F;
      if (floatLight < 0.5F) {
         alpha = floatLight * 2.0F;
      }

      return getMarkerDrawOptions(icons, 32, color, camera.getDrawX(x), camera.getDrawY(y), alpha, xOffset, yOffset);
   }

   static DrawOptions getMarkerDrawOptions(String icons, int fontSize, Color color, int drawX, int drawY, float alpha, int xOffset, int yOffset) {
      FontOptions fontOptions = new FontOptions(fontSize).outline().color(color).alphaf(alpha);
      if (icons.length() == 1) {
         char icon = icons.charAt(0);
         int drawXOffset = drawX + xOffset - FontManager.bit.getWidthCeil(icon, fontOptions) / 2;
         int drawYOffset = drawY + yOffset - FontManager.bit.getHeightCeil(icon, fontOptions);
         return () -> FontManager.bit.drawChar(drawXOffset, drawYOffset, icon, fontOptions);
      } else {
         int drawXOffset = drawX + xOffset - FontManager.bit.getWidthCeil(icons, fontOptions) / 2;
         int drawYOffset = drawY + yOffset - FontManager.bit.getHeightCeil(icons, fontOptions);
         return () -> FontManager.bit.drawString(drawXOffset, drawYOffset, icons, fontOptions);
      }
   }

   class GivenQuest {
      public int questUniqueID;
      public Quest quest;

      public GivenQuest(int questUniqueID, QuestManager questManager) {
         this.questUniqueID = questUniqueID;
         if (questManager != null) {
            this.updateQuest(questManager);
         }
      }

      public GivenQuest(int questUniqueID, Quest quest) {
         this.questUniqueID = questUniqueID;
         this.quest = quest;
      }

      private void updateQuest(QuestManager questManager) {
         this.quest = questManager.getQuest(this.questUniqueID);
      }
   }

   class QuestGiverObject {
      public int serverUpdateInterval = 5000;
      public int clientUpdateInterval = 1000;
      public final Mob mob;
      public final boolean shouldSaveQuests;
      private final Object questLock = new Object();
      private int serverUpdateTicker = this.serverUpdateInterval;
      private final HashSet<Long> activeAuths = new HashSet<>();
      private final HashMap<Long, ArrayList<QuestGiver.GivenQuest>> serverQuests = new HashMap<>();
      private int clientUpdateTicker = this.clientUpdateInterval;
      private final ArrayList<QuestGiver.GivenQuest> clientQuests = new ArrayList<>();

      public QuestGiverObject(Mob mob, boolean shouldSaveQuests) {
         if (!(mob instanceof QuestGiver)) {
            throw new IllegalArgumentException("Mob must implement QuestGiver interface");
         }

         this.mob = mob;
         this.shouldSaveQuests = shouldSaveQuests;
      }

      public void addSaveData(SaveData save) {
         if (this.shouldSaveQuests) {
            SaveData questData = new SaveData("questGiver");

            for (Entry<Long, ArrayList<QuestGiver.GivenQuest>> e : this.serverQuests.entrySet()) {
               if (!e.getValue().isEmpty()) {
                  SaveData clientData = new SaveData("quests");
                  clientData.addLong("auth", e.getKey());
                  clientData.addIntArray("uniqueIDs", e.getValue().stream().mapToInt(q -> q.questUniqueID).toArray());
                  questData.addSaveData(clientData);
               }
            }

            save.addSaveData(questData);
         }
      }

      public void applyLoadData(LoadData save) {
         if (this.shouldSaveQuests) {
            this.serverQuests.clear();
            LoadData questData = save.getFirstLoadDataByName("questGiver");
            if (questData != null) {
               for (LoadData clientData : questData.getLoadDataByName("quests")) {
                  long auth = clientData.getLong("auth", -1L);
                  if (auth != -1L) {
                     int[] questUniqueIDs = questData.getIntArray("uniqueIDs", new int[0], false);
                     if (questUniqueIDs.length > 0) {
                        synchronized (this.questLock) {
                           ArrayList<QuestGiver.GivenQuest> quests = this.serverQuests.compute(auth, (k, last) -> last == null ? new ArrayList<>() : last);

                           for (int uniqueID : questUniqueIDs) {
                              QuestGiver.GivenQuest quest = new QuestGiver.GivenQuest(uniqueID, this.mob.getLevel().getServer().world.getQuests());
                              if (quest.quest != null) {
                                 quests.add(quest);
                              } else {
                                 GameLog.warn.println("Could not find quest for quest giver with unique ID " + uniqueID);
                              }
                           }
                        }
                     }
                  }
               }

               int[] questUniqueIDs = questData.getIntArray("questUniqueIDs", new int[0], false);

               for (int uniqueID : questUniqueIDs) {
                  this.mob.getLevel().getServer().world.getQuests().removeQuest(uniqueID);
               }
            } else {
               GameLog.warn.println("Could not load quest giver data for mob");
            }
         }
      }

      public void clientTick() {
         this.clientUpdateTicker -= 50;
         if (this.clientUpdateTicker <= 0) {
            this.clientUpdateTicker = this.clientUpdateInterval;
            synchronized (this.questLock) {
               for (QuestGiver.GivenQuest quest : this.clientQuests) {
                  quest.updateQuest(this.mob.getLevel().getClient().quests);
               }
            }
         }
      }

      public ArrayList<QuestGiver.GivenQuest> getRequestedQuests(ServerClient client) {
         this.activeAuths.add(client.authentication);
         return this.serverQuests.compute(client.authentication, (auth, last) -> {
            List<Quest> givenQuests = ((QuestGiver)this.mob).getGivenQuests(client);
            return givenQuests.stream().map(q -> new QuestGiver.GivenQuest(q.getUniqueID(), q)).collect(Collectors.toCollection(ArrayList::new));
         });
      }

      public void serverTick() {
         this.serverUpdateTicker -= 50;
         if (this.serverUpdateTicker <= 0) {
            this.serverUpdateTicker = this.serverUpdateInterval;
            synchronized (this.questLock) {
               HashSet<Long> removeActiveAuths = new HashSet<>();

               for (long auth : this.activeAuths) {
                  ServerClient client = this.mob.getLevel().getServer().getClientByAuth(auth);
                  ArrayList<QuestGiver.GivenQuest> lastQuests = this.serverQuests.get(auth);
                  if (client != null && client.isSamePlace(this.mob.getLevel()) && lastQuests != null) {
                     boolean sendPacket = false;
                     List<Quest> nextQuests = ((QuestGiver)this.mob).getGivenQuests(client);
                     ArrayList<QuestGiver.GivenQuest> newQuests = new ArrayList<>(nextQuests.size());

                     for (Quest quest : nextQuests) {
                        int questUniqueID = quest.getUniqueID();
                        boolean foundLastQuest = false;
                        ListIterator<QuestGiver.GivenQuest> li = lastQuests.listIterator();

                        while (li.hasNext()) {
                           QuestGiver.GivenQuest lastQuest = li.next();
                           if (lastQuest.questUniqueID == questUniqueID) {
                              newQuests.add(new QuestGiver.GivenQuest(questUniqueID, quest));
                              li.remove();
                              foundLastQuest = true;
                              break;
                           }
                        }

                        if (!foundLastQuest) {
                           newQuests.add(new QuestGiver.GivenQuest(questUniqueID, quest));
                           sendPacket = true;
                        }
                     }

                     this.serverQuests.put(auth, newQuests);
                     if (sendPacket || !lastQuests.isEmpty()) {
                        client.sendPacket(new PacketQuestGiverUpdate(this.mob.getUniqueID(), newQuests.stream().mapToInt(q -> q.questUniqueID).toArray()));
                     }
                  } else {
                     removeActiveAuths.add(auth);
                  }
               }

               for (long auth : removeActiveAuths) {
                  this.activeAuths.remove(auth);
               }
            }
         }
      }

      public QuestMarkerOptions getMarkerOptions(PlayerMob perspective) {
         List<QuestGiver.GivenQuest> quests = null;
         if (perspective != null) {
            if (perspective.isClientClient()) {
               quests = this.clientQuests;
            } else if (perspective.isServerClient()) {
               quests = this.serverQuests.get(perspective.getServerClient().authentication);
            }
         }

         if (quests != null && !quests.isEmpty()) {
            boolean newQuest = false;
            boolean hasQuest = false;
            boolean canCompleteQuest = false;
            synchronized (this.questLock) {
               for (QuestGiver.GivenQuest gq : quests) {
                  if (gq.quest == null) {
                     newQuest = true;
                  } else {
                     hasQuest = true;
                     if (perspective.getNetworkClient() != null) {
                        canCompleteQuest = canCompleteQuest || gq.quest.canComplete(perspective.getNetworkClient());
                     }
                  }
               }
            }

            StringBuilder icons = new StringBuilder();
            if (newQuest) {
               icons.append('!');
            }

            if (hasQuest) {
               icons.append('?');
            }

            Color color = hasQuest && !canCompleteQuest ? new Color(100, 100, 100) : new Color(200, 200, 50);
            return new QuestMarkerOptions(icons.toString(), color);
         } else {
            return null;
         }
      }
   }
}
