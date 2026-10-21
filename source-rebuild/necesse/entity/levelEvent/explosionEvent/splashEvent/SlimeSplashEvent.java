package necesse.entity.levelEvent.explosionEvent.splashEvent;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.hostile.SwampSlimeMob;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.ToolItemSummonedMob;
import necesse.gfx.ThemeColorRegistry;
import necesse.inventory.item.toolItem.summonToolItem.SummonToolItem;

public class SlimeSplashEvent extends SplashEvent {
   public SlimeSplashEvent() {
      this(0.0F, 0.0F, 64, new GameDamage(0.0F), 0.0F, null);
   }

   public SlimeSplashEvent(float x, float y, int range, GameDamage damage, float toolTier, Mob owner) {
      super(x, y, range, damage, false, toolTier, owner);
   }

   @Override
   public void init() {
      super.init();
      if (!this.isClient() && this.ownerMob != null) {
         if (this.ownerMob instanceof ItemAttackerMob) {
            ItemAttackerMob attackerMob = (ItemAttackerMob)this.ownerMob;

            for (int i = 0; i < 5; i++) {
               var slime = (Mob & ToolItemSummonedMob)MobRegistry.getMob("playerpoisonslime", this.level);
               Mob castedMob = (Mob)slime;
               attackerMob.serverFollowersManager.addFollower("summonedmob", castedMob, FollowPosition.WALK_CLOSE, "summonedmob", 1.0F, 5, null, false);
               Float spawnPoint = SummonToolItem.findSpawnLocation(castedMob, castedMob.getLevel(), this.x, this.y);
               slime.updateDamage(this.damage);
               this.getLevel().entityManager.addMob(castedMob, spawnPoint.x, spawnPoint.y);
            }
         } else if (this.ownerMob.isHostile) {
            SwampSlimeMob swampSlimeMob = new SwampSlimeMob();
            this.getLevel().entityManager.addMob(swampSlimeMob, this.x, this.y);
         }
      }
   }

   @Override
   protected Color getInnerSplashColor() {
      return ThemeColorRegistry.SLIME.getRandomColor();
   }

   @Override
   protected Color getOuterSplashColor() {
      return this.getInnerSplashColor().brighter().brighter().brighter();
   }
}
