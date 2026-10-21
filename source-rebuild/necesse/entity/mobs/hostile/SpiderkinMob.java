package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.HumanTexture;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SpiderkinMob extends HostileMob {
   public static LootTable lootTable = new LootTable();
   public static GameDamage damage = new GameDamage(115.0F);
   protected HumanTexture texture;

   public SpiderkinMob() {
      this(500, 60, 30);
   }

   public SpiderkinMob(int health, int speed, int armor) {
      super(health);
      this.setSpeed(speed);
      this.setArmor(armor);
      this.setFriction(3.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-14, -41, 28, 48);
      this.swimMaskMove = 20;
      this.swimMaskOffset = -4;
      this.swimSinkOffset = -4;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new ConfusedCollisionPlayerChaserWandererAI<>(null, 384, damage, 100, 40000));
      if (this.texture == null) {
         this.texture = MobRegistry.Textures.spiderkin;
      }
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), this.texture.body, GameRandom.globalRandom.nextInt(5), 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      int drawX = camera.getDrawX(x) - 22 - 10;
      int drawY = camera.getDrawY(y) - 44 - 7;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      HumanDrawOptions humanDrawOptions = new HumanDrawOptions(level, this.texture)
         .sprite(sprite)
         .mask(swimMask)
         .dir(dir)
         .light(light)
         .applyEnemyTracker(this, perspective);
      humanDrawOptions.hatTexture(
         new HumanDrawOptions.HumanDrawOptionsGetter() {
            @Override
            public DrawOptions getDrawOptions(
               Level level,
               PlayerMob player,
               int dirx,
               int spriteX,
               int spriteY,
               int spriteRes,
               int drawXx,
               int drawYx,
               int width,
               int height,
               boolean mirrorX,
               boolean mirrorY,
               GameLight lightx,
               boolean hasGlowEffect,
               int glowHash,
               float alpha,
               MaskShaderOptions mask
            ) {
               return MobRegistry.Textures.spiderkin_light
                  .initDraw()
                  .sprite(spriteX, spriteY, spriteRes)
                  .startGlowOptions(level, glowHash)
                  .light(lightx.minLevelCopy(150.0F))
                  .apply(hasGlowEffect)
                  .alpha(alpha)
                  .size(width, height)
                  .mirror(mirrorX, mirrorY)
                  .addMaskShader(mask)
                  .pos(drawXx, drawYx);
            }
         },
         ArmorItem.HairDrawMode.NO_HAIR
      );
      final DrawOptions drawOptions = humanDrawOptions.pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }
}
