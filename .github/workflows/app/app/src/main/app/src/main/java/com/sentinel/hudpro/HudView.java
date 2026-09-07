package com.sentinel.hudpro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class HudView extends View {

    private Paint cyanPaint, redPaint, textPaint, subTextPaint, dialPaint;
    private float gForce = 1.0f;
    private int speed = 72;
    private boolean isDrowsy = false;
    private float scanAngle = 0f;

    public HudView(Context context) {
        super(context);
        init();
    }

    private void init() {
        cyanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cyanPaint.setColor(Color.parseColor("#00F5FF"));
        cyanPaint.setStyle(Paint.Style.STROKE);
        cyanPaint.setStrokeWidth(3.5f);

        dialPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialPaint.setColor(Color.parseColor("#00F5FF"));
        dialPaint.setStyle(Paint.Style.STROKE);
        dialPaint.setStrokeWidth(8f);

        redPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redPaint.setColor(Color.parseColor("#FF0055"));
        redPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        redPaint.setStrokeWidth(5f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(42);
        textPaint.setFakeBoldText(true);

        subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subTextPaint.setColor(Color.parseColor("#00F5FF"));
        subTextPaint.setTextSize(22);
        subTextPaint.setLetterSpacing(0.15f);
    }

    public void updateStatus(float g, int spd, boolean drowsy) {
        this.gForce = g;
        this.speed = spd;
        this.isDrowsy = drowsy;
        this.scanAngle = (this.scanAngle + 5) % 360;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        // 1. Futuristic Corner Accents
        drawCyberCorner(canvas, 40, 40, 120, 1, 1);
        drawCyberCorner(canvas, w - 40, 40, 120, -1, 1);
        drawCyberCorner(canvas, 40, h - 40, 120, 1, -1);
        drawCyberCorner(canvas, w - 40, h - 40, 120, -1, -1);

        // 2. Header Banner
        canvas.drawText("SENTINEL HUD // AI EYE-GUARD PRO", 70, 75, subTextPaint);
        subTextPaint.setColor(Color.parseColor("#00FF88"));
        canvas.drawText("SYSTEM: ARMED & MONITORING", w - 450, 75, subTextPaint);
        subTextPaint.setColor(Color.parseColor("#00F5FF"));

        // 3. Central Driver Eye/Face Tracking Reticle
        float cx = w / 2.0f;
        float cy = h / 2.0f;

        // Outer Reticle Circle
        canvas.drawCircle(cx, cy, 140, cyanPaint);
        
        // Rotating AI Radar Arc
        RectF arcRect = new RectF(cx - 155, cy - 155, cx + 155, cy + 155);
        canvas.drawArc(arcRect, scanAngle, 70, false, dialPaint);

        // Face Crosshairs
        canvas.drawLine(cx - 180, cy, cx - 110, cy, cyanPaint);
        canvas.drawLine(cx + 110, cy, cx + 180, cy, cyanPaint);
        canvas.drawLine(cx, cy - 180, cx, cy - 110, cyanPaint);
        canvas.drawLine(cx, cy + 110, cx, cy + 180, cyanPaint);

        // 4. Left Dial: Speedometer
        textPaint.setTextSize(58);
        canvas.drawText(speed + "", 85, h - 140, textPaint);
        canvas.drawText("KM/H", 85, h - 90, subTextPaint);

        // 5. Right Dial: Live G-Force / Impact
        textPaint.setTextSize(46);
        canvas.drawText(String.format("%.2f G", gForce), w - 260, h - 140, textPaint);
        canvas.drawText("IMPACT TELEMETRY", w - 260, h - 90, subTextPaint);

        // 6. Critical Alert State (डुलकी लागल्यास किंवा अपघाताचा झटका बसल्यास)
        if (isDrowsy) {
            redPaint.setTextSize(54);
            redPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("⚠️ CRITICAL: DROWSINESS DETECTED!", cx, cy - 190, redPaint);
            canvas.drawText("PULL OVER / WAKE UP!", cx, cy + 220, redPaint);
            redPaint.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawCyberCorner(Canvas canvas, float x, float y, float size, int dirX, int dirY) {
        canvas.drawLine(x, y, x + (size * dirX), y, cyanPaint);
        canvas.drawLine(x, y, x, y + (size * dirY), cyanPaint);
        canvas.drawLine(x + (size * dirX * 0.4f), y + (size * dirY * 0.4f), x + (size * dirX * 0.4f), y, cyanPaint);
    }
}

