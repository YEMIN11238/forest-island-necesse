package necesse.entity.mobs.hostile.bosses;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.client.Client;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class FallenWizardGhostMob extends BossMob {
   public FallenWizardGhostMob() {
      super(100);
      this.setSpeed(60.0F);
      this.setFriction(3.0F);
      this.setArmor(40);
      this.setKnockbackModifier(0.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-22, -18, 44, 36);
      this.selectBox = new Rectangle(-19, -52, 38, 64);
   }

   public void moveToPos(int tileX, int tileY) {
      this.ai = new BehaviourTreeAI<>(this, new FallenWizardGhostMob.GhostAINode<>(tileX, tileY));
   }

   @Override
   public boolean canBeHit(Attacker attacker) {
      return false;
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return super.getLevelCollisionFilter().addFilter(tp -> tp.object().object.isWall || tp.object().object.isRock);
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
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
      int drawX = camera.getDrawX(x) - 48;
      int drawY = camera.getDrawY(y) - 75;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      HumanDrawOptions humanDrawOptions = new HumanDrawOptions(level, MobRegistry.Textures.fallenWizard)
         .sprite(sprite, 96)
         .size(96, 96)
         .alpha(0.7F)
         .dir(dir)
         .light(light)
         .applyEnemyTracker(this, perspective);
      final DrawOptions drawOptions = humanDrawOptions.pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.human_big_shadow;
      int res = shadowTexture.getHeight();
      int drawX = camera.getDrawX(x) - res / 2;
      int drawY = camera.getDrawY(y) - res / 2;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().sprite(this.getDir(), 0, res).light(light).pos(drawX, drawY);
   }

   @Override
   public boolean shouldDrawOnMap() {
      return true;
   }

   @Override
   public Rectangle drawOnMapBox(double tileScale, boolean isMinimap) {
      return new Rectangle(-12, -28, 24, 34);
   }

   @Override
   public GameTooltips getMapTooltips() {
      return new StringTooltips(this.getDisplayName() + " " + this.getHealth() + "/" + this.getMaxHealth());
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      super.drawOnMap(tickManager, client, x, y, tileScale, drawBounds, isMinimap);
      int drawX = x - 24;
      int drawY = y - 34;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(this.getDrawX(), this.getDrawY(), dir);
      new HumanDrawOptions(this.getLevel(), MobRegistry.Textures.fallenWizard).sprite(sprite, 96).alpha(0.5F).dir(dir).size(48, 48).draw(drawX, drawY);
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.SLOW, 0.0F).max(0.2F), new ModifierValue<>(BuffModifiers.ATTACK_MOVEMENT_MOD, 0.0F));
   }

   public static class GhostAINode<T extends Mob> extends MoveTaskAINode<T> {
      public final int tileX;
      public final int tileY;
      public boolean hasStartedMoving;

      public GhostAINode(int tileX, int tileY) {
         this.tileX = tileX;
         this.tileY = tileY;
      }

      @Override
      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         this.moveToTileTask(this.tileX, this.tileY, null, path -> {
            path.moveIfWithin(-1, -1, null);
            this.hasStartedMoving = true;
            return AINodeResult.SUCCESS;
         });
      }

      @Override
      public void init(T mob, Blackboard<T> blackboard) {
      }

      @Override
      public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
         if (this.hasStartedMoving && !blackboard.mover.isMoving()) {
            mob.remove();
         }

         return AINodeResult.SUCCESS;
      }
   }
}
