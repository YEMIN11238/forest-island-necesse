package localforest.visual;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/** Art metadata, in source pixels, rather than a second engine-sized body pivot.
 * Every point is reviewed against its exact discrete authored attack frame.
 */
public final class AttackHandSockets {
    public static final int FRAME_SIZE=64,FOOT_X=32,FOOT_Y=52,FRAME_COUNT=6;
    private static final String RESOURCE="resources/mosslume/characters/attack-hand-sockets.properties";
    private static final String[] DIRS={"up","right","down","left"};
    public record Grip(int x,int y) {
        public int worldX(int footX,int scale){return footX+(x-FOOT_X)*scale;}
        public int worldY(int footY,int scale){return footY+(y-FOOT_Y)*scale;}
    }
    private static volatile Map<String,Grip[][]> sockets;
    public static void init(){
        if(sockets!=null)return;
        synchronized(AttackHandSockets.class){
            if(sockets!=null)return;
            try(InputStream input=AttackHandSockets.class.getClassLoader().getResourceAsStream(RESOURCE)){
                if(input==null)throw new IOException("Missing packaged hand sockets: "+RESOURCE);
                sockets=read(input);
            }catch(IOException invalid){throw new IllegalStateException("Cannot load authored hand sockets",invalid);}
        }
    }
    public static Grip at(String actor,int direction,int frame){
        init();return at(sockets,actor,direction,frame);
    }
    static Grip at(Map<String,Grip[][]> source,String actor,int direction,int frame){
        Grip[][] directions=source.get(actor);
        if(directions==null)throw new IllegalArgumentException("No reviewed attack hand sockets for "+actor);
        return directions[Math.floorMod(direction,4)][Math.max(0,Math.min(FRAME_COUNT-1,frame))];
    }
    static Map<String,Grip[][]> read(InputStream input)throws IOException{
        Properties properties=new Properties();properties.load(input);
        Map<String,Grip[][]> result=new LinkedHashMap<>();
        for(String key:properties.stringPropertyNames()){
            int divider=key.lastIndexOf('.');
            if(divider<1)throw new IOException("Invalid hand socket key: "+key);
            String actor=key.substring(0,divider),name=key.substring(divider+1);int dir=-1;
            for(int i=0;i<DIRS.length;i++)if(DIRS[i].equals(name))dir=i;
            if(dir<0)throw new IOException("Invalid hand direction: "+key);
            String[] coords=properties.getProperty(key).split(",");
            if(coords.length!=FRAME_COUNT*2)throw new IOException("Expected six hand points: "+key);
            Grip[] frames=new Grip[FRAME_COUNT];
            for(int i=0;i<frames.length;i++){
                int x,y;
                try{x=Integer.parseInt(coords[i*2].trim());y=Integer.parseInt(coords[i*2+1].trim());}
                catch(NumberFormatException bad){throw new IOException("Non-integer hand point: "+key,bad);}
                if(x<0||x>=FRAME_SIZE||y<0||y>=FRAME_SIZE)throw new IOException("Hand outside frame: "+key);
                frames[i]=new Grip(x,y);
            }
            result.computeIfAbsent(actor,k->new Grip[4][])[dir]=frames;
        }
        for(var actor:result.entrySet())for(int dir=0;dir<4;dir++)
            if(actor.getValue()[dir]==null)throw new IOException("Missing hand direction: "+actor.getKey()+"."+DIRS[dir]);
        return Map.copyOf(result);
    }
    private AttackHandSockets(){}
}
