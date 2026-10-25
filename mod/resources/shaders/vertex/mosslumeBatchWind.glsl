#version 120
// Same MIT Simple Wind Shader 2D gust equation as mosslumeWind.glsl.
// Native-order buffered submission adapted from Apache-2.0 libGDX SpriteBatch.
uniform float windTime;
attribute vec4 aPositionUV;
attribute vec4 aFootSize;
attribute vec4 aShape;
attribute vec4 aWind;
attribute vec4 aAtlasRect;
attribute vec4 aLamp0;
attribute vec4 aLamp1;
attribute vec4 aLamp2;
attribute vec4 aLight0;
attribute vec4 aLight1;
varying vec4 vFootSize;
varying vec4 vShape;
varying vec4 vAtlasRect;
varying vec4 vLamp0;
varying vec4 vLamp1;
varying vec4 vLamp2;
varying vec4 vLight0;
varying vec4 vLight1;

void main() {
    vec2 uv=aPositionUV.zw;
    float timer=windTime*aWind.x+aLight1.z;
    float gust=0.62+0.38*sin(windTime*0.27-aFootSize.x*0.0017-aFootSize.y*0.0011);
    float carrier=sin(timer)*0.62+sin(timer*0.47+1.9)*0.25+cos(timer*1.73)*0.13;
    float weight=pow(max(0.0,1.0-uv.y-aWind.z),aWind.w);
    float bend=carrier*(0.45+0.55*gust)*aWind.y*weight;
    gl_Position=gl_ModelViewProjectionMatrix*vec4(aPositionUV.x+bend,aPositionUV.y,0.0,1.0);
    vFootSize=aFootSize;vShape=aShape;vAtlasRect=aAtlasRect;
    vLamp0=aLamp0;vLamp1=aLamp1;vLamp2=aLamp2;
    vLight0=aLight0;vLight1=vec4(aLight1.xy,uv);
}
