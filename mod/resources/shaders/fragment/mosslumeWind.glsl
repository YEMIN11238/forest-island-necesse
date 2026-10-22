#version 120
uniform sampler2D texture;
uniform vec2 texture_pixel_size;
uniform float smoothing_factor;
uniform int pixelCoverage;
uniform int surfaceLighting;
uniform vec3 ambientRGB;
uniform vec2 worldFoot;
uniform vec2 spriteSize;
uniform float elevation;
uniform int roundedSurface;
uniform vec3 skyDirection;
uniform vec4 lamp0;
uniform vec4 lamp1;
uniform vec4 lamp2;
uniform vec4 lampColor0;
uniform vec4 lampColor1;
uniform vec4 lampColor2;

vec3 lampLight(vec3 point, vec3 normal, vec4 lamp, vec4 color) {
    vec3 delta = lamp.xyz - point;
    float distanceRatio = length(delta.xy) / max(1.0, lamp.w);
    float falloff = 1.0 - smoothstep(0.0, 1.0, distanceRatio);
    float facing = 0.68 + 0.32 * max(0.0, dot(normal, normalize(delta + vec3(0.0, 0.0, 0.01))));
    return color.rgb * color.a * falloff * facing * 0.76;
}

vec3 surfaceLight(vec2 uv) {
    // A shallow crown volume; art's painted highlights remain authoritative.
    // World-space positions keep lamps fixed as the camera moves.
    vec3 normal = roundedSurface != 0
        ? normalize(vec3((uv.x - 0.5) * 1.35, (uv.y - 0.5) * 0.42, 0.92))
        : normalize(vec3((uv.x - 0.5) * 0.30, -0.12, 1.0));
    vec3 point = vec3(worldFoot + vec2((uv.x - 0.5) * spriteSize.x,
                      (uv.y - 1.0) * spriteSize.y * 0.12), (1.0 - uv.y) * elevation * 0.48);
    vec3 result = ambientRGB * (0.83 + 0.20 * max(0.0, dot(normal, normalize(skyDirection))));
    result += lampLight(point, normal, lamp0, lampColor0);
    result += lampLight(point, normal, lamp1, lampColor1);
    result += lampLight(point, normal, lamp2, lampColor2);
    return min(result, vec3(1.24));
}

/*
 * Sub-pixel accurate pixel sprite filtering by Morris Tabor Arroad
 * https://godotshaders.com/author/mortarroad/
 * https://godotshaders.com/shader/sub-pixel-accurate-pixel-sprite-filtering/
 * License: CC0. The UV mapping below is adapted from the engine's existing
 * fragPixelScaling.glsl. It keeps pixel interiors flat and covers only the
 * screen-pixel footprint at a source-pixel boundary.
 *
 * Sampling is adapted to four NEAREST texel-center reads rather than changing
 * the texture's global sampler state. Alpha-aware interpolation avoids dark
 * fringes caused by RGB=0 in transparent sprite pixels.
 */
vec4 premultipliedTexel(vec2 sourcePixel) {
    vec2 center = (sourcePixel + vec2(0.5)) * texture_pixel_size;
    center = clamp(center, texture_pixel_size * 0.5, vec2(1.0) - texture_pixel_size * 0.5);
    vec4 color = texture2D(texture, center);
    color.rgb *= color.a;
    return color;
}

vec4 pixelCoveredSample(vec2 uv) {
    vec2 uv_width = max(fwidth(uv), vec2(0.0000001));
    vec2 sprite_screen_resolution = smoothing_factor / uv_width;
    vec2 uv_pixel_src = floor(uv / texture_pixel_size + 0.499);
    vec2 edge = uv_pixel_src * texture_pixel_size * sprite_screen_resolution;
    vec2 uv_pixel = uv * sprite_screen_resolution;
    vec2 uv_factor = clamp(uv_pixel - edge + 0.5, 0.0, 1.0);
    vec2 mappedUV = (mix(uv_pixel_src - 1.0, uv_pixel_src, uv_factor) + 0.5) * texture_pixel_size;

    vec2 sourcePosition = mappedUV / texture_pixel_size - 0.5;
    vec2 first = floor(sourcePosition);
    vec2 weight = clamp(sourcePosition - first, 0.0, 1.0);
    vec4 upper = mix(premultipliedTexel(first), premultipliedTexel(first + vec2(1.0, 0.0)), weight.x);
    vec4 lower = mix(premultipliedTexel(first + vec2(0.0, 1.0)), premultipliedTexel(first + vec2(1.0, 1.0)), weight.x);
    vec4 color = mix(upper, lower, weight.y);
    if (color.a > 0.000001) color.rgb /= color.a;
    else color = vec4(0.0);
    return color;
}

void main() {
    // The source stays NEAREST. Coverage is local to moving plants; it does
    // not blur the world render target or change the source pixel dimensions.
    vec4 color = pixelCoverage != 0 ? pixelCoveredSample(gl_TexCoord[0].xy)
                                   : texture2D(texture, gl_TexCoord[0].xy);
    gl_FragColor = surfaceLighting != 0
        ? vec4(color.rgb * surfaceLight(gl_TexCoord[0].xy), color.a * gl_Color.a)
        : color * gl_Color;
}
