#version 120
// MIT GDQuest water_2D.gdshader, 05a3931a: noise UV + sine/cosine waves.
// World coordinates, depth blending, two moving layers and shore foam are local adaptations.
uniform sampler2D texture;
uniform sampler2D noiseTexture;
uniform float visualTime;
uniform vec2 artSize;
uniform float nightAmount;
varying vec2 worldPosition;
void main(){
    vec2 world=worldPosition;
    vec2 noiseUV=world/1024.0+vec2(visualTime*.009,-visualTime*.006);
    vec2 displacement=texture2D(noiseTexture,fract(noiseUV)).rg*2.0-1.0;
    vec2 second=texture2D(noiseTexture,fract(world/713.0+vec2(-visualTime*.005,visualTime*.008))).gr*2.0-1.0;
    vec2 sine=vec2(sin(visualTime*.48+world.x*.008+world.y*.004),cos(visualTime*.36+world.y*.007-world.x*.003));
    vec2 flow=vec2(visualTime*1.8,visualTime*.65)+displacement*7.0+second*3.0+sine*2.0;
    vec3 painted=texture2D(texture,fract((world+flow)/artSize)).rgb;
    float front=sin(world.x*.0036+world.y*.0020-visualTime*.31);
    float cross=sin(world.x*.0014-world.y*.0028+visualTime*.23);
    vec3 tint=vec3(.62+.065*front,.86+.065*cross,1.0);
    float shallow=clamp(gl_Color.a,0.0,1.0);
    float shimmer=(displacement.x+second.y+sine.y)*.016;
    vec3 color=painted*tint+vec3(.02,.075,.08)*shimmer;
    float foam=pow(shallow,12.0)*(.035+.025*sin(visualTime*1.2+world.x*.08+world.y*.06));
    color=mix(color,vec3(.36,.74,.74),foam);
    // Native terrain lighting is applied after this pass, exactly once.
    gl_FragColor=vec4(color,.54-.18*shallow);
}
