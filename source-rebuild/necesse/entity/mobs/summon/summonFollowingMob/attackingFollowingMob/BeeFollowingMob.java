package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerFollowerCollisionChaserAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.mobMovement.MobMovement;
import necesse.entity.mobs.mobMovement.MobMovementCharge;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class BeeFollowingMob extends AttackingFollowingMob {
   protected final int randomAnimationOffset;

   public BeeFollowingMob() {
      super(10);
      this.setSpeed(120.0F);
      this.setFriction(0.75F);
      this.setKnockbackModifier(0.0F);
      this.moveAccuracy = 30;
      this.collision = new Rectangle(-9, -7, 18, 14);
      this.selectBox = new Rectangle(-20, -34, 40, 32);
      this.hitBox = new Rectangle(-13, -16, 26, 30);
      this.randomAnimationOffset = GameRandom.globalRandom.nextInt(900);
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new PlayerFollowerCollisionChaserAI<>(576, this.summonDamage, 20, 800, 640, 0), new AIMover() {
         @Override
         public MobMovement getTargetMovement(Mob targetMob, boolean targetStopWhenColliding) {
            return new MobMovementCharge(targetMob, 20.0F, targetStopWhenColliding);
         }
      });
   }

   @Override
   public int getFlyingHeight() {
      return 20;
   }

   @Override
   public boolean canPushMob(Mob other) {
      return other instanceof BeeFollowingMob;
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
      int drawX = camera.getDrawX(x) - 16;
      int drawY = camera.getDrawY(y) - 22;
      int dir = this.getDir();
      int animationTime = 1000;
      long time = level.getTime();
      Point sprite = this.getAnimSprite(x, y, dir);
      TextureDrawOptions shadow = MobRegistry.Textures.honeyBee.shadow.initDraw().sprite(0, dir, 32).light(light).pos(drawX, drawY);
      tileList.add(tm -> shadow.draw());
      float bobbingFloat = GameUtils.getBobbing(time + this.randomAnimationOffset, animationTime);
      drawY -= 6;
      drawY += (int)(bobbingFloat * 5.0F);
      final DrawOptions options = MobRegistry.Textures.bee
         .initDraw()
         .sprite(sprite.x, sprite.y, 32)
         .startGlowOptions(level, this.getID())
         .light(light)
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
   public Point getAnimSprite(int x, int y, int dir) {
      long time = this.getTime();
      time += new GameRandom(this.getUniqueID()).nextInt(200);
      return new Point(GameUtils.getAnim(time, 2, 200), dir);
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.concat(super.getDefaultModifiers(), Stream.of(new ModifierValue<>(BuffModifiers.BOUNCY, true)));
   }

   @Override
   public int stoppingDistance(float friction, float currentSpeed) {
      return 0;
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return DeathMessageTable.fromRange("bee", 3);
   }
}
