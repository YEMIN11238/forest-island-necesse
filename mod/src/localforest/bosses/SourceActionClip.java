package localforest.bosses;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/** Source GIF timing, independent of rendering and native attack damage. No invented frames. */
public record SourceActionClip(String file,int columns,int frameSize,int footX,int footY,int frameMillis,int damageFrame,int[] windup,int[] strike,int[] recovery,Set<Integer> emptyFrames) {
    public record Pick(boolean source,int column) {}
    public SourceActionClip {
        if(file==null||!file.matches("[a-z][a-z0-9-]{1,80}")||columns<2||columns>32||frameSize<64||frameSize>256||footX<0||footY<0||footX>=frameSize||footY>=frameSize||frameMillis<30||frameMillis>250)throw new IllegalArgumentException("Invalid source Boss clip");
        windup=windup.clone();strike=strike.clone();recovery=recovery.clone();emptyFrames=Set.copyOf(emptyFrames);
        if(windup.length==0||strike.length==0||recovery.length==0)throw new IllegalArgumentException("Source Boss clip needs all attack phases");
        for(int[] phase:new int[][]{windup,strike,recovery})for(int frame:phase)if(frame<0||frame>=columns)throw new IllegalArgumentException("Source frame exceeds clip bounds");
        if(Arrays.stream(strike).noneMatch(frame->frame==damageFrame))throw new IllegalArgumentException("Source damage pose must belong to strike phase");
        for(int frame:emptyFrames)if(frame<0||frame>=columns)throw new IllegalArgumentException("Invisible source frame exceeds bounds");
    }
    public int minimumWindupMillis(){return windup.length*frameMillis;}
    public int minimumActiveMillis(){return strike.length*frameMillis;}
    public int minimumRecoveryMillis(){return recovery.length*frameMillis;}
    public int damageDelayMillis(){for(int i=0;i<strike.length;i++)if(strike[i]==damageFrame)return i*frameMillis;throw new IllegalStateException("Source damage frame missing");}
    /** Hold source idle before the authored windup; let recovery finish before returning to idle. */
    public Pick pick(long elapsed,int nativeWindup,int nativeActive){
        elapsed=Math.max(0,elapsed);
        if(elapsed<nativeWindup){long intro=Math.max(0,nativeWindup-minimumWindupMillis());int index=(int)Math.max(0,(elapsed-intro)/frameMillis);return new Pick(true,windup[Math.min(windup.length-1,index)]);}
        if(elapsed<nativeWindup+nativeActive){int index=(int)((elapsed-nativeWindup)/frameMillis);return new Pick(true,strike[Math.min(strike.length-1,index)]);}
        int index=(int)((elapsed-nativeWindup-nativeActive)/frameMillis);
        return index>=recovery.length?new Pick(false,0):new Pick(true,recovery[Math.max(0,index)]);
    }
    public static SourceActionClip read(Properties properties,String key){
        int[] strike=indices(required(properties,key+".strike"));
        return new SourceActionClip(required(properties,key+".file"),Integer.parseInt(required(properties,key+".columns")),Integer.parseInt(properties.getProperty(key+".frameSize","96")),Integer.parseInt(properties.getProperty(key+".footX","48")),Integer.parseInt(properties.getProperty(key+".footY","80")),Integer.parseInt(properties.getProperty(key+".frameMillis","90")),Integer.parseInt(properties.getProperty(key+".damageFrame",String.valueOf(strike.length==0?-1:strike[0]))),
            indices(required(properties,key+".windup")),strike,indices(required(properties,key+".recovery")),new HashSet<>(Arrays.stream(indices(properties.getProperty(key+".emptyFrames",""))).boxed().toList()));
    }
    private static String required(Properties p,String key){String value=p.getProperty(key);if(value==null||value.isBlank())throw new IllegalArgumentException("Missing source clip metadata: "+key);return value.trim();}
    private static int[] indices(String value){return value.isBlank()?new int[0]:Arrays.stream(value.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();}
}
