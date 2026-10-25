package localforest.companions;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import net.bytebuddy.asm.Advice;

/** NPC-owned identity extension. HumanShop has no native GND field, so this map is saved with the NPC itself. */
public final class NativeNpcIdentity {
    private static final String SAVE_KEY="MOSSLUME_IDENTITY",UUID_KEY="uuid";
    private static final Map<HumanShop,GNDItemMap> identities=Collections.synchronizedMap(new WeakHashMap<>());
    record Legacy(String uuid,int mobId,String level,String name,String nativeType,Integer seed) {}

    public static String get(HumanShop mob){
        GNDItemMap data=identities.get(mob);return data==null?"":validUuid(data.getString(UUID_KEY,""));
    }
    public static void bind(HumanShop mob,String uuid){
        String valid=validUuid(uuid);if(valid.isEmpty())throw new IllegalArgumentException("Invalid NPC identity UUID");
        if(valid.equals(get(mob)))return;
        GNDItemMap data=identities.computeIfAbsent(mob,key->new GNDItemMap());
        data.setString(UUID_KEY,valid);mob.markDirty();
    }
    public static String template(HumanShop mob){GNDItemMap data=identities.get(mob);return data==null?"":data.getString("template","");}
    public static void bindTemplate(HumanShop mob,String template,int originX,int originY){
        if(NpcDefinition.find(template)==null)throw new IllegalArgumentException("Unknown NPC template");
        GNDItemMap data=identities.computeIfAbsent(mob,key->new GNDItemMap());
        data.setString("template",template);data.setInt("originX",originX);data.setInt("originY",originY);mob.markDirty();
    }
    public static int originX(HumanShop mob){GNDItemMap data=identities.get(mob);return data==null?mob.getTileX():data.getInt("originX",mob.getTileX());}
    public static int originY(HumanShop mob){GNDItemMap data=identities.get(mob);return data==null?mob.getTileY():data.getInt("originY",mob.getTileY());}
    public static boolean promoted(HumanShop mob){GNDItemMap data=identities.get(mob);return data!=null&&data.getBoolean("promoted");}
    public static void promote(HumanShop mob){GNDItemMap data=identities.computeIfAbsent(mob,key->new GNDItemMap());data.setBoolean("promoted",true);mob.markDirty();}
    static String validUuid(String uuid){
        if(uuid==null)return "";
        try{String canonical=UUID.fromString(uuid).toString();return canonical.equals(uuid)?canonical:"";}
        catch(IllegalArgumentException invalid){return "";}
    }
    static boolean matchesStable(String saved,String observed){return !validUuid(saved).isEmpty()&&saved.equals(observed);}
    static boolean canMigrate(Legacy old,int mobId,String level,String name,String nativeType,int seed){
        // Missing UUID is the only migration path. Numeric IDs alone are never identity.
        return old.uuid().isEmpty()&&old.mobId()==mobId&&old.level().equals(level)
            &&!old.name().isBlank()&&old.name().equals(name)
            &&(old.nativeType().isEmpty()||old.nativeType().equals(nativeType))
            &&(old.seed()==null||old.seed()==seed);
    }
    public static void writeNpc(HumanShop mob,SaveData save){
        GNDItemMap data=identities.get(mob);if(data==null||get(mob).isEmpty())return;
        SaveData own=new SaveData(SAVE_KEY);data.addSaveData(own);save.addSaveData(own);
    }
    public static void readNpc(HumanShop mob,LoadData save){
        String uuid=readPayload(save);
        LoadData own=save.getFirstLoadDataByName(SAVE_KEY);
        if(uuid.isEmpty())identities.remove(mob);else identities.put(mob,new GNDItemMap(own));
    }
    static void writePayload(SaveData save,String uuid){
        if(validUuid(uuid).isEmpty())return;
        SaveData own=new SaveData(SAVE_KEY);new GNDItemMap().setString(UUID_KEY,uuid).addSaveData(own);save.addSaveData(own);
    }
    static String readPayload(LoadData save){
        LoadData own=save.getFirstLoadDataByName(SAVE_KEY);return own==null?"":validUuid(new GNDItemMap(own).getString(UUID_KEY,""));
    }
    @ModMethodPatch(target=HumanShop.class,name="addSaveData",arguments={SaveData.class})
    public static final class SavePatch {
        @Advice.OnMethodExit public static void after(@Advice.This HumanShop mob,@Advice.Argument(0) SaveData save){NativeNpcIdentity.writeNpc(mob,save);}
    }
    @ModMethodPatch(target=HumanShop.class,name="applyLoadData",arguments={LoadData.class})
    public static final class LoadPatch {
        @Advice.OnMethodExit public static void after(@Advice.This HumanShop mob,@Advice.Argument(0) LoadData save){NativeNpcIdentity.readNpc(mob,save);}
    }
    private NativeNpcIdentity(){}
}
