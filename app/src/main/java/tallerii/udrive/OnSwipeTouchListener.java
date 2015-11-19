package tallerii.udrive;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * Detecta los movimientos del dedo por la pantalla.
 */
public class OnSwipeTouchListener implements OnTouchListener {

    public final GestureDetector gestureDetector;

    float mPreviousX;
    float mPreviousY;

    float nextX;
    float nextY;

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent){

        return true;
    }

    /**
     * Devuelve el X previo del movimiento del dedo por la pantalla.
     * @return la coordenada X previa al movimiento del dedo por la pantalla.
     */
    public float getX(){
        return mPreviousX;
    }

    /**
     * Devuelve el Y previo del movimiento del dedo por la pantalla.
     * @return la coordenada Y previa al movimiento del dedo por la pantalla.
     */
    public float getY(){
        return mPreviousY;
    }

    public OnSwipeTouchListener (Context ctx){
        gestureDetector = new GestureDetector(ctx, new GestureListener());
    }

    private final class GestureListener extends SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
//            mPreviousX = e1.getX();
//            mPreviousY = e1.getY();
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
//                            onSwipeRight();
                        } else {
//                            onSwipeLeft();
                        }
                    }
                    result = true;
                }
                else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom();
                    }
                }
                result = true;

            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }
    }

    /**
     * Detecta cuando el usuario deslizo el dedo hacia abajo por la pantalla.
     */
    public void onSwipeBottom() {
    }
}
