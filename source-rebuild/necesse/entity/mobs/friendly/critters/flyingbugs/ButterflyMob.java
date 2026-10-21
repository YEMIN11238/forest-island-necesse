package necesse.entity.mobs.friendly.critters.flyingbugs;

import java.awt.Point;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.seasons.GameSeasons;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.entity.mobs.MobSpawnLocation;
import necesse.entity.mobs.MobTexture;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class ButterflyMob extends SmallFlyingBugCritterMob {
   public ButterflyMob.SmallButterflyType butterflyType;
   protected boolean overrideButterflyType;

   public ButterflyMob() {
      this(
         GameRandom.globalRandom.getOneOf(ButterflyMob.SmallButterflyType.RED, ButterflyMob.SmallButterflyType.ORANGE, ButterflyMob.SmallButterflyType.YELLOW),
         true
      );
   }

   public ButterflyMob(ButterflyMob.SmallButterflyType butterflyType) {
      this(butterflyType, false);
   }

   public ButterflyMob(ButterflyMob.SmallButterflyType butterflyType, boolean overrideButterflyType) {
      super(2);
      this.butterflyType = butterflyType;
      this.overrideButterflyType = overrideButterflyType;
      this.ambientSoundCooldownMin = 500;
      this.ambientSoundCooldownMax = 1250;
   }

   @Override
   public void applyLoadData(LoadData save) {
      if (save.hasLoadDataByName("butterflyType")) {
         this.butterflyType = save.getEnum(ButterflyMob.SmallButterflyType.class, "butterflyType");
         this.overrideButterflyType = false;
      }

      super.applyLoadData(save);
   }

   @Override
   public void addSaveData(SaveData save) {
      if (!this.overrideButterflyType) {
         save.addEnum("butterflyType", this.butterflyType);
      }

      super.addSaveData(save);
   }

   @Override
   public LootTable getLootTable() {
      String itemStringID = this.butterflyType == null ? ButterflyMob.SmallButterflyType.RED.droppedItemStringID : this.butterflyType.droppedItemStringID;
      return new LootTable(new LootItem(itemStringID));
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextEnum(this.butterflyType);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.butterflyType = reader.getNextEnum(ButterflyMob.SmallButterflyType.class);
   }

   @Override
   public void init() {
      super.init();
      if (!this.isClient()) {
         if (this.overrideButterflyType) {
            if (this.getLevel().getBiome(this.getTileX(), this.getTileY()).equals(BiomeRegistry.SNOW)) {
               this.butterflyType = ButterflyMob.SmallButterflyType.BLUE;
               if (this.getLevel().getIdentifier().equals(LevelIdentifier.CAVE_IDENTIFIER)) {
                  this.butterflyType = ButterflyMob.SmallButterflyType.WHITE;
               }
            }

            if (this.getLevel().getBiome(this.getTileX(), this.getTileY()).equals(BiomeRegistry.DESERT)) {
               this.butterflyType = ButterflyMob.SmallButterflyType.GREEN;
            }

            if (this.getLevel().getBiome(this.getTileX(), this.getTileY()).equals(BiomeRegistry.PLAINS)
               && this.getLevel().getIdentifier().equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)) {
               this.butterflyType = ButterflyMob.SmallButterflyType.PURPLE;
            }

            if (this.getLevel().getIdentifier().equals(LevelIdentifier.SURFACE_IDENTIFIER)
               && this.getLevel().getWorldEntity().isNight()
               && GameRandom.globalRandom.getEveryXthChance(3)) {
               this.butterflyType = ButterflyMob.SmallButterflyType.BLACK;
            }

            if (GameSeasons.isAprilFools()) {
               this.butterflyType = ButterflyMob.SmallButterflyType.FROG;
            } else if (GameSeasons.isValentines()) {
               this.butterflyType = ButterflyMob.SmallButterflyType.HEART;
            }
         }
      }
   }

   @Override
   protected MobTexture getBugTexture() {
      return MobRegistry.Textures.butterfly;
   }

   @Override
   protected Point getWingFlapSpriteAnim() {
      Point butterflySpriteAnim = super.getWingFlapSpriteAnim();
      butterflySpriteAnim.x = butterflySpriteAnim.x + this.butterflyType.ordinal() * 2;
      return butterflySpriteAnim;
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.butterflywings).volume(0.04F).basePitch(0.97F).pitchVariance(0.05F);
   }

   @Override
   protected void addHoverTooltips(ListGameTooltips tooltips, boolean debug) {
      tooltips.add(ItemRegistry.getLocalization(ItemRegistry.getItemID(this.butterflyType.droppedItemStringID)));
   }

   @Override
   public MobSpawnLocation checkSpawnLocation(MobSpawnLocation location) {
      return super.checkSpawnLocation(location).checkTile((tileX, tileY) -> !this.getLevel().getTile(tileX, tileY).isFloor);
   }

   public enum SmallButterflyType {
      RED("butterflyred"),
      ORANGE("butterflyorange"),
      YELLOW("butterflyyellow"),
      GREEN("butterflygreen"),
      BLUE("butterflyblue"),
      PURPLE("butterflypurple"),
      BLACK("butterflyblack"),
      WHITE("butterflywhite"),
      FROG("butterflyfrog"),
      HEART("butterflyheart");

      public final String droppedItemStringID;

      SmallButterflyType(String droppedItemStringID) {
         this.droppedItemStringID = droppedItemStringID;
      }
   }
}
