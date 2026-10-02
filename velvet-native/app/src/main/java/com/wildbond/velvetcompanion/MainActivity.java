package com.wildbond.velvetcompanion;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;

public class MainActivity extends Activity {
    private CompanionRenderer renderer;
    private TextView status;
    private TextView chat;
    private EditText input;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.rgb(8,8,12));
        getWindow().setNavigationBarColor(Color.rgb(8,8,12));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(8,8,12));

        GLSurfaceView gl = new GLSurfaceView(this);
        gl.setEGLContextClientVersion(2);
        renderer = new CompanionRenderer();
        gl.setRenderer(renderer);
        gl.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        root.addView(gl, new FrameLayout.LayoutParams(-1,-1));

        LinearLayout overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setPadding(24,18,24,24);
        root.addView(overlay, new FrameLayout.LayoutParams(-1,-1));

        TextView title = new TextView(this);
        title.setText("VELVET");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setGravity(Gravity.START);
        overlay.addView(title, new LinearLayout.LayoutParams(-1,-2));

        status = new TextView(this);
        status.setText("Čekám na tebe");
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(13);
        overlay.addView(status, new LinearLayout.LayoutParams(-1,-2));

        View spacer = new View(this);
        overlay.addView(spacer, new LinearLayout.LayoutParams(1,0,1f));

        chat = new TextView(this);
        chat.setText("Velvet: Ahoj. Zkus: zamávej, otoč se, pojď blíž, sedni si, usměj se nebo vstaň.");
        chat.setTextColor(Color.WHITE);
        chat.setTextSize(14);
        chat.setPadding(18,14,18,14);
        chat.setBackgroundColor(Color.argb(215,20,20,28));
        overlay.addView(chat, new LinearLayout.LayoutParams(-1,-2));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0,12,0,0);
        overlay.addView(row, new LinearLayout.LayoutParams(-1,-2));

        input = new EditText(this);
        input.setHint("Řekni jí, co má udělat…");
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);
        input.setSingleLine(true);
        input.setBackgroundColor(Color.argb(210,32,24,40));
        input.setPadding(16,10,16,10);
        row.addView(input, new LinearLayout.LayoutParams(0,-2,1f));

        Button go = new Button(this);
        go.setText("GO");
        row.addView(go, new LinearLayout.LayoutParams(-2,-2));
        go.setOnClickListener(v -> submit());
        input.setOnEditorActionListener((v, actionId, event) -> { submit(); return true; });

        setContentView(root);
    }

    private void submit() {
        String raw = input.getText().toString().trim();
        if (raw.isEmpty()) return;
        String t = raw.toLowerCase(Locale.getDefault());
        String reply;
        if (containsAny(t,"zamávej","mávni","wave")) {
            renderer.action = CompanionRenderer.Action.WAVE; status.setText("Mávám"); reply = "Jasně 🙂";
        } else if (containsAny(t,"otoč se","otoč","turn around")) {
            renderer.action = CompanionRenderer.Action.TURN; status.setText("Otáčím se"); reply = "Tak jo — otáčím se.";
        } else if (containsAny(t,"pojď blíž","blíž","come closer")) {
            renderer.action = CompanionRenderer.Action.CLOSER; status.setText("Jsem blíž"); reply = "Už jsem blíž.";
        } else if (containsAny(t,"sedni","sednout","sit")) {
            renderer.action = CompanionRenderer.Action.SIT; status.setText("Sedím"); reply = "Dobře, sednu si.";
        } else if (containsAny(t,"usměj","úsměv","smile")) {
            renderer.action = CompanionRenderer.Action.SMILE; status.setText("Usmívám se"); reply = "Takovýhle úsměv? 😊";
        } else if (containsAny(t,"vstaň","stůj","reset","normálně")) {
            renderer.action = CompanionRenderer.Action.IDLE; status.setText("Čekám na tebe"); reply = "Jsem zpátky.";
        } else {
            reply = "Tomu zatím nerozumím jako pohybu, ale slyším tě.";
            status.setText("Poslouchám");
        }
        chat.setText("Ty: " + raw + "\nVelvet: " + reply);
        input.setText("");
    }

    private boolean containsAny(String text, String... words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }

    public static class CompanionRenderer implements GLSurfaceView.Renderer {
        enum Action { IDLE, WAVE, TURN, CLOSER, SIT, SMILE }
        volatile Action action = Action.IDLE;
        private Cube cube;
        private final float[] projection = new float[16], view = new float[16], vp = new float[16], model = new float[16], mvp = new float[16];
        private float phase = 0f;

        @Override public void onSurfaceCreated(javax.microedition.khronos.egl.EGLConfig config) {
            GLES20.glClearColor(0.025f,0.02f,0.04f,1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            cube = new Cube();
        }

        @Override public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
            GLES20.glViewport(0,0,width,height);
            float ratio = (float) width / Math.max(1,height);
            Matrix.perspectiveM(projection,0,45f,ratio,0.1f,100f);
            Matrix.setLookAtM(view,0,0f,0.8f,5.6f,0f,0.5f,0f,0f,1f,0f);
            Matrix.multiplyMM(vp,0,projection,0,view,0);
        }

        @Override public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            phase += 0.035f;
            float rootY = action == Action.SIT ? -0.45f : 0f;
            float rootZ = action == Action.CLOSER ? 1.1f : 0f;
            float turn = action == Action.TURN ? 180f : 0f;
            float bob = (float)Math.sin(phase) * 0.025f;

            Matrix.setIdentityM(model,0);
            Matrix.translateM(model,0,0f,rootY + bob,rootZ);
            Matrix.rotateM(model,0,turn,0f,1f,0f);

            drawPart(0f,1.25f,0f,0.34f,0.42f,0.30f,0.92f,0.72f,0.82f,0f,0f,0f); // head
            drawPart(0f,0.78f,0f,0.52f,0.68f,0.34f,0.48f,0.22f,0.62f,0f,0f,0f); // torso
            drawPart(0f,0.35f,0f,0.56f,0.30f,0.38f,0.52f,0.18f,0.55f,0f,0f,0f); // hips

            float wave = action == Action.WAVE ? (float)Math.sin(phase*3f)*35f - 55f : -8f;
            drawPart(-0.38f,0.83f,0f,0.14f,0.55f,0.14f,0.72f,0.46f,0.78f,0f,0f,12f);
            drawPart(-0.48f,0.45f,0f,0.12f,0.48f,0.12f,0.80f,0.58f,0.72f,0f,0f,8f);
            drawPart(0.38f,0.83f,0f,0.14f,0.55f,0.14f,0.72f,0.46f,0.78f,0f,0f,-12f);
            drawPart(0.50f,0.55f,0f,0.12f,0.48f,0.12f,0.80f,0.58f,0.72f,0f,0f,wave);

            float knee = action == Action.SIT ? 68f : 0f;
            drawPart(-0.18f,-0.10f,0f,0.18f,0.62f,0.18f,0.34f,0.20f,0.50f,knee,0f,0f);
            drawPart(0.18f,-0.10f,0f,0.18f,0.62f,0.18f,0.34f,0.20f,0.50f,knee,0f,0f);
            drawPart(-0.18f,-0.63f, action==Action.SIT ? 0.30f : 0f,0.16f,0.58f,0.16f,0.28f,0.18f,0.42f, action==Action.SIT ? -70f : 0f,0f,0f);
            drawPart(0.18f,-0.63f, action==Action.SIT ? 0.30f : 0f,0.16f,0.58f,0.16f,0.28f,0.18f,0.42f, action==Action.SIT ? -70f : 0f,0f,0f);

            if (action == Action.SMILE) {
                drawPart(-0.10f,1.28f,-0.18f,0.035f,0.035f,0.035f,1f,1f,1f,0f,0f,0f);
                drawPart(0.10f,1.28f,-0.18f,0.035f,0.035f,0.035f,1f,1f,1f,0f,0f,0f);
                drawPart(0f,1.13f,-0.19f,0.13f,0.025f,0.025f,1f,0.35f,0.55f,0f,0f,0f);
            }
        }

        private void drawPart(float x,float y,float z,float sx,float sy,float sz,float r,float g,float b,float rx,float ry,float rz) {
            float[] local = new float[16], world = new float[16];
            Matrix.setIdentityM(local,0);
            Matrix.translateM(local,0,x,y,z);
            Matrix.rotateM(local,0,rx,1,0,0);
            Matrix.rotateM(local,0,ry,0,1,0);
            Matrix.rotateM(local,0,rz,0,0,1);
            Matrix.scaleM(local,0,sx,sy,sz);
            Matrix.multiplyMM(world,0,model,0,local,0);
            Matrix.multiplyMM(mvp,0,vp,0,world,0);
            cube.draw(mvp,r,g,b);
        }
    }

    static class Cube {
        private final FloatBuffer vertices;
        private final int program;
        private final int aPos,uMvp,uColor;
        private static final float[] V = {
            -1,-1,-1, 1,-1,-1, 1,1,-1, -1,-1,-1, 1,1,-1, -1,1,-1,
            -1,-1,1, 1,1,1, 1,-1,1, -1,-1,1, -1,1,1, 1,1,1,
            -1,-1,-1, -1,1,-1, -1,1,1, -1,-1,-1, -1,1,1, -1,-1,1,
            1,-1,-1, 1,-1,1, 1,1,1, 1,-1,-1, 1,1,1, 1,1,-1,
            -1,-1,-1, -1,-1,1, 1,-1,1, -1,-1,-1, 1,-1,1, 1,-1,-1,
            -1,1,-1, 1,1,1, -1,1,1, -1,1,-1, 1,1,-1, 1,1,1
        };
        Cube() {
            vertices = ByteBuffer.allocateDirect(V.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            vertices.put(V).position(0);
            String vs = "attribute vec3 aPos; uniform mat4 uMvp; void main(){ gl_Position=uMvp*vec4(aPos,1.0); }";
            String fs = "precision mediump float; uniform vec3 uColor; void main(){ gl_FragColor=vec4(uColor,1.0); }";
            program = link(vs,fs);
            aPos = GLES20.glGetAttribLocation(program,"aPos");
            uMvp = GLES20.glGetUniformLocation(program,"uMvp");
            uColor = GLES20.glGetUniformLocation(program,"uColor");
        }
        void draw(float[] mvp,float r,float g,float b) {
            GLES20.glUseProgram(program);
            GLES20.glEnableVertexAttribArray(aPos);
            GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,0,vertices);
            GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);
            GLES20.glUniform3f(uColor,r,g,b);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,V.length/3);
            GLES20.glDisableVertexAttribArray(aPos);
        }
        private static int shader(int type,String src) {
            int s=GLES20.glCreateShader(type); GLES20.glShaderSource(s,src); GLES20.glCompileShader(s); return s;
        }
        private static int link(String vs,String fs) {
            int p=GLES20.glCreateProgram(); GLES20.glAttachShader(p,shader(GLES20.GL_VERTEX_SHADER,vs)); GLES20.glAttachShader(p,shader(GLES20.GL_FRAGMENT_SHADER,fs)); GLES20.glLinkProgram(p); return p;
        }
    }
}
