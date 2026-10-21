package necesse.entity.mobs.friendly.critters.flyingbugs;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobTexture;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.FlyingBugCritterAI;
import necesse.entity.mobs.friendly.critters.PeacefulCritterMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public abstract class FlyingBugCritterMob extends PeacefulCritterMob {
   protected short flyingSeed;
   protected float flyingHeightDiff;
   protected float flyingHeight;
   protected int spriteRes;
   protected int spriteHeight;

   public FlyingBugCritterMob(int health) {
      super(health);
      this.setSpeed(11.0F);
      this.setFriction(1.0F);
      this.collision = new Rectangle(-8, -7, 16, 14);
      this.hitBox = new Rectangle(-10, -20, 20, 28);
      this.selectBox = new Rectangle(-13, -28, 26, 34);
      this.flyingSeed = (short)GameRandom.globalRandom.nextInt(32767);
      this.isHostile = false;
      this.isCritter = true;
      this.spriteRes = 64;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new FlyingBugCritterAI<>());
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextFloat(this.flyingHeightDiff);
      writer.putNextShort(this.flyingSeed);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.flyingHeightDiff = reader.getNextFloat();
      this.flyingSeed = reader.getNextShort();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.globalTick();
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.globalTick();
   }

   protected long getSeededTime() {
      return this.getWorldTime() + (this.flyingSeed & 4095);
   }

   protected void globalTick() {
      long seedFactor = Math.abs(this.flyingSeed % 3) + 1;
      int dirFactor = this.getDir() % 2 + 1;
      double sin = Math.sin(this.getSeededTime() * 0.001);
      double sinFast = Math.sin(this.getSeededTime() * 0.005 + seedFactor * 0.002);
      float flyingHeightChangeSpeed = dirFactor * (float)sin;
      this.flyingHeightDiff += flyingHeightChangeSpeed;
      this.flyingHeightDiff = GameMath.limit(this.flyingHeightDiff, -(this.spriteRes * 0.5F), this.spriteRes);
      this.flyingHeight = GameMath.limit(this.spriteRes / 2 + (int)this.flyingHeightDiff, this.spriteRes / 4, this.spriteRes) + (float)sinFast * 4.0F;
   }

   @Override
   protected void checkCollision() {
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return super.getLevelCollisionFilter().addFilter(tp -> tp.object().isSolid());
   }

   @Override
   public boolean canPushMob(Mob other) {
      return false;
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public boolean canBeTargeted(Mob attacker, NetworkClient attackerClient) {
      return true;
   }

   @Override
   public boolean isHealthBarVisible() {
      return false;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         int var10007 = this.spriteRes / 2;
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), this.getBugTexture().body, 4, i, var10007, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   protected abstract MobTexture getBugTexture();

   @Override
   public int getFlyingHeight() {
      return (int)this.flyingHeight;
   }

   @Override
   public void addDrawables(
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
      int flyHeight = this.getFlyingHeight();
      int drawX = camera.getDrawX(x);
      int drawY = camera.getDrawY(y);
      Point sprite = this.getWingFlapSpriteAnim();
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final DrawOptions options = this.getBugTexture()
         .body
         .initDraw()
         .sprite(sprite.x, sprite.y, this.spriteRes)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX - this.spriteRes / 2, drawY - (this.spriteRes / 2 + this.spriteRes / 4) - flyHeight);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
      TextureDrawOptions shadow = MobRegistry.Textures.bird_shadow.initDraw().sprite(0, 0, 32).light(light).pos(drawX - 17, drawY - 24);
      tileList.add(tm -> shadow.draw());
   }

   @Override
   public int getRockSpeed() {
      return 3;
   }

   protected Point getWingFlapSpriteAnim() {
      return new Point(GameUtils.getAnim(this.getSeededTime(), 2, 250 + this.getSeededWingFlapSpeedSkew()), this.getDir());
   }

   protected int getSeededWingFlapSpeedSkew() {
      return (this.flyingSeed & 15) * 4;
   }
}
