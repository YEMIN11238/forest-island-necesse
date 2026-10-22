#version 120
/*
 * Adapted from sharp-shimmerless by zadpos, public domain / CC0.
 * https://github.com/Woohyun-Kang/Sharp-Shimmerless-Shader
 * Commit: 2a74e7af599335c2ce5e493f823a757925c80db3
 * Upstream GLSL source: shaders_glsl/shaders/sharp-shimmerless.glsl
 *
 * Adaptation: use the engine's fixed-function vertScene UVs and sampler;
 * derive upstream pixel/scale varyings in the fragment stage. Input FBOs
 * are unpadded, so TextureSize == InputSize. Replaces the original final
 * upscale pass; never applied to transparent individual sprites.
 * The input sampler must remain LINEAR for the box-area reconstruction.
 */
uniform sampler2D texture;
uniform vec2 InputSize;
uniform vec2 OutputSize;

void main() {
    vec2 pixel = gl_TexCoord[0].xy * OutputSize;
    vec2 scale = OutputSize / InputSize;
    vec2 invscale = InputSize / OutputSize;
    vec2 pixel_tl = floor(pixel);
    vec2 pixel_br = ceil(pixel);
    vec2 texel_tl = floor(invscale * pixel_tl);
    vec2 texel_br = floor(invscale * pixel_br);
    vec2 mod_texel = texel_br + vec2(0.5);
    mod_texel -= (vec2(1.0) - step(texel_br, texel_tl)) * (scale * texel_br - pixel_tl);
    gl_FragColor = vec4(texture2D(texture, mod_texel / InputSize).rgb, 1.0);
}
