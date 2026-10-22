#version 120
uniform sampler2D mask0;uniform sampler2D mask1;uniform sampler2D mask2;uniform sampler2D mask3;
uniform sampler2D mask4;uniform sampler2D mask5;uniform sampler2D mask6;uniform sampler2D mask7;
uniform vec4 source0;uniform vec4 source1;uniform vec4 source2;uniform vec4 source3;
uniform vec4 source4;uniform vec4 source5;uniform vec4 source6;uniform vec4 source7;
uniform vec2 cameraWorld;uniform vec2 sceneSize;
float energy(vec4 s,vec2 world) {
    if(s.w<0.0)return s.z;
    return s.z*(1.0-smoothstep(0.0,1.0,length(world-s.xy)/max(1.0,s.w)));
}
void main() {
    vec2 uv=gl_TexCoord[0].xy;
    vec2 world=vec2(uv.x,1.0-uv.y)*sceneSize+cameraWorld;
    float e0=energy(source0,world),e1=energy(source1,world),e2=energy(source2,world),e3=energy(source3,world);
    float e4=energy(source4,world),e5=energy(source5,world),e6=energy(source6,world),e7=energy(source7,world);
    float blocked=e0*texture2D(mask0,uv).a+e1*texture2D(mask1,uv).a+e2*texture2D(mask2,uv).a+e3*texture2D(mask3,uv).a;
    blocked+=e4*texture2D(mask4,uv).a+e5*texture2D(mask5,uv).a+e6*texture2D(mask6,uv).a+e7*texture2D(mask7,uv).a;
    float alpha=min(.58,blocked/(.52+e0+e1+e2+e3+e4+e5+e6+e7)*.97);
    gl_FragColor=vec4(.012,.031,.051,alpha);
}
