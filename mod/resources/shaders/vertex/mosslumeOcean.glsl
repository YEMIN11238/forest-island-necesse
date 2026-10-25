#version 120
uniform vec2 cameraWorld;
varying vec2 worldPosition;
void main(){
    worldPosition=gl_Vertex.xy+cameraWorld;
    gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;
    gl_TexCoord[0]=gl_MultiTexCoord0;
    gl_FrontColor=gl_Color;gl_BackColor=gl_Color;
}
