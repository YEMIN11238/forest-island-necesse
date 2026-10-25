package localforest.world;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.util.TicketSystemList;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;

/** Runs the shipped kernel's water stack with minimal headless biome fixtures.
 * This checks sampling contracts without creating a Level, graphics or a save. */
public final class CoastalHydrologyTest {
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
        BiomeRegistry.UNKNOWN=BiomeRegistry.registerBiome("unknown",new Biome(),false);
        BiomeRegistry.FOREST=BiomeRegistry.registerBiome("forest",new Biome(),false);
        BiomeRegistry.PLAINS=BiomeRegistry.registerBiome("plains",new Biome(),false);
        BiomeRegistry.DESERT=BiomeRegistry.registerBiome("desert",new Biome(),false);
        BiomeRegistry.SWAMP=BiomeRegistry.registerBiome("swamp",new Biome(),false);
        BiomeRegistry.SNOW=BiomeRegistry.registerBiome("snow",new Biome(),false);
        var field=BiomeRegistry.class.getDeclaredField("biomeWeights");field.setAccessible(true);
        ((TicketSystemList<Biome>)field.get(null)).addObject(100,BiomeRegistry.FOREST);
        MosslumeContinuousBiome.register();int worlds=0,maxExit=0;
        for(String seed:new String[]{"4212026","mosslume","苔光","12345","coastal-5","coastal-6","coastal-7","coastal-8"}){
            var d=WorldDefinition.fromSeed(WorldDefinition.encodeSeed(seed,"continuous","standard"));
            var ours=new MosslumeContinuousStack(d);var nativeStack=new BiomeGeneratorStack((int)(d.seed^(d.seed>>>32)));
            require(ours.coast.nativeExitVerified,"No native dry exit for seed "+seed);
            maxExit=Math.max(maxExit,-ours.coast.exitY);
            for(int y=-44;y<=26;y+=2)for(int x=-30;x<=30;x+=2){
                int tx=d.campX+x,ty=d.campY+y;var terrain=ours.coast.terrain(tx,ty);
                if(terrain==CoastalCampLayout.Terrain.NATIVE)continue;
                boolean water=terrain==CoastalCampLayout.Terrain.WATER;
                require(ours.isSurfaceExpensiveWater(tx,ty)==water,"Surface terrain disagrees with POI");
                require(ours.isSurfaceOcean(tx,ty)==water&&ours.isSurfaceOceanOrRiver(tx,ty)==water,"Ocean/river mask disagreement");
                require(ours.isSurfaceExpensiveWaterOrBeach(tx,ty)==water&&ours.isSurfaceOceanOrBeach(tx,ty)==water&&ours.isSurfaceOceanOrRiverOrBeach(tx,ty)==water,"Water-or-beach mask disagreement");
                require(!ours.isSurfaceOceanBeach(tx,ty)&&!ours.isSurfaceOceanOrRiverBeach(tx,ty),"Authored grass cliff became sand");
            }
            for(int n=0;n<24;n++){
                int x=-2048+n*173,y=1024-n*97;
                if(ours.coast.owned(x,y))continue;
                require(ours.isSurfaceExpensiveWater(x,y)==nativeStack.isSurfaceExpensiveWater(x,y),"Remote native water changed");
                require(ours.isSurfaceOcean(x,y)==nativeStack.isSurfaceOcean(x,y),"Remote native ocean changed");
                require(ours.isSurfaceOceanOrRiverBeach(x,y)==nativeStack.isSurfaceOceanOrRiverBeach(x,y),"Remote native shore changed");
            }
            worlds++;
        }
        System.out.println("CoastalHydrologyTest PASS worlds="+worlds+" nativeKernelSampling=true classifierContracts=8 dryNativeExits="+worlds+" maxExitDistance="+maxExit+" remoteWaterUnchanged=true");
    }
}
