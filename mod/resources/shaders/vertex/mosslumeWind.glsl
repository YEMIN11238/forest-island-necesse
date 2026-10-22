#version 120
// Adapted from Simple Wind Shader 2D, Jaccomo Lorenz (Maujoe), MIT.
// Upstream commit: 5a1ee79571530749b707c8fa3154acbe90aee727.
// The upstream sin/cos gust equation and height anchoring are retained.
uniform float windTime;
uniform float speed;
uniform float minStrength;
uniform float maxStrength;
uniform float strengthScale;
uniform float interval;
uniform float detail;
uniform float heightOffset;
uniform float heightPower;
uniform float windPhase;

float getWind(vec2 uv, float timer) {
    // Stable object-space phase replaces Godot's canvas-position distortion.
    float time = timer * speed + windPhase;
    float diff = pow(maxStrength - minStrength, 2.0);
    float strength = clamp(minStrength + diff + sin(time / interval) * diff,
                           minStrength, maxStrength) * strengthScale;
    float heightWeight = pow(max(0.0, (1.0 - uv.y) - heightOffset), heightPower);
    return (sin(time) + cos(time * detail)) * strength * heightWeight;
}

void main() {
    vec4 vertex = gl_Vertex;
    vertex.x += getWind(gl_MultiTexCoord0.xy, windTime);
    gl_Position = gl_ModelViewProjectionMatrix * vertex;
    gl_TexCoord[0] = gl_MultiTexCoord0;
    gl_FrontColor = gl_Color;
    gl_BackColor = gl_Color;
}
