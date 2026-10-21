package necesse.engine.commands.serverCommands;

import java.awt.Point;
import java.util.Comparator;
import java.util.function.Function;
import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.MultiParameterHandler;
import necesse.engine.commands.parameterHandlers.ServerClientParameterHandler;
import necesse.engine.commands.parameterHandlers.StringParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketPlayerMovement;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.WorldDeathLocation;
import necesse.level.gameObject.RespawnObject;
import necesse.level.maps.Level;

public class TeleportServerCommand extends ModularChatCommand {
   public TeleportServerCommand(String name) {
      super(
         name,
         "Teleports player1 to player2 or other location",
         PermissionLevel.ADMIN,
         true,
         new CmdParameter("player1", new ServerClientParameterHandler(true, false), true),
         new CmdParameter(
            "player2/home/death/spawn/level",
            new MultiParameterHandler(new ServerClientParameterHandler(), new StringParameterHandler(null, "spawn", "home", "death"))
         )
      );
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      ServerClient from = (ServerClient)args[0];
      Object[] multi = (Object[])args[1];
      TeleportServerCommand.TeleportPos to = this.clientPos((ServerClient)multi[0]);
      String preset = (String)multi[1];
      if (from == null) {
         logs.add("Missing player to teleport");
      } else {
         if (to == null) {
            switch (preset) {
               case "home":
                  to = this.spawnPos(from, server);
                  break;
               case "death":
                  to = this.deathPos(from);
                  if (to == null) {
                     logs.add("Could not find death location");
                     return;
                  }
                  break;
               case "spawn":
                  to = new TeleportServerCommand.TeleportPos(
                     null,
                     "spawn",
                     server.world.worldEntity.spawnLevelIdentifier,
                     server.world.worldEntity.spawnTile.x * 32 + 16,
                     server.world.worldEntity.spawnTile.y * 32 + 16
                  );
                  break;
               default:
                  LevelIdentifier levelIdentifier = new LevelIdentifier(preset);
                  if (!server.world.levelExists(levelIdentifier)) {
                     logs.add("Could not find destination");
                     return;
                  }

                  to = new TeleportServerCommand.TeleportPos(null, preset, levelIdentifier, level -> {
                     int levelX = from.playerMob.getX();
                     int levelY = from.playerMob.getY();
                     int withinLevel = 128;
                     if (level.tileWidth > 0) {
                        levelX = GameMath.limit(levelX, withinLevel, level.tileWidth * 32 - withinLevel);
                     }

                     if (level.tileHeight > 0) {
                        levelY = GameMath.limit(levelY, withinLevel, level.tileHeight * 32 - withinLevel);
                     }

                     return new Point(levelX, levelY);
                  });
            }
         }

         if (from == to.client) {
            logs.add("Cannot teleport player to self");
         } else {
            from.playerMob.dx = 0.0F;
            from.playerMob.dy = 0.0F;
            logs.add("Teleported " + from.getName() + " to " + to.name);
            if (from.isSamePlace(to.levelIdentifier)) {
               Point pos = to.levelPosGetter.apply(from.getLevel());
               from.playerMob.setPos(pos.x, pos.y, true);
               from.playerMob.nextMovementTeleportBuffer();
               server.network.sendToClientsWithEntity(new PacketPlayerMovement(from, true), from.playerMob);
            } else {
               from.changeLevel(to.levelIdentifier, to.levelPosGetter, true);
            }
         }
      }
   }

   private TeleportServerCommand.TeleportPos clientPos(ServerClient client) {
      return client == null
         ? null
         : new TeleportServerCommand.TeleportPos(client, client.getName(), client.getLevelIdentifier(), client.playerMob.getX(), client.playerMob.getY());
   }

   private TeleportServerCommand.TeleportPos spawnPos(ServerClient client, Server server) {
      client.validateSpawnPoint(true);
      Point offset = new Point(16, 16);
      if (!client.isDefaultSpawnPoint()) {
         offset = RespawnObject.calculateSpawnOffset(server.world.getLevel(client.spawnLevelIdentifier), client.spawnTile.x, client.spawnTile.y, client);
      }

      return new TeleportServerCommand.TeleportPos(
         null, "spawn", client.spawnLevelIdentifier, client.spawnTile.x * 32 + offset.x, client.spawnTile.y * 32 + offset.y
      );
   }

   private TeleportServerCommand.TeleportPos deathPos(ServerClient client) {
      WorldDeathLocation deathLocation = client.streamDeathLocations().max(Comparator.comparingInt(d -> d.deathTime)).orElse(null);
      return deathLocation == null
         ? null
         : new TeleportServerCommand.TeleportPos(null, "recent death", deathLocation.levelIdentifier, deathLocation.x, deathLocation.y);
   }

   private static class TeleportPos {
      public final ServerClient client;
      public final String name;
      public final LevelIdentifier levelIdentifier;
      public final Function<Level, Point> levelPosGetter;

      private TeleportPos(ServerClient client, String name, LevelIdentifier levelIdentifier, Function<Level, Point> levelPosGetter) {
         this.client = client;
         this.name = name;
         this.levelIdentifier = levelIdentifier;
         this.levelPosGetter = levelPosGetter;
      }

      private TeleportPos(ServerClient client, String name, LevelIdentifier levelIdentifier, int levelX, int levelY) {
         this(client, name, levelIdentifier, level -> new Point(levelX, levelY));
      }
   }
}
