package necesse.entity.mobs.friendly.critters.caveling;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.SoundSettings;
import necesse.engine.sound.SoundSettingsRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.HumanTexture;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobSpawnLocation;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.CritterRunAINode;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.critters.CritterMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.SingleRockSmall;
import necesse.level.maps.Level;
import necesse.level.maps.TilePosition;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.light.GameLight;

public class CavelingMob extends CritterMob {
   public static LootTable lootTable = new LootTable(new LootItemInterface() {
      @Override
      public void addPossibleLoot(LootList list, Object... extra) {
         CavelingMob self = LootTable.expectExtra(CavelingMob.class, extra, 0);
         if (self != null && self.item != null) {
            list.addCustom(self.item);
         }
      }

      @Override
      public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
         CavelingMob self = LootTable.expectExtra(CavelingMob.class, extra, 0);
         if (self != null && self.item != null) {
            if (self.preventLootMultiplier) {
               lootMultiplier = 1.0F;
            }

            int itemAmount = LootTable.getLootAmount(random, self.item.getAmount(), lootMultiplier);
            if (itemAmount > 0) {
               int stacks = Math.min(10, itemAmount);
               int amountPerStack = itemAmount / stacks;
               int stacksWithOneMore = itemAmount - amountPerStack * stacks;

               for (int i = 0; i < stacks; i++) {
                  int amountInStack = amountPerStack + (i < stacksWithOneMore ? 1 : 0);
                  list.add(self.item.copy(amountInStack));
               }
            }
         }
      }
   }, new ChanceLootItem(0.025F, "cavelingdisplay"));
   public HumanTexture texture;
   public Color popParticleColor;
   public String singleRockSmallStringID;
   public boolean isRock = true;
   public InventoryItem item;
   public boolean preventLootMultiplier;
   protected SoundPlayer runSoundPlayer;

   public CavelingMob(int health, int speed) {
      super(health);
      this.setSpeed(speed);
      this.setFriction(3.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-16, -40, 32, 50);
      this.swimMaskMove = 12;
      this.swimMaskOffset = 4;
      this.swimSinkOffset = 0;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (this.item != null) {
         SaveData itemSave = new SaveData("item");
         this.item.addSaveData(itemSave);
         save.addSaveData(itemSave);
      }

      save.addBoolean("preventLootMultiplier", this.preventLootMultiplier);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.item = InventoryItem.fromLoadData(save.getFirstLoadDataByName("item"));
      this.preventLootMultiplier = save.getBoolean("preventLootMultiplier", this.preventLootMultiplier, false);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      InventoryItem.addPacketContent(this.item, writer);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.item = InventoryItem.fromContentPacket(reader);
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new CritterRunAINode<>());
   }

   @Override
   public void changedRunning() {
      super.changedRunning();
      if (this.isClient()) {
         if (!this.isRunning() && this.runSoundPlayer != null) {
            this.runSoundPlayer.stop();
         } else {
            this.runSoundPlayer = SoundManager.playSound(SoundSettingsRegistry.smallFootsteps, this);
            if (this.runSoundPlayer != null) {
               this.runSoundPlayer.refreshLooping(0.5F);
            }
         }
      }
   }

   @Override
   public void dispose() {
      super.dispose();
      if (this.runSoundPlayer != null) {
         this.runSoundPlayer.stop();
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.tickIsRock();
      if (this.isRunning() && this.runSoundPlayer != null) {
         this.runSoundPlayer.refreshLooping(0.5F);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.tickIsRock();
   }

   public void tickIsRock() {
      boolean nextIsRock = !this.isAccelerating() && this.dx == 0.0F && this.dy == 0.0F;
      if (this.isRock != nextIsRock) {
         this.isRock = nextIsRock;
         if (this.isClient() && this.popParticleColor != null) {
            for (int i = 0; i < 20; i++) {
               int startHeight = GameRandom.globalRandom.getIntBetween(2, 10);
               this.getLevel()
                  .entityManager
                  .addParticle(
                     this.x + GameRandom.globalRandom.floatGaussian() * 8.0F,
                     this.y - 4.0F + GameRandom.globalRandom.floatGaussian() * 4.0F,
                     Particle.GType.IMPORTANT_COSMETIC
                  )
                  .movesFriction(GameRandom.globalRandom.floatGaussian() * 20.0F, GameRandom.globalRandom.floatGaussian() * 16.0F, 2.0F)
                  .color(this.popParticleColor)
                  .heightMoves(startHeight, startHeight + 20)
                  .lifeTime(1000);
            }
         }
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      HumanTexture texture = this.texture != null ? this.texture : MobRegistry.Textures.stoneCaveling;

      for (int i = 0; i < 6; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), texture.body, i, 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY), Particle.GType.IMPORTANT_COSMETIC
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
      int drawY = camera.getDrawY(y) - 48;
      int dir = this.getDir();
      HumanTexture texture = this.texture != null ? this.texture : MobRegistry.Textures.stoneCaveling;
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      boolean hasSpelunker = perspective != null && perspective.buffManager.getModifier(BuffModifiers.SPELUNKER);
      if (!hasSpelunker) {
         hasSpelunker = this.shouldEnemyTrackerGlow(perspective) && perspective != null && perspective.buffManager.getModifier(BuffModifiers.ENEMY_TRACKER);
      }

      boolean asRock = this.objectDrawOptions(list, light, hasSpelunker, drawX, drawY);
      if (!asRock) {
         Point sprite = this.getAnimSprite(x, y, dir);
         drawY += this.getBobbing(x, y);
         final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
         final TextureDrawOptionsEnd rightArmOptions = texture.rightArms
            .initDraw()
            .sprite(sprite.x, sprite.y, 64)
            .addMaskShader(swimMask)
            .startGlowOptions(this, this.getID())
            .light(light)
            .apply(hasSpelunker)
            .pos(drawX, drawY);
         final TextureDrawOptionsEnd bodyOptions = texture.body
            .initDraw()
            .sprite(sprite.x, sprite.y, 64)
            .addMaskShader(swimMask)
            .startGlowOptions(this, this.getID())
            .light(light)
            .apply(hasSpelunker)
            .pos(drawX, drawY);
         final TextureDrawOptionsEnd itemOptions;
         if (this.item == null) {
            itemOptions = null;
         } else {
            Color drawColor = this.item.item.getDrawColor(this.item, perspective);
            int itemBobbing = sprite.x != 1 && sprite.x != 3 ? 0 : 2;
            GameLight itemLight = hasSpelunker ? light.minLevelCopy(100.0F) : light;
            itemOptions = this.item
               .item
               .getItemSprite(this.item, perspective)
               .initDraw()
               .colorLight(drawColor, itemLight)
               .mirror(sprite.y < 2, false)
               .size(32)
               .posMiddle(drawX + 32, drawY + 16 + itemBobbing + swimMask.drawYOffset);
         }

         final TextureDrawOptionsEnd leftArmOptions = texture.leftArms
            .initDraw()
            .sprite(sprite.x, sprite.y, 64)
            .addMaskShader(swimMask)
            .startGlowOptions(this, this.getID())
            .light(light)
            .apply(hasSpelunker)
            .pos(drawX, drawY);
         list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
               swimMask.use();
               rightArmOptions.draw();
               bodyOptions.draw();
               swimMask.stop();
               if (itemOptions != null) {
                  itemOptions.draw();
               }

               swimMask.use();
               leftArmOptions.draw();
               swimMask.stop();
            }
         });
         TextureDrawOptions shadow = MobRegistry.Textures.caveling_shadow.initDraw().sprite(sprite.x, sprite.y, 64).light(light).pos(drawX, drawY);
         tileList.add(tm -> shadow.draw());
      }
   }

   protected boolean objectDrawOptions(List<MobDrawable> list, GameLight light, boolean hasSpelunker, int drawX, int drawY) {
      if (this.dx == 0.0F && this.dy == 0.0F && this.singleRockSmallStringID != null) {
         GameObject gameObject = ObjectRegistry.getObject(this.singleRockSmallStringID);
         if (gameObject instanceof SingleRockSmall) {
            GameTexture rockTexture = ((SingleRockSmall)gameObject).texture;
            int rockSprite = new GameRandom(this.getUniqueID()).nextInt(rockTexture.getWidth() / 32);
            final TextureDrawOptionsEnd drawOptions = rockTexture.initDraw()
               .sprite(rockSprite, 0, 32, rockTexture.getHeight())
               .startGlowOptions(this, this.getID())
               .light(light)
               .apply(hasSpelunker)
               .pos(drawX + 16, drawY + 16 - rockTexture.getHeight() + 32);
            list.add(new MobDrawable() {
               @Override
               public void draw(TickManager tickManager) {
                  drawOptions.draw();
               }
            });
            return true;
         }
      }

      return false;
   }

   @Override
   public int getRockSpeed() {
      return 10;
   }

   @Override
   public boolean isLavaImmune() {
      return true;
   }

   @Override
   public boolean isSlimeImmune() {
      return true;
   }

   @Override
   public int getTileWanderPriority(TilePosition pos, Biome baseBiome) {
      return 0;
   }

   @Override
   public MobSpawnLocation checkSpawnLocation(MobSpawnLocation location) {
      return location.checkNotSolidTile()
         .checkNotOnSurfaceInsideOnFloor()
         .checkNotLevelCollides()
         .checkTile((x, y) -> this.getLevel().getStaticLightLevelFloat(x, y) <= 50.0F);
   }

   public LootTable getCavelingDropsAsLootTable() {
      return new LootTable();
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return null;
   }

   @Override
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameResources.cavelingHurt);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.cavelingDeath);
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
