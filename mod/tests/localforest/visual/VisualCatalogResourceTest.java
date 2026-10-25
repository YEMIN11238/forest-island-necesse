package localforest.visual;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import necesse.engine.modLoader.GameFileEntry;
import necesse.gfx.res.ResourceEncoder;
import necesse.gfx.res.ResourceFolder;

/** Runs against real native resource indexing and the packaged mod JAR, without creating a window. */
public final class VisualCatalogResourceTest {
    public static void main(String[] args)throws Exception{
        Path path=Path.of(args.length>0?args[0]:System.getProperty("mosslume.test.modJar","mod/dist/localforest-island.jar"));
        try(JarFile jar=new JarFile(path.toFile())){
            List<GameFileEntry> entries=jar.stream().map(entry->new GameFileEntry(jar,entry)).toList();
            ResourceFolder folder=new ResourceFolder();folder.addModResources(entries,null);
            Field resources=ResourceEncoder.class.getDeclaredField("resources");resources.setAccessible(true);resources.set(null,folder);
            check(ResourceEncoder.getResourceBytes("mosslume/biomes/wetland/tree.png").length>32,"Native PNG indexing failed");
            try{ResourceEncoder.getResourceBytes("mosslume/biomes/wetland/catalog.json");throw new AssertionError("Expected JSON exclusion did not occur");}
            catch(FileNotFoundException expected){/* Exact native failure that caused the startup regression. */}
            String[] biomes={"wetland","mushroom","ruins","snowfield","desert","volcano"};
            for(String biome:biomes){
                String resource="resources/mosslume/biomes/"+biome+"/catalog.json";
                URL url=VisualMaterials.class.getClassLoader().getResource(resource);
                check(url!=null&&url.getProtocol().equals("jar"),"Catalog was not resolved from a packaged JAR: "+resource+" "+url);
                VisualMaterials.catalog(biome);
            }
            Field data=VisualMaterials.class.getDeclaredField("anchors");data.setAccessible(true);
            @SuppressWarnings("unchecked") Map<String,float[]> anchors=(Map<String,float[]>)data.get(null);
            float[] tree=anchors.get("mosslume/biomes/wetland/tree");
            check(tree!=null&&tree[0]==32&&tree[1]==78,"Packaged tree anchor was not parsed correctly");
            for(String biome:biomes){
                check(anchors.containsKey("mosslume/biomes/"+biome+"/structure-0"),"Missing structure foot for "+biome);
                check(anchors.containsKey("mosslume/biomes/"+biome+"/flora-0"),"Missing flora foot for "+biome);
            }
            long first=VisualClock.frameIndex();VisualClock.frame(null);long second=VisualClock.frameIndex();
            check(second==first+1,"Render frame identity must advance independently of animation time");
            System.out.println("VisualCatalogResourceTest PASS: native PNG index, native JSON exclusion, six JAR ClassLoader catalogs, exact anchors, independent render-frame identity");
        }
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
