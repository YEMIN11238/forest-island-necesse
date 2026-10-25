package localforest.biomes;

import java.util.LinkedHashMap;
import java.util.Map;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.levelData.MobSave;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.level.maps.Level;

/** In-memory native registry/loot/save checks; callers must supply a disposable test level. */
public final class BiomeCreatureChecks {
    public static Map<String,Object> verifyDefinitions(Level level){
        if(level==null||!level.isServer())throw new IllegalArgumentException("Native server level required");
        int registry=0,loot=0,saves=0,brains=0;
        for(var species:BiomeCreatures.SPECIES){
            Mob mob=MobRegistry.getMob(species.id(),level);
            require(mob!=null&&species.type().isInstance(mob),"Registry type "+species.id());registry++;
            mob.setPos(1024,1024,true);mob.canDespawn=false;mob.setSpawnTilePosition(32,32);
            mob.init();require(mob.ai!=null,"Native AI "+species.id());brains++;
            require(mob.shouldSave()&&!mob.canDespawn(),"Persistent mob "+species.id());
            var items=mob.getLootTable().getNewList(new GameRandom(20261024),1,mob);
            if(species.drop()!=null){require(items.stream().anyMatch(i->i.item.getStringID().equals(species.drop())&&i.getAmount()>0),"Native loot "+species.id());loot++;}
            var saved=MobSave.getSave("MOB",mob);
            Mob restored=MobSave.loadSave(saved.toLoadData(),level);
            require(restored!=null&&restored.getStringID().equals(species.id())&&!restored.canDespawn&&restored.getUniqueID()==mob.getUniqueID()&&restored.getX()==1024&&restored.getY()==1024,"Native save roundtrip "+species.id());saves++;
        }
        Map<String,Object> out=new LinkedHashMap<>();out.put("nativeRegistryTypes",registry);out.put("nativeBehaviourTrees",brains);out.put("nativeLootTables",loot);out.put("nativeSaveRoundTrips",saves);
        out.put("scope","In-memory native registry, AI construction, loot-table generation and save/load; not physical player attacks or full disk reload.");return out;
    }
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private BiomeCreatureChecks(){}
}
