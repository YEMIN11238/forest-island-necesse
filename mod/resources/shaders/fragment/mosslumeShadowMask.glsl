#version 120
uniform sampler2D texture;
uniform sampler2D waterMap;
uniform vec2 mapSize;
uniform int receiverMode;
uniform float heightFade;
varying vec2 worldPixel;
void main() {
    vec2 tile=floor(worldPixel/32.0);
    float water=texture2D(waterMap,(tile+vec2(.5))/mapSize).r;
    if(receiverMode==1&&water>.5)discard;
    if(receiverMode==2&&water<.5)discard;
    float a=texture2D(texture,gl_TexCoord[0].xy).a*gl_Color.a*(1.0-heightFade*(1.0-gl_TexCoord[0].y));
    gl_FragColor=vec4(a);
}
