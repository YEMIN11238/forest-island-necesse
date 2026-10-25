// macOS GL2.1 regression. Creates an offline CGL context, never a game/window.
// Run from the project root: swift -suppress-warnings scripts/verify_visual_texture_state.swift
import Foundation
import OpenGL
import OpenGL.GL

var pixel:CGLPixelFormatObj?;var count:GLint=0
var attributes:[CGLPixelFormatAttribute]=[kCGLPFAAllowOfflineRenderers,CGLPixelFormatAttribute(rawValue:0)]
guard CGLChoosePixelFormat(&attributes,&pixel,&count)==kCGLNoError,let pixel=pixel else{fatalError("No GL pixel format")}
var context:CGLContextObj?;guard CGLCreateContext(pixel,nil,&context)==kCGLNoError,let context=context else{fatalError("No GL context")}
CGLSetCurrentContext(context)
defer{CGLSetCurrentContext(nil);CGLDestroyContext(context);CGLDestroyPixelFormat(pixel)}

func check(_ value:Bool,_ message:String){if !value{fatalError(message)}}
func integer(_ name:GLenum)->GLint{var result:GLint=0;glGetIntegerv(name,&result);return result}
func bind(_ unit:Int,_ name:GLuint){glActiveTexture(GLenum(GL_TEXTURE0)+GLenum(unit));glBindTexture(GLenum(GL_TEXTURE_2D),name)}
// This pair deliberately matches TextureStateScope. Assert that contract too,
// so a later helper change cannot silently leave this native test out of date.
let helper=try String(contentsOfFile:"mod/src/localforest/visual/TextureStateScope.java",encoding:.utf8)
check(helper.contains("GL11.glPushAttrib(GL11.GL_TEXTURE_BIT)")&&helper.contains("GL11.glPopAttrib()"),"Production scope no longer matches native regression")
func scope(_ body:() throws->Void) rethrows{
    glPushAttrib(GLbitfield(GL_TEXTURE_BIT));defer{glPopAttrib()};try body()
}
func texture(_ rgba:[UInt8])->GLuint{
    var name:GLuint=0;glGenTextures(1,&name);glBindTexture(GLenum(GL_TEXTURE_2D),name)
    glTexParameteri(GLenum(GL_TEXTURE_2D),GLenum(GL_TEXTURE_MIN_FILTER),GL_NEAREST)
    glTexParameteri(GLenum(GL_TEXTURE_2D),GLenum(GL_TEXTURE_MAG_FILTER),GL_NEAREST)
    rgba.withUnsafeBytes{glTexImage2D(GLenum(GL_TEXTURE_2D),0,GL_RGBA,1,1,0,GLenum(GL_RGBA),GLenum(GL_UNSIGNED_BYTE),$0.baseAddress)}
    return name
}
func snapshot()->[GLint]{
    let active=integer(GLenum(GL_ACTIVE_TEXTURE));var names:[GLint]=[]
    for unit in 0..<8{glActiveTexture(GLenum(GL_TEXTURE0)+GLenum(unit));names.append(integer(GLenum(GL_TEXTURE_BINDING_2D)))}
    glActiveTexture(GLenum(active));names.append(active);return names
}
var native:[GLuint]=[]
for i in 0..<8{native.append(texture([UInt8(i*29),120,200,255]))}
let normal=texture([128,128,255,255]),height=texture([70,64,255,255]),noise=texture([30,120,0,255])
for i in 0..<8{bind(i,native[i])};glActiveTexture(GLenum(GL_TEXTURE5))
let before=snapshot()
// Reproduce the old leak: stopping/changing programs cannot restore samplers.
bind(1,normal);bind(2,height);glActiveTexture(GLenum(GL_TEXTURE0))
check(snapshot() != before,"Missing regression precondition: auxiliary texture bindings did not change")
for i in 0..<8{bind(i,native[i])};glActiveTexture(GLenum(GL_TEXTURE5))
for _ in 0..<100{
    scope{
        bind(1,normal);bind(2,height);bind(0,normal)
        let outer=snapshot()
        scope{bind(1,noise);glActiveTexture(GLenum(GL_TEXTURE0))}
        check(snapshot()==outer,"Nested water pass did not restore surface samplers")
    }
    check(snapshot()==before,"Surface pass changed native recolor/overlay or active texture")
}
enum ProbeError:Error{case expected}
do{try scope{bind(1,noise);bind(2,height);throw ProbeError.expected}}
catch ProbeError.expected{}
check(snapshot()==before,"Exception path leaked texture state")
check(integer(GLenum(GL_ATTRIB_STACK_DEPTH))==0,"Unbalanced texture attribute stack")
check(glGetError()==GLenum(GL_NO_ERROR),"GL error in texture state regression")

// Small state-only cost check; this is not a frame-time claim or GPU benchmark.
let iterations=20000
glFinish();let start=DispatchTime.now().uptimeNanoseconds
for _ in 0..<iterations{scope{bind(1,normal);bind(2,height);bind(0,normal)}}
glFinish();let perScope=Double(DispatchTime.now().uptimeNanoseconds-start)/Double(iterations)/1000
print("TextureStateScope PASS GL=\(String(cString:glGetString(GLenum(GL_VERSION)))) units=0...7 active=5 nested=true exception=true stack=balanced stateOnlyMicrosecondsPerScope=\(perScope)")
