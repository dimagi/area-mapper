package richard.chard.lu.android.areamapper;

import android.view.MotionEvent;
import android.view.View;

/**
 * @author Richard Lu
 */
public class StopPropagationTouchListener implements View.OnTouchListener {

    private static StopPropagationTouchListener instance;

    public static StopPropagationTouchListener getInstance() {
        if (instance == null) {
            instance = new StopPropagationTouchListener();
        }

        return instance;
    }

    private StopPropagationTouchListener() {}

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return true;
    }

}
