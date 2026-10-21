package necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.PlayerFollowerAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TileFinderFlyingAINode;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class WillOWispMob extends PetFollowingMob {
   public WillOWispMob() {
      super(10);
      this.setSpeed(80.0F);
      this.setFriction(2.0F);
      this.moveAccuracy = 10;
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-16, -40, 32, 50);
      this.swimMaskMove = 22;
      this.swimMaskOffset = 16;
      this.swimSinkOffset = -12;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new WillOWispMob.WillOWispAI<>(480, 64), new FlyingAIMover());
   }

   public float getDesiredHeight() {
      float perc = GameUtils.getAnimFloat(this.getLevel().getWorldEntity().getTime(), 3000);
      return GameMath.sin(perc * 360.0F) * 10.0F - 20.0F;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.getLevel()
         .entityManager
         .addParticle(
            this.x + GameRandom.globalRandom.getFloatBetween(-5.0F, 5.0F),
            this.y + GameRandom.globalRandom.getFloatBetween(-5.0F, 5.0F) + this.getDesiredHeight(),
            Particle.GType.IMPORTANT_COSMETIC
         )
         .movesConstant(0.0F, -10.0F)
         .ignoreLight(true)
         .givesLight(193.0F, 0.5F)
         .color(
            (options, lifeTime, timeAlive, lifePercent) -> options.color(
               new Color(
                  Math.max(36 - (int)(36.0F * lifePercent), 0), Math.max(174 - (int)(97.0F * lifePercent), 0), Math.max(214 - (int)(120.0F * lifePercent), 0)
               )
            )
         )
         .size((options, lifeTime, timeAlive, lifePercent) -> options.size((int)(20.0F * (1.0F - lifePercent)), (int)(20.0F * (1.0F - lifePercent))))
         .lifeTime(500);
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return null;
   }

   @Override
   public void playDeathSound() {
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
      super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      GameTexture texture = MobRegistry.Textures.willOWisp;
      int drawX = camera.getDrawX(x) - texture.getWidth() / 2;
      int drawY = camera.getDrawY(y) + (int)this.getDesiredHeight() - texture.getHeight() / 2;
      final DrawOptions options = texture.initDraw()
         .startGlowOptions(level, this.getID())
         .light(light.minLevelCopy(150.0F))
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
   }

   @Override
   public int getRockSpeed() {
      return 10;
   }

   static class WillOWispAI<T extends WillOWispMob> extends SelectorAINode<T> {
      public WillOWispAI(int teleportDistance, int stoppingDistance) {
         this.addChild(new TileFinderFlyingAINode<T>() {
            public void init(T mob, Blackboard<T> blackboard) {
               super.init(mob, blackboard);
               this.maxDistance = 20;
            }

            public boolean isValidTile(T mob, Blackboard<T> blackboard, int tileX, int tileY) {
               return mob.getLevel().getObject(tileX, tileY).isPressurePlate;
            }

            public Point getStartTile(T mob, Blackboard<T> blackboard) {
               Mob followingMob = mob.getFollowingMob();
               return followingMob != null ? new Point(followingMob.getTileX(), followingMob.getTileY()) : null;
            }
         });
         this.addChild(new PlayerFollowerAINode<>(teleportDistance, stoppingDistance));
      }
   }
}
