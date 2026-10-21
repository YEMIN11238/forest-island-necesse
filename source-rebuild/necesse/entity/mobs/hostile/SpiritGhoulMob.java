package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.RuneSpiritPoolEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SpiritGhoulMob extends HostileMob {
   public static LootTable lootTable = new LootTable(
      ChanceLootItem.between(0.15F, "coin", 8, 22), ChanceLootItem.between(0.15F, "amber", 1, 2), ChanceLootItem.between(0.1F, "dryadsapling", 1, 2)
   );
   protected double distanceRanSinceLastPoolSpawn;

   public SpiritGhoulMob() {
      super(275);
      this.setSpeed(15.0F);
      this.setFriction(4.0F);
      this.setSwimSpeed(1.5F);
      this.setKnockbackModifier(0.5F);
      this.setArmor(20);
      this.moveAccuracy = 8;
      this.collision = new Rectangle(-12, -5, 24, 20);
      this.hitBox = new Rectangle(-16, -8, 32, 26);
      this.selectBox = new Rectangle(-20, -20, 40, 40);
      this.swimMaskMove = 16;
      this.swimMaskOffset = 0;
      this.swimSinkOffset = -4;
   }

   @Override
   public void init() {
      super.init();
      ConfusedCollisionPlayerChaserWandererAI<SpiritGhoulMob> chaserAINode = new ConfusedCollisionPlayerChaserWandererAI<>(
         null, 768, new GameDamage(52.0F), 50, 40000
      );
      this.ai = new BehaviourTreeAI<>(this, chaserAINode);
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
               new FleshParticle(this.getLevel(), MobRegistry.Textures.spiritGhoul.body, i, 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      int drawY = camera.getDrawY(y) - 36;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions body = MobRegistry.Textures.spiritGhoul
         .body
         .initDraw()
         .sprite(sprite.x, sprite.y, 64)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            body.draw();
            swimMask.stop();
         }
      });
   }

   @Override
   public int getSwimMaskMove() {
      return this.getDir() != 2 ? super.getSwimMaskMove() + 4 : super.getSwimMaskMove();
   }

   @Override
   public int getRockSpeed() {
      return 15;
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.FRICTION, 0.0F).min(0.75F));
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (!this.inLiquid()) {
         double distanceRan = this.getDistanceRan();
         double poolSpawnRunDistance = 16.0;
         if (distanceRan - this.distanceRanSinceLastPoolSpawn > poolSpawnRunDistance) {
            RuneSpiritPoolEvent event = new RuneSpiritPoolEvent(this, (int)this.x, (int)this.y, GameRandom.globalRandom, new GameDamage(38.0F), 4.0F);
            this.getLevel().entityManager.events.add(event);
            this.distanceRanSinceLastPoolSpawn = distanceRan;
         }
      }
   }
}
