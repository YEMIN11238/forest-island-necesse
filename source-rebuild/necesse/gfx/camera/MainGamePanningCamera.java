package necesse.gfx.camera;

import java.awt.geom.Point2D.Float;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.client.Client;
import necesse.engine.state.MainGame;
import necesse.engine.util.GameMath;

public class MainGamePanningCamera extends MainGameCamera {
   private float xBuffer;
   private float yBuffer;
   private float dx;
   private float dy;
   private float speed;

   public MainGamePanningCamera(int x, int y) {
      super(x, y);
   }

   @Override
   public void tickCamera(TickManager tickManager, MainGame mainGame, Client client) {
      this.xBuffer = this.xBuffer + this.dx * this.speed * tickManager.getDelta() / 250.0F;
      this.yBuffer = this.yBuffer + this.dy * this.speed * tickManager.getDelta() / 250.0F;

      while (this.xBuffer >= 1.0F || this.xBuffer <= -1.0F) {
         float v = Math.signum(this.xBuffer);
         this.x = (int)(this.x + v);
         this.xBuffer -= v;
      }

      while (this.yBuffer >= 1.0F || this.yBuffer <= -1.0F) {
         float v = Math.signum(this.yBuffer);
         this.y = (int)(this.y + v);
         this.yBuffer -= v;
      }
   }

   public void setDirection(float dx, float dy) {
      Float p = GameMath.normalize(dx, dy);
      this.dx = p.x;
      this.dy = p.y;
   }

   public float getSpeed() {
      return this.speed;
   }

   public void setSpeed(float speed) {
      this.speed = speed;
   }

   public float getXDir() {
      return this.dx;
   }

   public float getYDir() {
      return this.dy;
   }

   public void invertXDir() {
      this.dx = -this.dx;
   }

   public void invertYDir() {
      this.dy = -this.dy;
   }
}
