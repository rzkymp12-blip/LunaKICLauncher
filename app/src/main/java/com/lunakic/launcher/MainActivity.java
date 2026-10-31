package com.lunakic.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(7,21,43));
        getWindow().setNavigationBarColor(Color.rgb(7,21,43));
        setContentView(new LunaView());
    }

    static class AppEntry {
        String label, pkg;
        android.graphics.drawable.Drawable icon;
        AppEntry(String l, String p, android.graphics.drawable.Drawable i) { label=l; pkg=p; icon=i; }
    }

    class LunaView extends View {
        Paint p = new Paint(3);
        float downX, downY, fingerX, fingerY, reveal = 0f, revealAtDown = 0f;
        ArrayList<AppEntry> apps = new ArrayList<>();
        float density;
        LunaView() {
            super(MainActivity.this);
            density = getResources().getDisplayMetrics().density;
            fingerX = getResources().getDisplayMetrics().widthPixels/2f;
            fingerY = getResources().getDisplayMetrics().heightPixels/2f;
            loadApps();
        }
        void loadApps() {
            Intent q = new Intent(Intent.ACTION_MAIN, null);
            q.addCategory(Intent.CATEGORY_LAUNCHER);
            PackageManager pm = getPackageManager();
            for (ResolveInfo r : pm.queryIntentActivities(q, 0)) {
                String pkg = r.activityInfo.packageName;
                if (!pkg.equals(getPackageName()))
                    apps.add(new AppEntry(r.loadLabel(pm).toString(), pkg, r.loadIcon(pm)));
            }
            Collections.sort(apps, (a,b) -> a.label.compareToIgnoreCase(b.label));
        }
        float dp(float v) { return v*density; }
        @Override protected void onDraw(Canvas c) {
            float w=getWidth(), h=getHeight();
            p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff07152b,0xff12385a,0xff07152b},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p); p.setShader(null);
            // Temporary abstract Luna-stage backdrop; character artwork will be added in the next asset pass.
            p.setColor(0x2239cfff);
            for(int i=0;i<9;i++) {
                float x=w*(0.12f+(i%3)*0.38f)+(fingerX-w/2)*0.012f;
                float y=h*(0.27f+(i/3)*0.19f)+(fingerY-h/2)*0.012f;
                c.drawCircle(x,y,dp(34+(i%3)*9),p);
            }
            p.setColor(Color.WHITE); p.setTypeface(Typeface.create("sans-serif-light",0));
            p.setTextSize(dp(15)); c.drawText("LUNA  •  KIC",dp(24),dp(52),p);
            p.setTextSize(dp(46)); c.drawText(new java.text.SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()),dp(24),dp(112),p);
            p.setTextSize(dp(13)); p.setColor(0xffd8f4ff);
            c.drawText(new java.text.SimpleDateFormat("EEEE, dd MMMM",Locale.getDefault()).format(new Date()),dp(27),dp(138),p);
            p.setTextSize(dp(11)); c.drawText("SWIPE UP TO OPEN ICE DASHBOARD",dp(24),h-dp(32),p);
            if(reveal>0.01f) drawDashboard(c,w,h);
        }
        void drawDashboard(Canvas c,float w,float h) {
            float top=h*(1f-reveal);
            p.setColor(0xf2091b33); c.drawRoundRect(dp(9),top+dp(9),w-dp(9),h-dp(9),dp(24),dp(24),p);
            p.setColor(0xff9beaff); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1.3f));
            c.drawRoundRect(dp(9),top+dp(9),w-dp(9),h-dp(9),dp(24),dp(24),p); p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE); p.setTextSize(dp(23)); p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText("ICE DASHBOARD",dp(25),top+dp(52),p);
            p.setTextSize(dp(11)); p.setTypeface(Typeface.DEFAULT); p.setColor(0xffbfeeff);
            c.drawText("Tap an app to launch",dp(25),top+dp(73),p);
            int cols=4; float cell=(w-dp(34))/cols; float startY=top+dp(106);
            for(int i=0;i<apps.size() && i<48;i++) {
                int col=i%cols,row=i/cols; float x=dp(17)+col*cell+cell/2f,y=startY+row*dp(82);
                if(y>h-dp(35)) break;
                p.setColor(0x5539cfff); c.drawCircle(x,y,dp(23),p);
                AppEntry a=apps.get(i); a.icon.setBounds((int)(x-dp(16)),(int)(y-dp(16)),(int)(x+dp(16)),(int)(y+dp(16))); a.icon.draw(c);
                p.setColor(Color.WHITE); p.setTextSize(dp(9));
                String label=a.label.length()>12?a.label.substring(0,11)+"…":a.label;
                c.drawText(label,x-p.measureText(label)/2,y+dp(38),p);
            }
            p.setColor(0xff9beaff); p.setTextSize(dp(10)); c.drawText("SWIPE DOWN TO CLOSE",dp(25),h-dp(24),p);
        }
        void launchAppAt(float x, float y) {
            if (reveal < 0.5f) return;
            float top = getHeight() * (1f - reveal);
            float cell = (getWidth() - dp(34)) / 4f;
            float startY = top + dp(106);
            if (y < startY - dp(40)) return;
            int col = (int)((x - dp(17)) / cell);
            int row = (int)((y - startY + dp(41)) / dp(82));
            if (col < 0 || col >= 4 || row < 0) return;
            int index = row * 4 + col;
            if (index < 0 || index >= apps.size() || index >= 48) return;
            float centerY = startY + row * dp(82);
            if (Math.abs(y - centerY) > dp(43)) return;
            AppEntry app = apps.get(index);
            Intent launch = getPackageManager().getLaunchIntentForPackage(app.pkg);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { startActivity(launch); }
                catch (Exception ex) {
                    android.widget.Toast.makeText(MainActivity.this, "Tidak dapat membuka " + app.label, android.widget.Toast.LENGTH_SHORT).show();
                }
            } else {
                android.widget.Toast.makeText(MainActivity.this, "Aplikasi tidak memiliki layar peluncur", android.widget.Toast.LENGTH_SHORT).show();
            }
        }

        @Override public boolean performClick() {
            super.performClick();
            return true;
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float x=e.getX(), y=e.getY();
            switch(e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX=x; downY=y; fingerX=x; fingerY=y; revealAtDown=reveal;
                    invalidate(); return true;
                case MotionEvent.ACTION_MOVE: {
                    fingerX=x; fingerY=y;
                    float dy=y-downY;
                    reveal=Math.max(0f,Math.min(1f,revealAtDown - dy/(getHeight()*0.65f)));
                    invalidate(); return true;
                }
                case MotionEvent.ACTION_UP: {
                    float dx=x-downX, dy=y-downY;
                    if (Math.abs(dx)<dp(12) && Math.abs(dy)<dp(12)) {
                        performClick();
                        launchAppAt(x,y);
                    } else if (dy < -dp(45)) {
                        reveal=1f;
                    } else if (dy > dp(45)) {
                        reveal=0f;
                    }
                    invalidate(); return true;
                }
                case MotionEvent.ACTION_CANCEL:
                    reveal = reveal > 0.5f ? 1f : 0f;
                    invalidate(); return true;
            }
            return true;
        }
    }
}
