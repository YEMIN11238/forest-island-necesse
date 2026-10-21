package necesse.engine.storyObjectives.utils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.level.maps.Level;

public abstract class MobMarkerObjectiveManager {
   public int maxMobsToKeepTrackOf;
   public int maxTileRange;
   protected long nextSearchTime;
   protected List<Mob> foundMobs = new ArrayList<>();

   public MobMarkerObjectiveManager(int maxMobsToKeepTrackOf, int maxTileRange) {
      this.maxMobsToKeepTrackOf = maxMobsToKeepTrackOf;
      this.maxTileRange = maxTileRange;
   }

   public void clientTick(PlayerMob player) {
      if (this.nextSearchTime <= player.getLocalTime()) {
         Level level = player.getLevel();
         if (level != null) {
            this.foundMobs.clear();
            this.foundMobs = player.getLevel()
               .entityManager
               .mobs
               .streamInRegionsInTileRange(player.getX(), player.getY(), this.maxTileRange)
               .filter(this::isValidMob)
               .sorted(Comparator.comparingDouble(player::getDistance))
               .limit(this.maxMobsToKeepTrackOf)
               .collect(Collectors.toList());
            this.nextSearchTime = player.getLocalTime() + (this.foundMobs.isEmpty() ? 1000 : 2000);
         }
      }
   }

   public abstract boolean isValidMob(Mob var1);

   public Mob getFirstFoundMob() {
      return this.foundMobs.isEmpty() ? null : this.foundMobs.get(0);
   }

   public List<Mob> getFoundMobs() {
      return this.foundMobs;
   }

   public void clearFoundMobs() {
      this.foundMobs.clear();
   }

   public DrawOptions getArrowHudDrawOptions(GameMessage foundMessage, GameMessage nonFoundMessage, GameCamera camera, PlayerMob perspective) {
      DrawOptionsList drawOptions = new DrawOptionsList();

      for (Mob mob : this.getFoundMobs()) {
         Rectangle selectionBox = mob.getSelectBox();
         drawOptions.add(
            StoryObjective.getLevelTextDrawOptions(foundMessage, selectionBox.x + selectionBox.width / 2, selectionBox.y, camera, perspective, true)
         );
      }

      if (drawOptions.isEmpty() && nonFoundMessage != null) {
         drawOptions.add(
            StoryObjective.getLevelTextDrawOptions(nonFoundMessage, perspective.getDrawX(), perspective.getDrawY() - 48, camera, perspective, false)
         );
      }

      return drawOptions;
   }
}
