package localforest.visual;

public final class CharacterAnimationTest {
    public static void main(String[] args){
        check(CharacterAnimation.direction(0,-1,2)==0&&CharacterAnimation.direction(1,0,2)==1,"up/right");
        check(CharacterAnimation.direction(0,1,0)==2&&CharacterAnimation.direction(-1,0,0)==3,"down/left");
        check(CharacterAnimation.direction(0,0,3)==3,"stable direction without aim");
        check(CharacterAnimation.frame(-1,6)==0&&CharacterAnimation.frame(1,6)==5,"clamps ends");
        check(CharacterAnimation.frame(Float.NaN,6)==0,"invalid progress cannot address outside atlas");
        check(CharacterAnimation.frame(.5f,6)==3,"shared native attack progress");
        check(Math.abs(CharacterAnimation.breath(12,7))<=1.25,"bounded upper-body breath");
        check(CharacterAnimation.breath(12,7)!=CharacterAnimation.breath(12,8),"characters do not all breathe in phase");
        System.out.println("CharacterAnimationTest PASS: native-progress frames and four-way aim");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
