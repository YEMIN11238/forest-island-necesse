package necesse.entity.mobs.friendly.critters;

import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobSpawnLocation;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.IdleAnimationAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.CritterAI;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class FrogMob extends CritterJumpingMob {
   public static LootTable lootTable = new LootTable(new LootItem("frogleg"));
   protected long idleAnimTime;
   protected int ribbitCooldownSeconds = 9;
   public final EmptyMobAbility startIdleAnimationAbility;

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   public FrogMob() {
      this.setSpeed(20.0F);
      this.setFriction(2.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-16, -28, 32, 34);
      this.swimMaskMove = 12;
      this.swimMaskOffset = 0;
      this.swimSinkOffset = 0;
      this.startIdleAnimationAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            FrogMob.this.idleAnimTime = FrogMob.this.getWorldEntity().getLocalTime();
            if (FrogMob.this.isClient()) {
               FrogMob.this.playAmbientSound();
            }
         }
      });
      this.shouldPlayAmbience = false;
   }

   @Override
   public void init() {
      super.init();
      CritterAI<FrogMob> critterAI = new CritterAI<>();
      critterAI.addChildFirst(new IdleAnimationAINode<FrogMob>() {
         @Override
         public int getIdleAnimationCooldown(GameRandom random) {
            return random.getIntBetween(20 * FrogMob.this.ribbitCooldownSeconds, 20 * FrogMob.this.ribbitCooldownSeconds + 5);
         }

         public void runIdleAnimation(FrogMob mob) {
            mob.startIdleAnimationAbility.runAndSend();
         }
      });
      this.ai = new BehaviourTreeAI<>(this, critterAI);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.frog.body, 12, i, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      int drawX = camera.getDrawX(x) - 31;
      int drawY = camera.getDrawY(y) - 54;
      int dir = this.getDir();
      boolean inLiquid = this.inLiquid(x, y);
      int sprite;
      if (inLiquid) {
         sprite = 5;
      } else {
         sprite = this.getJumpAnimationFrame(5);
      }

      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      int dirSprite = dir;
      int shadowSprite = sprite;
      if (!this.isAccelerating() && sprite == 0) {
         long idleAnimTimePassed = this.getWorldEntity().getLocalTime() - this.idleAnimTime;
         if (idleAnimTimePassed <= 500L) {
            dirSprite += 4;
            sprite = (int)(idleAnimTimePassed / 100L);
         }
      }

      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = MobRegistry.Textures.frog
         .body
         .initDraw()
         .sprite(sprite, dirSprite, 64)
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
      TextureDrawOptions shadow = MobRegistry.Textures.frog.shadow.initDraw().sprite(shadowSprite, dir, 64).light(light).pos(drawX, drawY);
      tileList.add(tm -> shadow.draw());
   }

   @Override
   public MobSpawnLocation checkSpawnLocation(MobSpawnLocation location) {
      return super.checkSpawnLocation(location)
         .checkTile(
            (tileX, tileY) -> {
               int tileID = this.getLevel().getTileID(tileX, tileY);
               return tileID == TileRegistry.swampGrassID
                  || tileID == TileRegistry.overgrownSwampGrassID
                  || tileID == TileRegistry.mudID
                  || tileID == TileRegistry.swampRockID
                  || tileID == TileRegistry.deepSwampRockID;
            }
         );
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameRandom.globalRandom.getOneOf(GameResources.frogAmbient1, GameResources.frogAmbient2, GameResources.frogAmbient3))
         .volume(1.1F);
   }

   @Override
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameResources.frogHurt).volume(0.8F);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.frogDeath).volume(0.3F);
   }
}
