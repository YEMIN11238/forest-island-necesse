package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameRandom;
import necesse.engine.util.PointSetAbstract;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.levelEvent.actions.EmptyLevelEventAction;
import necesse.entity.levelEvent.actions.LevelEventAction;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.hostile.bosses.TheCursedCroneMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.chargeUpSpiritSkullParticle;
import necesse.entity.projectile.SpiritSkullProjectile;

public class CursedCroneSpiritSkullsEvent extends LevelEvent {
   protected Mob owner;
   protected ArrayList<Point> pointsToShootBetween = new ArrayList<>();
   protected int shootIntervalInMillis;
   protected long eventStartTime;
   protected long nextProjectileTime;
   protected CursedCroneSpiritSkullsEvent.FireProjectileAction fireProjectileAction = this.registerAction(
      new CursedCroneSpiritSkullsEvent.FireProjectileAction()
   );
   protected int keepAliveTimer;
   protected final EmptyLevelEventAction refreshAlive = this.registerAction(new EmptyLevelEventAction() {
      @Override
      protected void run() {
         CursedCroneSpiritSkullsEvent.this.keepAliveTimer = 0;
      }
   });

   public CursedCroneSpiritSkullsEvent() {
      super(true);
   }

   public CursedCroneSpiritSkullsEvent(Mob owner, ArrayList<Point> pointsToShootBetween, int shootIntervalInMillis) {
      this();
      this.owner = owner;
      this.pointsToShootBetween = pointsToShootBetween;
      this.shootIntervalInMillis = shootIntervalInMillis;
   }

   @Override
   public boolean isNetworkImportant() {
      return true;
   }

   @Override
   public void init() {
      super.init();
      this.eventStartTime = this.getTime();
      this.nextProjectileTime = this.getTime();
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.tickAliveTimer();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.tickAliveTimer();
      if (this.getTime() >= this.nextProjectileTime) {
         int startPointIndex = this.getRandomStartPointIndex();
         Point startPoint = this.pointsToShootBetween.get(startPointIndex);
         Point targetPoint = this.pointsToShootBetween.get(this.getRandomEndPointIndex(startPointIndex));
         this.fireProjectileAction.runAndSend(startPoint, targetPoint);
         this.nextProjectileTime = this.getTime() + this.shootIntervalInMillis;
      }
   }

   protected void tickAliveTimer() {
      this.keepAliveTimer++;
      if (this.keepAliveTimer >= 400) {
         this.over();
      }
   }

   public void refreshAliveTimer() {
      if (this.keepAliveTimer >= 200) {
         if (this.isServer()) {
            this.refreshAlive.runAndSend();
         }

         this.keepAliveTimer = 0;
      }
   }

   public void chargeUpToShoot(final Point startPos, final Point endPos) {
      if (this.isClient()) {
         Particle startPosParticle = new chargeUpSpiritSkullParticle(this.level, startPos.x, startPos.y, (long)(this.shootIntervalInMillis * 0.65F));
         this.level.entityManager.addParticle(startPosParticle, Particle.GType.CRITICAL);
      } else {
         this.level.entityManager.events.addHidden(new WaitForSecondsEvent(this.shootIntervalInMillis * 0.75F / 1000.0F) {
            @Override
            public void onWaitOver() {
               CursedCroneSpiritSkullsEvent.this.fireProjectile(startPos, endPos);
            }
         });
      }
   }

   public void fireProjectile(Point startPos, Point endPos) {
      if (!this.isClient()) {
         int distance = (int)startPos.distance(endPos);
         SpiritSkullProjectile spiritSkullProjectile = new SpiritSkullProjectile(
            this.getLevel(), this.owner, startPos.x, startPos.y, endPos.x, endPos.y, 50.0F, distance, TheCursedCroneMob.spiritSkullsDamage, 50
         );
         this.getLevel().entityManager.projectiles.add(spiritSkullProjectile);
      }
   }

   public int getRandomStartPointIndex() {
      return GameRandom.globalRandom.getIntBetween(0, this.pointsToShootBetween.size() - 1);
   }

   public int getRandomEndPointIndex(int startIndex) {
      int endPointIndex = GameRandom.globalRandom.getIntBetween(0, this.pointsToShootBetween.size() - 1);

      while (endPointIndex == startIndex) {
         endPointIndex = GameRandom.globalRandom.getIntBetween(0, this.pointsToShootBetween.size() - 1);
      }

      return endPointIndex;
   }

   @Override
   public PointSetAbstract<?> getRegionPositions() {
      return this.owner != null ? this.owner.getRegionPositions() : super.getRegionPositions();
   }

   @Override
   public Point getSaveToRegionPos() {
      return this.owner != null
         ? new Point(this.level.regionManager.getRegionCoordByTile(this.owner.getTileX()), this.level.regionManager.getRegionCoordByTile(this.owner.getTileY()))
         : super.getSaveToRegionPos();
   }

   public class FireProjectileAction extends LevelEventAction {
      protected void runAndSend(Point startPoint, Point targetPoint) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextInt(startPoint.x);
         writer.putNextInt(startPoint.y);
         writer.putNextInt(targetPoint.x);
         writer.putNextInt(targetPoint.y);
         this.runAndSendAction(content);
      }

      @Override
      public void executePacket(PacketReader reader) {
         Point startPoint = new Point(reader.getNextInt(), reader.getNextInt());
         Point endPoint = new Point(reader.getNextInt(), reader.getNextInt());
         CursedCroneSpiritSkullsEvent.this.chargeUpToShoot(startPoint, endPoint);
      }
   }
}
