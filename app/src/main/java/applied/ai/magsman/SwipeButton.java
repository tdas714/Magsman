package applied.ai.magsman;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

public class SwipeButton extends RelativeLayout {
    private ImageView slidingButton;
    private float initialX;
    public boolean active;
    private int initialButtonWidth;
    private TextView centerText;

    private Drawable disabledDrawable;
    private Drawable enabledDrawable;

    public SwipeButton(Context context) {
        super(context);

        init(context, null, -1, -1);
    }

    public SwipeButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs, -1, -1);
    }

    public SwipeButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs, defStyleAttr, -1);
    }

    @TargetApi(21)
    public SwipeButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    @SuppressLint("ResourceAsColor")
    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        active = false;
        RelativeLayout background = new RelativeLayout(context);

        LayoutParams layoutParamsView = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParamsView.setMarginStart(60);
        layoutParamsView.setMarginEnd(60);
        layoutParamsView.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

        background.setBackground(ContextCompat.getDrawable(context, R.drawable.button_right));

        addView(background, layoutParamsView);

        final TextView centerText = new TextView(context);
        this.centerText = centerText;

        LayoutParams layoutParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        centerText.setText("Swipe Right To Play"); //add any text you need
        centerText.setTextColor(Color.WHITE);
        centerText.setTextSize(24);
        centerText.setTypeface(ResourcesCompat.getFont(context, R.font.kanit));
        centerText.setPadding(45, 45, 45, 45);
        background.addView(centerText, layoutParams);

        final ImageView swipeButton = new ImageView(context);
        this.slidingButton = swipeButton;

        disabledDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_double_arrow_24);
        enabledDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_double_arrow_24);

        slidingButton.setImageDrawable(disabledDrawable);
        slidingButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        slidingButton.setPadding(80, 5, 5, 5);

        Log.e("init: ", String.valueOf(slidingButton.getScaleX()));
        LayoutParams layoutParamsButton = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        layoutParamsButton.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        layoutParamsButton.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        swipeButton.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_button));
        swipeButton.setImageDrawable(disabledDrawable);
        addView(swipeButton, layoutParamsButton);

        setOnTouchListener(getButtonTouchListener());
    }


    private OnTouchListener getButtonTouchListener() {
        return new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (initialX == 0) {
                            initialX = slidingButton.getX();
                        }if (event.getX() > initialX + slidingButton.getWidth() / 2 && event.getX() + slidingButton.getWidth() / 2 < getWidth()) {
                        slidingButton.setX(event.getX() - slidingButton.getWidth() / 2);
                        centerText.setAlpha(1 - 1.3f * (slidingButton.getX() + slidingButton.getWidth()) / getWidth());
                        }

                        if  (event.getX() + slidingButton.getWidth() / 2 > getWidth() &&
                                slidingButton.getX() + slidingButton.getWidth() / 2 < getWidth()) {
                            slidingButton.setX(getWidth() - slidingButton.getWidth());
                        }

                        if  (event.getX() < slidingButton.getWidth() / 2 &&
                                slidingButton.getX() > 0) {
                            slidingButton.setX(0);
                        }

                        return true;
                    case MotionEvent.ACTION_UP:
                        //Release logic here
                        if (active) {
//                            collapseButton();
                        } else {
                            initialButtonWidth = slidingButton.getWidth();

                            if (slidingButton.getX() + slidingButton.getWidth() > getWidth() * 0.90) {
                                expandButton();
                            } else {
                                moveButtonBack();
                            }
                        }

                        return true;
                }

                return false;
            }
        };
    }

    private void expandButton() {
        final ValueAnimator positionAnimator =
                ValueAnimator.ofFloat(slidingButton.getX(), 0);
        positionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float x = (Float) positionAnimator.getAnimatedValue();
                slidingButton.setX(x);
            }
        });


        final ValueAnimator widthAnimator = ValueAnimator.ofInt(
                slidingButton.getWidth(),
                getWidth());

        widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewGroup.LayoutParams params = slidingButton.getLayoutParams();
                params.width = (Integer) widthAnimator.getAnimatedValue();
                slidingButton.setLayoutParams(params);
            }
        });


        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                active = true;
//                active = false;
                collapseButton();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                slidingButton.setScaleX(-1);
            }
        });

        animatorSet.playTogether(positionAnimator, widthAnimator);
        animatorSet.start();
    }
//    ==================================================================
    public void collapseButton() {
        final ValueAnimator widthAnimator = ValueAnimator.ofInt(
                slidingButton.getWidth(),
                initialButtonWidth);

        widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewGroup.LayoutParams params =  slidingButton.getLayoutParams();
                params.width = (Integer) widthAnimator.getAnimatedValue();
                slidingButton.setLayoutParams(params);
            }
        });

        widthAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                active = false;
                slidingButton.setScaleX(1);
            }
        });

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(
                centerText, "alpha", 1);

        AnimatorSet animatorSet = new AnimatorSet();

        animatorSet.playTogether(objectAnimator, widthAnimator);
        animatorSet.start();
    }
//    ============================================================
    private void moveButtonBack() {
        final ValueAnimator positionAnimator =
                ValueAnimator.ofFloat(slidingButton.getX(), 0);
        positionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        positionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float x = (Float) positionAnimator.getAnimatedValue();
                slidingButton.setX(x);
            }
        });

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(
                centerText, "alpha", 1);

        positionAnimator.setDuration(10);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(objectAnimator, positionAnimator);
        animatorSet.start();
    }
}