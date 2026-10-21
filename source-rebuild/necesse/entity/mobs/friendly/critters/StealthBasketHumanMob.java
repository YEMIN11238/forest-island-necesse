package necesse.entity.mobs.friendly.critters;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobSpawnLocation;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.leaves.CritterRunAINode;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.GameSkin;
import necesse.gfx.HumanLook;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.light.GameLight;

public class StealthBasketHumanMob extends CritterMob {
   public static LootTable lootTable = new LootTable(new LootItem("stealthbasket").preventLootMultiplier());
   public HumanLook look = new HumanLook();
   public InventoryItem helmet;

   public StealthBasketHumanMob() {
      super(500);
      this.setSpeed(50.0F);
      this.setFriction(3.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-16, -40, 32, 50);
      this.swimMaskMove = 12;
      this.swimMaskOffset = 4;
      this.swimSinkOffset = 0;
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public void init() {
      super.init();
      CritterRunAINode<StealthBasketHumanMob> critterRunAINode = new CritterRunAINode<StealthBasketHumanMob>() {
         protected void onRootSet(AINode<StealthBasketHumanMob> root, StealthBasketHumanMob mob, Blackboard<StealthBasketHumanMob> blackboard) {
            super.onRootSet(root, mob, blackboard);
            blackboard.onEvent(
               this.startRunEventType,
               event -> {
                  ArrayList<GameMessage> ouchMessage = HumanMob.getLocalMessages("mobmsg", "stealthbasketangry", 10);
                  StealthBasketHumanMob.this.getServer()
                     .network
                     .sendToClientsWithEntity(new PacketMobChat(mob.getUniqueID(), GameRandom.globalRandom.getOneOf(ouchMessage)), mob);
               }
            );
         }
      };
      critterRunAINode.runDistance = 40;
      this.ai = new BehaviourTreeAI<>(this, critterRunAINode);
      this.look = new HumanLook(GameRandom.globalRandom, false);
      this.helmet = new InventoryItem("stealthbasket");
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      if (this.isServer()) {
         ArrayList<GameMessage> deathMessage = HumanMob.getLocalMessages("mobmsg", "stealthbasketangry", 10);
         this.getServer().network.sendToClientsWithEntity(new PacketMobChat(this.getUniqueID(), GameRandom.globalRandom.getOneOf(deathMessage)), this);
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      GameSkin gameSkin = this.look.getGameSkin(true);

      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), gameSkin, GameRandom.globalRandom.nextInt(5), 8, 32, this.x, this.y, 10.0F, knockbackX, knockbackY),
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
      boolean inLiquid = this.inLiquid(x, y);
      if (inLiquid) {
         sprite.x = 0;
      }

      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      HumanDrawOptions humanOptions = new HumanDrawOptions(level, this.look, false)
         .sprite(sprite)
         .mask(swimMask)
         .dir(dir)
         .light(light)
         .applyEnemyTracker(this, perspective);
      if (inLiquid) {
         humanOptions.armSprite(2);
         humanOptions.mask(MobRegistry.Textures.runeboundboat_mask[sprite.y % 4], 0, -7);
      }

      if (this.helmet != null) {
         humanOptions.helmet(this.helmet);
      }

      final DrawOptions drawOptions = humanOptions.pos(drawX, drawY);
      final DrawOptions boat = inLiquid ? MobRegistry.Textures.runeboundBoat.initDraw().sprite(0, sprite.y, 64).light(light).pos(drawX, drawY + 7) : null;
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            if (boat != null) {
               boat.draw();
            }

            drawOptions.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   public MobSpawnLocation checkSpawnLocation(MobSpawnLocation location) {
      return location.checkNotSolidTile().checkNotOnSurfaceInsideOnFloor().checkNotLevelCollides();
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return null;
   }

   @Override
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameResources.hurt);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return null;
   }

   @Override
   public float getSpeedModifier() {
      float modifier = super.getSpeedModifier();
      if (this.getLevel() != null) {
         modifier *= this.getLevel().buffManager.getModifier(LevelModifiers.ENEMY_SPEED);
      }

      return modifier;
   }

   @Override
   public float getMaxHealthModifier() {
      float modifier = super.getMaxHealthModifier();
      if (this.getLevel() != null) {
         modifier *= this.getLevel().buffManager.getModifier(LevelModifiers.ENEMY_MAX_HEALTH);
      }

      return modifier;
   }
}
