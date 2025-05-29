package com.spidroid.starry.auth;

import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularProgressBar extends View {
  private Paint backgroundPaint;
  private Paint progressPaint;
  private RectF rectF;
  private float progress = 0;
  private ValueAnimator progressAnimator;

  // Customizable attributes
  private int backgroundColor = 0xFFD1D1D1;
  private int progressColor = 0xFFFFFFFF;
  private float strokeWidth = 4f;

  public CircularProgressBar(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    // Background paint (gray circle)
    backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    backgroundPaint.setColor(backgroundColor);
    backgroundPaint.setStyle(Paint.Style.STROKE);
    backgroundPaint.setStrokeWidth(strokeWidth);

    // Progress paint (white circle)
    progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    progressPaint.setColor(progressColor);
    progressPaint.setStyle(Paint.Style.STROKE);
    progressPaint.setStrokeWidth(strokeWidth);
    progressPaint.setStrokeCap(Paint.Cap.ROUND);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawOval(rectF, backgroundPaint);
    float angle = 360 * progress / 100;
    canvas.drawArc(rectF, -90, angle, false, progressPaint);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    rectF = new RectF(strokeWidth / 2, strokeWidth / 2, w - strokeWidth / 2, h - strokeWidth / 2);
  }

  public void setProgress(float progress) {
    this.progress = progress;
    invalidate();
  }

  // Optional: Add methods to customize colors
  public void setProgressColor(int color) {
    progressPaint.setColor(color);
    invalidate();
  }

  public void setBackgroundColor(int color) {
    backgroundPaint.setColor(color);
    invalidate();
  }

  public void startIndeterminateAnimation() {
    if (progressAnimator != null && progressAnimator.isRunning()) {
      return;
    }
    progressAnimator = ValueAnimator.ofFloat(0, 100);
    progressAnimator.setDuration(1000);
    progressAnimator.setRepeatCount(ValueAnimator.INFINITE);
    progressAnimator.setInterpolator(new LinearInterpolator());
    progressAnimator.addUpdateListener(
        animation -> {
          float progress = (float) animation.getAnimatedValue();
          setProgress(progress);
        });
    progressAnimator.start();
  }

  public void stopIndeterminateAnimation() {
    if (progressAnimator != null) {
      progressAnimator.cancel();
      progressAnimator = null;
    }
  }
}
