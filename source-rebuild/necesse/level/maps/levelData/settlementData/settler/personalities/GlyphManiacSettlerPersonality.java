package necesse.level.maps.levelData.settlementData.settler.personalities;

import java.awt.geom.Point2D.Float;
import java.util.Comparator;
import java.util.stream.Stream;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.RayLinkedList;
import necesse.entity.levelEvent.mobAbilityLevelEvent.AmethystGlyphEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.EmeraldGlyphEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.RubyGlyphEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.SapphireGlyphEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.TopazGlyphEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.projectile.Projectile;
import necesse.gfx.GameColor;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;

public class GlyphManiacSettlerPersonality extends SettlerPersonality {
   public static int START_SECONDS_COOLDOWN = 40;
   public static int PERCENT_MODIFIER_PER_QUALITY_OF_LIFE = 25;
   public long lastGlyphTime;

   public GlyphManiacSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("lastGlyphTime", this.lastGlyphTime);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.lastGlyphTime = save.getLong("lastGlyphTime", this.lastGlyphTime, false);
   }

   @Override
   public GameMessage getDescription() {
      return new LocalMessage("personalities", this.getStringID() + "_desc")
         .addReplacement("name", GameColor.ITEM_QUEST.getColorCode() + this.mob.getSettlerName() + GameColor.NO_COLOR.getColorCode())
         .addReplacement("seconds", GameColor.GREEN.getColorCode() + START_SECONDS_COOLDOWN + GameColor.NO_COLOR.getColorCode())
         .addReplacement("percent", GameColor.GREEN.getColorCode() + PERCENT_MODIFIER_PER_QUALITY_OF_LIFE + "%" + GameColor.NO_COLOR.getColorCode());
   }

   @Override
   public GameTooltips getTooltip() {
      ListGameTooltips list = new ListGameTooltips();
      list.add(super.getTooltip());
      GameMessage effectMessage = new GameMessageBuilder()
         .append(GameColor.GREEN.getColorCode())
         .append(new LocalMessage("personalities", "glyphmaniac_effect", "value", this.getCurrentFrequencySeconds()))
         .append(GameColor.NO_COLOR.getColorCode());
      list.add(new SpacerGameTooltip(10));
      list.add(new LocalMessage("personalities", "glyphmaniac_current"));
      list.add(effectMessage);
      return list;
   }

   @Override
   public void gameTick() {
      super.gameTick();
      if ((this.mob.isInCombat() || this.mob.adventureParty.isInAdventureParty())
         && this.mob.getTime() > this.lastGlyphTime + this.getCurrentFrequencySeconds() * 1000L) {
         this.lastGlyphTime = this.mob.getTime();
         Mob targetChosen = this.getGlyphTarget();
         if (targetChosen != null) {
            Float targetPos = Projectile.getPredictedTargetPos(targetChosen, this.mob.x, this.mob.y, 1000.0F, -10.0F);
            this.spawnGlyph(targetPos);
         }
      }
   }

   private Mob getGlyphTarget() {
      int checkForMobsRange = 384;
      return Stream.concat(
            this.mob
               .getLevel()
               .entityManager
               .mobs
               .streamInRegionsInRange(this.mob.x, this.mob.y, checkForMobsRange)
               .filter(m -> m.isHuman)
               .map(m -> (HumanMob)m)
               .filter(hm -> hm.isFriendlyHuman(this.mob)),
            this.mob
               .getLevel()
               .entityManager
               .players
               .streamInRegionsInRange(this.mob.x, this.mob.y, checkForMobsRange)
               .filter(p -> !p.canBeTargeted(this.mob, this.mob.getPvPOwner()))
         )
         .filter(m -> m.getDistance(this.mob) <= checkForMobsRange)
         .min(Comparator.comparing(rec$ -> rec$.getHealthPercent()))
         .orElse(this.mob);
   }

   public int getCurrentFrequencySeconds() {
      float modifier = 1.0F / (1.0F + PERCENT_MODIFIER_PER_QUALITY_OF_LIFE * this.mob.getSettlerQualityOfLife() / 100.0F);
      return (int)(START_SECONDS_COOLDOWN * modifier);
   }

   private void spawnGlyph(Float targetPos) {
      GameRandom random = GameRandom.globalRandom;
      Float glyphPoint = this.getGlyphPoint(this.mob.getLevel(), targetPos, this.mob);
      random.runOneOf(() -> {
         SapphireGlyphEvent sapphire = new SapphireGlyphEvent(this.mob, (int)glyphPoint.x, (int)glyphPoint.y, random, 0);
         this.mob.addAndSendAttackerLevelEvent(sapphire);
      }, () -> {
         AmethystGlyphEvent amethyst = new AmethystGlyphEvent(this.mob, (int)glyphPoint.x, (int)glyphPoint.y, random);
         this.mob.addAndSendAttackerLevelEvent(amethyst);
      }, () -> {
         RubyGlyphEvent ruby = new RubyGlyphEvent(this.mob, random);
         this.mob.addAndSendAttackerLevelEvent(ruby);
      }, () -> {
         TopazGlyphEvent topaz = new TopazGlyphEvent(this.mob, (int)glyphPoint.x, (int)glyphPoint.y, random, 0);
         this.mob.addAndSendAttackerLevelEvent(topaz);
      }, () -> {
         EmeraldGlyphEvent emerald = new EmeraldGlyphEvent(this.mob, (int)glyphPoint.x, (int)glyphPoint.y, random);
         this.mob.addAndSendAttackerLevelEvent(emerald);
      });
   }

   public Float getGlyphPoint(Level level, Float targetPoint, Mob mob) {
      Float normalizedVector = GameMath.normalize(targetPoint.x - mob.x, targetPoint.y - mob.y);
      RayLinkedList<LevelObjectHit> hits = GameUtils.castRay(
         level,
         mob.x,
         mob.y,
         normalizedVector.x,
         normalizedVector.y,
         targetPoint.distance(mob.x, mob.y),
         0,
         new CollisionFilter().projectileCollision().addFilter(tp -> tp.object().object.isWall || tp.object().object.isRock)
      );
      return !hits.isEmpty() ? new Float(mob.x, mob.y) : targetPoint;
   }
}
