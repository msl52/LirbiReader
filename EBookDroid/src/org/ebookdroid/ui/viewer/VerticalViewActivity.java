package org.ebookdroid.ui.viewer;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.ToastPosition;
import org.ebookdroid.ui.viewer.viewers.PdfSurfaceView;
import org.emdev.ui.AbstractActionActivity;
import org.emdev.utils.LengthUtils;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.PasswordDialog;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.BrightnessHelper;
import com.foobnix.pdf.info.widget.RecentUpates;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.search.view.CloseAppDialog;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.TTSNotification;
import com.foobnix.ui2.FileMetaCore;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.MyContextWrapper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class VerticalViewActivity extends AbstractActionActivity<VerticalViewActivity, ViewerActivityController> {
    public static final String PERCENT_EXTRA = "percent";
    public static final DisplayMetrics DM = new DisplayMetrics();

    IView view;

    private FrameLayout frameLayout;

    /**
     * Instantiates a new base viewer activity.
     */
    public VerticalViewActivity() {
        super();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        LOG.d("VerticalViewActivity", "onNewIntent");
        if (TTSNotification.ACTION_TTS.equals(intent.getAction())) {
            return;
        }
        if (!intent.filterEquals(getIntent())) {
            finish();
            startActivity(intent);
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.ui.AbstractActionActivity#createController()
     */
    @Override
    protected ViewerActivityController createController() {
        return new ViewerActivityController(this);
    }

    private Handler handler;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        intetrstialTimeoutSec = ADS.FULL_SCREEN_TIMEOUT_SEC;
        AppState.get().load(this);

        FileMetaCore.checkOrCreateMetaInfo(this);

        if (AppState.get().isRememberMode && AppState.get().isAlwaysOpenAsMagazine) {
            super.onCreate(savedInstanceState);
            finish();
            ExtUtils.showDocument(this, getIntent().getData());
            return;
        } else {
            if (getIntent().getData() != null) {
                String path = getIntent().getData().getPath();
                final BookSettings bs = SettingsManager.getBookSettings(path);
                // AppState.get().setNextScreen(bs.isNextScreen);
                if (bs != null) {
                    // AppState.get().isLocked = bs.isLocked;
                    AppState.get().autoScrollSpeed = bs.speed;
                    AppState.get().isCut = bs.isTextFormat() ? false : bs.splitPages;
                    AppState.get().isCrop = bs.cropPages;
                    AppState.get().isDouble = false;
                    AppState.get().isDoubleCoverAlone = false;
                    AppState.get().isLocked = bs.isLocked;
                    TempHolder.get().pageDelta = bs.pageDelta;
                    if (AppState.get().isCropPDF && !bs.isTextFormat()) {
                        AppState.get().isCrop = true;
                    }
                }
                BookCSS.get().detectLang(path);
            }

            getController().beforeCreate(this);

            BrightnessHelper.applyBrigtness(this);

            if (AppState.get().isDayNotInvert) {
                setTheme(R.style.StyledIndicatorsWhite);
            } else {
                setTheme(R.style.StyledIndicatorsBlack);
            }
            super.onCreate(savedInstanceState);
        }

        if (PasswordDialog.isNeedPasswordDialog(this)) {
            return;
        }
        setContentView(R.layout.activity_vertical_view);

        Android6.checkPermissions(this);

        getController().createWrapper(this);
        frameLayout = (FrameLayout) findViewById(R.id.documentView);

        view = new PdfSurfaceView(getController());

        frameLayout.addView(view.getView());

        getController().afterCreate(this);

        // ADS.activate(this, adView);

        handler = new Handler();

    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(MyContextWrapper.wrap(context));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Android6.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DocumentController.doRotation(this);

        if (AppState.get().isFullScreen) {
            Keyboards.hideNavigation(this);
        }
        getController().onResume();
        if (handler != null) {
            handler.removeCallbacks(closeRunnable);
        }

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            getController().getDocumentController().goToPage(data.getIntExtra("page", 0));
        }
    }

    boolean needToRestore = false;

    @Override
    protected void onPause() {
        super.onPause();
        LOG.d("onPause", this.getClass());
        getController().onPause();
        needToRestore = AppState.get().isAutoScroll;
        AppState.get().isAutoScroll = false;
        AppState.get().save(this);
        TempHolder.isSeaching = false;
        if (handler != null) {
            handler.postDelayed(closeRunnable, AppState.APP_CLOSE_AUTOMATIC);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Analytics.onStart(this);
        if (needToRestore) {
            AppState.get().isAutoScroll = true;
            getController().getListener().onAutoScroll();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        // Analytics.onStop(this);
        RecentUpates.updateAll(this);
    }

    Runnable closeRunnable = new Runnable() {

        @Override
        public void run() {
            LOG.d("Close App");
            getController().closeActivityFinal(null);
            MainTabs2.closeApp(VerticalViewActivity.this);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    public void currentPageChanged(final String pageText, final String bookTitle) {
        if (LengthUtils.isEmpty(pageText)) {
            return;
        }
        final AppSettings app = AppSettings.getInstance();

        if (app.pageNumberToastPosition == ToastPosition.Invisible) {
            return;
        }

    }

    Dialog rotatoinDialog;
    boolean isInitPosistion;
    int isInitOrientation;

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final boolean currentPosistion = Dips.screenHeight() > Dips.screenWidth();

        if (ExtUtils.isTextFomat(getIntent()) /* && isInitOrientation == AppState.get().orientation */) {

            if (rotatoinDialog != null) {
                try {
                    rotatoinDialog.dismiss();
                } catch (Exception e) {
                    LOG.e(e);
                }
            }

            if (isInitPosistion != currentPosistion) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setCancelable(false);
                dialog.setMessage(R.string.apply_a_new_screen_orientation_);
                dialog.setPositiveButton(R.string.yes, new AlertDialog.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doConifChange();
                        isInitPosistion = currentPosistion;
                    }
                });
                rotatoinDialog = dialog.show();
            }
        } else {
            doConifChange();
        }

        isInitOrientation = AppState.get().orientation;
    }

    public void doConifChange() {
        try {
            if (!getController().getDocumentController().isInitialized()) {
                LOG.d("Skip onConfigurationChanged");
                return;
            }
        } catch (Exception e) {
            LOG.e(e);
            return;
        }

        AppState.get().save(this);

        if (ExtUtils.isTextFomat(getIntent())) {

            double value = getController().getDocumentModel().getPercentRead();
            getIntent().putExtra(PERCENT_EXTRA, value);

            LOG.d("READ PERCEnt", value);

            getController().closeActivityFinal(new Runnable() {

                @Override
                public void run() {
                    startActivity(getIntent());
                }
            });

        }
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getController().afterPostCreate();
        isInitPosistion = Dips.screenHeight() > Dips.screenWidth();
        isInitOrientation = AppState.get().orientation;
    }

    @Override
    public boolean onGenericMotionEvent(final MotionEvent event) {
        if (Integer.parseInt(Build.VERSION.SDK) >= 12) {
            return GenericMotionEvent12.onGenericMotionEvent(event, this);
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(final int keyCode, final KeyEvent event) {
        // Toast.makeText(this, "onKeyLongPress", Toast.LENGTH_SHORT).show();
        if (CloseAppDialog.checkLongPress(this, event)) {
            CloseAppDialog.showOnLongClickDialog(getController().getActivity(), null, getController().getListener());
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // Toast.makeText(this, "onBackPressed", Toast.LENGTH_SHORT).show();

        if (isInterstialShown()) {
            getController().closeActivityFinal(null);
            return;
        }
        if (getController().getWrapperControlls().checkBack(new KeyEvent(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BACK))) {
            return;
        }

        if (AppState.get().isShowLongBackDialog) {
            CloseAppDialog.showOnLongClickDialog(getController().getActivity(), null, getController().getListener());
        } else {
            showInterstial();
        }

    }

    private boolean isMyKey = false;

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        LOG.d("onKeyUp");
        if (isMyKey) {
            return true;
        }

        if (getController().getWrapperControlls().dispatchKeyEventUp(event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        LOG.d("onKeyDown");

        int repeatCount = event.getRepeatCount();
        if (repeatCount >= 1 && repeatCount < 6) {
            return true;
        }

        isMyKey = false;
        if (getController().getWrapperControlls().dispatchKeyEventDown(event)) {
            isMyKey = true;
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onFinishActivity() {
        getController().closeActivityFinal(null);

    }

}
