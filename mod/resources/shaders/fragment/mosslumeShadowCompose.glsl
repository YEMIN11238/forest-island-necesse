#version 120
uniform sampler2D mask0;uniform sampler2D mask1;uniform sampler2D mask2;uniform sampler2D mask3;
uniform sampler2D mask4;uniform sampler2D mask5;uniform sampler2D mask6;uniform sampler2D mask7;
uniform vec4 source0;uniform vec4 source1;uniform vec4 source2;uniform vec4 source3;
uniform vec4 source4;uniform vec4 source5;uniform vec4 source6;uniform vec4 source7;
uniform vec2 cameraWorld;uniform vec2 sceneSize;
uniform float shadowOpacity;
float energy(vec4 s,vec2 world) {
    if(s.w<0.0)return s.z;
    float fall=1.0-smoothstep(0.0,1.0,length(world-s.xy)/max(1.0,s.w));
    return s.z*fall*fall;
}
// A narrow receiver-only penumbra; original sprite sampling remains untouched.
float penumbra(sampler2D image,vec2 uv){
    vec2 d=vec2(2.25)/sceneSize;
    return texture2D(image,uv).a*.5+texture2D(image,uv+d).a*.25+texture2D(image,uv-d).a*.25;
}
void main() {
    vec2 uv=gl_TexCoord[0].xy;
    vec2 world=vec2(uv.x,1.0-uv.y)*sceneSize+cameraWorld;
    float e0=energy(source0,world),e1=energy(source1,world),e2=energy(source2,world),e3=energy(source3,world);
    float e4=energy(source4,world),e5=energy(source5,world),e6=energy(source6,world),e7=energy(source7,world);
    float blocked=e0*penumbra(mask0,uv)+e1*penumbra(mask1,uv)+e2*penumbra(mask2,uv)+e3*penumbra(mask3,uv);
    blocked+=e4*penumbra(mask4,uv)+e5*penumbra(mask5,uv)+e6*penumbra(mask6,uv)+e7*penumbra(mask7,uv);
    float alpha=min(shadowOpacity,blocked/(.80+e0+e1+e2+e3+e4+e5+e6+e7)*.85);
    gl_FragColor=vec4(.035,.060,.100,alpha);
}
