package com.haraka.animator;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private final ArrayList<Pose> frames = new ArrayList<>();
    private final ArrayList<Pose> undo = new ArrayList<>(), redo = new ArrayList<>();
    private final Handler handler = new Handler();
    private int current = 0;
    private boolean onion = true, playing = false;
    private FigureView figure;
    private LinearLayout timeline;
    private TextView status;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!playing) return;
            if (frames.size() > 1) current = (current + 1) % frames.size();
            update();
            handler.postDelayed(this, 125);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(9,13,22));
        getWindow().setNavigationBarColor(Color.rgb(9,13,22));
        load();
        makeUi();
        update();
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }

    private void makeUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(8), dp(10), dp(6));
        root.setBackgroundColor(Color.rgb(9,13,22));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText("حَرَكة");
        title.setTextSize(25);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(243,247,253));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(35)));

        TextView sub = new TextView(this);
        sub.setText("استوديو تحريك الشخصيات إطارًا بإطار");
        sub.setTextSize(12);
        sub.setTextColor(Color.rgb(145,160,182));
        sub.setGravity(Gravity.CENTER);
        root.addView(sub, new LinearLayout.LayoutParams(-1, dp(22)));

        LinearLayout tools1 = row();
        button(tools1, "تراجع", this::undo);
        button(tools1, "إعادة", this::redo);
        button(tools1, "نسخ", this::duplicate);
        button(tools1, "إطار +", this::addFrame);
        button(tools1, "حذف", this::deleteFrame);
        button(tools1, "الجلد", () -> { onion = !onion; update(); });
        root.addView(scroller(tools1), new LinearLayout.LayoutParams(-1, dp(52)));

        figure = new FigureView();
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, 0, 1f);
        fp.topMargin = dp(4); fp.bottomMargin = dp(4);
        root.addView(figure, fp);

        TextView hint = new TextView(this);
        hint.setText("اسحب المفاصل لتحريك الشخصية");
        hint.setTextColor(Color.rgb(143,160,184));
        hint.setTextSize(11); hint.setGravity(Gravity.CENTER);
        root.addView(hint, new LinearLayout.LayoutParams(-1, dp(21)));

        timeline = row();
        HorizontalScrollView ts = scroller(timeline);
        root.addView(ts, new LinearLayout.LayoutParams(-1, dp(55)));

        LinearLayout tools2 = row();
        button(tools2, "السابق", () -> step(-1));
        button(tools2, "تشغيل", this::togglePlay);
        button(tools2, "التالي", () -> step(1));
        button(tools2, "حفظ", () -> { save(); toast("تم حفظ المشروع"); });
        button(tools2, "PNG", this::exportPng);
        button(tools2, "تصفير", this::reset);
        root.addView(scroller(tools2), new LinearLayout.LayoutParams(-1, dp(52)));

        status = new TextView(this);
        status.setTextColor(Color.rgb(83,214,199));
        status.setTextSize(11); status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(23)));
        setContentView(root);
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private HorizontalScrollView scroller(LinearLayout content) {
        HorizontalScrollView s = new HorizontalScrollView(this);
        s.setHorizontalScrollBarEnabled(false);
        s.addView(content);
        return s;
    }

    private void button(LinearLayout parent, String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label); b.setTextSize(12); b.setAllCaps(false);
        b.setTextColor(Color.rgb(231,238,248));
        b.setMinHeight(dp(40)); b.setPadding(dp(10),0,dp(10),0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(24,33,49)); bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.rgb(48,64,86)); b.setBackground(bg);
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(43));
        p.setMargins(dp(3),dp(3),dp(3),dp(3));
        parent.addView(b,p);
    }

    private void update() {
        if (figure != null) figure.invalidate();
        if (status != null) status.setText("الإطار " + (current+1) + " / " + frames.size() +
                (playing ? "   •   تشغيل" : "   •   تحرير"));
        if (timeline != null) {
            timeline.removeAllViews();
            for (int i=0;i<frames.size();i++) {
                final int index=i;
                TextView cell=new TextView(this);
                cell.setText(String.valueOf(i+1)); cell.setGravity(Gravity.CENTER);
                cell.setTextSize(14); cell.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                cell.setTextColor(i==current ? Color.rgb(8,18,25) : Color.rgb(215,224,238));
                GradientDrawable bg=new GradientDrawable();
                bg.setColor(i==current ? Color.rgb(83,214,199) : Color.rgb(23,32,47));
                bg.setCornerRadius(dp(11)); bg.setStroke(dp(1),Color.rgb(48,63,83));
                cell.setBackground(bg); cell.setOnClickListener(v->{current=index;update();});
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(43),dp(42));
                p.setMargins(dp(3),dp(4),dp(3),dp(4)); timeline.addView(cell,p);
            }
        }
    }

    private void snapshot() {
        undo.add(frames.get(current).copy());
        if (undo.size()>40) undo.remove(0);
        redo.clear();
    }
    private void undo() {
        if (undo.isEmpty()) { toast("لا يوجد ما يمكن التراجع عنه"); return; }
        redo.add(frames.get(current).copy());
        frames.set(current,undo.remove(undo.size()-1)); update();
    }
    private void redo() {
        if (redo.isEmpty()) { toast("لا يوجد إجراء لإعادته"); return; }
        undo.add(frames.get(current).copy());
        frames.set(current,redo.remove(redo.size()-1)); update();
    }
    private void addFrame() {
        frames.add(current+1,frames.get(current).copy()); current++; update();
    }
    private void duplicate() { addFrame(); }
    private void deleteFrame() {
        if (frames.size()==1) { snapshot(); frames.set(0,new Pose()); }
        else { frames.remove(current); current=Math.min(current,frames.size()-1); }
        update();
    }
    private void step(int n) { current=Math.max(0,Math.min(frames.size()-1,current+n)); update(); }
    private void togglePlay() {
        playing=!playing;
        if (playing) handler.post(tick); else handler.removeCallbacks(tick);
        update();
    }
    private void reset() { snapshot(); frames.set(current,new Pose()); update(); }

    private void save() {
        try {
            JSONObject obj=new JSONObject(); obj.put("version",1); obj.put("current",current);
            JSONArray all=new JSONArray();
            for (Pose pose:frames) {
                JSONArray a=new JSONArray();
                for(float v:pose.xy) a.put((double)v);
                all.put(a);
            }
            obj.put("frames",all);
            getSharedPreferences("haraka_project",MODE_PRIVATE).edit().putString("data",obj.toString()).apply();
        } catch(Exception ignored) { toast("تعذر حفظ المشروع"); }
    }
    private void load() {
        try {
            String raw=getSharedPreferences("haraka_project",MODE_PRIVATE).getString("data",null);
            if(raw!=null) {
                JSONObject obj=new JSONObject(raw); JSONArray all=obj.getJSONArray("frames");
                for(int i=0;i<all.length();i++) {
                    JSONArray a=all.getJSONArray(i); Pose p=new Pose();
                    for(int j=0;j<Math.min(a.length(),p.xy.length);j++) p.xy[j]=(float)a.getDouble(j);
                    frames.add(p);
                }
                current=Math.max(0,Math.min(obj.optInt("current",0),frames.size()-1));
            }
        } catch(Exception ignored) { frames.clear(); }
        if(frames.isEmpty()) frames.add(new Pose());
    }

    private void exportPng() {
        if(figure.getWidth()<1 || figure.getHeight()<1) return;
        Bitmap bmp=Bitmap.createBitmap(figure.getWidth(),figure.getHeight(),Bitmap.Config.ARGB_8888);
        figure.draw(new Canvas(bmp));
        try {
            ContentValues values=new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME,"Haraka_Frame_"+(current+1)+".png");
            values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
            if(Build.VERSION.SDK_INT>=29)
                values.put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/Haraka");
            Uri uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
            if(uri==null) throw new Exception("تعذر إنشاء الصورة");
            try(OutputStream out=getContentResolver().openOutputStream(uri)) {
                if(out==null) throw new Exception("تعذر فتح الصورة");
                bmp.compress(Bitmap.CompressFormat.PNG,100,out);
            }
            toast("حُفظت الصورة في مجلد الصور");
        } catch(Exception e) { toast("فشل التصدير: "+e.getMessage()); }
        finally { bmp.recycle(); }
    }

    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }
    @Override protected void onPause() { super.onPause(); save(); }
    @Override protected void onDestroy() {
        super.onDestroy(); playing=false; handler.removeCallbacksAndMessages(null);
    }

    private static class Pose {
        final float[] xy={0,-150, 0,-105, -42,-98, -73,-58, -84,-18,
                42,-98, 73,-58, 84,-18, 0,0, -30,57, -34,112, 30,57, 34,112};
        Pose copy() { Pose p=new Pose(); System.arraycopy(xy,0,p.xy,0,xy.length); return p; }
    }

    private class FigureView extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private float sx=1, ox, oy;
        private int selected=-1;
        FigureView() { super(MainActivity.this); setBackgroundColor(Color.rgb(14,21,33)); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); c.drawColor(Color.rgb(14,21,33));
            paint.setColor(Color.rgb(31,44,62));
            for(int x=dp(16);x<getWidth();x+=dp(28))
                for(int y=dp(16);y<getHeight();y+=dp(28)) c.drawCircle(x,y,dp(1),paint);
            ox=getWidth()/2f; oy=getHeight()*.57f;
            sx=Math.max(.55f,Math.min(1.18f,Math.min(getWidth()/430f,getHeight()/410f)));
            c.save(); c.translate(ox,oy); c.scale(sx,sx);
            paint.setColor(Color.rgb(65,85,108)); paint.setStrokeWidth(2);
            c.drawLine(-175,132,175,132,paint);
            if(onion && current>0) drawPose(c,frames.get(current-1),Color.rgb(83,214,199),65);
            drawPose(c,frames.get(current),Color.rgb(243,247,253),255);
            c.restore();
            paint.setColor(Color.rgb(145,160,183)); paint.setTextSize(dp(11)); paint.setTextAlign(Paint.Align.CENTER);
            c.drawText("اسحب المفاصل لتغيير الوضعية",getWidth()/2f,getHeight()-dp(14),paint);
        }
        private void drawPose(Canvas c,Pose pose,int color,int alpha) {
            float[] p=pose.xy; paint.setColor(color); paint.setAlpha(alpha);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND); paint.setStrokeWidth(9);
            line(c,p,0,1); line(c,p,1,2); line(c,p,2,3); line(c,p,3,4);
            line(c,p,1,5); line(c,p,5,6); line(c,p,6,7); line(c,p,1,8);
            line(c,p,8,9); line(c,p,9,10); line(c,p,8,11); line(c,p,11,12);
            paint.setStyle(Paint.Style.FILL); paint.setAlpha(alpha);
            canvasCircle(c,p[0],p[1],24);
            for(int i=1;i<13;i++) canvasCircle(c,p[i*2],p[i*2+1],7.5f);
            paint.setAlpha(255);
        }
        private void canvasCircle(Canvas c,float x,float y,float r) { c.drawCircle(x,y,r,paint); }
        private void line(Canvas c,float[] p,int a,int b) {
            c.drawLine(p[a*2],p[a*2+1],p[b*2],p[b*2+1],paint);
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            Pose pose=frames.get(current);
            float x=(e.getX()-ox)/sx, y=(e.getY()-oy)/sx;
            if(e.getAction()==MotionEvent.ACTION_DOWN) {
                selected=nearest(pose,x,y);
                if(selected>=0) snapshot();
                return true;
            }
            if(e.getAction()==MotionEvent.ACTION_MOVE && selected>=0) {
                pose.xy[selected*2]=x; pose.xy[selected*2+1]=y; invalidate(); return true;
            }
            if(e.getAction()==MotionEvent.ACTION_UP || e.getAction()==MotionEvent.ACTION_CANCEL) {
                selected=-1; performClick(); return true;
            }
            return true;
        }
        private int nearest(Pose p,float x,float y) {
            int best=-1; float dist=42*42;
            for(int i=0;i<13;i++) {
                float dx=p.xy[i*2]-x,dy=p.xy[i*2+1]-y,d=dx*dx+dy*dy;
                if(d<dist) { dist=d; best=i; }
            }
            return best;
        }
        @Override public boolean performClick() { super.performClick(); return true; }
    }
}
