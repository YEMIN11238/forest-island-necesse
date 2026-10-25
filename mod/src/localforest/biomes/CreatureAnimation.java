package localforest.biomes;

/** Authored directional action frames. Timings agree with the server AttackCycle. */
public final class CreatureAnimation {
    public enum Action {
        ATTACK("attack",4),HURT("hurt",2),DEATH("death",4);
        public final String suffix;public final int columns;
        Action(String suffix,int columns){this.suffix=suffix;this.columns=columns;}
    }
    public static final int FRAME=64,FOOT_X=32,FOOT_Y=52,HURT_MILLIS=220,DEATH_MILLIS=900;
    public static int directionRow(int nativeDirection){return switch(Math.floorMod(nativeDirection,4)){case 0->2;case 1->1;case 2->0;default->3;};}
    public static int attackFrame(float progress){return progress<.22f?0:progress<.43f?1:progress<.62f?2:3;}
    public static int hurtFrame(long elapsedMillis){return elapsedMillis<110?0:1;}
    public static int deathFrame(long elapsedMillis){return elapsedMillis<120?0:elapsedMillis<260?1:elapsedMillis<440?2:3;}
    public static float deathAlpha(long elapsedMillis){return elapsedMillis<700?1:Math.max(0,Math.min(1,(DEATH_MILLIS-elapsedMillis)/200f));}
    private CreatureAnimation(){}
}
