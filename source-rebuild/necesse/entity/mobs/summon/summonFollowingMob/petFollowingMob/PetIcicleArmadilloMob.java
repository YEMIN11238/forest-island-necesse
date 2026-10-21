package necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.PlayerFollowerAINode;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class PetIcicleArmadilloMob extends PetFollowingMob {
   protected int normalSpeed = 20;
   protected int ballSpeed = 200;
   protected boolean isBallForm = false;

   public PetIcicleArmadilloMob() {
      super(10);
      this.setSpeed(this.normalSpeed);
      this.setFriction(2.0F);
      this.moveAccuracy = 10;
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-16, -30, 32, 36);
      this.swimMaskMove = 8;
      this.swimMaskOffset = 0;
      this.swimSinkOffset = 0;
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextBoolean(this.isBallForm);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.isBallForm = reader.getNextBoolean();
      this.updateSpeed();
      this.buffManager.updateBuffs();
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new PlayerFollowerAINode<>(640, 32));
      this.updateSpeed();
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isBallForm) {
         Color mapColor = this.getLevel().getTile(this.getTileX(), this.getTileY()).mapColor;
         int colorOffset = GameRandom.globalRandom.getIntBetween(-20, 20);
         int mapColorR = GameMath.limit(mapColor.getRed() + colorOffset, 0, 255);
         int mapColorG = GameMath.limit(mapColor.getGreen() + colorOffset, 0, 255);
         int mapColorB = GameMath.limit(mapColor.getBlue() + colorOffset, 0, 255);
         Color particleColor = new Color(mapColorR, mapColorG, mapColorB);
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameRandom.globalRandom.getIntBetween(-10, 10), this.y + GameRandom.globalRandom.getIntBetween(-30, 0), Particle.GType.COSMETIC
            )
            .color(particleColor)
            .movesConstant(-this.dx / 2.0F, -this.dy / 2.0F)
            .lifeTime(500);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.checkFormTick();
   }

   protected void checkFormTick() {
      Mob attackOwner = this.getAttackOwner();
      if (attackOwner != null) {
         float distanceToOwner = GameMath.diamondDistance(this.x, this.y, attackOwner.x, attackOwner.y);
         if (distanceToOwner > 320.0F) {
            if (!this.isBallForm) {
               this.isBallForm = true;
               this.updateSpeed();
               this.buffManager.updateBuffs();
               this.sendMovementPacket(false);
            }
         } else if (distanceToOwner < 48.0F && this.isBallForm) {
            this.isBallForm = false;
            this.updateSpeed();
            this.buffManager.updateBuffs();
            this.sendMovementPacket(false);
         }
      }
   }

   protected void updateSpeed() {
      if (this.isBallForm) {
         this.accelerationMod = 0.1F;
         this.setSpeed(this.ballSpeed);
      } else {
         this.accelerationMod = 0.5F;
         this.setSpeed(this.normalSpeed);
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.icicleArmadillo, i, 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
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
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 52;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      int anim = 6 + GameUtils.getAnim(level.getTime(), 2, 100);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = MobRegistry.Textures.icicleArmadillo
         .initDraw()
         .sprite(this.isBallForm ? anim : sprite.x, sprite.y, 64)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            options.draw();
            swimMask.stop();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.icicleArmadillo_shadow;
      int height = shadowTexture.getHeight();
      int width = shadowTexture.getWidth();
      int drawX = camera.getDrawX(x) - width / 2;
      int drawY = camera.getDrawY(y) - height / 2;
      return shadowTexture.initDraw().sprite(0, 0, width, height).light(light).pos(drawX, drawY + 4);
   }

   @Override
   public int getRockSpeed() {
      return 10;
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.icicleArmadillo).volume(0.7F).basePitch(1.0F);
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return this.isBallForm
         ? Stream.concat(super.getDefaultModifiers(), Stream.of(new ModifierValue<>(BuffModifiers.BOUNCY, true)))
         : super.getDefaultModifiers();
   }
}
