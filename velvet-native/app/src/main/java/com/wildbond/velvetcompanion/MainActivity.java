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
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private CompanionRenderer renderer;
    private TextView status, chat, appearanceLabel;
    private EditText input;
    private LinearLayout appearancePanel;
    private int hairIndex = 0, outfitIndex = 0, eyeIndex = 0, styleIndex = 0;

    private final int[] hairColors = {
        Color.rgb(40,24,18), Color.rgb(18,18,22), Color.rgb(125,28,42),
        Color.rgb(225,190,120), Color.rgb(90,50,125), Color.rgb(190,70,120)
    };
    private final String[] hairNames = {"Kaštanové", "Černé", "Bordó", "Blond", "Fialové", "Růžové"};
    private final int[] outfitColors = {
        Color.rgb(105,24,70), Color.rgb(24,24,30), Color.rgb(130,18,32),
        Color.rgb(45,35,95), Color.rgb(20,72,68), Color.rgb(230,225,218)
    };
    private final String[] outfitNames = {"Velvet", "Noir", "Crimson", "Midnight", "Emerald", "Ivory"};
    private final int[] eyeColors = {
        Color.rgb(72,120,160), Color.rgb(62,120,80), Color.rgb(130,90,55), Color.rgb(120,75,145), Color.rgb(80,80,90)
    };
    private final String[] eyeNames = {"Modré", "Zelené", "Hnědé", "Fialové", "Šedé"};
    private final String[] styleNames = {"Dlouhé", "Bob", "Vlnité"};

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.rgb(7,6,11));
        getWindow().setNavigationBarColor(Color.rgb(7,6,11));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(7,6,11));

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

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        overlay.addView(top, new LinearLayout.LayoutParams(-1,-2));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        top.addView(brand, new LinearLayout.LayoutParams(0,-2,1f));

        TextView title = new TextView(this);
        title.setText("VELVET");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        brand.addView(title);

        status = new TextView(this);
        status.setText("online • čekám na tebe");
        status.setTextColor(Color.rgb(192,172,204));
        status.setTextSize(12);
        brand.addView(status);

        Button look = new Button(this);
        look.setText("VZHLED ✦");
        top.addView(look, new LinearLayout.LayoutParams(-2,-2));

        appearancePanel = new LinearLayout(this);
        appearancePanel.setOrientation(LinearLayout.VERTICAL);
        appearancePanel.setPadding(16,12,16,12);
        appearancePanel.setBackgroundColor(Color.argb(235,22,16,30));
        appearancePanel.setVisibility(View.GONE);
        overlay.addView(appearancePanel, new LinearLayout.LayoutParams(-1,-2));

        appearanceLabel = new TextView(this);
        appearanceLabel.setTextColor(Color.WHITE);
        appearanceLabel.setTextSize(13);
        appearancePanel.addView(appearanceLabel);

        addAppearanceButton("VLASY", v -> { hairIndex=(hairIndex+1)%hairColors.length; renderer.hairColor=hairColors[hairIndex]; refreshAppearance(); });
        addAppearanceButton("STŘIH", v -> { styleIndex=(styleIndex+1)%styleNames.length; renderer.hairStyle=styleIndex; refreshAppearance(); });
        addAppearanceButton("OČI", v -> { eyeIndex=(eyeIndex+1)%eyeColors.length; renderer.eyeColor=eyeColors[eyeIndex]; refreshAppearance(); });
        addAppearanceButton("OUTFIT", v -> { outfitIndex=(outfitIndex+1)%outfitColors.length; renderer.outfitColor=outfitColors[outfitIndex]; refreshAppearance(); });
        refreshAppearance();
        look.setOnClickListener(v -> appearancePanel.setVisibility(appearancePanel.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));

        View spacer = new View(this);
        overlay.addView(spacer, new LinearLayout.LayoutParams(1,0,1f));

        chat = new TextView(this);
        chat.setText("Velvet: Ahoj ♡ Můžeš mi říkat, co mám dělat, nebo si změnit vzhled nahoře.");
        chat.setTextColor(Color.WHITE);
        chat.setTextSize(14);
        chat.setPadding(18,14,18,14);
        chat.setBackgroundColor(Color.argb(220,18,14,25));
        overlay.addView(chat, new LinearLayout.LayoutParams(-1,-2));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0,12,0,0);
        overlay.addView(row, new LinearLayout.LayoutParams(-1,-2));

        input = new EditText(this);
        input.setHint("Řekni jí, co má udělat…");
        input.setHintTextColor(Color.rgb(140,128,150));
        input.setTextColor(Color.WHITE);
        input.setSingleLine(true);
        input.setBackgroundColor(Color.argb(225,31,22,41));
        input.setPadding(16,10,16,10);
        row.addView(input, new LinearLayout.LayoutParams(0,-2,1f));

        Button go = new Button(this);
        go.setText("➤");
        row.addView(go, new LinearLayout.LayoutParams(-2,-2));
        go.setOnClickListener(v -> submit());
        input.setOnEditorActionListener((v, actionId, event) -> { submit(); return true; });

        setContentView(root);
    }

    private void addAppearanceButton(String label, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setOnClickListener(listener);
        appearancePanel.addView(b, new LinearLayout.LayoutParams(-1,-2));
    }

    private void refreshAppearance() {
        if (appearanceLabel != null) appearanceLabel.setText(
            "Vlasy: " + hairNames[hairIndex] + "  •  " + styleNames[styleIndex] +
            "\nOči: " + eyeNames[eyeIndex] + "  •  Outfit: " + outfitNames[outfitIndex]
        );
    }

    private void submit() {
        String raw = input.getText().toString().trim();
        if (raw.isEmpty()) return;
        String t = raw.toLowerCase(Locale.getDefault());
        String reply;
        if (containsAny(t,"zamávej","mávni","wave")) {
            renderer.action = CompanionRenderer.Action.WAVE; status.setText("online • mávám"); reply = "Jasně 🙂";
        } else if (containsAny(t,"otoč se","otoč","turn around")) {
            renderer.action = CompanionRenderer.Action.TURN; status.setText("online • otáčím se"); reply = "Tak jo — otáčím se.";
        } else if (containsAny(t,"pojď blíž","blíž","come closer")) {
            renderer.action = CompanionRenderer.Action.CLOSER; status.setText("online • jsem blíž"); reply = "Už jsem blíž ♡";
        } else if (containsAny(t,"sedni","sednout","sit")) {
            renderer.action = CompanionRenderer.Action.SIT; status.setText("online • sedím"); reply = "Dobře, sednu si.";
        } else if (containsAny(t,"usměj","úsměv","smile")) {
            renderer.action = CompanionRenderer.Action.SMILE; status.setText("online • usmívám se"); reply = "Takovýhle úsměv? 😊";
        } else if (containsAny(t,"zatancuj","tanči","dance")) {
            renderer.action = CompanionRenderer.Action.DANCE; status.setText("online • tančím"); reply = "Dobře 😏";
        } else if (containsAny(t,"skoč","jump")) {
            renderer.action = CompanionRenderer.Action.JUMP; status.setText("online • skáču"); reply = "Hop!";
        } else if (containsAny(t,"pózu","pozu","pose")) {
            renderer.action = CompanionRenderer.Action.POSE; status.setText("online • pózuju"); reply = "Co říkáš na tuhle?";
        } else if (containsAny(t,"vstaň","stůj","reset","normálně")) {
            renderer.action = CompanionRenderer.Action.IDLE; status.setText("online • čekám na tebe"); reply = "Jsem zpátky.";
        } else {
            reply = "Tomu zatím nerozumím jako pohybu, ale slyším tě.";
            status.setText("online • poslouchám");
        }
        chat.setText("Ty: " + raw + "\nVelvet: " + reply);
        input.setText("");
    }

    private boolean containsAny(String text, String... words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }

    public static class CompanionRenderer implements GLSurfaceView.Renderer {
        enum Action { IDLE, WAVE, TURN, CLOSER, SIT, SMILE, DANCE, JUMP, POSE }
        volatile Action action = Action.IDLE;
        volatile int hairColor = Color.rgb(40,24,18);
        volatile int outfitColor = Color.rgb(105,24,70);
        volatile int eyeColor = Color.rgb(72,120,160);
        volatile int hairStyle = 0;
        private Mesh cube, sphere;
        private final float[] projection = new float[16], view = new float[16], vp = new float[16], root = new float[16], mvp = new float[16];
        private float phase = 0f;

        @Override public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
            GLES20.glClearColor(0.025f,0.018f,0.038f,1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            cube = Mesh.cube();
            sphere = Mesh.sphere(16,24);
        }

        @Override public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
            GLES20.glViewport(0,0,width,height);
            float ratio = (float) width / Math.max(1,height);
            Matrix.perspectiveM(projection,0,40f,ratio,0.1f,100f);
            Matrix.setLookAtM(view,0,0f,0.82f,5.4f,0f,0.55f,0f,0f,1f,0f);
            Matrix.multiplyMM(vp,0,projection,0,view,0);
        }

        @Override public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            phase += 0.035f;
            float rootY = action == Action.SIT ? -0.42f : 0f;
            if (action == Action.JUMP) rootY += Math.max(0f,(float)Math.sin(phase*2.4f))*0.7f;
            float rootZ = action == Action.CLOSER ? 1.05f : 0f;
            float turn = action == Action.TURN ? 180f : 0f;
            float sway = action == Action.DANCE ? (float)Math.sin(phase*2.2f)*14f : 0f;
            float bob = (float)Math.sin(phase)*0.018f;

            Matrix.setIdentityM(root,0);
            Matrix.translateM(root,0,0f,rootY+bob,rootZ);
            Matrix.rotateM(root,0,turn+sway,0f,1f,0f);

            float[] skin = rgb(Color.rgb(232,188,166));
            float[] hair = rgb(hairColor);
            float[] outfit = rgb(outfitColor);
            float[] eyes = rgb(eyeColor);

            // hair back + head
            drawSphere(0f,1.38f,0.08f,0.39f,0.49f,0.35f,hair,0,0,0);
            drawSphere(0f,1.38f,-0.02f,0.33f,0.39f,0.30f,skin,0,0,0);

            // hair styles
            if (hairStyle == 0) {
                drawSphere(-0.28f,1.06f,0.08f,0.15f,0.52f,0.14f,hair,0,0,8);
                drawSphere(0.28f,1.06f,0.08f,0.15f,0.52f,0.14f,hair,0,0,-8);
            } else if (hairStyle == 1) {
                drawSphere(-0.27f,1.22f,0.06f,0.14f,0.30f,0.13f,hair,0,0,5);
                drawSphere(0.27f,1.22f,0.06f,0.14f,0.30f,0.13f,hair,0,0,-5);
            } else {
                drawSphere(-0.29f,1.05f,0.08f,0.17f,0.46f,0.16f,hair,0,0,15);
                drawSphere(0.29f,1.05f,0.08f,0.17f,0.46f,0.16f,hair,0,0,-15);
            }

            // eyes + mouth
            drawSphere(-0.105f,1.42f,-0.287f,0.045f,0.035f,0.025f,eyes,0,0,0);
            drawSphere(0.105f,1.42f,-0.287f,0.045f,0.035f,0.025f,eyes,0,0,0);
            float smileY = action == Action.SMILE ? 1.27f : 1.29f;
            drawCube(0f,smileY,-0.29f, action==Action.SMILE?0.13f:0.09f,0.018f,0.02f,new float[]{0.68f,0.17f,0.28f},0,0, action==Action.SMILE?5:0);

            // neck, shoulders, torso, waist, hips
            drawSphere(0f,1.08f,0f,0.13f,0.18f,0.13f,skin,0,0,0);
            drawSphere(0f,0.86f,0f,0.47f,0.36f,0.28f,outfit,0,0,0);
            drawSphere(0f,0.55f,0f,0.34f,0.28f,0.24f,outfit,0,0,0);
            drawSphere(0f,0.29f,0f,0.44f,0.26f,0.30f,outfit,0,0,0);

            float rightArm = action == Action.WAVE ? (float)Math.sin(phase*3.0f)*28f-78f : (action==Action.POSE?-55f:-12f);
            float leftArm = action == Action.POSE ? 45f : 12f;
            drawLimb(-0.42f,0.82f,0f,0.12f,0.40f,0.12f,skin,0,0,leftArm);
            drawLimb(-0.49f,0.45f,0f,0.105f,0.34f,0.105f,skin,0,0,leftArm/2f);
            drawLimb(0.42f,0.82f,0f,0.12f,0.40f,0.12f,skin,0,0,rightArm);
            drawLimb(0.51f,0.48f,0f,0.105f,0.34f,0.105f,skin,0,0,rightArm/2f);

            float upperLegRot = action == Action.SIT ? 67f : 0f;
            float lowerLegRot = action == Action.SIT ? -70f : 0f;
            drawLimb(-0.20f,-0.08f,0f,0.18f,0.48f,0.18f,skin,upperLegRot,0,0);
            drawLimb(0.20f,-0.08f,0f,0.18f,0.48f,0.18f,skin,upperLegRot,0,0);
            drawLimb(-0.20f,-0.53f,action==Action.SIT?0.29f:0f,0.145f,0.46f,0.145f,skin,lowerLegRot,0,0);
            drawLimb(0.20f,-0.53f,action==Action.SIT?0.29f:0f,0.145f,0.46f,0.145f,skin,lowerLegRot,0,0);
            drawSphere(-0.20f,-0.94f,action==Action.SIT?0.55f:-0.06f,0.17f,0.10f,0.30f,outfit,0,0,0);
            drawSphere(0.20f,-0.94f,action==Action.SIT?0.55f:-0.06f,0.17f,0.10f,0.30f,outfit,0,0,0);
        }

        private float[] rgb(int c) { return new float[]{Color.red(c)/255f,Color.green(c)/255f,Color.blue(c)/255f}; }

        private void drawLimb(float x,float y,float z,float sx,float sy,float sz,float[] c,float rx,float ry,float rz) {
            drawSphere(x,y,z,sx,sy,sz,c,rx,ry,rz);
        }

        private void drawSphere(float x,float y,float z,float sx,float sy,float sz,float[] c,float rx,float ry,float rz) {
            drawMesh(sphere,x,y,z,sx,sy,sz,c,rx,ry,rz);
        }
        private void drawCube(float x,float y,float z,float sx,float sy,float sz,float[] c,float rx,float ry,float rz) {
            drawMesh(cube,x,y,z,sx,sy,sz,c,rx,ry,rz);
        }
        private void drawMesh(Mesh mesh,float x,float y,float z,float sx,float sy,float sz,float[] c,float rx,float ry,float rz) {
            float[] local=new float[16], world=new float[16];
            Matrix.setIdentityM(local,0);
            Matrix.translateM(local,0,x,y,z);
            Matrix.rotateM(local,0,rx,1,0,0);
            Matrix.rotateM(local,0,ry,0,1,0);
            Matrix.rotateM(local,0,rz,0,0,1);
            Matrix.scaleM(local,0,sx,sy,sz);
            Matrix.multiplyMM(world,0,root,0,local,0);
            Matrix.multiplyMM(mvp,0,vp,0,world,0);
            mesh.draw(mvp,c[0],c[1],c[2]);
        }
    }

    static class Mesh {
        private final FloatBuffer vertices;
        private final int count, program, aPos, uMvp, uColor;

        private Mesh(float[] v) {
            count=v.length/3;
            vertices=ByteBuffer.allocateDirect(v.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            vertices.put(v).position(0);
            String vs="attribute vec3 aPos; uniform mat4 uMvp; void main(){ gl_Position=uMvp*vec4(aPos,1.0); }";
            String fs="precision mediump float; uniform vec3 uColor; void main(){ gl_FragColor=vec4(uColor,1.0); }";
            program=link(vs,fs);
            aPos=GLES20.glGetAttribLocation(program,"aPos");
            uMvp=GLES20.glGetUniformLocation(program,"uMvp");
            uColor=GLES20.glGetUniformLocation(program,"uColor");
        }

        static Mesh cube() {
            return new Mesh(new float[]{
                -1,-1,-1, 1,-1,-1, 1,1,-1, -1,-1,-1, 1,1,-1, -1,1,-1,
                -1,-1,1, 1,1,1, 1,-1,1, -1,-1,1, -1,1,1, 1,1,1,
                -1,-1,-1, -1,1,-1, -1,1,1, -1,-1,-1, -1,1,1, -1,-1,1,
                1,-1,-1, 1,-1,1, 1,1,1, 1,-1,-1, 1,1,1, 1,1,-1,
                -1,-1,-1, -1,-1,1, 1,-1,1, -1,-1,-1, 1,-1,1, 1,-1,-1,
                -1,1,-1, 1,1,1, -1,1,1, -1,1,-1, 1,1,-1, 1,1,1
            });
        }

        static Mesh sphere(int stacks,int slices) {
            ArrayList<Float> out=new ArrayList<>();
            for(int i=0;i<stacks;i++) {
                float p1=(float)Math.PI*(-0.5f+(float)i/stacks);
                float p2=(float)Math.PI*(-0.5f+(float)(i+1)/stacks);
                for(int j=0;j<slices;j++) {
                    float t1=(float)(2*Math.PI*j/slices), t2=(float)(2*Math.PI*(j+1)/slices);
                    add(out,p1,t1); add(out,p2,t1); add(out,p2,t2);
                    add(out,p1,t1); add(out,p2,t2); add(out,p1,t2);
                }
            }
            float[] v=new float[out.size()]; for(int i=0;i<v.length;i++) v[i]=out.get(i);
            return new Mesh(v);
        }
        private static void add(ArrayList<Float> o,float p,float t) {
            float cp=(float)Math.cos(p);
            o.add(cp*(float)Math.cos(t)); o.add((float)Math.sin(p)); o.add(cp*(float)Math.sin(t));
        }

        void draw(float[] mvp,float r,float g,float b) {
            GLES20.glUseProgram(program);
            GLES20.glEnableVertexAttribArray(aPos);
            GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,0,vertices);
            GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);
            GLES20.glUniform3f(uColor,r,g,b);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
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
