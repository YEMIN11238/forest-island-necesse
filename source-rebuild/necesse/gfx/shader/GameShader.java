package necesse.gfx.shader;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import necesse.gfx.Renderer;
import necesse.gfx.shader.shaderVariable.ShaderVariable;
import org.lwjgl.opengl.GL20;

public class GameShader {
   protected int program;
   protected int vertex;
   protected int fragment;
   public final String vertexPath;
   public final String fragmentPath;
   private final ArrayList<ShaderVariable<?>> variables = new ArrayList<>();

   public GameShader(String vertex, String fragment) {
      this.vertexPath = vertex;
      this.fragmentPath = fragment;
      this.reload();
   }

   public void reload() {
      if (this.program != 0) {
         GL20.glDeleteProgram(this.program);
      }

      this.program = GL20.glCreateProgram();
      this.vertex = ShaderLoader.loadVertexShader(this.vertexPath);
      this.fragment = ShaderLoader.loadFragmentShader(this.fragmentPath);
      GL20.glAttachShader(this.program, this.vertex);
      GL20.glAttachShader(this.program, this.fragment);
      GL20.glLinkProgram(this.program);
      GL20.glValidateProgram(this.program);
   }

   public int getUniform(String location) {
      return GL20.glGetUniformLocation(this.program, location);
   }

   public int getProgram() {
      return this.program;
   }

   public final void _ScreenUse() {
      GL20.glUseProgram(this.program);
   }

   public void use() {
      Renderer.useShader(this);
      this.pass1i("texture", 0);
   }

   public void stop() {
      Renderer.stopShader(this);
   }

   public void delete() {
      GL20.glDeleteProgram(this.program);
      GL20.glDeleteShader(this.vertex);
      GL20.glDeleteShader(this.fragment);
   }

   public ArrayList<ShaderVariable<?>> getVariables() {
      return this.variables;
   }

   protected void addVariable(ShaderVariable<?> variable) {
      this.variables.add(variable);
   }

   public void pass1f(String variable, float v0) {
      GL20.glUniform1f(this.getUniform(variable), v0);
   }

   public void pass1fv(String variable, float[] v0) {
      GL20.glUniform1fv(this.getUniform(variable), v0);
   }

   public float get1f(String variable) {
      return GL20.glGetUniformf(this.program, this.getUniform(variable));
   }

   public void pass2f(String variable, float v0, float v1) {
      GL20.glUniform2f(this.getUniform(variable), v0, v1);
   }

   public float[] get2f(String variable) {
      float[] results = new float[2];
      GL20.glGetUniformfv(this.program, this.getUniform(variable), results);
      return results;
   }

   public void pass3f(String variable, float v0, float v1, float v2) {
      GL20.glUniform3f(this.getUniform(variable), v0, v1, v2);
   }

   public float[] get3f(String variable) {
      float[] results = new float[3];
      GL20.glGetUniformfv(this.program, this.getUniform(variable), results);
      return results;
   }

   public void pass4f(String variable, float v0, float v1, float v2, float v3) {
      GL20.glUniform4f(this.getUniform(variable), v0, v1, v2, v3);
   }

   public float[] get4f(String variable) {
      float[] results = new float[4];
      GL20.glGetUniformfv(this.program, this.getUniform(variable), results);
      return results;
   }

   public void pass1i(String variable, int v0) {
      GL20.glUniform1i(this.getUniform(variable), v0);
   }

   public int get1i(String variable) {
      return GL20.glGetUniformi(this.program, this.getUniform(variable));
   }

   public void pass2i(String variable, int v0, int v1) {
      GL20.glUniform2i(this.getUniform(variable), v0, v1);
   }

   public int[] get2i(String variable) {
      int[] results = new int[2];
      GL20.glGetUniformiv(this.program, this.getUniform(variable), results);
      return results;
   }

   public void pass3i(String variable, int v0, int v1, int v2) {
      GL20.glUniform3i(this.getUniform(variable), v0, v1, v2);
   }

   public int[] get3i(String variable) {
      int[] results = new int[3];
      GL20.glGetUniformiv(this.program, this.getUniform(variable), results);
      return results;
   }

   public void pass4i(String variable, int v0, int v1, int v2, int v3) {
      GL20.glUniform4i(this.getUniform(variable), v0, v1, v2, v3);
   }

   public int[] get4i(String variable) {
      int[] results = new int[4];
      GL20.glGetUniformiv(this.program, this.getUniform(variable), results);
      return results;
   }

   public void pass1(String variable, FloatBuffer values) {
      GL20.glUniform1fv(this.getUniform(variable), values);
   }

   public void pass1(String variable, IntBuffer values) {
      GL20.glUniform1iv(this.getUniform(variable), values);
   }

   public void pass2(String variable, FloatBuffer values) {
      GL20.glUniform2fv(this.getUniform(variable), values);
   }

   public void pass2(String variable, IntBuffer values) {
      GL20.glUniform1iv(this.getUniform(variable), values);
   }

   public void pass3(String variable, FloatBuffer values) {
      GL20.glUniform2fv(this.getUniform(variable), values);
   }

   public void pass3(String variable, IntBuffer values) {
      GL20.glUniform1iv(this.getUniform(variable), values);
   }

   public void pass4(String variable, FloatBuffer values) {
      GL20.glUniform2fv(this.getUniform(variable), values);
   }

   public void pass4(String variable, IntBuffer values) {
      GL20.glUniform1iv(this.getUniform(variable), values);
   }
}
