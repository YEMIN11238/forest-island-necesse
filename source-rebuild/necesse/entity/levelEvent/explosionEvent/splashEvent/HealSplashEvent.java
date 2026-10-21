package necesse.entity.levelEvent.explosionEvent.splashEvent;

import java.awt.Color;
import java.util.stream.Stream;
import necesse.engine.util.GameUtils;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MobHealthChangeEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.gfx.ThemeColorRegistry;

public class HealSplashEvent extends SplashEvent {
   public HealSplashEvent() {
      this(0.0F, 0.0F, 96, new GameDamage(0.0F), 0.0F, null);
   }

   public HealSplashEvent(float x, float y, int range, GameDamage damage, float toolTier, Mob owner) {
      super(x, y, range, damage, false, toolTier, owner);
      this.destroysGrass = false;
   }

   @Override
   protected boolean canHitMob(Mob target) {
      return target.canBeHit(this);
   }

   @Override
   protected Stream<Mob> streamTargets() {
      return Stream.concat(
         this.level.entityManager.mobs.getInRegionByTileRange((int)this.x / 32, (int)this.y / 32, this.range / 32 + 2).stream(),
         GameUtils.streamServerClients(this.level).map(c -> c.playerMob)
      );
   }

   @Override
   protected void onMobWasHit(Mob target, float distance) {
      int heal = (int)(target.getMaxHealth() * 0.05F);
      if (!target.isPlayer && !target.isHuman) {
         heal = 10;
      }

      LevelEvent event = new MobHealthChangeEvent(target, heal);
      this.level.entityManager.addLevelEvent(event);
   }

   @Override
   protected Color getInnerSplashColor() {
      return ThemeColorRegistry.HEAL.getRandomColor();
   }

   @Override
   protected Color getOuterSplashColor() {
      return this.getInnerSplashColor().brighter().brighter().brighter();
   }
}
