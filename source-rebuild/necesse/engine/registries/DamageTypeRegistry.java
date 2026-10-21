package necesse.engine.registries;

import java.util.stream.Collectors;
import necesse.engine.modLoader.LoadedMod;
import necesse.entity.mobs.gameDamageType.DamageType;
import necesse.entity.mobs.gameDamageType.MagicDamageType;
import necesse.entity.mobs.gameDamageType.MeleeDamageType;
import necesse.entity.mobs.gameDamageType.NormalDamageType;
import necesse.entity.mobs.gameDamageType.RangedDamageType;
import necesse.entity.mobs.gameDamageType.SummonDamageType;
import necesse.entity.mobs.gameDamageType.TrueDamageType;

public class DamageTypeRegistry extends GameRegistry<DamageTypeRegistry.DamageTypeRegistryElement> {
   public static final DamageTypeRegistry instance = new DamageTypeRegistry();
   public static DamageType NORMAL;
   public static DamageType TRUE;
   public static DamageType MELEE;
   public static DamageType RANGED;
   public static DamageType MAGIC;
   public static DamageType SUMMON;

   private DamageTypeRegistry() {
      super("GameDamageType", 122);
   }

   @Override
   public void registerCore() {
      registerDamageType("normal", NORMAL = new NormalDamageType());
      registerDamageType("true", TRUE = new TrueDamageType());
      registerDamageType("melee", MELEE = new MeleeDamageType());
      registerDamageType("ranged", RANGED = new RangedDamageType());
      registerDamageType("magic", MAGIC = new MagicDamageType());
      registerDamageType("summon", SUMMON = new SummonDamageType());
   }

   protected void onRegister(DamageTypeRegistry.DamageTypeRegistryElement element, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
      for (DamageTypeRegistry.DamageTypeRegistryElement element : this.getElements()) {
         element.type.onDamageTypeRegistryClosed();
      }
   }

   public static int registerDamageType(String stringID, DamageType type) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register damage types");
      } else {
         return instance.register(stringID, new DamageTypeRegistry.DamageTypeRegistryElement(type));
      }
   }

   public static DamageType getDamageType(int id) {
      if (id == -1) {
         return null;
      }

      DamageTypeRegistry.DamageTypeRegistryElement element = instance.getElement(id);
      return element == null ? null : element.type;
   }

   public static int getDamageTypeID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static DamageType getDamageType(String stringID) {
      return getDamageType(getDamageTypeID(stringID));
   }

   public static String getDamageTypeStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static Iterable<DamageType> getDamageTypes() {
      return instance.streamElements().map(e -> e.type).collect(Collectors.toList());
   }

   protected static class DamageTypeRegistryElement implements IDDataContainer {
      public final DamageType type;

      public DamageTypeRegistryElement(DamageType type) {
         this.type = type;
      }

      @Override
      public IDData getIDData() {
         return this.type.idData;
      }
   }
}
