package localforest.scene;

import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.GNDRegistry;
import necesse.engine.save.SaveData;

/** Exercises the actual residence GND writer across packet/save round trips and a primary-house restamp. */
public final class ResidenceMetadataTest {
    private static void check(boolean good,String why){if(!good)throw new AssertionError(why);}
    public static void main(String[] args){
        GNDRegistry.instance.registerCore();GNDItemMap data=new GNDItemMap();
        data.setBoolean("mosslumeStructureSchemaV1",true).setInt("mosslumeHouseCount",1).setInt("mosslumeHouse0X",26).setInt("mosslumeHouse0Y",26);
        check(SceneStructures.registerResidenceData(data,"npc-district-1",-3,-6,7,6),"First residence was not registered");
        check(!SceneStructures.registerResidenceData(data,"npc-district-1",-3,-6,7,6),"Zero footpoint omission caused repeated registration/network invalidation");
        SceneStructures.registerResidenceData(data,"npc-district-2",24,-2,7,6);
        SceneStructures.registerResidenceData(data,"npc-district-3",3,-6,7,6);
        check(data.getInt("mosslumeResidenceCountV1")==3,"Repeated connection duplicated a residence");
        check(SceneStructures.houseCount(data)==1,"NPC residences overwrote the primary house list");
        // Reproduce WorldDefinition's primary-house stamp after a disk/packet round trip.
        SaveData payload=new SaveData("GND");data.addSaveData(payload);
        GNDItemMap restored=new GNDItemMap(new GNDItemMap(payload.toLoadData()).getContentPacket());
        restored.setInt("mosslumeHouseCount",1).setInt("mosslumeHouse0X",30).setInt("mosslumeHouse0Y",30);
        check(restored.getInt("mosslumeResidenceCountV1")==3,"Primary-house restamp erased residence roofs");
        check(restored.getInt("mosslumeResidence0X")==0&&restored.getInt("mosslumeResidence0Y")==0,"Native omitted-zero round trip moved a residence");
        StructureInstance hut=new StructureInstance("npc-district-1",restored.getInt("mosslumeResidence0X"),restored.getInt("mosslumeResidence0Y"),
            restored.getInt("mosslumeResidence0Width"),restored.getInt("mosslumeResidence0Depth"));
        check(hut.left==-3&&hut.right==3&&hut.top==-6&&hut.bottom==-1&&hut.doorTileX==0&&hut.doorTileY==-1,"Native shell/door and shared art footpoint disagree");
        check(Math.abs(hut.visualScale-2.8)<.0001,"Seven-tile shell still uses five-tile roof scale");
        check(hut.contains(-2,-4)&&hut.contains(0,-4)&&hut.contains(2,-4)&&!hut.contains(0,0),"Bed/interior/entry roof occlusion bounds disagree");
        var same=new StructureInstance("moved",100,100,7,6);
        check(Math.abs(same.windowX(0)-hut.windowX(0)-3200)<.001&&Math.abs(same.windowY()-hut.windowY()-3200)<.001,"Window lights lost shared translated footpoint");
        check(SceneStructures.registerResidenceData(restored,"npc-district-1",-4,-6,7,6)&&restored.getInt("mosslumeResidenceCountV1")==3,"Moving a residence added a duplicate slot");
        System.out.println("ResidenceMetadataTest PASS: actual native GND writer/save/packet, zero/negative anchors, idempotence, independent main-house stamp, 7x6 collision/bed/door/roof geometry and scaled window lights; no game or model");
    }
}
