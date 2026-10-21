package necesse.entity.mobs;

import java.util.List;
import java.util.stream.Collectors;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameUtils;
import necesse.level.maps.Level;

public class MobHealthScaling {
   private final Mob owner;
   private final float playerPercentStart;
   private final float playerPercentDec;
   private final float partyPercentStart;
   private final float partyPercentDec;
   private int startMaxHealth;
   private int healthIncrease;
   private int lastPlayers;
   private int lastPartyMembers;
   public static final int PLAYER_AFFECTS_HEALTH_WITHIN_TILE_RANGE = 50;

   public MobHealthScaling(Mob owner, float playerPercentStart, float playerPercentDecreasePer, float partyPercentStart, float partyPercentDecreasePer) {
      this.owner = owner;
      this.startMaxHealth = owner.getMaxHealthFlat();
      this.playerPercentStart = playerPercentStart;
      this.playerPercentDec = playerPercentDecreasePer;
      this.partyPercentStart = partyPercentStart;
      this.partyPercentDec = partyPercentDecreasePer;
   }

   public MobHealthScaling(Mob owner) {
      this(owner, 0.8F, 0.04F, 0.2F, 0.02F);
   }

   public void serverTick() {
      Level level = this.owner.getLevel();
      if (this.owner != null && level != null) {
         if (this.owner.isServer()) {
            MobHealthScaling.PlayerCount nextCount = getPlayerCount(this.owner);
            if (this.lastPlayers < nextCount.players
               || this.lastPartyMembers != nextCount.partyMembers
               || this.startMaxHealth != this.owner.getMaxHealthFlat()
               || nextCount.players != this.lastPlayers && this.owner.getHealth() >= this.owner.getMaxHealth() && !this.owner.isInCombat()) {
               if (this.owner.getHealth() < this.owner.getMaxHealth()) {
                  this.lastPlayers = Math.max(this.lastPlayers, nextCount.players);
               } else {
                  this.lastPlayers = nextCount.players;
               }

               this.lastPartyMembers = nextCount.partyMembers;
               this.startMaxHealth = this.owner.getMaxHealthFlat();
               this.updateHealthIncrease(true);
               this.owner.sendHealthPacket(false);
            }
         }
      }
   }

   public void setupHealthPacket(PacketWriter writer, boolean isFull) {
      writer.putNextByteUnsigned(this.lastPlayers);
      writer.putNextShortUnsigned(this.lastPartyMembers);
   }

   public void applyHealthPacket(PacketReader reader, boolean isFull) {
      this.lastPlayers = reader.getNextByteUnsigned();
      this.lastPartyMembers = reader.getNextShortUnsigned();
      this.updateHealthIncrease(true);
   }

   public void updatedMaxHealth() {
      this.startMaxHealth = this.owner.getMaxHealthFlat();
      this.updateHealthIncrease(false);
   }

   private void updateHealthIncrease(boolean updateHealthPercent) {
      float percHealth = this.owner.getHealthPercent();
      this.healthIncrease = Math.round(
         this.startMaxHealth
            * (
               GameUtils.getMultiplayerScaling(this.lastPlayers, this.playerPercentStart, this.playerPercentDec)
                  - 1.0F
                  + (GameUtils.getMultiplayerScaling(this.lastPartyMembers + 1, Integer.MAX_VALUE, this.partyPercentStart, this.partyPercentDec) - 1.0F)
            )
      );
      if (updateHealthPercent) {
         int health = (int)(this.owner.getMaxHealth() * percHealth);
         this.owner.setHealthHidden(health);
      }
   }

   public int getHealthIncrease() {
      return this.healthIncrease;
   }

   public static MobHealthScaling.PlayerCount getPlayerCount(Mob owner) {
      return getPlayerCount(owner.getLevel(), owner.getX(), owner.getY());
   }

   public static MobHealthScaling.PlayerCount getPlayerCount(Level level, float levelX, float levelY) {
      List<PlayerMob> players = level.entityManager
         .players
         .streamAreaTileRange((int)levelX, (int)levelY, 50)
         .filter(p -> !p.removed())
         .filter(playerx -> playerx.getDiagonalMoveDistance(levelX, levelY) <= 1600.0F)
         .collect(Collectors.toList());
      int nextPlayers = players.size();
      int nextPartyMembers = 0;

      for (PlayerMob player : players) {
         if (player.isServerClient()) {
            ServerClient client = player.getServerClient();
            synchronized (client.adventureParty.MOBS_LOCK) {
               nextPartyMembers += (int)client.adventureParty
                  .getMobs()
                  .stream()
                  .filter(m -> m.getLevel().isSamePlace(level))
                  .filter(m -> m.getDiagonalMoveDistance(levelX, levelY) <= 1600.0F)
                  .count();
            }
         }
      }

      return new MobHealthScaling.PlayerCount(nextPlayers, nextPartyMembers);
   }

   public static class PlayerCount {
      public int players;
      public int partyMembers;

      public PlayerCount(int players, int partyMembers) {
         this.players = players;
         this.partyMembers = partyMembers;
      }
   }
}
