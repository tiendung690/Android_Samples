/**
 * Camera2 Sample
 * Face Detection using Camera2 API
 * 2019-02-01 K.OHWADA
 */

package jp.ohwada.android.camera28;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import android.hardware.camera2.params.Face;

import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jp.ohwada.android.camera28.util.CameraPerm;
import jp.ohwada.android.camera28.util.Camera2Source;
import jp.ohwada.android.camera28.util.DisplayUtil;
import jp.ohwada.android.camera28.util.ToastMaster;
import jp.ohwada.android.camera28.ui.CameraSourcePreview;
import jp.ohwada.android.camera28.ui.GraphicOverlay;
import jp.ohwada.android.camera28.ui.FaceOverlay;
import jp.ohwada.android.camera28.ui.AutoFitTextureView; 


/**
 * class MainActivity 
 * original : https://github.com/EzequielAdrianM/Camera2Vision
 */
public class MainActivity extends Activity {

        // debug
	private final static boolean D = true;
    private final static String TAG = "Camera2";
    private final static String TAG_SUB = "MainActivity";

    // Camera2Source
    private Camera2Source mCamera2Source = null;

    // view
    private CameraSourcePreview mPreview;

    private GraphicOverlay mGraphicOverlay;
    private FaceOverlay mFaceOverlay;

    // default camera face
    private boolean usingFrontCamera = false;

    // utility
    private CameraPerm mCameraPerm;

    // face detect 
    private boolean isFaceDetectRunning = false;


/**
 * onCreate
 */ 
@Override
protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Button btnFlip = (Button) findViewById(R.id.Button_flip);
           btnFlip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    flipCameraFace();
                }
            }); // btnFlip


        Button btnDetect = (Button) findViewById(R.id.Button_detect);
            btnDetect .setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    detectFace();
                }
            }); // btnDetect 


    // view
    mPreview = (CameraSourcePreview) findViewById(R.id.preview);
    mGraphicOverlay = (GraphicOverlay) findViewById(R.id.overlay);
    mFaceOverlay = new FaceOverlay(mGraphicOverlay);

     // utility
    mCameraPerm = new CameraPerm(this);



} // onCreate



/**
 * onResume
 */ 
    @Override
    protected void onResume() {
        super.onResume();
    startCameraSource();
} // onResume


/**
 * onPause
 */ 
    @Override
    protected void onPause() {
        log_d("onPause");
        super.onPause();
        stopCameraSource();
    }


/**
 * onDestroy
 */ 
    @Override
    protected void onDestroy() {
log_d("onDestroy");
        super.onDestroy();
        stopCameraSource();
    }


/**
 * onRequestPermissionsResult
 */ 
    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions,  int[] grantResults) {
        log_d("onRequestPermissionsResult");
        mCameraPerm.onRequestPermissionsResult(requestCode, permissions,  grantResults); 
        startCameraSource();
} // onRequestPermissionsResult


/**
 * detectFace
 */ 
private void detectFace() {
    if(isFaceDetectRunning) {
        isFaceDetectRunning = false;
        stopDetectFace();
        showToast("stop detect");
    } else {
        isFaceDetectRunning = true;
        startDetectFace() ;
        showToast("start detect");
    }
} // detectFace


/**
 * startDetectFace
 */ 
private void startDetectFace() {
        int displayRotation = DisplayUtil.getDisplayRotation(this);
        mFaceOverlay.setDisplayRotation(displayRotation);
         if(mCamera2Source != null) {
                Rect sensorArraySize = mCamera2Source.getSensorArraySize();
                mFaceOverlay.setSensorArraySize(sensorArraySize);
                int sensorOrientation = mCamera2Source.getSensorOrientation();
                mFaceOverlay.setSensorOrientation(sensorOrientation);   
               boolean isFront = mCamera2Source.isCameraFacingFront();
                mFaceOverlay.setCameraFacingFront(isFront); 
                mCamera2Source.startFaceDetect(cameraFaceDetectCallback);
        }
} // startDetectFace


/**
 * stopDetectFace
 */ 
private void stopDetectFace() {
        if(mCamera2Source != null) {
                mCamera2Source.stopFaceDetect();
        }
} // stopDetectFace


/**
 * flipCameraFace
 */ 
private void flipCameraFace() {
        if(usingFrontCamera) {
                stopCameraSource();
                usingFrontCamera = false;
                startCameraSource();
                showToast("flip to back");
        } else {
                stopCameraSource();
                usingFrontCamera = true;
                startCameraSource();
                showToast("flip to front");
           }
} // witchFace



/**
 * createCameraSourceFront
 */ 
    private Camera2Source createCameraSourceFront() {
        log_d("createCameraSourceFront");
        Camera2Source camera2Source = 
        new Camera2Source.Builder(this) 
                    .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                    .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                    .setFacing(Camera2Source.CAMERA_FACING_FRONT)
                    .setErrorCallback(cameraErrorCallback)
                    .build();
        return camera2Source;
} // createCameraSourceFront


/**
 * createCameraSourceBack
 */ 
    private Camera2Source createCameraSourceBack() {
        log_d(" createCameraSourceBack");
        Camera2Source camera2Source = 
        new Camera2Source.Builder(this) 
                    .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                    .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                   .setFacing(Camera2Source.CAMERA_FACING_BACK)
                    .setErrorCallback(cameraErrorCallback)
                    .build();

        return camera2Source;
} // createCameraSourceBack


/**
 * startCameraSource
 */ 
    private void startCameraSource() {
            if(mCameraPerm.requestCameraPermissions()) {
                return;
            }

        Camera2Source camera2Source = null;
        		if(usingFrontCamera) {
        			        camera2Source = createCameraSourceFront();
        		} else {
        			       camera2Source = createCameraSourceBack();
        		}
            if(camera2Source != null) {
                mCamera2Source = camera2Source;
                //mPreview.start(camera2Source, mGraphicOverlay);
                mPreview.start(camera2Source);
            }
} // startCameraSource


/**
 * stopCameraSource
 */ 
    private void stopCameraSource() {
        mPreview.stop();
    }


/**
  * Shows an error message dialog.
 */
private void showErrorDialog(String msg) {
             new AlertDialog.Builder(this)
                    .setMessage(msg)
                    .setPositiveButton(R.string.button_ok, null)
                    .show();
} // showErrorDialog


/**
 * showToast
 */
private void showToast( String msg ) {
		ToastMaster.makeText( this, msg, Toast.LENGTH_LONG ).show();
} // showToast


/**
  * showErrorDialog on the UI thread.
 */
private void showErrorDialog_onUI(final String msg) {
    runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showErrorDialog(msg);
                }
    });
}


/**
  * ShowToast on the UI thread.
 */
private void showToast_onUI(final String msg) {
    runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast(msg);
                }
    });
} // showToast_onUI


/**
 * write into logcat
 */ 
private void log_d( String msg ) {
	    if (D) Log.d( TAG, TAG_SUB + " " + msg );
} // log_d


/**
 * FaceDetectCallback
 */ 
 private Camera2Source.FaceDetectCallback cameraFaceDetectCallback = new Camera2Source.FaceDetectCallback () {
        @Override
        public void onDetect(Face[] faces) {
            procDetect(faces);
        }

}; // FaceDetectCallback


/**
 * procDetect
 */ 
private void procDetect(Face[] faces) {
    if( !isFaceDetectRunning ) return;
    int len = faces.length;
    if (len==0) return;

    for(int i=0; i<len; i++) {
        Face face = faces[i];
        if(face == null) continue;
    }

    updateOverlay(faces[0]);
    showToast_onUI("detect face: " + len);
} // procDetect


 /**
 * updateOverlay
 */
private void updateOverlay(Face face) {
    if(face == null) return;
    log_d( "updateOverlay: " + face.toString() );
    mFaceOverlay.setFace(face);
    mGraphicOverlay.add(mFaceOverlay);
} // updateOverlay


/**
 * CameraErrorCallback
 */ 
 Camera2Source.ErrorCallback cameraErrorCallback = new Camera2Source.ErrorCallback() {
        @Override
        public void onError(String msg) {
            showErrorDialog_onUI(msg);
        }
    }; // CameraErrorCallback 


} // class MainActivity 
