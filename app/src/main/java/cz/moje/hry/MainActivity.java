package cz.moje.hry;

import android.app.Activity;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;
    private final int[] devIds = new int[] { -1, -1 };
    private final float[][] ax = new float[2][6];
    private final boolean[][] bt = new boolean[2][17];
    private final String[] last = new String[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0D1230);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                last[0] = null;
                last[1] = null;
                for (int i = 0; i < 2; i++) {
                    if (devIds[i] != -1) pushState(i);
                }
            }
        });
        web.setWebChromeClient(new WebChromeClient());
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.requestFocus();
        web.loadUrl("file:///android_asset/site/index.html");
        hideBars();
    }

    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideBars();
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override
    protected void onPause() { super.onPause(); web.onPause(); }

    @Override
    protected void onResume() { super.onResume(); web.onResume(); }

    /* ---------- gamepad: forward to the page as window.__np ---------- */

    private int slotFor(int id) {
        for (int i = 0; i < 2; i++) if (devIds[i] == id) return i;
        for (int i = 0; i < 2; i++) {
            if (devIds[i] == -1) { devIds[i] = id; return i; }
        }
        return -1;
    }

    private boolean gameUrl() {
        String u = web.getUrl();
        return u != null && (u.contains("/bombici/") || u.contains("/hanojske-veze/") || u.contains("/test-ovladace/"));
    }

    private static boolean isPadSource(int src) {
        return (src & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                || (src & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
    }

    private static int padIndex(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A: return 0;
            case KeyEvent.KEYCODE_BUTTON_B: return 1;
            case KeyEvent.KEYCODE_BUTTON_X: return 2;
            case KeyEvent.KEYCODE_BUTTON_Y: return 3;
            case KeyEvent.KEYCODE_BUTTON_L1: return 4;
            case KeyEvent.KEYCODE_BUTTON_R1: return 5;
            case KeyEvent.KEYCODE_BUTTON_L2: return 6;
            case KeyEvent.KEYCODE_BUTTON_R2: return 7;
            case KeyEvent.KEYCODE_BUTTON_SELECT: return 8;
            case KeyEvent.KEYCODE_BUTTON_START: return 9;
            case KeyEvent.KEYCODE_BUTTON_THUMBL: return 10;
            case KeyEvent.KEYCODE_BUTTON_THUMBR: return 11;
            case KeyEvent.KEYCODE_DPAD_UP: return 12;
            case KeyEvent.KEYCODE_DPAD_DOWN: return 13;
            case KeyEvent.KEYCODE_DPAD_LEFT: return 14;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return 15;
            case KeyEvent.KEYCODE_BUTTON_MODE: return 16;
            default: return -1;
        }
    }

    private static float dz(float v) {
        if (Math.abs(v) < 0.15f) return 0f;
        return Math.round(v * 100f) / 100f;
    }

    private void pushState(int s) {
        StringBuilder sb = new StringBuilder();
        sb.append("window.__np=window.__np||[];window.__np[").append(s)
          .append("]={connected:true,id:'Android pad ").append(s).append("',axes:[");
        for (int i = 0; i < 4; i++) {
            if (i > 0) sb.append(',');
            sb.append(ax[s][i]);
        }
        sb.append("],buttons:[");
        for (int i = 0; i < 17; i++) {
            if (i > 0) sb.append(',');
            sb.append(bt[s][i] ? "{pressed:true,value:1}" : "{pressed:false,value:0}");
        }
        sb.append("]};");
        String js = sb.toString();
        if (js.equals(last[s])) return;
        last[s] = js;
        web.evaluateJavascript(js, null);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if ((ev.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                && ev.getAction() == MotionEvent.ACTION_MOVE) {
            int s = slotFor(ev.getDeviceId());
            if (s >= 0) {
                ax[s][0] = dz(ev.getAxisValue(MotionEvent.AXIS_X));
                ax[s][1] = dz(ev.getAxisValue(MotionEvent.AXIS_Y));
                ax[s][2] = dz(ev.getAxisValue(MotionEvent.AXIS_Z));
                ax[s][3] = dz(ev.getAxisValue(MotionEvent.AXIS_RZ));
                float hx = ev.getAxisValue(MotionEvent.AXIS_HAT_X);
                float hy = ev.getAxisValue(MotionEvent.AXIS_HAT_Y);
                bt[s][14] = hx < -0.5f;
                bt[s][15] = hx > 0.5f;
                bt[s][12] = hy < -0.5f;
                bt[s][13] = hy > 0.5f;
                bt[s][6] = ev.getAxisValue(MotionEvent.AXIS_LTRIGGER) > 0.5f;
                bt[s][7] = ev.getAxisValue(MotionEvent.AXIS_RTRIGGER) > 0.5f;
                pushState(s);
                return true;
            }
        }
        return super.dispatchGenericMotionEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int idx = padIndex(e.getKeyCode());
        if (idx >= 0 && isPadSource(e.getSource())) {
            int s = slotFor(e.getDeviceId());
            if (s >= 0) {
                boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
                bt[s][idx] = down;
                pushState(s);
                if (gameUrl()) {
                    if (idx == 8 && down && e.getRepeatCount() == 0 && web.canGoBack()) web.goBack();
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(e);
    }
}
