#version 120
uniform sampler2D texture;
// x: day amount, y: source saturation, z: source brightness.
uniform vec3 dayMaterial;
uniform float nightAmount;

void main() {
    vec4 color = texture2D(texture, gl_TexCoord[0].xy);
    // Path atlas tiles contain both grass and ochre soil. The same green mask
    // on plain grass and atlas verges keeps their shared edges identical and
    // leaves the warm soil color in place. This precedes native terrain light.
    float green = smoothstep(-0.10,0.10,color.g-color.r)
                * smoothstep(0.0,0.10,color.g-color.b);
    float luminance = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 graded = mix(vec3(luminance), color.rgb, dayMaterial.y) * dayMaterial.z;
    color.rgb = mix(color.rgb, graded, dayMaterial.x * green);
    float yellowHighlight=smoothstep(.62,.92,luminance)*green*dayMaterial.x;
    color.rgb*=vec3(1.0-.05*yellowHighlight,1.0-.02*yellowHighlight,1.0);
    color.b+=.02*yellowHighlight;
    // Retain the painted leaf/grass greens. Only a small neutral component lets
    // warm lamp hue show without turning the whole night landscape blue-grey.
    vec3 nightSource=mix(color.rgb,vec3(luminance*.92,luminance*.98,luminance*1.06),.22);
    color.rgb=mix(color.rgb,nightSource,nightAmount*green);
    gl_FragColor = color * gl_Color;
}
