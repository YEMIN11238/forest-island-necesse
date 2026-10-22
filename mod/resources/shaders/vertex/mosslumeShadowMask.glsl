#version 120
uniform vec2 cameraWorld;
varying vec2 worldPixel;
void main() {
    gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;
    gl_TexCoord[0]=gl_MultiTexCoord0;
    gl_FrontColor=gl_Color;
    gl_BackColor=gl_Color;
    worldPixel=gl_Vertex.xy+cameraWorld;
}
