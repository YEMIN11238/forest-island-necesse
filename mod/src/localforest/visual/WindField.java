package localforest.visual;

/** One field for visible meshes and projected silhouettes. GLSL equations mirror this class.
 * Height anchoring and strength modulation derive from Maujoe's MIT Simple Wind Shader 2D;
 * the world-space travelling gust and deterministic per-plant phase are local integration.
 */
public final class WindField {
    public static final float CROWN_SPEED=.90f,CROWN_AMPLITUDE=.050f;
    public static final float PLANT_SPEED=1.13f,PLANT_AMPLITUDE=.038f;
    public static float phase(int x,int y){
        int n=x*374761393+y*668265263;n=(n^(n>>>13))*1274126177;n^=n>>>16;
        return (n&0xffff)/65535f*6.2831853f;
    }
    public static float offset(double seconds,int tx,int ty,float uvY,float width,
            VisualMaterialDefinition material,boolean crown,boolean enabled){
        if(!enabled||material!=null&&material.rigidity>=1)return 0;
        float speed=material==null?(crown?CROWN_SPEED:PLANT_SPEED):material.windSpeed;
        float scale=width*(material==null?(crown?CROWN_AMPLITUDE:PLANT_AMPLITUDE):material.windAmplitude);
        float pivot=material==null?(crown?.10f:.07f):material.windPivot;
        float power=crown?1.5f:1.9f;
        return evaluate(seconds,tx*32+16,ty*32+29,phase(tx,ty),speed,scale,pivot,power,uvY);
    }
    static float evaluate(double seconds,float wx,float wy,float phase,float speed,float scale,float pivot,float power,float v){
        double t=seconds*speed+phase;
        double gust=.62+.38*Math.sin(seconds*.27-wx*.0017-wy*.0011);
        double carrier=Math.sin(t)*.62+Math.sin(t*.47+1.9)*.25+Math.cos(t*1.73)*.13;
        return (float)(carrier*(.45+.55*gust)*scale*Math.pow(Math.max(0,1-v-pivot),power));
    }
    private WindField(){}
}
