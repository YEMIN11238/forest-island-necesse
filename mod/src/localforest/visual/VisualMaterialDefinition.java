package localforest.visual;

import java.awt.image.BufferedImage;

/** Immutable source-pixel geometry. Collision remains native and separate from the canopy. */
public final class VisualMaterialDefinition {
    public final String id;
    public final int width,height,minX,minY,maxX,maxY;
    public final float anchorX,anchorY,targetHeight,shadowHeight,rigidity,windSpeed,windAmplitude,windPivot;
    public final boolean crown;
    public VisualMaterialDefinition(String id,BufferedImage image,float anchorX,float anchorY,
            float targetHeight,float shadowHeight,float rigidity,boolean crown){
        this.id=id;width=image.getWidth();height=image.getHeight();
        int l=width,t=height,r=0,b=0;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)if((image.getRGB(x,y)>>>24)>=64){l=Math.min(l,x);t=Math.min(t,y);r=Math.max(r,x+1);b=Math.max(b,y+1);}
        minX=l==width?0:l;minY=t==height?0:t;maxX=r;maxY=b;
        this.anchorX=anchorX;this.anchorY=anchorY;this.targetHeight=targetHeight;this.shadowHeight=shadowHeight;
        this.rigidity=rigidity;this.crown=crown;
        windSpeed=crown?WindField.CROWN_SPEED:WindField.PLANT_SPEED;
        windAmplitude=(crown?WindField.CROWN_AMPLITUDE:WindField.PLANT_AMPLITUDE)*(1-rigidity);
        windPivot=crown?.10f:.07f;
    }
    public float scale(float fallback){return targetHeight>0&&maxY>minY?targetHeight/(maxY-minY):fallback;}
    /** House layers share a scale-2 baseline; prefab size must scale relief and casts together.
     * Fixed-target vegetation/biome materials already describe their calibrated world height.
     */
    public float effectiveShadowHeight(float scale){
        return id.contains("house-layers/")||id.equals("mosslume-house-shadow")?shadowHeight*scale*.5f:shadowHeight;
    }
    public float left(float footX,float scale){return footX-anchorX*scale;}
    public float top(float footY,float scale){return footY-anchorY*scale;}
    /** SDV-Radiance normals.fx Sobel silhouette/painted-relief bake, adapted to CPU once per source.
     * MIT upstream 65c153a9. Neighbour reads are bounded to this sprite, never its atlas neighbours.
     */
    public byte[] bakeNormals(BufferedImage image){
        byte[] pixels=new byte[width*height*4];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            float gx=0,gy=0;
            for(int radius=1;radius<=2;radius++){
                float w=radius==1?1:.5f;
                gx+=w*(alpha(image,x+radius,y-radius)+2*alpha(image,x+radius,y)+alpha(image,x+radius,y+radius)-alpha(image,x-radius,y-radius)-2*alpha(image,x-radius,y)-alpha(image,x-radius,y+radius));
                gy+=w*(alpha(image,x-radius,y+radius)+2*alpha(image,x,y+radius)+alpha(image,x+radius,y+radius)-alpha(image,x-radius,y-radius)-2*alpha(image,x,y-radius)-alpha(image,x+radius,y-radius));
            }
            float lx=lum(image,x+1,y-1)+2*lum(image,x+1,y)+lum(image,x+1,y+1)-lum(image,x-1,y-1)-2*lum(image,x-1,y)-lum(image,x-1,y+1);
            float ly=lum(image,x-1,y+1)+2*lum(image,x,y+1)+lum(image,x+1,y+1)-lum(image,x-1,y-1)-2*lum(image,x,y-1)-lum(image,x+1,y-1);
            // Lower painted-relief contribution than the source: generated art is noisier.
            float nx=-gx*.48f+lx*.045f,ny=-gy*.48f+ly*.045f;
            float length=(float)Math.sqrt(nx*nx+ny*ny+1);int at=(x+y*width)*4;
            pixels[at]=(byte)Math.round((nx/length*.5f+.5f)*255);
            pixels[at+1]=(byte)Math.round((ny/length*.5f+.5f)*255);
            pixels[at+2]=(byte)Math.round(255/length);
            pixels[at+3]=(byte)(image.getRGB(x,y)>>>24);
        }
        return pixels;
    }
    private static int pixel(BufferedImage i,int x,int y){return i.getRGB(Math.max(0,Math.min(i.getWidth()-1,x)),Math.max(0,Math.min(i.getHeight()-1,y)));}
    private static float alpha(BufferedImage i,int x,int y){return (pixel(i,x,y)>>>24)/255f;}
    private static float lum(BufferedImage i,int x,int y){int c=pixel(i,x,y);return (((c>>>16)&255)*.299f+((c>>>8)&255)*.587f+(c&255)*.114f)/255f*(c>>>24)/255f;}
}
