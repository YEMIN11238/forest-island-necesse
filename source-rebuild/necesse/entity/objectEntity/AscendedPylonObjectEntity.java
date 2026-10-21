package necesse.entity.objectEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.AscendedPylonDummyMob;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class AscendedPylonObjectEntity extends ObjectEntity {
   private int dummyMobID = -1;
   private float loadedMobHealthPercent = 1.0F;
   public static ArrayList<Function<AscendedPylonDummyMob, AscendedPylonDummyMob.AscendedPylonAttack>> possibleAttacks = new ArrayList<>(
      Arrays.asList(
         AscendedPylonDummyMob.SlimeQuakeAscendedPylonAttack::new,
         AscendedPylonDummyMob.MagicVolleyAscendedPylonAttack::new,
         AscendedPylonDummyMob.BatJailPylonAttack::new,
         AscendedPylonDummyMob.SpawnGauntletsPylonAttack::new,
         AscendedPylonDummyMob.AscendedShardBombsPylonAttack::new,
         AscendedPylonDummyMob.AscendedSlashesPylonAttack::new
      )
   );
   public ArrayList<Integer> possibleAttackIndexes = getNewDefaultAttackIndexesList();
   public ArrayList<Integer> currentAttackRotation = new ArrayList<>();

   public static ArrayList<Integer> getNewDefaultAttackIndexesList() {
      return IntStream.range(0, possibleAttacks.size()).boxed().collect(Collectors.toCollection(ArrayList::new));
   }

   public AscendedPylonObjectEntity(Level level, int x, int y) {
      super(level, "ascendedpylon", x, y);
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      AscendedPylonDummyMob mob = this.getMob();
      if (mob != null) {
         save.addFloat("healthPercent", mob.getHealthPercent());
      }

      save.addIntCollection("attackIndexes", this.possibleAttackIndexes);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.loadedMobHealthPercent = save.getFloat("healthPercent", 1.0F, false);
      this.possibleAttackIndexes = save.getIntCollection("attackIndexes", this.possibleAttackIndexes, false);
   }

   @Override
   public void setupContentPacket(PacketWriter writer) {
      if (this.dummyMobID == -1) {
         this.generateMobID();
      }

      writer.putNextInt(this.dummyMobID);
   }

   @Override
   public void applyContentPacket(PacketReader reader) {
      this.dummyMobID = reader.getNextInt();
   }

   @Override
   public void clientTick() {
      super.clientTick();
      AscendedPylonDummyMob m = this.getMob();
      if (m != null) {
         m.keepAlive(this);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      AscendedPylonDummyMob m = this.getMob();
      if (m == null) {
         m = this.generateMobID();
         this.markDirty();
      }

      m.keepAlive(this);
   }

   private AscendedPylonDummyMob generateMobID() {
      AscendedPylonDummyMob lastMob = this.getMob();
      if (lastMob != null) {
         lastMob.remove();
      }

      AscendedPylonDummyMob m = new AscendedPylonDummyMob();
      m.setLevel(this.getLevel());
      m.setHealth((int)(m.getMaxHealth() * this.loadedMobHealthPercent));
      float var10002 = this.tileX * 32 + 16;
      int var10003 = this.tileY * 32;
      this.getLevel().entityManager.addMob(m, var10002, var10003 + 16);
      this.dummyMobID = m.getUniqueID();
      return m;
   }

   private AscendedPylonDummyMob getMob() {
      if (this.dummyMobID == -1) {
         return null;
      }

      Mob m = this.getLevel().entityManager.mobs.get(this.dummyMobID, false);
      return m != null ? (AscendedPylonDummyMob)m : null;
   }

   public boolean isShielded() {
      AscendedPylonDummyMob mob = this.getMob();
      return mob != null && !mob.canTakeDamage();
   }

   public DrawOptions getEffectDrawOptions(GameTexture effectsTexture, int tileX, int tileY, GameLight light, GameCamera camera) {
      AscendedPylonDummyMob mob = this.getMob();
      return mob == null ? null : mob.getEffectDrawOptions(effectsTexture, tileX, tileY, light, camera);
   }

   public float getDamagePercent() {
      AscendedPylonDummyMob mob = this.getMob();
      return mob == null ? 0.0F : 1.0F - mob.getHealthPercent();
   }

   public AscendedPylonDummyMob.AscendedPylonAttack getNextAttack(AscendedPylonDummyMob mob) {
      if (this.currentAttackRotation.isEmpty()) {
         if (this.possibleAttackIndexes.isEmpty()) {
            this.possibleAttackIndexes = getNewDefaultAttackIndexesList();
         }

         this.currentAttackRotation.addAll(this.possibleAttackIndexes);
         Collections.shuffle(this.currentAttackRotation, GameRandom.globalRandom);
      }

      int nextIndex = GameRandom.globalRandom.nextInt(this.currentAttackRotation.size());
      int nextAttackIndex = this.currentAttackRotation.remove(nextIndex);
      if (nextAttackIndex >= 0 && nextAttackIndex < possibleAttacks.size()) {
         return possibleAttacks.get(nextAttackIndex).apply(mob);
      }

      this.possibleAttackIndexes.remove(Integer.valueOf(nextAttackIndex));
      this.currentAttackRotation.remove(Integer.valueOf(nextAttackIndex));
      return null;
   }

   @Override
   public void remove() {
      super.remove();
      AscendedPylonDummyMob m = this.getMob();
      if (m != null) {
         m.remove();
      }
   }

   public GameTooltips getPylonHoverTooltip(PlayerMob perspective) {
      AscendedPylonDummyMob mob = this.getMob();
      return mob == null ? null : new StringTooltips(this.getObject().getDisplayName() + " " + mob.getHealth() + "/" + mob.getMaxHealth());
   }

   @Override
   public void onMouseHover(PlayerMob perspective, boolean debug) {
      super.onMouseHover(perspective, debug);
      if (debug) {
         GameTooltipManager.addTooltip(new StringTooltips("MobID: " + this.dummyMobID), TooltipLocation.INTERACT_FOCUS);
      }
   }
}
