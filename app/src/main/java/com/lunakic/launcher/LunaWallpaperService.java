package com.lunakic.launcher;

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.view.MotionEvent;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

public class LunaWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine() { return new LunaEngine(); }

    class LunaEngine extends Engine implements SensorEventListener {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random random = new Random();
        final ArrayList<Particle> particles = new ArrayList<>();
        Bitmap artwork;
        SensorManager sensors;
        Sensor accelerometer;
        float tiltX=0, tiltY=0, touchX=.5f, touchY=.5f, pulse=0;
        boolean visible=false, touching=false;
        long lastFrame=0;
        final Runnable drawTask = new Runnable() { @Override public void run() { drawFrame(); } };

        class Particle {
            float x,y,vx,vy,r,life=1;
            Particle(float x,float y) {
                this.x=x; this.y=y;
                double a=random.nextDouble()*Math.PI*2;
                float speed=0.7f+random.nextFloat()*3.2f;
                vx=(float)Math.cos(a)*speed; vy=(float)Math.sin(a)*speed;
                r=2+random.nextFloat()*5;
            }
        }

        LunaEngine() {
            try (InputStream in=getAssets().open("luna_home.jpg")) {
                artwork=BitmapFactory.decodeStream(in);
            } catch(Exception ignored) {}
            sensors=(SensorManager)getSystemService(SENSOR_SERVICE);
            accelerometer=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            setTouchEventsEnabled(true);
        }

        @Override public void onVisibilityChanged(boolean v) {
            visible=v;
            if(v) { if(accelerometer!=null) sensors.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_UI); drawFrame(); }
            else { handler.removeCallbacks(drawTask); sensors.unregisterListener(this); }
        }
        @Override public void onSurfaceChanged(SurfaceHolder h,int f,int w,int height) { super.onSurfaceChanged(h,f,w,height); drawFrame(); }
        @Override public void onSurfaceDestroyed(SurfaceHolder h) { visible=false; handler.removeCallbacks(drawTask); sensors.unregisterListener(this); super.onSurfaceDestroyed(h); }
        @Override public void onTouchEvent(MotionEvent e) {
            touchX=e.getX()/Math.max(1,getSurfaceHolder().getSurfaceFrame().width());
            touchY=e.getY()/Math.max(1,getSurfaceHolder().getSurfaceFrame().height());
            if(e.getAction()==MotionEvent.ACTION_DOWN || e.getAction()==MotionEvent.ACTION_MOVE) {
                touching=true; pulse=1;
                if(e.getAction()==MotionEvent.ACTION_DOWN) for(int i=0;i<28;i++) particles.add(new Particle(touchX,touchY));
            } else if(e.getAction()==MotionEvent.ACTION_UP) touching=false;
            while(particles.size()>130) particles.remove(0);
            drawFrame();
        }
        @Override public void onSensorChanged(SensorEvent e) {
            tiltX=Math.max(-1.5f,Math.min(1.5f,e.values[0]/9.8f));
            tiltY=Math.max(-1.5f,Math.min(1.5f,e.values[1]/9.8f));
        }
        @Override public void onAccuracyChanged(Sensor s,int a) {}

        void drawFrame() {
            handler.removeCallbacks(drawTask);
            if(!visible) return;
            SurfaceHolder holder=getSurfaceHolder();
            Canvas c=null;
            try {
                c=holder.lockCanvas();
                if(c!=null) {
                    int w=c.getWidth(), h=c.getHeight();
                    if(artwork!=null) {
                        float scale=Math.max((float)w/artwork.getWidth(),(float)h/artwork.getHeight());
                        float dw=artwork.getWidth()*scale, dh=artwork.getHeight()*scale;
                        float px=(w-dw)/2f + tiltX*12f, py=(h-dh)/2f + tiltY*12f;
                        c.drawBitmap(artwork,null,new RectF(px,py,px+dw,py+dh),paint);
                    } else c.drawColor(Color.rgb(5,18,38));
                    if(pulse>0) {
                        float x=touchX*w,y=touchY*h,rad=(1.05f-pulse)*190+12;
                        paint.setShader(new RadialGradient(x,y,rad,new int[]{0x885eeaff,0x4439cfff,0x0039cfff},null,Shader.TileMode.CLAMP));
                        c.drawCircle(x,y,rad,paint); paint.setShader(null);
                        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2); paint.setColor(0xaad8f8ff);
                        c.drawCircle(x,y,rad,paint); paint.setStyle(Paint.Style.FILL);
                    }
                    for(int i=particles.size()-1;i>=0;i--) {
                        Particle p=particles.get(i); p.x+=p.vx/Math.max(1,w); p.y+=p.vy/Math.max(1,h); p.life-=.018f;
                        paint.setColor(((int)(Math.max(0,p.life)*220)<<24)|0x00c8f7ff);
                        c.drawCircle(p.x*w,p.y*h,p.r*Math.max(0,p.life),paint);
                        if(p.life<=0) particles.remove(i);
                    }
                    pulse=Math.max(0,pulse-.025f);
                }
            } catch(Exception ignored) { }
            finally { if(c!=null) holder.unlockCanvasAndPost(c); }
            if(visible) handler.postDelayed(drawTask,33);
        }
    }
}