package necesse.level.maps.levelData;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.gameAreaSearch.TiledRegionSearch;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.hostile.bosses.TheCursedCroneMob;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

public class CursedCroneArenasLevelData extends SingleRegionBasedLevelData<CursedCroneArenasLevelData.ArenaData> implements RegionLevelDataComponent {
   protected void addRegionSaveData(SaveData save, CursedCroneArenasLevelData.ArenaData data) {
      data.addSaveData(save);
   }

   protected CursedCroneArenasLevelData.ArenaData loadRegionData(Region region, LoadData save) {
      return loadArenaData(save);
   }

   public CursedCroneArenasLevelData.ArenaData addArena(int tileX, int tileY) {
      CursedCroneArenasLevelData.ArenaData arena = new CursedCroneArenasLevelData.ArenaData(tileX, tileY);
      this.addArena(arena);
      return arena;
   }

   protected void addArena(CursedCroneArenasLevelData.ArenaData arena) {
      this.setDataInRegion(arena.regionX, arena.regionY, arena);
   }

   public boolean hasAnySpawnedCrone() {
      int mobID = MobRegistry.getMobID("thecursedcrone");

      for (CursedCroneArenasLevelData.ArenaData arena : this.getAllData()) {
         Mob found = this.level
            .entityManager
            .mobs
            .streamInRegionsInTileRange(arena.tileX * 32 + 16, arena.tileY * 32 + 16, 30)
            .filter(mob -> mob.getID() == mobID)
            .findFirst()
            .orElse(null);
         if (found != null) {
            return true;
         }
      }

      return false;
   }

   public CursedCroneArenasLevelData.ArenaData getArena(int tileX, int tileY) {
      int regionX = GameMath.getRegionCoordByTile(tileX);
      int regionY = GameMath.getRegionCoordByTile(tileY);
      return this.getDataInRegion(regionX, regionY);
   }

   public boolean isArenaTile(int tileX, int tileY) {
      CursedCroneArenasLevelData.ArenaData arena = this.getArena(tileX, tileY);
      return arena == null ? false : arena.tileX == tileX && arena.tileY == tileY;
   }

   public CursedCroneArenasLevelData.ArenaData getClosestArena(int tileX, int tileY, int maxTileRange) {
      CursedCroneArenasLevelData.ArenaData arena = new TiledRegionSearch(this.level, tileX, tileY, maxTileRange)
         .stream()
         .map(v -> this.getDataInRegion(v.x, v.y))
         .filter(Objects::nonNull)
         .findBestDistance(1, Comparator.comparingDouble(p -> GameMath.diagonalMoveDistance(p.tileX, p.tileY, tileX, tileY)))
         .orElse(null);
      return arena != null && GameMath.getExactDistance(tileX, tileY, arena.tileX, arena.tileY) > maxTileRange ? null : arena;
   }

   public static CursedCroneArenasLevelData getCursedCroneArenasData(Level level, boolean createIfNoneExists) {
      LevelData cursedCroneData = level.getLevelData("cursedcronearenatiles");
      if (cursedCroneData instanceof CursedCroneArenasLevelData) {
         return (CursedCroneArenasLevelData)cursedCroneData;
      }

      if (!createIfNoneExists) {
         return null;
      }

      CursedCroneArenasLevelData newCursedCroneData = new CursedCroneArenasLevelData();
      level.addLevelData("cursedcronearenatiles", newCursedCroneData);
      return newCursedCroneData;
   }

   public static GameMessage startFight(Level level, int tileX, int tileY) {
      CursedCroneArenasLevelData arenas = getCursedCroneArenasData(level, false);
      if (arenas != null) {
         CursedCroneArenasLevelData.ArenaData closestArena = arenas.getClosestArena(tileX, tileY, 10);
         if (closestArena != null) {
            Mob mob = MobRegistry.getMob("thecursedcrone", level);
            TheCursedCroneMob cursedCroneMob = (TheCursedCroneMob)mob;
            cursedCroneMob.setArenaData(closestArena);
            cursedCroneMob.resetPositionToArena();
            cursedCroneMob.startAnimation(TheCursedCroneMob.AnimationState.DEFAULT_FLYING, 500, true);
            cursedCroneMob.isHostile = true;
            level.entityManager.mobs.add(mob);
            level.getServer().network.sendToClientsWithEntity(new PacketChatMessage(new LocalMessage("misc", "bossawoke", "name", mob.getLocalization())), mob);
            if (level instanceof IncursionLevel) {
               ((IncursionLevel)level).onBossSummoned(mob);
            }

            return null;
         }
      }

      return new LocalMessage("itemtooltip", "spiriturnfail");
   }

   public static CursedCroneArenasLevelData.ArenaData loadArenaData(LoadData save) {
      Point centerTile = save.getPoint("centerTile", null);
      if (centerTile == null) {
         throw new LoadDataException("ArenaData centerTile is null");
      }

      CursedCroneArenasLevelData.ArenaData data = new CursedCroneArenasLevelData.ArenaData(centerTile.x, centerTile.y);
      data.spiritSkullTiles.addAll(save.getPointCollection("spiritSkullTiles", new ArrayList<>()));
      return data;
   }

   public static class ArenaData extends SingleRegionBasedLevelData.RegionData {
      public final int tileX;
      public final int tileY;
      public final ArrayList<Point> spiritSkullTiles = new ArrayList<>();

      protected ArenaData(int tileX, int tileY) {
         super(GameMath.getRegionCoordByTile(tileX), GameMath.getRegionCoordByTile(tileY));
         this.tileX = tileX;
         this.tileY = tileY;
      }

      protected SaveData getSaveData(String name) {
         SaveData save = new SaveData(name);
         this.addSaveData(save);
         return save;
      }

      protected void addSaveData(SaveData save) {
         save.addPoint("centerTile", new Point(this.tileX, this.tileY));
         save.addPointCollection("spiritSkullTiles", this.spiritSkullTiles);
      }
   }
}
