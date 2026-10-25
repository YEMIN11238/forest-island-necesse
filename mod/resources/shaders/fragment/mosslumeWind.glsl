#version 120
uniform sampler2D texture;
uniform vec2 texture_pixel_size;
uniform float smoothing_factor;
uniform int pixelCoverage;
uniform int surfaceLighting;
uniform vec3 dayMaterial;
uniform float nightAmount;
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

uniform sampler2D normalTexture;
uniform int hasNormalTexture;
uniform sampler2D occlusionMap;
uniform int occlusionEnabled;
uniform vec4 occlusionBounds;

// SurfaceMap/floodlight height-ray occlusion, SDV-Radiance MIT concepts.
// The native tile map is camera-bounded; samples never cause region generation.
float lampVisibility(vec3 point,vec4 lamp){
    if(occlusionEnabled==0)return 1.0;
    float visibility=1.0;
    for(int i=1;i<=12;i++){
        float t=float(i)/13.0;
        vec3 ray=mix(point,lamp.xyz,t);
        vec2 uv=(ray.xy-occlusionBounds.xy)/occlusionBounds.zw;
        if(uv.x<0.0||uv.y<0.0||uv.x>=1.0||uv.y>=1.0)continue;
        // The receiving sprite must not shadow itself via its own collision tile.
        vec2 own=floor(worldFoot/32.0),cell=floor(ray.xy/32.0);
        if(abs(own.x-cell.x)<0.5&&abs(own.y-cell.y)<0.5)continue;
        vec4 blocker=texture2D(occlusionMap,uv);
        float blockHeight=blocker.r*255.0;
        float penumbra=smoothstep(ray.z-7.0,ray.z+7.0,blockHeight);
        visibility=min(visibility,1.0-penumbra*blocker.b*.88);
    }
    return visibility;
}

vec3 lampLight(vec3 point, vec3 normal, vec4 lamp, vec4 color) {
    if(color.a<=0.0)return vec3(0.0);
    vec3 delta = lamp.xyz - point;
    float distanceRatio = length(delta.xy) / max(1.0, lamp.w);
    float falloff = 1.0 - smoothstep(0.0, 1.0, distanceRatio);
    falloff *= falloff;
    float facing = 0.35 + 0.65 * max(0.0, dot(normal, normalize(delta + vec3(0.0, 0.0, 0.01))));
    return color.rgb * color.a * falloff * facing * lampVisibility(point,lamp) * 0.82;
}

vec3 surfaceLight(vec2 uv) {
    // A shallow crown volume; art's painted highlights remain authoritative.
    // World-space positions keep lamps fixed as the camera moves.
    vec3 normal = roundedSurface != 0
        ? normalize(vec3((uv.x - 0.5) * 1.35, (uv.y - 0.5) * 0.42, 0.92))
        : normalize(vec3((uv.x - 0.5) * 0.30, -0.12, 1.0));
    vec4 baked=texture2D(normalTexture,uv);
    vec3 relief=vec3(baked.rg*2.0-1.0,baked.b);
    if(hasNormalTexture!=0&&baked.a>0.02)normal=normalize(mix(normal,relief,.68));
    vec3 point = vec3(worldFoot + vec2((uv.x - 0.5) * spriteSize.x,
                      (uv.y - 1.0) * spriteSize.y * 0.12), (1.0 - uv.y) * elevation * 0.90);
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
    if(color.a<=0.0){gl_FragColor=vec4(0.0);return;}
    // Only crowns have a nonzero day amount. Keep painted highlight luminance
    // while reducing neon saturation; night takes the original arithmetic path.
    float foliage=smoothstep(-.10,.10,color.g-color.r)*smoothstep(0.0,.10,color.g-color.b);
    float luminance=dot(color.rgb,vec3(.2126,.7152,.0722));
    vec3 daySource=mix(vec3(luminance),color.rgb,dayMaterial.y)*dayMaterial.z;
    color.rgb=mix(color.rgb,daySource,dayMaterial.x*foliage);
    float yellowHighlight=smoothstep(.62,.92,luminance)*foliage*dayMaterial.x;
    color.rgb*=vec3(1.0-.08*yellowHighlight,1.0-.035*yellowHighlight,1.0);
    color.b+=.035*yellowHighlight;
    vec3 nightSource=mix(color.rgb,vec3(luminance*.92,luminance*.98,luminance*1.06),.22);
    color.rgb=mix(color.rgb,nightSource,nightAmount*foliage);
    gl_FragColor = surfaceLighting != 0
        ? vec4(color.rgb * surfaceLight(gl_TexCoord[0].xy), color.a * gl_Color.a)
        : color * gl_Color;
}
