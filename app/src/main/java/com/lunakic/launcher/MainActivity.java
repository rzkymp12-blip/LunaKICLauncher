package com.lunakic.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.animation.ValueAnimator;
import android.view.animation.PathInterpolator;
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
        ValueAnimator snapAnimator;
        String searchText = "";
        String selectedCategory = "All Apps";
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

        void settleReveal(float target) {
            if (snapAnimator != null) snapAnimator.cancel();
            snapAnimator = ValueAnimator.ofFloat(reveal, target);
            snapAnimator.setDuration(420);
            snapAnimator.setInterpolator(new PathInterpolator(0.22f, 1f, 0.36f, 1f));
            snapAnimator.addUpdateListener(a -> {
                reveal = (Float)a.getAnimatedValue();
                invalidate();
            });
            snapAnimator.start();
        }
        @Override protected void onDraw(Canvas c) {
            float w=getWidth(), h=getHeight();
            p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff061326,0xff103957,0xff07172d},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p); p.setShader(null);
            // Ice-blue ambient glow follows the user's finger.
            p.setColor(0x1839cfff);
            for(int i=0;i<10;i++) {
                float x=w*(0.08f+(i%3)*0.42f)+(fingerX-w/2)*0.012f;
                float y=h*(0.12f+(i/3)*0.22f)+(fingerY-h/2)*0.012f;
                c.drawCircle(x,y,dp(30+(i%3)*10),p);
            }
            drawHome(c,w,h);
            // Dashboard rises from the bottom with a soft, character-like ease.
            float eased = reveal * reveal * (3f - 2f * reveal);
            if (eased > 0.001f) {
                int save=c.save();
                float offset=(1f-eased)*h;
                c.translate(0,offset);
                drawDashboard(c,w,h);
                c.restoreToCount(save);
            }
        }

        void drawGlassCard(Canvas c,float left,float top,float right,float bottom,float radius) {
            p.setShader(new LinearGradient(left,top,right,bottom,0x553b9fc4,0x2226d8ff,Shader.TileMode.CLAMP));
            c.drawRoundRect(left,top,right,bottom,radius,radius,p);
            p.setShader(null);
            p.setColor(0x889beaff); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1));
            c.drawRoundRect(left,top,right,bottom,radius,radius,p);
            p.setStyle(Paint.Style.FILL);
        }

        int batteryPercent() {
            Intent battery = registerReceiver(null, new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery == null) return -1;
            int level = battery.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
            return level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
        }

        void drawHome(Canvas c,float w,float h) {
            p.setColor(Color.WHITE); p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            p.setTextSize(dp(14)); c.drawText("LUNA  /  KIC",dp(24),dp(48),p);
            p.setColor(0xffa9eaff); p.setTextSize(dp(10));
            c.drawText("ICE BLUE INTERACTIVE LAUNCHER",dp(24),dp(66),p);
            p.setColor(Color.WHITE); p.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
            p.setTextSize(dp(54));
            c.drawText(new java.text.SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()),dp(22),dp(145),p);
            p.setColor(0xffd5f3ff); p.setTextSize(dp(14));
            c.drawText(new java.text.SimpleDateFormat("EEEE, dd MMMM",Locale.getDefault()).format(new Date()),dp(26),dp(171),p);

            float cardTop=dp(194), cardBottom=dp(274);
            drawGlassCard(c,dp(18),cardTop,w-dp(18),cardBottom,dp(18));
            p.setColor(0xff9beaff); p.setTextSize(dp(10)); p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText("DEVICE STATUS",dp(34),cardTop+dp(24),p);
            int battery=batteryPercent();
            p.setColor(Color.WHITE); p.setTextSize(dp(21)); p.setTypeface(Typeface.create("sans-serif-medium",0));
            c.drawText(battery>=0 ? battery+"% BATTERY" : "BATTERY STATUS",dp(34),cardTop+dp(52),p);
            p.setColor(0xffbcecff); p.setTextSize(dp(10)); p.setTypeface(Typeface.DEFAULT);
            c.drawText("QUICK ACCESS  •  "+apps.size()+" APPS",dp(34),cardTop+dp(68),p);

            float sectionY=Math.max(dp(315),h*0.43f);
            p.setColor(Color.WHITE); p.setTextSize(dp(16)); p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText("Aplikasi favorit",dp(22),sectionY,p);
            p.setColor(0xffa9eaff); p.setTextSize(dp(10)); p.setTypeface(Typeface.DEFAULT);
            c.drawText("Ketuk ikon untuk membuka",dp(22),sectionY+dp(18),p);

            float cell=w/4f;
            float firstY=sectionY+dp(65);
            for(int i=0;i<apps.size() && i<8;i++) {
                int col=i%4,row=i/4;
                float x=cell*(col+0.5f), y=firstY+row*dp(82);
                p.setColor(0x5539cfff); c.drawCircle(x,y,dp(25),p);
                AppEntry a=apps.get(i);
                a.icon.setBounds((int)(x-dp(18)),(int)(y-dp(18)),(int)(x+dp(18)),(int)(y+dp(18)));
                a.icon.draw(c);
                p.setColor(Color.WHITE); p.setTextSize(dp(9));
                String label=a.label.length()>13?a.label.substring(0,12)+"…":a.label;
                c.drawText(label,x-p.measureText(label)/2,y+dp(40),p);
            }
            p.setColor(0xff9beaff); p.setTextSize(dp(10));
            c.drawText("GESER KE ATAS UNTUK SEMUA APLIKASI",dp(22),h-dp(28),p);
        }

        void launchApp(AppEntry app) {
            Intent launch=getPackageManager().getLaunchIntentForPackage(app.pkg);
            if(launch!=null) {
                try { startActivity(launch); }
                catch(Exception ex) {
                    android.widget.Toast.makeText(MainActivity.this,"Tidak dapat membuka "+app.label,android.widget.Toast.LENGTH_SHORT).show();
                }
            } else {
                android.widget.Toast.makeText(MainActivity.this,"Aplikasi tidak memiliki layar peluncur",android.widget.Toast.LENGTH_SHORT).show();
            }
        }

        void launchHomeAppAt(float x,float y) {
            if(reveal>=0.5f) return;
            float sectionY=Math.max(dp(315),getHeight()*0.43f);
            float firstY=sectionY+dp(65);
            int col=(int)(x/(getWidth()/4f));
            int row=Math.round((y-firstY)/dp(82));
            if(col<0||col>=4||row<0||row>1) return;
            float centerY=firstY+row*dp(82);
            if(Math.abs(y-centerY)>dp(45)) return;
            int index=row*4+col;
            if(index>=0&&index<apps.size()&&index<8) launchApp(apps.get(index));
        }
        ArrayList<AppEntry> visibleApps() {
            ArrayList<AppEntry> result = new ArrayList<>();
            String needle = searchText.toLowerCase(Locale.getDefault());
            for (AppEntry a : apps) {
                String meta = (a.label + " " + a.pkg).toLowerCase(Locale.getDefault());
                if (!needle.isEmpty() && !meta.contains(needle)) continue;
                if (!"All Apps".equals(selectedCategory)) {
                    boolean match;
                    if ("Games".equals(selectedCategory)) match = meta.contains("game") || meta.contains("play") || meta.contains("pubg") || meta.contains("mobile.legends") || meta.contains("roblox") || meta.contains("genshin");
                    else if ("Social".equals(selectedCategory)) match = meta.contains("social") || meta.contains("whatsapp") || meta.contains("instagram") || meta.contains("facebook") || meta.contains("telegram") || meta.contains("tiktok") || meta.contains("discord") || meta.contains("messenger") || meta.contains("twitter") || meta.contains("x.");
                    else match = meta.contains("settings") || meta.contains("camera") || meta.contains("file") || meta.contains("clock") || meta.contains("calendar") || meta.contains("calculator") || meta.contains("bluetooth") || meta.contains("browser") || meta.contains("chrome") || meta.contains("maps");
                    if (!match) continue;
                }
                result.add(a);
            }
            return result;
        }

        void showSearchDialog() {
            final android.widget.EditText edit = new android.widget.EditText(MainActivity.this);
            edit.setSingleLine(true);
            edit.setText(searchText);
            edit.setHint("Nama aplikasi...");
            new android.app.AlertDialog.Builder(MainActivity.this)
                .setTitle("Cari aplikasi")
                .setView(edit)
                .setNegativeButton("Batal", (d,w) -> {})
                .setPositiveButton("Cari", (d,w) -> { searchText = edit.getText().toString().trim(); invalidate(); })
                .show();
        }

        void drawDashboard(Canvas c,float w,float h) {
            // Full-screen dashboard canvas. Do not leave the home-screen widgets visible behind it.
            p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff061326,0xff102f50,0xff06172d},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p); p.setShader(null);
            p.setColor(0x1a48bfff);
            for(int i=0;i<12;i++) {
                float gx=w*(0.08f+(i%3)*0.42f), gy=h*(0.08f+(i/3)*0.23f);
                c.drawCircle(gx,gy,dp(34+(i%3)*10),p);
            }
            float panelH=Math.min(dp(218),h*0.34f), top=h-panelH;
            float left=dp(14), right=w-dp(14), split=left+dp(137);
            p.setColor(0xd9071830);
            c.drawRoundRect(left,top,right,h-dp(12),dp(22),dp(22),p);
            p.setColor(0x99a8eaff); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1.1f));
            c.drawRoundRect(left,top,right,h-dp(12),dp(22),dp(22),p); p.setStyle(Paint.Style.FILL);
            float cx=w/2f, ay=top-dp(4);
            p.setColor(0xff8feaff); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(3));
            for(int k=0;k<3;k++) { Path a=new Path(); float yy=ay-dp(22-k);
                a.moveTo(cx-dp(9),yy-dp(5)); a.lineTo(cx,yy+dp(5)); a.lineTo(cx+dp(9),yy-dp(5)); c.drawPath(a,p); }
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x668fc9ed); c.drawLine(split,top+dp(14),split,h-dp(25),p);
            p.setColor(Color.WHITE); p.setTypeface(Typeface.create("sans-serif-light",0)); p.setTextSize(dp(29));
            c.drawText(new java.text.SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()),left+dp(15),top+dp(48),p);
            p.setColor(0xffd7efff); p.setTypeface(Typeface.DEFAULT); p.setTextSize(dp(10));
            c.drawText(new java.text.SimpleDateFormat("EEE, dd MMM",Locale.getDefault()).format(new Date()),left+dp(16),top+dp(68),p);
            int battery=batteryPercent();
            c.drawText("❄  ICE BLUE  •  "+(battery>=0?battery+"%":""),left+dp(16),top+dp(88),p);
            c.drawText("Love day  ♡",left+dp(16),top+dp(105),p);
            String[] cats={"Tools","Games","Social","All Apps"};
            float areaL=split+dp(7), areaW=right-areaL-dp(5), tabW=areaW/4f;
            for(int i=0;i<cats.length;i++) { float tx=areaL+i*tabW;
                boolean active=selectedCategory.equals(cats[i]);
                p.setColor(active?0xffe5f8ff:0xffb8cde0); p.setTextSize(dp(9));
                p.setTypeface(active?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
                c.drawText(cats[i],tx+dp(4),top+dp(24),p);
                if(active) { p.setColor(0xff65dfff); c.drawRoundRect(tx+dp(3),top+dp(29),tx+tabW-dp(5),top+dp(31),dp(2),dp(2),p); }
            }
            ArrayList<AppEntry> shown=visibleApps(); int cols=6; float cell=areaW/cols, firstY=top+dp(60);
            for(int i=0;i<shown.size()&&i<12;i++) { int col=i%cols,row=i/cols;
                float x=areaL+col*cell+cell/2f,y=firstY+row*dp(55); AppEntry a=shown.get(i);
                p.setColor(0x5547bde9); c.drawRoundRect(x-dp(17),y-dp(17),x+dp(17),y+dp(17),dp(8),dp(8),p);
                a.icon.setBounds((int)(x-dp(14)),(int)(y-dp(14)),(int)(x+dp(14)),(int)(y+dp(14))); a.icon.draw(c);
                p.setColor(Color.WHITE); p.setTextSize(dp(7)); p.setTypeface(Typeface.DEFAULT);
                String label=a.label.length()>9?a.label.substring(0,8)+"…":a.label;
                c.drawText(label,x-p.measureText(label)/2,y+dp(27),p);
            }
        }
        void launchAppAt(float x,float y) {
            if(reveal<0.5f) return;
            float panelH=Math.min(dp(218),getHeight()*0.34f), top=getHeight()-panelH;
            float left=dp(14), right=getWidth()-dp(14), split=left+dp(137);
            if(x<split) return;
            float areaL=split+dp(7), areaW=right-areaL-dp(5), tabW=areaW/4f;
            if(y>=top+dp(4)&&y<=top+dp(38)) {
                int tab=(int)((x-areaL)/tabW); String[] cats={"Tools","Games","Social","All Apps"};
                if(tab>=0&&tab<4) { selectedCategory=cats[tab]; invalidate(); } return;
            }
            float cell=areaW/6f, firstY=top+dp(60);
            if(y<firstY-dp(18)) return;
            int col=(int)((x-areaL)/cell), row=(int)((y-firstY+dp(18))/dp(55));
            if(col<0||col>=6||row<0||row>1) return;
            int index=row*6+col; ArrayList<AppEntry> shown=visibleApps();
            if(index<shown.size()&&index<12) launchApp(shown.get(index));
        }

        @Override public boolean performClick() {
            super.performClick();
            return true;
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float x=e.getX(), y=e.getY();
            switch(e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (snapAnimator != null) snapAnimator.cancel();
                    downX=x; downY=y; fingerX=x; fingerY=y; revealAtDown=reveal;
                    invalidate(); return true;
                case MotionEvent.ACTION_MOVE: {
                    fingerX=x; fingerY=y;
                    float dx=x-downX, dy=y-downY;
                    // Only vertical gestures control the dashboard. Horizontal swipes do nothing.
                    if (Math.abs(dy)>dp(10) && Math.abs(dy)>Math.abs(dx)*1.15f) {
                        reveal=Math.max(0f,Math.min(1f,revealAtDown - dy/(getHeight()*0.72f)));
                    }
                    invalidate(); return true;
                }
                case MotionEvent.ACTION_UP: {
                    float dx=x-downX, dy=y-downY;
                    if (Math.abs(dx)<dp(12) && Math.abs(dy)<dp(12)) {
                        performClick();
                        if (reveal >= 0.5f) launchAppAt(x,y);
                        else launchHomeAppAt(x,y);
                    } else if (Math.abs(dy)>Math.abs(dx)*1.15f && Math.abs(dy)>dp(45)) {
                        // Swipe up opens; swipe down closes.
                        settleReveal(dy<0f ? 1f : 0f);
                    } else {
                        settleReveal(revealAtDown>=0.5f?1f:0f);
                    }
                    invalidate(); return true;
                }
                case MotionEvent.ACTION_CANCEL:
                    settleReveal(revealAtDown>=0.5f?1f:0f);
                    invalidate(); return true;
            }
            return true;
        }
    }
}
