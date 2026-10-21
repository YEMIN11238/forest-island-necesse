package necesse.level.gameObject;

import java.awt.Color;
import necesse.engine.registries.TileRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.SimulatePriorityList;

public class GlowCoralObject extends GrassObject {
   public static double spreadChance = GameMath.getAverageSuccessRuns(2000.0);

   public GlowCoralObject() {
      super("glowcoral", 1);
      this.playerCanPlaceOnOrganic = false;
      this.grassValidTileIDs.add(TileRegistry.puddleCobble);
      this.mapColor = new Color(191, 90, 62);
      this.stackSize = 250;
      this.displayMapTooltip = true;
      this.weaveAmount = 0.05F;
      this.extraWeaveSpace = 32;
      this.randomYOffset = 3.0F;
      this.randomXOffset = 10.0F;
      this.lightHue = 343.0F;
      this.lightSat = 0.5F;
      this.lightLevel = 50;
      this.isLightTransparent = true;
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      super.tickEffect(level, layerID, tileX, tileY);
      if (level.getWorldEntity().isNight() || level.isCave) {
         GameRandom random = GameRandom.globalRandom;
         if (random.getChance(0.01F)) {
            int posX = tileX * 32 + random.nextInt(32);
            int posY = tileY * 32 + random.nextInt(32);
            level.entityManager
               .addParticle(posX, posY, Particle.GType.IMPORTANT_COSMETIC)
               .movesFriction(0.0F, -2.0F, -0.5F)
               .color(new Color(255, 118, 118))
               .sizeFades(11, 22)
               .minDrawLight(100)
               .lifeTime(1500)
               .height(16.0F);
         }
      }
   }

   public GrassSpreadOptions getSpreadOptions(Level level) {
      return GrassSpreadOptions.init(this, level).maxSpread(5, 9, 2);
   }

   @Override
   public void tick(Level level, int x, int y) {
      super.tick(level, x, y);
      if (level.isServer() && GameRandom.globalRandom.getChance(spreadChance)) {
         this.getSpreadOptions(level).tickSpread(x, y, true);
      }
   }

   @Override
   public void addSimulateLogic(Level level, int x, int y, long ticks, SimulatePriorityList list, boolean sendChanges) {
      super.addSimulateLogic(level, x, y, ticks, list, sendChanges);
      this.getSpreadOptions(level).addSimulateSpread(x, y, spreadChance, ticks, list, sendChanges);
   }

   @Override
   public void playDamageSound(Level level, int x, int y, boolean damageDone) {
      SoundManager.playSound(
         GameResources.waterblob, SoundEffect.effect(x * 32 + 16, y * 32 + 16).volume(2.5F).pitch(0.5F + GameRandom.globalRandom.getFloatBetween(-0.1F, 0.1F))
      );
      SoundManager.playSound(
         GameResources.crackdeath, SoundEffect.effect(x * 32 + 16, y * 32 + 16).volume(1.5F).pitch(0.5F + GameRandom.globalRandom.getFloatBetween(-0.1F, 0.1F))
      );
   }
}
