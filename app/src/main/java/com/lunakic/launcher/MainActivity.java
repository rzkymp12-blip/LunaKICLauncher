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
            if(reveal>0.01f) drawDashboard(c,w,h);
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
            float top=h*(1f-reveal);
            p.setColor(0xff071a31); c.drawRoundRect(dp(9),top+dp(9),w-dp(9),h-dp(9),dp(24),dp(24),p);
            p.setColor(0xff9beaff); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1.3f));
            c.drawRoundRect(dp(9),top+dp(9),w-dp(9),h-dp(9),dp(24),dp(24),p); p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE); p.setTextSize(dp(22)); p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText("ICE DASHBOARD",dp(22),top+dp(43),p);
            drawGlassCard(c,dp(18),top+dp(57),w-dp(18),top+dp(99),dp(14));
            p.setColor(0xffbfeeff); p.setTextSize(dp(12)); p.setTypeface(Typeface.DEFAULT);
            c.drawText("⌕  "+(searchText.isEmpty()?"Search apps...":searchText),dp(32),top+dp(83),p);
            String[] cats={"All Apps","Tools","Games","Social"};
            float tabW=(w-dp(36))/4f;
            for(int i=0;i<cats.length;i++) {
                float x=dp(18)+i*tabW;
                boolean active=selectedCategory.equals(cats[i]);
                p.setColor(active?0xff9beaff:0xffb6cde0); p.setTextSize(dp(10));
                p.setTypeface(active?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
                c.drawText(cats[i],x+dp(5),top+dp(123),p);
                if(active) { p.setColor(0xff45cfff); c.drawRoundRect(x+dp(4),top+dp(129),x+tabW-dp(9),top+dp(131),dp(2),dp(2),p); }
            }
            ArrayList<AppEntry> shown=visibleApps();
            p.setColor(0xffd5f3ff); p.setTextSize(dp(9)); p.setTypeface(Typeface.DEFAULT);
            c.drawText(shown.size()+" aplikasi",dp(23),top+dp(149),p);
            int cols=4; float cell=(w-dp(34))/cols; float startY=top+dp(184);
            for(int i=0;i<shown.size();i++) {
                int col=i%cols,row=i/cols; float x=dp(17)+col*cell+cell/2f,y=startY+row*dp(82);
                if(y>h-dp(48)) break;
                p.setColor(0x5539cfff); c.drawCircle(x,y,dp(23),p);
                AppEntry a=shown.get(i); a.icon.setBounds((int)(x-dp(16)),(int)(y-dp(16)),(int)(x+dp(16)),(int)(y+dp(16))); a.icon.draw(c);
                p.setColor(Color.WHITE); p.setTextSize(dp(9));
                String label=a.label.length()>12?a.label.substring(0,11)+"…":a.label;
                c.drawText(label,x-p.measureText(label)/2,y+dp(38),p);
            }
            p.setColor(0xff9beaff); p.setTextSize(dp(10)); c.drawText("SWIPE DOWN TO CLOSE",dp(22),h-dp(24),p);
        }
        void launchAppAt(float x, float y) {
            if (reveal < 0.5f) return;
            float top = getHeight() * (1f - reveal);
            float cell = (getWidth() - dp(34)) / 4f;
            float startY = top + dp(184);
            if (y < startY - dp(40)) return;
            int col = (int)((x - dp(17)) / cell);
            int row = (int)((y - startY + dp(41)) / dp(82));
            if (col < 0 || col >= 4 || row < 0) return;
            int index = row * 4 + col;
            ArrayList<AppEntry> shown=visibleApps();
            if (index < 0 || index >= shown.size()) return;
            float centerY = startY + row * dp(82);
            if (Math.abs(y - centerY) > dp(43)) return;
            launchApp(shown.get(index));
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
                        if (reveal >= 0.5f) {
                            float top=getHeight()*(1f-reveal);
                            if (y>=top+dp(57) && y<=top+dp(99)) showSearchDialog();
                            else if (y>=top+dp(103) && y<=top+dp(137)) {
                                int tab=(int)((x-dp(18))/((getWidth()-dp(36))/4f));
                                String[] cats={"All Apps","Tools","Games","Social"};
                                if(tab>=0&&tab<cats.length) { selectedCategory=cats[tab]; invalidate(); }
                            } else launchAppAt(x,y);
                        } else launchHomeAppAt(x,y);
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
