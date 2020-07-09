package com.monkey.miclockview;

import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;

//仿小米时钟
public class MiClockView extends View {

    private Canvas mCanvas;
    private Paint mTextPaint;
    private Rect mTextRect;
    private Paint mCirclePaint;
    private float mCircleStrokeWidth = 2;
    private RectF mCircleRectF;
    private Paint mScaleArcPaint;
    private RectF mScaleArcRectF;
    private Paint mScaleLinePaint;
    private Paint mHourHandPaint;
    private Paint mMinuteHandPaint;
    private Paint mSecondHandPaint;
    private Path mHourHandPath;
    private Path mMinuteHandPath;
    private Path mSecondHandPath;
    private int mLightColor;
    private int mDarkColor;
    private int mBackgroundColor;
    private float mTextSize;
    private float mRadius;
    private float mScaleLength;
    private float mHourDegree;
    private float mMinuteDegree;
    private float mSecondDegree;
    private float mDefaultPadding;
    private float mPaddingLeft;
    private float mPaddingTop;
    private float mPaddingRight;
    private float mPaddingBottom;
    private float mCanvasTranslateX;
    private float mCanvasTranslateY;

    public MiClockView(Context context) {
        this(context, null);
    }

    public MiClockView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiClockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MiClockView, defStyleAttr, 0);
        mBackgroundColor = ta.getColor(R.styleable.MiClockView_backgroundColor, Color.parseColor("#237EAD"));
        setBackgroundColor(mBackgroundColor);
        mLightColor = ta.getColor(R.styleable.MiClockView_lightColor, Color.parseColor("#ffffff"));
        mDarkColor = ta.getColor(R.styleable.MiClockView_darkColor, Color.parseColor("#80ffffff"));
        mTextSize = ta.getDimension(R.styleable.MiClockView_textSize, DensityUtils.sp2px(context, 14));
        ta.recycle();

        mHourHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHourHandPaint.setStyle(Paint.Style.FILL);
        mHourHandPaint.setColor(mDarkColor);

        mMinuteHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMinuteHandPaint.setColor(mLightColor);

        mSecondHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSecondHandPaint.setStyle(Paint.Style.FILL);
        mSecondHandPaint.setColor(mLightColor);

        mScaleLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScaleLinePaint.setStyle(Paint.Style.STROKE);
        mScaleLinePaint.setColor(mBackgroundColor);

        mScaleArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScaleArcPaint.setStyle(Paint.Style.STROKE);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(mDarkColor);
        mTextPaint.setTextSize(mTextSize);

        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeWidth(mCircleStrokeWidth);
        mCirclePaint.setColor(mDarkColor);

        mTextRect = new Rect();
        mCircleRectF = new RectF();
        mScaleArcRectF = new RectF();
        mHourHandPath = new Path();
        mMinuteHandPath = new Path();
        mSecondHandPath = new Path();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureDimension(widthMeasureSpec), measureDimension(heightMeasureSpec));
    }

    private int measureDimension(int measureSpec) {
        int result;
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            result = size;
        } else {
            result = 800;
            if (mode == MeasureSpec.AT_MOST) {
                result = Math.min(result, size);
            }
        }
        return result;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //宽和高分别去掉padding值，取min的一半即表盘的半径
        mRadius = Math.min(w - getPaddingLeft() - getPaddingRight(),
                h - getPaddingTop() - getPaddingBottom()) / 2;
        mDefaultPadding = 0.12f * mRadius;//根据比例确定默认padding大小
        mPaddingLeft = mDefaultPadding + w / 2 - mRadius + getPaddingLeft();
        mPaddingTop = mDefaultPadding + h / 2 - mRadius + getPaddingTop();
        mPaddingRight = mPaddingLeft;
        mPaddingBottom = mPaddingTop;
        mScaleLength = 0.12f * mRadius;//根据比例确定刻度线长度
        mScaleArcPaint.setStrokeWidth(mScaleLength);
        mScaleLinePaint.setStrokeWidth(0.012f * mRadius);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        mCanvas = canvas;
        getTimeDegree();
        drawTimeText();
        drawScaleLine();
        drawSecondHand();
        drawHourHand();
        drawMinuteHand();
        invalidate();
    }



    private void getTimeDegree() {
        //是用毫秒可以使秒针流畅运行
        Calendar calendar = Calendar.getInstance();
        float Second = calendar.get(Calendar.MILLISECOND);
        float second = calendar.get(Calendar.SECOND) + Second / 1000;
        float minute = calendar.get(Calendar.MINUTE) + second / 60;
        float hour = calendar.get(Calendar.HOUR) + minute / 60;
        mSecondDegree = second / 60 * 360;
        mMinuteDegree = minute / 60 * 360;
        mHourDegree = hour / 12 * 360;
    }


    private void drawTimeText() {
        //绘制数字
        String timeText = "12";
        mTextPaint.getTextBounds(timeText, 0, timeText.length(), mTextRect);
        int textLargeWidth = mTextRect.width();//两位数字的宽
        mCanvas.drawText("12", getWidth() / 2 - textLargeWidth / 2, mPaddingTop + mTextRect.height(), mTextPaint);
        timeText = "3";
        mTextPaint.getTextBounds(timeText, 0, timeText.length(), mTextRect);
        int textSmallWidth = mTextRect.width();//一位数字的宽
        mCanvas.drawText("3", getWidth() - mPaddingRight - mTextRect.height() / 2 - textSmallWidth / 2,
                getHeight() / 2 + mTextRect.height() / 2, mTextPaint);
        mCanvas.drawText("6", getWidth() / 2 - textSmallWidth / 2, getHeight() - mPaddingBottom, mTextPaint);
        mCanvas.drawText("9", mPaddingLeft + mTextRect.height() / 2 - textSmallWidth / 2,
                getHeight() / 2 + mTextRect.height() / 2, mTextPaint);

        //画4个弧
        mCircleRectF.set(mPaddingLeft + mTextRect.height() / 2 + mCircleStrokeWidth / 2,
                mPaddingTop + mTextRect.height() / 2 + mCircleStrokeWidth / 2,
                getWidth() - mPaddingRight - mTextRect.height() / 2 + mCircleStrokeWidth / 2,
                getHeight() - mPaddingBottom - mTextRect.height() / 2 + mCircleStrokeWidth / 2);
        for (int i = 0; i < 4; i++) {
            mCanvas.drawArc(mCircleRectF, 5 + 90 * i, 80, false, mCirclePaint);
        }
    }


    private void drawScaleLine() {
        mCanvas.save();
        mCanvas.translate(mCanvasTranslateX, mCanvasTranslateY);
        mScaleArcRectF.set(mPaddingLeft + 1.5f * mScaleLength + mTextRect.height() / 2,
                mPaddingTop + 1.5f * mScaleLength + mTextRect.height() / 2,
                getWidth() - mPaddingRight - mTextRect.height() / 2 - 1.5f * mScaleLength,
                getHeight() - mPaddingBottom - mTextRect.height() / 2 - 1.5f * mScaleLength);
        mCanvas.drawArc(mScaleArcRectF, 0, 360, false, mScaleArcPaint);
        //画背景色刻度线
        for (int i = 0; i < 200; i++) {
            mCanvas.drawLine(getWidth() / 2, mPaddingTop + mScaleLength + mTextRect.height() / 2,
                    getWidth() / 2, mPaddingTop + 2 * mScaleLength + mTextRect.height() / 2, mScaleLinePaint);
            mCanvas.rotate(1.8f, getWidth() / 2, getHeight() / 2);
        }
        mCanvas.restore();
    }


    private void drawSecondHand() {
        mCanvas.save();
        mCanvas.translate(mCanvasTranslateX * 4f, mCanvasTranslateY * 4f);
        mCanvas.rotate(mSecondDegree, getWidth() / 2, getHeight() / 2);
        if (mSecondHandPath.isEmpty()) {
            mSecondHandPath.reset();
            float offset = mPaddingTop + mTextRect.height() / 2;
            mSecondHandPath.moveTo(getWidth() / 2 - 0.018f * mRadius, getHeight() / 2 - 0.03f * mRadius);
            mSecondHandPath.lineTo(getWidth() / 2 - 0.009f * mRadius, offset + 0.48f * mRadius);
            mSecondHandPath.quadTo(getWidth() / 2, offset + 0.46f * mRadius,
                    getWidth() / 2 + 0.009f * mRadius, offset + 0.48f * mRadius);
            mSecondHandPath.lineTo(getWidth() / 2 + 0.018f * mRadius, getHeight() / 2 - 0.03f * mRadius);
            mSecondHandPath.close();
            mSecondHandPaint.setColor(mLightColor);
        }
        mCanvas.drawPath(mSecondHandPath, mSecondHandPaint);
        mCircleRectF.set(getWidth() / 2 - 0.03f * mRadius, getHeight() / 2 - 0.03f * mRadius,
                getWidth() / 2 + 0.03f * mRadius, getHeight() / 2 + 0.03f * mRadius);
        mSecondHandPaint.setStyle(Paint.Style.STROKE);
        mSecondHandPaint.setStrokeWidth(0.01f * mRadius);
        mCanvas.drawArc(mCircleRectF, 0, 360, false, mSecondHandPaint);
        mCanvas.restore();
    }


    private void drawHourHand() {
        mCanvas.save();
        mCanvas.translate(mCanvasTranslateX * 1.2f, mCanvasTranslateY * 1.2f);
        mCanvas.rotate(mHourDegree, getWidth() / 2, getHeight() / 2);
        if (mHourHandPath.isEmpty()) {
            mHourHandPath.reset();
            float offset = mPaddingTop + mTextRect.height() / 2;
            mHourHandPath.moveTo(getWidth() / 2 - 0.018f * mRadius, getHeight() / 2 - 0.03f * mRadius);
            mHourHandPath.lineTo(getWidth() / 2 - 0.009f * mRadius, offset + 0.48f * mRadius);
            mHourHandPath.quadTo(getWidth() / 2, offset + 0.46f * mRadius,
                    getWidth() / 2 + 0.009f * mRadius, offset + 0.48f * mRadius);
            mHourHandPath.lineTo(getWidth() / 2 + 0.018f * mRadius, getHeight() / 2 - 0.03f * mRadius);
            mHourHandPath.close();
        }
        mHourHandPaint.setStyle(Paint.Style.FILL);
        mCanvas.drawPath(mHourHandPath, mHourHandPaint);

        mCircleRectF.set(getWidth() / 2 - 0.03f * mRadius, getHeight() / 2 - 0.03f * mRadius,
                getWidth() / 2 + 0.03f * mRadius, getHeight() / 2 + 0.03f * mRadius);
        mHourHandPaint.setStyle(Paint.Style.STROKE);
        mHourHandPaint.setStrokeWidth(0.01f * mRadius);
        mCanvas.drawArc(mCircleRectF, 0, 360, false, mHourHandPaint);
        mCanvas.restore();
    }

    private void drawMinuteHand() {
        mCanvas.save();
        mCanvas.translate(mCanvasTranslateX * 2f, mCanvasTranslateY * 2f);
        mCanvas.rotate(mMinuteDegree, getWidth() / 2, getHeight() / 2);
        if (mMinuteHandPath.isEmpty()) {
            mMinuteHandPath.reset();
            float offset = mPaddingTop + mTextRect.height() / 2;
            mMinuteHandPath.moveTo(getWidth() / 2 - 0.01f * mRadius, getHeight() / 2 - 0.03f * mRadius);
            mMinuteHandPath.lineTo(getWidth() / 2 - 0.008f * mRadius, offset + 0.365f * mRadius);
            mMinuteHandPath.quadTo(getWidth() / 2, offset + 0.345f * mRadius,
                    getWidth() / 2 + 0.008f * mRadius, offset + 0.365f * mRadius);
            mMinuteHandPath.lineTo(getWidth() / 2 + 0.01f * mRadius, getHeight() / 2 - 0.03f * mRadius);
            mMinuteHandPath.close();
        }
        mMinuteHandPaint.setStyle(Paint.Style.FILL);
        mCanvas.drawPath(mMinuteHandPath, mMinuteHandPaint);

        mCircleRectF.set(getWidth() / 2 - 0.03f * mRadius, getHeight() / 2 - 0.03f * mRadius,
                getWidth() / 2 + 0.03f * mRadius, getHeight() / 2 + 0.03f * mRadius);
        mMinuteHandPaint.setStyle(Paint.Style.STROKE);
        mMinuteHandPaint.setStrokeWidth(0.02f * mRadius);
        mCanvas.drawArc(mCircleRectF, 0, 360, false, mMinuteHandPaint);
        mCanvas.restore();
    }
}
