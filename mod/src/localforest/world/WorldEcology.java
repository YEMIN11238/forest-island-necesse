package localforest.world;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.server.ServerClient;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.MobSpawnTable;
import net.bytebuddy.asm.Advice;

/** Restricts native spawning to configured species; native AI, drops and cap machinery stay authoritative. */
public final class WorldEcology {
    public static MobSpawnTable table(Level level,boolean animals){
        WorldDefinition d=WorldDefinition.forLevel(level);MobSpawnTable out=new MobSpawnTable();
        if(d==null)return out;
        if(animals&&level.isCave)return out;
        if(!animals&&!level.isCave){float h=level.getWorldEntity().getDayTimeHourFloat();if(h>=7&&h<19)return out;}
        String[] species=d.content.csv(animals?"animals":"enemies");
        int max=d.content.integer(animals?"population.maxAnimals":"population.maxEnemies");
        for(String id:species)out.add(100,(l,c,p,purpose)->{
            if(!l.isTileWithinBounds(p.x,p.y,4)||l.getTile(p.x,p.y).isLiquid||l.getObject(p.x,p.y).isSolid)return false;
            double safe=Math.hypot(p.x-d.campX,p.y-d.campY);
            if(!animals&&safe<24)return false;
            if(animals&&safe<12)return false;
            long population=l.entityManager.mobs.stream().filter(m->java.util.Arrays.asList(species).contains(m.getStringID())).count();
            return population<max;
        },id);
        return out;
    }
    @ModMethodPatch(target=ServerClient.class,name="getMobSpawnTable",arguments={Level.class,int.class,int.class})
    public static class Enemies {
        @Advice.OnMethodExit public static void after(@Advice.Argument(0) Level level,@Advice.Return(readOnly=false) MobSpawnTable table){if(WorldDefinition.forLevel(level)!=null)table=WorldEcology.table(level,false);}
    }
    @ModMethodPatch(target=ServerClient.class,name="getCritterSpawnTable",arguments={Level.class,int.class,int.class})
    public static class Animals {
        @Advice.OnMethodExit public static void after(@Advice.Argument(0) Level level,@Advice.Return(readOnly=false) MobSpawnTable table){if(WorldDefinition.forLevel(level)!=null)table=WorldEcology.table(level,true);}
    }
    @ModMethodPatch(target=ServerClient.class,name="getMobSpawnCap",arguments={Level.class})
    public static class EnemyCap {
        @Advice.OnMethodExit public static void after(@Advice.Argument(0) Level level,@Advice.Return(readOnly=false) float cap){WorldDefinition d=WorldDefinition.forLevel(level);if(d!=null)cap=Math.min(cap,d.rules.equals("relaxed")?3:d.content.integer("population.maxEnemies"));}
    }
    private WorldEcology(){}
}
