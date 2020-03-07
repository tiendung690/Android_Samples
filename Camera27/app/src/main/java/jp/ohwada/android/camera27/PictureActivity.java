/**
 * Camera2 Sample
 * take picture using Camera2Source
 * 2019-02-01 K.OHWADA
 */

package jp.ohwada.android.camera27;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jp.ohwada.android.camera27.util.CameraPerm;
import jp.ohwada.android.camera27.util.Camera2Source;
import jp.ohwada.android.camera27.util.ToastMaster;
import jp.ohwada.android.camera27.ui.CameraSourcePreview;


/**
 * class PictureActivity 
 * original : https://github.com/EzequielAdrianM/Camera2Vision
 */
public class PictureActivity extends Activity {

        // debug
	private final static boolean D = true;
    private final static String TAG = "Camera2";
    private final static String TAG_SUB = "PictureActivity";

    // output file
    private static final String FILE_PREFIX = "camera_";
    private static final String FILE_EXT = ".jpg";
    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss_SSS";
    private static final int JPEG_QUALITY = 100;

    // Camera2Source
    private Camera2Source mCamera2Source = null;

    // view
    private CameraSourcePreview mPreview;


    // default camera face
    private boolean usingFrontCamera = false;

    // utility
    private CameraPerm mCameraPerm;

 

/**
 * onCreate
 */ 
@Override
protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_picture);

        Button btnFLip = (Button) findViewById(R.id.Button_flip);
    btnFLip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    flipCameraFace();
                }
            }); // btnFLip

        Button btnPicture = (Button) findViewById(R.id.Button_picture);
            btnPicture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    takePicture();
                }
            }); // btnPicture

    // view
    mPreview = (CameraSourcePreview) findViewById(R.id.preview);

     // utility
    mCameraPerm = new CameraPerm(this);

} // onCreate


/**
 * onResume
 */ 
    @Override
    protected void onResume() {
        super.onResume();
        log_d("onResume");
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
        mCameraPerm.onRequestPermissionsResult(requestCode, permissions,  grantResults); 
        startCameraSource();
} // onRequestPermissionsResult


/**
 * flipCameraFace
 */ 
private void flipCameraFace() {
        if(usingFrontCamera) {
                //stopCameraSource();
                usingFrontCamera = false;
                startCameraSource();
                showToast("flip to Back");
        } else {
                //stopCameraSource();
                usingFrontCamera = true;
                startCameraSource();
                showToast("flip to Front");
           }
} // witchFace


 /**
 * takePicture
 */
private void takePicture() {
              if(mCamera2Source != null) {
                mCamera2Source.takePicture(camera2SourcePictureCallback);
    }
} // takePicture


/**
 * createCameraSourceFront
 */ 
    private Camera2Source createCameraSourceFront() {
        log_d("createCameraSourceFront");
            Camera2Source camera2Source = new 
Camera2Source.Builder(this) 
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
        Camera2Source camera2Source = new 
Camera2Source.Builder(this) 
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
        log_d("startCameraSource");
        if(mCameraPerm.requestCameraPermissions()) {
                log_d("not permit");
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
                mPreview.start(camera2Source);
            }
} // startCameraSource


/**
 * stopCameraSource
 */ 
    private void stopCameraSource() {
        log_d("stopCameraSource");
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
private void log_d( int res_id ) {
    log_d( getString(res_id) );
} // log_d

/**
 * write into logcat
 */ 
private void log_d( String msg ) {
	    if (D) Log.d( TAG, TAG_SUB + " " + msg );
} // log_d



/**
 * PictureCallback
 */ 
 Camera2Source.PictureCallback camera2SourcePictureCallback = new Camera2Source.PictureCallback() {
        @Override
        public void onPictureTaken(Image image) {
            procPictureTaken(image);
        }
    }; // PictureCallback 


/**
 * procPictureTaken
 */ 
private void procPictureTaken(Image image) {
        File file = getOutputFile();
        saveImage(image, file);
        final String msg = "saved: " + file.toString();
        log_d(msg);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast(msg);
                }
            }); // runOnUiThread
} // procPictureTaken


/**
 * getOutputFile
 */ 
private File getOutputFile() {
            String filename = getOutputFileName(FILE_PREFIX, FILE_EXT);
        File file = new File(getExternalFilesDir(null), filename);
    return file;
} // getOutputFile


/**
 *getOutputFileName
 */
private String getOutputFileName(String prefix, String ext) {
   SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);
            String currentDateTime =  sdf.format(new Date());
            String filename = prefix + currentDateTime + ext;
    return filename;
} // getOutputFileName


/**
 * saveImage
 */ 
private void saveImage(Image image, File file) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                    if (out != null) out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
} // saveImage


/**
 * CameraErrorCallback
 */ 
 Camera2Source.ErrorCallback cameraErrorCallback = new Camera2Source.ErrorCallback() {
        @Override
        public void onError(String msg) {
            showErrorDialog_onUI(msg);
        }
    }; // CameraErrorCallback 


} // class PictureActivity 
