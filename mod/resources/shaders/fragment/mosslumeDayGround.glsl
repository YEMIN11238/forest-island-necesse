#version 120
uniform sampler2D texture;
// x: day amount, y: source saturation, z: source brightness.
uniform vec3 dayMaterial;

void main() {
    vec4 color = texture2D(texture, gl_TexCoord[0].xy);
    // Path atlas tiles contain both grass and ochre soil. The same green mask
    // on plain grass and atlas verges keeps their shared edges identical and
    // leaves the warm soil color in place. This precedes native terrain light.
    float green = smoothstep(0.02, 0.14, color.g - color.r)
                * smoothstep(0.02, 0.14, color.g - color.b);
    float luminance = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 graded = mix(vec3(luminance), color.rgb, dayMaterial.y) * dayMaterial.z;
    color.rgb = mix(color.rgb, graded, dayMaterial.x * green);
    gl_FragColor = color * gl_Color;
}
