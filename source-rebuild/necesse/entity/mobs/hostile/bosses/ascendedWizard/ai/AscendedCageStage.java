package necesse.entity.mobs.hostile.bosses.ascendedWizard.ai;

import java.awt.Point;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameMath;
import necesse.entity.levelEvent.explosionEvent.AscendedBombExplosionEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.level.maps.Level;
import necesse.level.maps.presets.AscendedCagePreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.PresetUtils;

public class AscendedCageStage<T extends AscendedWizardMob> extends AINode<T> implements AttackStageInterface<T> {
   private Point targetPoint;
   private int currentChargeTime;
   private int chargeTime;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      Mob target = blackboard.getObject(Mob.class, "currentTarget");
      this.targetPoint = new Point(target.getTileX() * 32 + 16, target.getTileY() * 32 + 16);
      Preset preset = new AscendedCagePreset();
      int placeTileX = target.getTileX() - 4;
      int placeTileY = target.getTileY() - 4;
      Level level = mob.getLevel();

      for (int presetTileX = 0; presetTileX < preset.width; presetTileX++) {
         int currentTileX = placeTileX + presetTileX;

         for (int presetTileY = 0; presetTileY < preset.height; presetTileY++) {
            int currentTileY = placeTileY + presetTileY;
            if (preset.getObject(presetTileX, presetTileY) > 0) {
               level.entityManager.destroyObjectOverride(0, currentTileX, currentTileY);
            }
         }
      }

      PresetUtils.placeAndSendPresetToClients(mob.getServer(), preset, level, placeTileX, placeTileY);
      mob.clearProblematicEntities();
      target.buffManager.removeBuff(BuffRegistry.Debuffs.ASCENDED_DARKNESS, true);
      this.currentChargeTime = 0;
      this.chargeTime = GameMath.lerp(mob.getHealthPercent(), 6000, 10000);
      mob.startCageAbility.runAndSend(this.targetPoint.x, this.targetPoint.y, this.chargeTime);
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (this.currentChargeTime > this.chargeTime) {
         AscendedBombExplosionEvent e = new AscendedBombExplosionEvent(
            this.targetPoint.x, this.targetPoint.y, 200, AscendedWizardMob.bombDamage, true, 10.0F, mob
         );
         mob.getLevel().entityManager.events.add(e);
         return AINodeResult.SUCCESS;
      } else {
         this.currentChargeTime += 50;
         return AINodeResult.RUNNING;
      }
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
