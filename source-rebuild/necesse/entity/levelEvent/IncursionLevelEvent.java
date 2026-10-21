package necesse.entity.levelEvent;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import necesse.engine.eventStatusBars.EventStatusBarData;
import necesse.engine.eventStatusBars.EventStatusBarManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.IncursionPerksRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.engine.util.HashMapArrayList;
import necesse.entity.levelEvent.actions.CustomIteratorLevelEventAction;
import necesse.entity.manager.MobLootTableDropsListenerEntityComponent;
import necesse.entity.manager.MobPrivateLootTableDropsListenerEntityComponent;
import necesse.entity.manager.OnMobAddedListenerEntityComponent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.bosses.BossSpawnPortalMob;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.InventoryItem;
import necesse.level.maps.IncursionLevel;

public abstract class IncursionLevelEvent
   extends LevelEvent
   implements OnMobAddedListenerEntityComponent,
   MobLootTableDropsListenerEntityComponent,
   MobPrivateLootTableDropsListenerEntityComponent {
   public boolean isDirty;
   public boolean isFighting;
   public boolean isDone;
   public boolean isCompleted;
   public boolean bossPortalSpawned;
   public int bossPortalAttemptsRemaining = 5;
   public boolean onlySpawnOnePortal = true;
   public ArrayList<Integer> bossPortalUniqueIDs = new ArrayList<>();
   public boolean countdownStarted;
   public int countdownTotal = 5000;
   public int countdownTimer;
   public String bossStringID;
   public int bossUniqueID;
   public ArrayList<Integer> spawnedBossesUniqueIDs = new ArrayList<>();
   public final CustomIteratorLevelEventAction updateProgressAction;
   protected HashMapArrayList<Long, InventoryItem> privateBossLootDropped = new HashMapArrayList<>();
   protected ArrayList<InventoryItem> publicLootDropped = new ArrayList<>();

   public IncursionLevelEvent() {
      this.shouldSave = true;
      this.updateProgressAction = this.registerAction(new CustomIteratorLevelEventAction() {
         @Override
         protected void write(PacketWriter writer) {
            IncursionLevelEvent.this.setupUpdatePacket(writer);
         }

         @Override
         protected void read(PacketReader reader) {
            IncursionLevelEvent.this.applyUpdatePacket(reader);
         }
      });
   }

   public IncursionLevelEvent(String bossStringID) {
      this();
      this.bossStringID = bossStringID;
   }

   @Override
   public boolean isNetworkImportant() {
      return true;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addSafeString("bossStringID", this.bossStringID);
      save.addBoolean("bossPortalSpawned", this.bossPortalSpawned);
      save.addInt("bossPortalAttemptsRemaining", this.bossPortalAttemptsRemaining);
      save.addIntArray("bossPortalUniqueIDs", this.bossPortalUniqueIDs.stream().mapToInt(i -> i).toArray());
      save.addBoolean("countdownStarted", this.countdownStarted);
      save.addInt("countdownTimer", this.countdownTimer);
      save.addInt("bossUniqueID", this.bossUniqueID);
      if (!this.spawnedBossesUniqueIDs.isEmpty()) {
         save.addIntCollection("spawnedBossesUniqueIDs", this.spawnedBossesUniqueIDs);
      }

      save.addBoolean("isFighting", this.isFighting);
      save.addBoolean("isDone", this.isDone);
      save.addBoolean("isCompleted", this.isCompleted);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.bossStringID = save.getSafeString("bossStringID", this.bossStringID, false);
      this.bossPortalSpawned = save.getBoolean("bossPortalSpawned", this.bossPortalSpawned, false);
      this.bossPortalAttemptsRemaining = save.getInt("bossPortalAttemptsRemaining", this.bossPortalAttemptsRemaining, false);
      this.bossPortalUniqueIDs = new ArrayList<>();

      for (int uniqueID : save.getIntArray("bossPortalUniqueIDs", new int[0], false)) {
         this.bossPortalUniqueIDs.add(uniqueID);
      }

      this.countdownStarted = save.getBoolean("countdownStarted", this.countdownStarted, false);
      this.countdownTimer = save.getInt("countdownTimer", this.countdownTimer, false);
      this.spawnedBossesUniqueIDs = new ArrayList<>(save.getIntCollection("spawnedBossesUniqueIDs", new ArrayList<>(), false));
      this.bossUniqueID = save.getInt("bossUniqueID", this.bossUniqueID, false);
      if (this.bossUniqueID != -1 && this.bossUniqueID != 0) {
         this.spawnedBossesUniqueIDs.add(this.bossUniqueID);
      }

      this.isFighting = save.getBoolean("isFighting", this.isFighting, false);
      this.isDone = save.getBoolean("isDone", this.isDone, false);
      this.isCompleted = save.getBoolean("isCompleted", this.isCompleted, false);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      this.setupUpdatePacket(writer);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.applyUpdatePacket(reader);
   }

   public void setupUpdatePacket(PacketWriter writer) {
      writer.putNextBoolean(this.countdownStarted);
      if (this.countdownStarted) {
         writer.putNextInt(this.countdownTimer);
      }

      writer.putNextBoolean(this.bossPortalSpawned);
      writer.putNextBoolean(this.isFighting);
      writer.putNextBoolean(this.isDone);
      writer.putNextBoolean(this.isCompleted);
   }

   public void applyUpdatePacket(PacketReader reader) {
      this.countdownStarted = reader.getNextBoolean();
      if (this.countdownStarted) {
         this.countdownTimer = reader.getNextInt();
      }

      this.bossPortalSpawned = reader.getNextBoolean();
      this.isFighting = reader.getNextBoolean();
      this.isDone = reader.getNextBoolean();
      this.isCompleted = reader.getNextBoolean();
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (!this.countdownStarted) {
         this.countdownTimer = 3000;
      }

      if (this.countdownTimer > 0) {
         this.countdownTimer -= 50;
      }

      if (!this.isFighting) {
         int current;
         int max;
         if (this.isDone) {
            current = 100;
            max = 100;
         } else if (this.countdownStarted) {
            current = this.countdownTimer;
            max = this.countdownTotal;
         } else if (this.bossPortalSpawned) {
            current = 100;
            max = 100;
         } else {
            current = this.getObjectiveCurrent();
            max = this.getObjectiveMax();
         }

         EventStatusBarManager.registerEventStatusBar(
            this.getUniqueID(),
            current,
            max,
            () -> new EventStatusBarData(EventStatusBarData.BarCategory.incursion, null) {
               @Override
               public FairTypeDrawOptions getDisplayNameDrawOptions() {
                  FairType fairType = new FairType();
                  FontOptions fontOptions = new FontOptions(16).outline();
                  if (IncursionLevelEvent.this.isCompleted) {
                     fairType.append(fontOptions, Localization.translate("ui", "incursionnowcomplete"));
                  } else if (IncursionLevelEvent.this.isDone) {
                     fairType.append(fontOptions, Localization.translate("ui", "incursionnowclose"));
                  } else if (IncursionLevelEvent.this.countdownStarted) {
                     fairType.append(fontOptions, Localization.translate("misc", "bossapproaching"));
                  } else if (IncursionLevelEvent.this.bossPortalSpawned) {
                     fairType.append(fontOptions, IncursionLevelEvent.this.getBossPortalSpawnedMessage().translate());
                  } else if (IncursionLevelEvent.this.level instanceof IncursionLevel && ((IncursionLevel)IncursionLevelEvent.this.level).incursionData != null
                     )
                   {
                     for (FairType objective : ((IncursionLevel)IncursionLevelEvent.this.level)
                        .incursionData
                        .getObjectives(((IncursionLevel)IncursionLevelEvent.this.level).incursionData, fontOptions)) {
                        fairType.append(fontOptions, "\n").append(objective.getGlyphsArray());
                     }
                  }

                  if (fairType.getLength() <= 0) {
                     return null;
                  }

                  fairType.applyParsers(TypeParsers.STRIP_GAME_COLOR, TypeParsers.ItemIcon(fontOptions.getSize()), TypeParsers.MobIcon(fontOptions.getSize()));
                  return fairType.getDrawOptions(FairType.TextAlign.CENTER);
               }

               @Override
               public GameMessage getStatusText(EventStatusBarData.StatusAtTime status) {
                  if (!IncursionLevelEvent.this.isCompleted
                     && !IncursionLevelEvent.this.isDone
                     && !IncursionLevelEvent.this.bossPortalSpawned
                     && !IncursionLevelEvent.this.countdownStarted) {
                     String percentString = (int)(status.getPercent() * 100.0F) + "%";
                     return new LocalMessage("ui", "incursionprogressbar", "percent", percentString);
                  } else {
                     return null;
                  }
               }

               @Override
               public Color getBufferColor() {
                  return null;
               }

               @Override
               public Color getFillColor() {
                  if (IncursionLevelEvent.this.isDone) {
                     return new Color(10, 117, 8);
                  } else if (IncursionLevelEvent.this.countdownStarted) {
                     return new Color(150, 13, 38);
                  } else {
                     return IncursionLevelEvent.this.bossPortalSpawned ? new Color(201, 104, 24) : new Color(24, 70, 201);
                  }
               }
            }
         );
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      boolean sendUpdate = this.isDirty;
      if ((this.isObjectiveDone() || this.isFighting || this.bossPortalSpawned) && !this.isDone) {
         if (!this.bossPortalSpawned) {
            boolean hasPlayer = GameUtils.streamNetworkClients(this.level).anyMatch(c -> c.playerMob != null);
            if (hasPlayer) {
               this.onObjectiveCompleted();
               sendUpdate = true;
            }
         } else if (this.countdownStarted) {
            if (this.bossUniqueID == 0) {
               this.countdownTimer -= 50;
               if (this.countdownTimer <= 0) {
                  Point spawnPos = this.getBossSpawnPosition();
                  Mob bossMob = this.spawnBossAtPosition(spawnPos.x, spawnPos.y);
                  if (bossMob != null) {
                     this.onBossSummoned(bossMob);
                     if (this.isServer()) {
                        this.level
                           .getServer()
                           .network
                           .sendToClientsWithEntity(new PacketChatMessage(new LocalMessage("misc", "bosssummon", "name", bossMob.getLocalization())), bossMob);
                     }
                  } else {
                     if (this.level instanceof IncursionLevel) {
                        ((IncursionLevel)this.level).markCanComplete(() -> null, null, client -> null);
                     }

                     this.isCompleted = true;
                     this.isDone = true;
                     this.isFighting = false;
                     this.bossUniqueID = -1;
                     this.spawnedBossesUniqueIDs.clear();
                  }
               }
            } else {
               Mob lastBoss = null;
               ListIterator<Integer> li = this.spawnedBossesUniqueIDs.listIterator();

               while (li.hasNext()) {
                  int bossUniqueID = li.next();
                  Mob bossMob = this.level.entityManager.mobs.get(bossUniqueID, true);
                  if (bossMob == null || bossMob.removed()) {
                     lastBoss = bossMob;
                     li.remove();
                  }
               }

               if (this.isFighting && (lastBoss == null || lastBoss.removed()) && this.spawnedBossesUniqueIDs.isEmpty()) {
                  boolean isDone = true;
                  if (this.isServer()) {
                     if (lastBoss != null && lastBoss.hasDied()) {
                        Mob finalLastBoss = lastBoss;
                        this.markCanComplete(
                           () -> finalLastBoss.getLootDropsPosition(null),
                           () -> finalLastBoss.streamAttackers()
                              .map(Attacker::getFirstPlayerOwner)
                              .filter(Objects::nonNull)
                              .filter(PlayerMob::isServerClient)
                              .map(PlayerMob::getServerClient)
                              .filter(Objects::nonNull),
                           finalLastBoss::getLootDropsPosition
                        );
                     } else if (this.bossPortalAttemptsRemaining > 0) {
                        this.spawnBossPortals(true);
                        isDone = false;
                     } else {
                        this.getServer().network.sendToClientsAtEntireLevel(new PacketChatMessage(new LocalMessage("ui", "incursionnowclose")), this.level);
                     }
                  }

                  this.isDone = isDone;
                  this.countdownStarted = false;
                  this.isFighting = false;
                  this.isDirty = true;
                  this.bossUniqueID = 0;
                  this.spawnedBossesUniqueIDs.clear();
               }
            }
         }
      }

      if (sendUpdate && this.isServer()) {
         this.updateProgressAction.runAndSend();
         this.isDirty = false;
      }
   }

   public void markCanComplete(
      Supplier<Point> sharedLootDropPositionGetter,
      Supplier<Stream<ServerClient>> clientsStreamGetter,
      Function<ServerClient, Point> privateLootDropPositionGetter
   ) {
      if (this.level instanceof IncursionLevel) {
         ((IncursionLevel)this.level).markCanComplete(sharedLootDropPositionGetter, clientsStreamGetter, privateLootDropPositionGetter);
      }

      this.isCompleted = true;
   }

   public abstract boolean isObjectiveDone();

   public abstract int getObjectiveCurrent();

   public abstract int getObjectiveMax();

   public float getRandomTabletDropChance(Mob mob) {
      return 0.016666668F;
   }

   public void forceObjectiveComplete() {
      if (!this.isObjectiveDone() && !this.bossPortalSpawned) {
         this.onObjectiveCompleted();
         this.isDirty = true;
      } else {
         Point bossPortalPosition;
         if (this.level instanceof IncursionLevel) {
            Point centerPosition = ((IncursionLevel)this.level).getBossPortalCenterPosition();
            int offset = ((IncursionLevel)this.level).getBossPortalCenterOffset();
            bossPortalPosition = new Point(centerPosition.x, centerPosition.y + offset);
         } else {
            bossPortalPosition = this.getReturnPortalLevelPosition();
         }

         this.markCanComplete(() -> bossPortalPosition, () -> GameUtils.streamServerClients(this.getLevel()), client -> bossPortalPosition);
         this.countdownStarted = false;
         this.isFighting = false;

         for (int mobUniqueID : this.spawnedBossesUniqueIDs) {
            Mob mob = this.getLevel().entityManager.mobs.get(mobUniqueID, false);
            if (mob != null) {
               mob.remove();
            }
         }

         this.spawnedBossesUniqueIDs.clear();
         this.removeExistingSpawnPortal();
      }
   }

   public GameMessage canSpawnBoss(String bossStringID) {
      if (this.bossStringID != null && !this.bossStringID.equals(bossStringID)) {
         return new LocalMessage("misc", "cannotsummonhere");
      } else if (this.isCompleted) {
         return new LocalMessage("misc", "cannotsummoncomplete");
      } else {
         return this.isFighting ? new LocalMessage("misc", "cannotsummonnow") : null;
      }
   }

   public void removeExistingSpawnPortal() {
      for (int uniqueID : this.bossPortalUniqueIDs) {
         Mob spawnPortal = uniqueID == -1 ? null : this.level.entityManager.mobs.get(uniqueID, false);
         if (spawnPortal != null && !spawnPortal.removed()) {
            spawnPortal.remove();
         }
      }

      this.bossPortalUniqueIDs.clear();
   }

   public void onObjectiveCompleted() {
      this.spawnBossPortals(true);
      if (this.level instanceof IncursionLevel) {
         ((IncursionLevel)this.level).onIncursionObjectiveCompleted(this);
      }
   }

   public Mob spawnBossAtPosition(int levelX, int levelY) {
      if (this.bossStringID != null) {
         Mob mob = MobRegistry.getMob(this.bossStringID, this.level);
         if (mob == null) {
            return null;
         }

         mob.setPos(levelX, levelY, true);
         if (this.level instanceof IncursionLevel) {
            ((IncursionLevel)this.level).updateBossOffsetPosition(mob, levelX, levelY);
         }

         this.level.regionManager.ensureRegionsAreLoaded(mob.getRegionPositions());
         this.level.entityManager.mobs.add(mob);
         return mob;
      } else {
         return null;
      }
   }

   public void spawnBossPortals(boolean reduceAttempts) {
      this.removeExistingSpawnPortal();
      int offset;
      Point centerPosition;
      if (this.level instanceof IncursionLevel) {
         centerPosition = ((IncursionLevel)this.level).getBossPortalCenterPosition();
         offset = ((IncursionLevel)this.level).getBossPortalCenterOffset();
      } else {
         centerPosition = this.getReturnPortalLevelPosition();
         offset = 96;
      }

      float startAngle = 90.0F;
      int portalSpawns = this.onlySpawnOnePortal ? 1 : Math.max(this.bossPortalAttemptsRemaining, 1);
      float anglePerPortal = 360.0F / portalSpawns;

      for (int i = 0; i < portalSpawns; i++) {
         float angle = GameMath.fixAngle(startAngle + i * anglePerPortal);
         Float dir = GameMath.getAngleDir(angle);
         BossSpawnPortalMob spawnPortal = (BossSpawnPortalMob)MobRegistry.getMob("bossspawnportal", this.level);
         if (this.onlySpawnOnePortal) {
            spawnPortal.remainingAttemptsTip = this.bossPortalAttemptsRemaining;
         }

         spawnPortal.setPos(centerPosition.x + dir.x * offset, centerPosition.y + dir.y * offset, true);
         this.level.regionManager.ensureRegionsAreLoaded(spawnPortal.getRegionPositions());
         this.level.entityManager.mobs.add(spawnPortal);
         this.bossPortalUniqueIDs.add(spawnPortal.getUniqueID());
      }

      this.bossPortalSpawned = true;
      this.countdownStarted = false;
      if (reduceAttempts) {
         this.bossPortalAttemptsRemaining--;
         GameMessage bossPortalMessage = this.getBossPortalSpawnedMessage();
         if (bossPortalMessage != null) {
            this.level.getServer().network.sendToClientsAtEntireLevel(new PacketChatMessage(bossPortalMessage), this.level);
         }
      }
   }

   public Point getReturnPortalLevelPosition() {
      return this.level instanceof IncursionLevel
         ? ((IncursionLevel)this.level).getReturnPortalPosition()
         : new Point(this.level.tileWidth * 32 / 2, this.level.tileHeight * 32 / 2);
   }

   public Point getBossSpawnPosition() {
      return this.level instanceof IncursionLevel ? ((IncursionLevel)this.level).getBossSpawnPosition() : this.getReturnPortalLevelPosition();
   }

   public void onBossSpawnTriggered(BossSpawnPortalMob portal) {
      if (portal != null) {
         portal.remove(0.0F, 0.0F, null, true);
         this.bossPortalUniqueIDs.remove(Integer.valueOf(portal.getUniqueID()));
         if (this.level.isIncursionLevel) {
            for (Integer currentIncursionPerkID : ((IncursionLevel)this.level).incursionData.currentIncursionPerkIDs) {
               IncursionPerksRegistry.getPerk(currentIncursionPerkID).onIncursionBossPortalClicked((IncursionLevel)this.level);
            }
         }
      }

      this.countdownStarted = true;
      this.countdownTimer = this.countdownTotal;
      this.level.getServer().network.sendToClientsAtEntireLevel(new PacketChatMessage(new LocalMessage("misc", "bossapproaching")), this.level);
      this.removeExistingSpawnPortal();
      this.isDirty = true;
   }

   public GameMessage getBossPortalSpawnedMessage() {
      return this.level instanceof IncursionLevel ? ((IncursionLevel)this.level).getBossPortalSpawnedMessage() : new LocalMessage("misc", "bossportal");
   }

   public void onBossSummoned(Mob mob) {
      this.bossUniqueID = mob.getUniqueID();
      this.spawnedBossesUniqueIDs.add(mob.getUniqueID());
      this.bossPortalSpawned = true;
      this.removeExistingSpawnPortal();
      this.countdownStarted = true;
      this.countdownTimer = 0;
      this.isFighting = true;
      this.isDone = false;
      this.isDirty = true;
      this.privateBossLootDropped.clearAll();
      this.publicLootDropped.clear();
   }

   @Override
   public void onMobSpawned(Mob mob) {
      if (!this.getLevel().isClient() && mob.isSecondaryIncursionBoss) {
         this.spawnedBossesUniqueIDs.add(mob.getUniqueID());
      }
   }

   @Override
   public void onLevelMobDropsLoot(Mob mob, Point dropPosition, ArrayList<InventoryItem> drops) {
      if (mob.isBoss() && this.spawnedBossesUniqueIDs.contains(mob.getUniqueID())) {
         if (this.spawnedBossesUniqueIDs.size() <= 1) {
            drops.addAll(this.publicLootDropped);
            this.publicLootDropped.clear();
         } else {
            this.publicLootDropped.addAll(drops);
            drops.clear();
         }
      }
   }

   @Override
   public int getLevelMobDropsLootPriority() {
      return Integer.MIN_VALUE;
   }

   @Override
   public void onLevelMobPrivateDropsLoot(Mob mob, ServerClient client, Point dropPosition, ArrayList<InventoryItem> drops) {
      if (mob.isBoss() && this.spawnedBossesUniqueIDs.contains(mob.getUniqueID()) && client != null) {
         if (this.spawnedBossesUniqueIDs.size() <= 1) {
            ArrayList<InventoryItem> loot = this.privateBossLootDropped.get(client.authentication);
            if (loot != null) {
               drops.addAll(loot);
            }

            this.privateBossLootDropped.clear(client.authentication);
         } else {
            this.privateBossLootDropped.addAll(client.authentication, drops);
            drops.clear();
         }
      }
   }

   @Override
   public int getLevelMobDropsPrivateLootPriority() {
      return Integer.MIN_VALUE;
   }
}
