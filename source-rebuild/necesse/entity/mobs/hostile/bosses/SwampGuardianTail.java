package necesse.entity.mobs.hostile.bosses;

import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.WormMobHead;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SwampGuardianTail extends SwampGuardianBody {
   private SwampGuardianBody getNextBodyPart(int count) {
      SwampGuardianBody next = this.next;

      for (int i = 0; i < count && next.next != null; i++) {
         next = next.next;
      }

      return next;
   }

   @Override
   protected void addDrawables(
      List<MobDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      Level level,
      int x,
      int y,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      if (this.isVisible()) {
         GameLight light = level.getLightLevel(this);
         int drawX = camera.getDrawX(x) - 48;
         int drawY = camera.getDrawY(y);
         float tailAngle = 0.0F;
         SwampGuardianBody next = this.getNextBodyPart(2);
         if (next != null) {
            tailAngle = GameMath.fixAngle(GameMath.getAngle(new Float(next.x - x, next.y - y)) + 180.0F);
         }

         WormMobHead.addAngledDrawable(
            list,
            this,
            new GameSprite(MobRegistry.Textures.swampGuardian, 1, 1, 96),
            MobRegistry.Textures.swampGuardian_mask,
            light,
            (int)this.height,
            tailAngle,
            drawX,
            drawY,
            64,
            perspective
         );
         this.addShadowDrawables(tileList, level, x, y, light, camera);
      }
   }
}
