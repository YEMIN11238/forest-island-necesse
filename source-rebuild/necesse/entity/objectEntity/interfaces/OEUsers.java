package necesse.entity.objectEntity.interfaces;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketOEUseUpdate;
import necesse.engine.network.packet.PacketOEUseUpdateFull;
import necesse.engine.network.packet.PacketOEUseUpdateFullRequest;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.regionSystem.SubRegion;

public interface OEUsers {
   default OEUsers.Users constructUsersObject(long timeoutTime) {
      return new OEUsers.Users(timeoutTime);
   }

   OEUsers.Users getUsersObject();

   GameMessage getCanUseError(Mob var1);

   default void startUser(Mob mob) {
      Level level = mob.getLevel();
      if (!level.isClient()) {
         boolean lastInUse = this.isInUse();
         OEUsers.Users users = this.getUsersObject();
         int mobUniqueID = mob.getUniqueID();
         OEUsers.UserTime last = users.users.get(mobUniqueID);
         long time = mob.getWorldEntity().getTime();
         if (last == null) {
            OEUsers.UserTime userTime = new OEUsers.UserTime(time);
            users.users.put(mobUniqueID, userTime);
            if (level.isServer()) {
               level.getServer().network.sendToClientsWithEntity(new PacketOEUseUpdate(this, mobUniqueID, true), (ObjectEntity)this);
            }

            this.onUsageChanged(mob, true);
            this.updateUserPosition(mob);
            userTime.hasTriggeredMobUsageChanged = true;
            if (!lastInUse) {
               this.onIsInUseChanged(true);
            }
         } else {
            last.refreshTime = time;
         }
      }
   }

   default void stopUser(Mob mob) {
      Level level = mob.getLevel();
      if (level != null && !level.isClient()) {
         int mobUniqueID = mob.getUniqueID();
         OEUsers.UserTime last = this.getUsersObject().users.remove(mobUniqueID);
         if (last != null) {
            this.onUsageChanged(mob, false);
            if (level.isServer()) {
               level.getServer().network.sendToClientsWithEntity(new PacketOEUseUpdate(this, mobUniqueID, false), (ObjectEntity)this);
            }

            if (!this.isInUse()) {
               this.onIsInUseChanged(false);
            }
         }
      }
   }

   default boolean isMobUsing(Mob mob) {
      OEUsers.Users users = this.getUsersObject();
      return users.users.containsKey(mob.getUniqueID());
   }

   default boolean isClientUsing(ServerClient client) {
      return client.playerMob != null && this.isMobUsing(client.playerMob);
   }

   default boolean isInUse() {
      return !this.getUsersObject().users.isEmpty();
   }

   default int getTotalUsers() {
      return this.getUsersObject().users.size();
   }

   default Collection<Integer> getUserUniqueIDs() {
      return this.getUsersObject().users.keySet();
   }

   default Stream<Mob> streamUsers(Level level) {
      return this.getUserUniqueIDs().stream().map(mobUniqueID -> GameUtils.getLevelMob(mobUniqueID, level, false)).filter(Objects::nonNull);
   }

   default void submitUpdatePacket(ObjectEntity entity, PacketOEUseUpdateFull packet) {
      this.getUsersObject().readUsersSpawnPacket(new PacketReader(packet.content), entity);
   }

   default void submitUpdatePacket(ObjectEntity entity, PacketOEUseUpdate packet) {
      Level level = entity.getLevel();
      boolean lastInUse = this.isInUse();
      OEUsers.Users users = this.getUsersObject();
      if (packet.isUsing) {
         OEUsers.UserTime userTime = new OEUsers.UserTime(level.getWorldEntity().getTime());
         users.users.put(packet.mobUniqueID, userTime);
         Mob mob = GameUtils.getLevelMob(packet.mobUniqueID, level, false);
         if (mob != null && mob.getLevel() != null) {
            this.onUsageChanged(mob, true);
            this.updateUserPosition(mob);
            userTime.hasTriggeredMobUsageChanged = true;
         }
      } else {
         users.users.remove(packet.mobUniqueID);
         Mob mob = GameUtils.getLevelMob(packet.mobUniqueID, level, false);
         if (mob != null && mob.getLevel() != null) {
            this.onUsageChanged(mob, false);
         } else {
            this.onUnknownUsageStopped(packet.mobUniqueID);
         }
      }

      boolean newIsInUse = this.isInUse();
      if (lastInUse != newIsInUse) {
         this.onIsInUseChanged(newIsInUse);
      }

      if (packet.totalUsers != this.getUsersObject().users.size() && entity.isClient()) {
         entity.getLevel().getClient().network.sendPacket(new PacketOEUseUpdateFullRequest(this));
      }
   }

   default void onUsageChanged(Mob mob, boolean using) {
      if (this instanceof ObjectEntity) {
         ObjectEntity objectEntity = (ObjectEntity)this;
         if (objectEntity.isClient()) {
            objectEntity.getObject()
               .playInteractSound(objectEntity.getLevel(), mob, objectEntity.tileX, objectEntity.tileY, this.getTotalUsers() == (using ? 1 : 0), !using);
         }
      }
   }

   void onIsInUseChanged(boolean var1);

   default void onUnknownUsageStopped(int uniqueID) {
   }

   default void updateUserPosition(Mob mob) {
   }

   default void updateUserToExitPos(Mob mob, float dirX, float dirY) {
   }

   static Point findExitPos(LevelObject levelObject, Mob mob, float dirX, float dirY) {
      return findExitPos(levelObject, mob, dirX, dirY, null);
   }

   static Point findExitPos(LevelObject levelObject, Mob mob, float dirX, float dirY, Function<Comparator<Point>, Comparator<Point>> comparatorModifier) {
      ArrayList<Point> possibleSpawns = new ArrayList<>();

      for (Point tile : levelObject.getMultiTile().getAdjacentTiles(levelObject.tileX, levelObject.tileY, true)) {
         int posX = tile.x * 32 + 16;
         int posY = tile.y * 32 + 16;
         if (!mob.collidesWith(levelObject.level, posX, posY)) {
            possibleSpawns.add(new Point(posX, posY));
         }
      }

      Float distancePoint = new Float(levelObject.tileX * 32 + 16 + dirX * 16.0F, levelObject.tileY * 32 + 16 + dirY * 16.0F);
      Comparator<Point> comparator = Comparator.comparingDouble(p -> p.distance(distancePoint.x, distancePoint.y));
      if (comparatorModifier != null) {
         comparator = comparatorModifier.apply(comparator);
      }

      Comparator<Point> regionSizeComparator = Comparator.comparingInt(p -> {
         int tileX = GameMath.getTileCoordinate(p.x);
         int tileY = GameMath.getTileCoordinate(p.y);
         SubRegion sr = levelObject.level.regionManager.getSubRegionByTile(tileX, tileY);
         if (sr != null && !sr.getType().isSolid) {
            int sameConnectedRegionSize = getConnectedRegionSize(sr, 100, sr1 -> !sr1.getType().isSolid);
            return 100 - sameConnectedRegionSize;
         } else {
            return 200;
         }
      });
      Comparator<Point> finalComparator;
      if (dirX == 0.0F && dirY == 0.0F) {
         finalComparator = regionSizeComparator.thenComparing(comparator);
      } else {
         finalComparator = comparator.thenComparing(regionSizeComparator);
      }

      return possibleSpawns.stream().min(finalComparator).orElse(null);
   }

   static int getConnectedRegionSize(SubRegion start, int maxSize, Predicate<SubRegion> validCheck) {
      return getConnectedRegionSize(new HashSet<>(), start, 0, maxSize, validCheck);
   }

   static int getConnectedRegionSize(HashSet<SubRegion> set, SubRegion current, int currentSize, int maxSize, Predicate<SubRegion> validCheck) {
      if (set.contains(current)) {
         return currentSize;
      }

      currentSize += current.size();
      if (currentSize >= maxSize) {
         return maxSize;
      }

      set.add(current);

      for (SubRegion adjacentRegion : current.getAdjacentRegions()) {
         if (validCheck.test(adjacentRegion)) {
            currentSize = getConnectedRegionSize(set, adjacentRegion, currentSize, maxSize, validCheck);
            if (currentSize >= maxSize) {
               return maxSize;
            }
         }
      }

      return currentSize;
   }

   class UserTime {
      public final long startUsageTime;
      public long refreshTime;
      public boolean hasTriggeredMobUsageChanged;

      public UserTime(long usageTime) {
         this.startUsageTime = this.refreshTime;
         this.refreshTime = usageTime;
      }
   }

   class Users {
      private final HashMap<Integer, OEUsers.UserTime> users = new HashMap<>();
      private final long timeoutTime;

      public Users(long timeoutTime) {
         this.timeoutTime = timeoutTime;
      }

      public void serverTick(ObjectEntity entity) {
         if (this.timeoutTime > -1L && !this.users.isEmpty()) {
            Level level = entity.getLevel();
            OEUsers oeUsers = (OEUsers)entity;
            long time = entity.getWorldEntity().getTime();
            LinkedList<Integer> removes = new LinkedList<>();

            for (Entry<Integer, OEUsers.UserTime> e : this.users.entrySet()) {
               OEUsers.UserTime user = e.getValue();
               if (user.refreshTime + this.timeoutTime < time) {
                  removes.add(e.getKey());
               }
            }

            for (int mobUniqueID : removes) {
               this.users.remove(mobUniqueID);
               Mob mob = GameUtils.getLevelMob(mobUniqueID, level, false);
               if (mob != null) {
                  oeUsers.onUsageChanged(mob, false);
               } else {
                  oeUsers.onUnknownUsageStopped(mobUniqueID);
               }

               if (level.isServer()) {
                  level.getServer().network.sendToClientsWithEntity(new PacketOEUseUpdate(oeUsers, mobUniqueID, false), entity);
               }
            }

            if (!removes.isEmpty() && !oeUsers.isInUse()) {
               oeUsers.onIsInUseChanged(false);
            }
         }
      }

      public void clientTick(ObjectEntity entity) {
         for (Entry<Integer, OEUsers.UserTime> e : this.users.entrySet()) {
            OEUsers.UserTime userTime = e.getValue();
            if (!userTime.hasTriggeredMobUsageChanged) {
               OEUsers oeUsers = (OEUsers)entity;
               Mob mob = GameUtils.getLevelMob(e.getKey(), entity.getLevel(), false);
               if (mob != null) {
                  oeUsers.onUsageChanged(mob, true);
                  oeUsers.updateUserPosition(mob);
                  userTime.hasTriggeredMobUsageChanged = true;
               }
            }
         }
      }

      public void writeUsersSpawnPacket(PacketWriter writer) {
         writer.putNextShortUnsigned(this.users.size());

         for (int mobUniqueID : this.users.keySet()) {
            writer.putNextInt(mobUniqueID);
         }
      }

      public void readUsersSpawnPacket(PacketReader reader, ObjectEntity entity) {
         Level level = entity.getLevel();
         OEUsers oeUsers = (OEUsers)entity;
         boolean lastInUse = oeUsers.isInUse();
         int size = reader.getNextShortUnsigned();
         HashSet<Integer> userUniqueIDs = new HashSet<>();

         for (int i = 0; i < size; i++) {
            userUniqueIDs.add(reader.getNextInt());
         }

         LinkedList<Integer> removes = new LinkedList<>();

         for (int mobUniqueID : this.users.keySet()) {
            if (!userUniqueIDs.contains(mobUniqueID)) {
               removes.add(mobUniqueID);
            }
         }

         for (int mobUniqueID : removes) {
            this.users.remove(mobUniqueID);
            Mob mob = level.entityManager.mobs.get(mobUniqueID, false);
            if (mob != null) {
               oeUsers.onUsageChanged(mob, false);
            } else {
               oeUsers.onUnknownUsageStopped(mobUniqueID);
            }
         }

         for (int mobUniqueID : userUniqueIDs) {
            if (!this.users.containsKey(mobUniqueID)) {
               OEUsers.UserTime userTime = new OEUsers.UserTime(level.getWorldEntity().getTime());
               this.users.put(mobUniqueID, userTime);
               Mob mob = GameUtils.getLevelMob(mobUniqueID, level, false);
               if (mob != null) {
                  oeUsers.onUsageChanged(mob, true);
                  userTime.hasTriggeredMobUsageChanged = true;
               }
            }
         }

         boolean newIsInUse = oeUsers.isInUse();
         if (lastInUse != newIsInUse) {
            oeUsers.onIsInUseChanged(newIsInUse);
         }
      }

      public void onRemoved(ObjectEntity entity) {
         OEUsers oeUsers = (OEUsers)entity;
         Level level = entity.getLevel();

         for (Integer mobUniqueID : this.users.keySet()) {
            Mob mob = GameUtils.getLevelMob(mobUniqueID, level, false);
            if (mob == null) {
               oeUsers.onUnknownUsageStopped(mobUniqueID);
            } else {
               oeUsers.onUsageChanged(mob, false);
            }
         }

         if (!this.users.isEmpty()) {
            this.users.clear();
            oeUsers.onIsInUseChanged(false);
         }
      }
   }
}
