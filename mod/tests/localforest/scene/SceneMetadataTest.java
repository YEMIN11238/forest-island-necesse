package localforest.scene;

import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.GNDRegistry;

/** The real native GND map drops zero values; exercise its actual packet round trip. */
public final class SceneMetadataTest {
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        GNDRegistry.instance.registerCore();
        GNDItemMap data=new GNDItemMap();
        require(SceneStructures.houseCount(data)==1,"Old fixed island fallback changed");
        data.setBoolean("mosslumeGenerated",true);
        data.setInt("mosslumeHouseX",102).setInt("mosslumeHouseY",86);
        data.setInt("mosslumeHouseCount",0);
        require(!data.hasKey("mosslumeHouseCount"),"Regression must reproduce native zero omission");
        data.setBoolean("mosslumeStructureSchemaV1",true);
        require(SceneStructures.houseCount(data)==0,"Explicit zero resurrected a fallback house");
        GNDItemMap read=new GNDItemMap(data.getContentPacket());
        require(read.getBoolean("mosslumeStructureSchemaV1")&&!read.hasKey("mosslumeHouseCount"),"Native packet did not preserve schema semantics");
        require(SceneStructures.houseCount(read)==0,"Packet reload resurrected a fallback house");
        read.setInt("mosslumeHouseCount",1);
        require(SceneStructures.houseCount(read)==1,"Surface structure disappeared");
        read.setInt("mosslumeHouseCount",0);
        require(SceneStructures.houseCount(read)==0,"Removing the last surface structure failed");
        // Render reads use prehashed native keys; retain native primitive/default
        // semantics and live edits, including negative coordinates and bad types.
        String key="mosslumeHouse0X";
        for(int value:new int[]{-1000000,-1,0,1,102,Integer.MAX_VALUE}) {
            read.setInt(key,value);
            require(SceneStructures.number(read,key.hashCode(),52)==read.getInt(key,52),"Prehashed anchor changed native zero/default semantics");
        }
        read.setString(key,"not-a-number");
        require(SceneStructures.number(read,key.hashCode(),52)==52,"Invalid metadata must use native fallback");
        read.clearItem(key);
        require(SceneStructures.number(read,key.hashCode(),52)==52,"Cleared metadata retained an old anchor");
        read.setInt("mosslumeHouseCount",64);
        require(SceneStructures.houseCount(read)==64,"Structure upper bound changed");
        System.out.println("SceneMetadataTest PASS: native zero omission, explicit zero, packet round trip, last-house removal");
    }
}
