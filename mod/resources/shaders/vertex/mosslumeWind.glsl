#version 120
// Adapted from Simple Wind Shader 2D, Jaccomo Lorenz (Maujoe), MIT.
// Upstream commit: 5a1ee79571530749b707c8fa3154acbe90aee727.
// Height anchoring derives from upstream; travelling gust/carrier match local WindField.java.
uniform float windTime;
uniform vec2 worldFoot;
uniform float speed;
uniform float strengthScale;
uniform float heightOffset;
uniform float heightPower;
uniform float windPhase;
uniform int normalizedMesh;
uniform vec4 meshRect;

float getWind(vec2 uv, float timer) {
    // Stable object-space phase replaces Godot's canvas-position distortion.
    float time = timer * speed + windPhase;
    float gust=0.62+0.38*sin(timer*0.27-worldFoot.x*0.0017-worldFoot.y*0.0011);
    float carrier=sin(time)*0.62+sin(time*0.47+1.9)*0.25+cos(time*1.73)*0.13;
    float weight=pow(max(0.0,1.0-uv.y-heightOffset),heightPower);
    return carrier*(0.45+0.55*gust)*strengthScale*weight;
}

void main() {
    vec4 vertex = gl_Vertex;
    if (normalizedMesh != 0) vertex.xy = meshRect.xy + vertex.xy * meshRect.zw;
    vertex.x += getWind(gl_MultiTexCoord0.xy, windTime);
    gl_Position = gl_ModelViewProjectionMatrix * vertex;
    gl_TexCoord[0] = gl_MultiTexCoord0;
    gl_FrontColor = gl_Color;
    gl_BackColor = gl_Color;
}
