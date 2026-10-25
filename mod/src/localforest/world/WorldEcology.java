package localforest.world;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.server.ServerClient;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.MobSpawnTable;
import net.bytebuddy.asm.Advice;
import localforest.biomes.BiomeCreatures;

/** Restricts native spawning to configured species; native AI, drops and cap machinery stay authoritative. */
public final class WorldEcology {
    public static MobSpawnTable table(Level level,boolean animals){
        return table(level,animals,-1,-1);
    }
    public static MobSpawnTable table(Level level,boolean animals,int tileX,int tileY){
        WorldDefinition d=WorldDefinition.forLevel(level);MobSpawnTable out=new MobSpawnTable();
        if(d==null)return out;
        if(animals&&level.isCave)return out;
        if(!animals&&!level.isCave){float h=level.getWorldEntity().getDayTimeHourFloat();if(h>=7&&h<19)return out;}
        boolean continuous=d.isContinuous()&&!level.isCave;
        boolean multi=d.isMultibiome()&&!level.isCave;
        BiomeLayout biomes=multi?BiomeCreatures.layout(level,d):null;
        ContinuousWorldLayout streaming=continuous?ContinuousWorldLayout.forWorld(d):null;
        String biome=multi?biomes.biome(tileX,tileY):continuous?streaming.biome(tileX,tileY):null;
        String[] biomeSpecies=(multi||continuous)?BiomeCreatures.ids(biome,animals):new String[0];
        String[] species=multi?biomeSpecies:continuous&&biomeSpecies.length>0?biomeSpecies:d.content.csv(animals?"animals":"enemies");
        String[] populationIDs=(multi||continuous)?java.util.stream.Stream.concat(java.util.Arrays.stream(BiomeCreatures.ids(null,animals)),java.util.Arrays.stream(d.content.csv(animals?"animals":"enemies"))).toArray(String[]::new):species;
        java.util.Set<String> populationSet=java.util.Set.of(populationIDs);
        int max=(multi||continuous)?(animals?BiomeCreatures.MAX_ANIMALS:BiomeCreatures.MAX_MONSTERS):d.content.integer(animals?"population.maxAnimals":"population.maxEnemies");
        for(String id:species)out.add(100,(l,c,p,purpose)->{
            if(!l.isTileWithinBounds(p.x,p.y,4)||l.getTile(p.x,p.y).isLiquid||l.getObject(p.x,p.y).isSolid)return false;
            double safe=Math.hypot(p.x-d.campX,p.y-d.campY);
            if(!animals&&safe<24)return false;
            if(animals&&safe<12)return false;
            if(multi&&!biomes.biome(p.x,p.y).equals(biome))return false;
            if(continuous&&!streaming.biome(p.x,p.y).equals(biome))return false;
            long population=l.entityManager.mobs.stream().filter(m->populationSet.contains(m.getStringID())).count();
            return population<max;
        },id);
        return out;
    }
    @ModMethodPatch(target=ServerClient.class,name="getMobSpawnTable",arguments={Level.class,int.class,int.class})
    public static class Enemies {
        @Advice.OnMethodExit public static void after(@Advice.Argument(0) Level level,@Advice.Argument(1) int tileX,@Advice.Argument(2) int tileY,@Advice.Return(readOnly=false) MobSpawnTable table){if(WorldDefinition.forLevel(level)!=null)table=WorldEcology.table(level,false,tileX,tileY);}
    }
    @ModMethodPatch(target=ServerClient.class,name="getCritterSpawnTable",arguments={Level.class,int.class,int.class})
    public static class Animals {
        @Advice.OnMethodExit public static void after(@Advice.Argument(0) Level level,@Advice.Argument(1) int tileX,@Advice.Argument(2) int tileY,@Advice.Return(readOnly=false) MobSpawnTable table){if(WorldDefinition.forLevel(level)!=null)table=WorldEcology.table(level,true,tileX,tileY);}
    }
    @ModMethodPatch(target=ServerClient.class,name="getMobSpawnCap",arguments={Level.class})
    public static class EnemyCap {
        @Advice.OnMethodExit public static void after(@Advice.Argument(0) Level level,@Advice.Return(readOnly=false) float cap){WorldDefinition d=WorldDefinition.forLevel(level);if(d!=null)cap=Math.min(cap,d.rules.equals("relaxed")?3:d.content.integer("population.maxEnemies"));}
    }
    private WorldEcology(){}
}
