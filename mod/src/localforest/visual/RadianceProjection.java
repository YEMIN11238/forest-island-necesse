package localforest.visual;

/**
 * Grounded affine silhouette projection ported from SDV-Radiance's
 * ShadowRenderer.ShadowProjection.ForSolid, commit
 * bff10581b2fd104f4ed094f67ae5afaa7a024c67 (MIT, copyright 2026 phuicmt).
 * The complete upstream notice is packaged in mosslume/licenses.
 */
public final class RadianceProjection {
    public final float acrossX, acrossY, alongX, alongY;

    private RadianceProjection(float ax,float ay,float lx,float ly) {
        acrossX=ax;acrossY=ay;alongX=lx;alongY=ly;
    }

    public static RadianceProjection solid(float shadowX,float shadowY,float stretch,float groundDepth) {
        float rotation=(float)Math.atan2(shadowX,-shadowY);
        float sin=(float)Math.sin(rotation),cos=(float)Math.cos(rotation);
        float k=Math.max(.05f,groundDepth);
        float gx=cos/k,gy=sin;
        float length=(float)Math.sqrt(gx*gx+gy*gy);
        float ax=gx/length,ay=gy*k/length;
        // Flip the entire across vector, never x alone: the latter would make
        // it parallel to the length vector and erase shadows at some angles.
        if(ax<0){ax=-ax;ay=-ay;}
        return new RadianceProjection(ax,ay,sin*stretch,-cos*stretch);
    }

    public float x(float across,float aboveFeet){return across*acrossX+aboveFeet*alongX;}
    public float y(float across,float aboveFeet){return across*acrossY+aboveFeet*alongY;}
    public float determinant(){return acrossX*alongY-acrossY*alongX;}
}
