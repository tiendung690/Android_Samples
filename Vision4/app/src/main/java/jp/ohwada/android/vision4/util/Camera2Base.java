/**
 * Vision Sample
 * 2019-02-01 K.OHWADA
 */

package jp.ohwada.android.vision4.util;


import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jp.ohwada.android.vision4.ui.AutoFitTextureView;


 /**
  *  class Camera2Base
 * similar to CameraSource of Vision API
 * original : https://github.com/EzequielAdrianM/Camera2Vision
  */
public class Camera2Base {


        // debug
	protected final static boolean D = true;
    protected final static String TAG = "vision";
    protected final static String TAG_PREV = "Camera2Base";

    protected final static String LF = "\n";


    // camera face
    public static final int CAMERA_FACING_BACK = CameraCharacteristics.LENS_FACING_BACK;
    public static final int CAMERA_FACING_FRONT = CameraCharacteristics.LENS_FACING_FRONT;
    protected int mFacing = CAMERA_FACING_BACK;

    // CaptureRequest
    // AE : auto exposure mode 
    public static final int CAMERA_FLASH_OFF = CaptureRequest.CONTROL_AE_MODE_OFF;
    public static final int CAMERA_FLASH_ON = CaptureRequest.CONTROL_AE_MODE_ON;
    public static final int CAMERA_FLASH_AUTO = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
    public static final int CAMERA_FLASH_ALWAYS = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
    public static final int CAMERA_FLASH_REDEYE = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE;
    protected int mFlashMode = CAMERA_FLASH_AUTO;

    // AF : auto focas mode
    public static final int CAMERA_AF_AUTO = CaptureRequest.CONTROL_AF_MODE_AUTO;
    public static final int CAMERA_AF_EDOF = CaptureRequest.CONTROL_AF_MODE_EDOF;
    public static final int CAMERA_AF_MACRO = CaptureRequest.CONTROL_AF_MODE_MACRO;
    public static final int CAMERA_AF_OFF = CaptureRequest.CONTROL_AF_MODE_OFF;
    public static final int CAMERA_AF_CONTINUOUS_PICTURE = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
    public static final int CAMERA_AF_CONTINUOUS_VIDEO = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
    protected int mFocusMode = CAMERA_AF_AUTO;


// hardware level
// 0
    public static final int DEVICE_LEVEL_LIMITED =           CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED;
// 1
    public static final int DEVICE_LEVEL_FULL =            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL;
// 2
    public static final int DEVICE_LEVEL_LEGACY =    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
// 3
    public static final int DEVICE_LEVEL_3 =          CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3;
     // Added in API level 28
     // 4
    //public static final int DEVICE_LEVEL_EXTERNAL =          CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL;

     protected static final int DEVICE_LEVEL_NONE = -1;  


    // CameraDevice StateCallback Error
    // value 1
   private static final int ERROR_CAMERA_IN_USE = CameraDevice.StateCallback.ERROR_CAMERA_IN_USE;
    // value 2
    private static final int ERROR_MAX_CAMERAS_IN_USE = CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE;
    // value 3
    private static final int ERROR_CAMERA_DISABLED = CameraDevice.StateCallback.ERROR_CAMERA_DISABLED;
    // value 4
   private static final int ERROR_CAMERA_DEVICE = CameraDevice.StateCallback.ERROR_CAMERA_DEVICE;
    // value 5
    private static final int ERROR_CAMERA_SERVICE = CameraDevice.StateCallback.ERROR_CAMERA_SERVICE;


    // choiceBestAspectPictureSize
    protected static final double RATIO_TOLERANCE = 0.1;
    protected static final double MAX_RATIO_TOLERANCE = 0.18;

    protected static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    protected static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();


    /**
     * Clockwise angle through which the output image needs to be rotated to be upright on the device screen in its native orientation
     */
    protected static int mSensorOrientation;


    /**
     * the rotation of the screen from its "natural" orientation.
     */
    protected int mDisplayRotation;


    protected Context mContext;


    // view
    protected AutoFitTextureView mTextureView;
    protected SurfaceView mViewSurfaceView;
    protected Surface mViewSurface;


    /**
     * A reference to the opened {@link CameraDevice}.
     */
    protected CameraDevice mCameraDevice;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    protected HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    protected Handler mBackgroundHandler;

    /**
     * Camera state: Showing camera preview.
     */
    protected static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    protected static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    protected static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    protected static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    protected static final int STATE_PICTURE_TAKEN = 4;


    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    protected CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    protected CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    protected int mState = STATE_PREVIEW;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    protected CameraCaptureSession mCaptureSession;


    /**
     * The {@link Size} of camera preview.
     */
    protected static Size mPreviewSize;

    /**
     *  The size of the largest JPEG format image of the camera　supports
     *  use for stil photo
     */
    protected static Size mLargestSize;


    /**
     * ID of the current {@link CameraDevice}.
     */
    protected String mCameraId;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    protected static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    protected static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * The area of the image sensor which corresponds to active pixels after any geometric distortion correction has been applied
     * The area that can be used by the auto-focus (AF) routine.
     */
    protected Rect mSensorArraySize;



    /**
     * metering regions that can be used by the auto-focus (AF)  routine.
     */
    protected boolean isMeteringAreaAFSupported = false;


    /**
     * isSwappedDimensions that can be used by the record video  routine.
     */
    protected boolean isSwappedDimensions = false;


    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }



    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    protected Semaphore mCameraOpenCloseLock = new Semaphore(1);

    // timeout 2500 msec
    protected static final long OPEN_CLOSE_LOCK_TIMEOUT = 2500;


    /**
     * Whether the current camera device supports Flash or not.
     */
    protected boolean isFlashSupported;


    /**
     * Callback interface used to indicate when an error occurs
     */
    public interface ErrorCallback{
        public void onError(String msg);
    }

    protected ErrorCallback mErrorCallback;


/**
   *  constractor
  */
        public Camera2Base() {
            // nop
        }

/**
   *  constractor
  */
        public Camera2Base(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("No context supplied.");
            }
            mContext = context;
        }


/**
   *  setFocusMode
  */
        public void setFocusMode(int mode) {
            mFocusMode = mode;
        }

/**
   *  setFlashMode
  */
        public void setFlashMode(int mode) {
            mFlashMode = mode;
        }

/**
   * setFacing
  */
        public void setFacing(int facing) {
            if ((facing != CAMERA_FACING_BACK) && (facing != CAMERA_FACING_FRONT)) {
                throw new IllegalArgumentException("Invalid camera: " + facing);
            }
            mFacing = facing;
    } // setFacing


/**
   *  setErrorCallback
  */
        public void setErrorCallback(ErrorCallback cb) {
            mErrorCallback = cb;
        }


 /**
  * getCameraFacing
  */
    public int getCameraFacing() {
        return mFacing;
}


 /**
  *  isCameraFacingFront
  */
    public boolean isCameraFacingFront() {
            boolean isFront = false;
            if (mFacing == CAMERA_FACING_FRONT) {
                    isFront = true;
            }
            return isFront;
}


/**
  * getPreviewSize
 */
    public Size getPreviewSize() {
        return mPreviewSize;
}


    /**
     * getSensorArraySize
     */
    public Rect getSensorArraySize() {
        return mSensorArraySize;
    }

    /**
     * isSwappedDimensions
     */
    public boolean isSwappedDimensions() {
        return isSwappedDimensions;
    }


    /**
     * getSensorOrientation
     */
    public int getSensorOrientation() {
        return mSensorOrientation;
    }


/**
  * startBackgroundThread
  */
    protected void startBackgroundThread() {
prev_log("startBackgroundThread");
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * stopBackgroundThread
     */
    protected void stopBackgroundThread() {
prev_log("stopBackgroundThread");
        try {
            if(mBackgroundThread != null) {
                mBackgroundThread.quitSafely();
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
}


    /**
     * stop
     */
    public void stop() {
            prev_log("stop");
        try {
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mCameraOpenCloseLock) {
                mCameraOpenCloseLock.release();
                mCameraOpenCloseLock = null;
            }
        } catch (Exception e) {
                e.printStackTrace();
        }
        stopBackgroundThread();
        stopExtend();
} // stop


/**
 * stopExtend
 */
protected void stopExtend() {
}


/**
 * isCamera2Native
 */
    public boolean isCamera2Native() {
            int deviceLevel = getDeviceLevel();
            prev_log("deviceLevel=" + deviceLevel);
            // This camera device is running in backward compatibility mode.
          boolean ret =  ((deviceLevel == DEVICE_LEVEL_LEGACY)||(deviceLevel == DEVICE_LEVEL_LIMITED)||(deviceLevel == DEVICE_LEVEL_FULL))? true: false;
            prev_log("isCamera2Native: " + ret);
        return ret;
} // isCamera2Native


/**
 * getDeviceLevel
 */
    protected int getDeviceLevel() {
            prev_log("getDeviceLevel");
            if (!CameraPerm.isCaremaGranted(mContext)) {
                prev_log("not permit");
                return DEVICE_LEVEL_NONE;
            }
            CameraManager manager = null;
            String cameraId = null;
            int deviceLevel = DEVICE_LEVEL_NONE;
        try {
            manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            if (manager != null) {
                    cameraId = getCameraId(manager, mFacing);
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                    deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            }
        } catch (Exception ex) {
                ex.printStackTrace();
        }
        if (manager == null) {
                prev_log("not support API");
                return DEVICE_LEVEL_NONE;
        }
        if( cameraId == null ) {
                prev_log("not found camera");
                return DEVICE_LEVEL_NONE;
        }
        return deviceLevel;
} // getDeviceLevel



/**
 * start with AutoFitTextureView
 */
    public void start(AutoFitTextureView textureView) {
        prev_log(" start AutoFitTextureView");
        mTextureView = textureView;
        setUpCameraOutputs(textureView.getWidth(), textureView.getHeight() );
        SurfaceTexture texture = textureView.getSurfaceTexture();
        if(mPreviewSize != null) {
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        }
        mViewSurface  = new Surface(texture);
        startBackgroundThread();
        prepareCamera();
        openCamera();
} // start


/**
 * start with SurfaceView
 */
    public void start(SurfaceView surfaceView) {
        prev_log(" start SurfaceView");
        mViewSurfaceView = surfaceView;
        SurfaceHolder holder = mViewSurfaceView.getHolder();
        mViewSurface = holder.getSurface();
        setUpCameraOutputs(surfaceView.getWidth(), surfaceView.getHeight());
        startBackgroundThread();
        prepareCamera();
        openCamera();
} // start


 /**
  * setUpCameraOutputs
  */
    protected void setUpCameraOutputs(int width, int height) {
        prev_log("setUpCameraOutputs");
        // check permission
        if (!CameraPerm.isCaremaGranted(mContext)) {
                notifyError("NOT Permission to access Camera");
                return;
        }

        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
         if(manager == null) {
                notifyError("This device doesn't support Camera2 API");
                return;
        }
        mCameraId = getCameraId(manager, mFacing);
        prev_log("cameraId= " + mCameraId);
         if(mCameraId == null) {
                notifyError("NOT found " + getCameraFaceString());
                return;
        }

        CameraCharacteristics characteristics = null;
        StreamConfigurationMap map = null;
        try {
                characteristics
                        = manager.getCameraCharacteristics(mCameraId);
                map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (characteristics == null) {
                    prev_log("characteristics null");
                    return;
        }
        if (map == null) {
                    prev_log("map null");
                    return;
        }
        
        // For still image captures, we use the largest available size.
        mLargestSize =  getJpegLargestSize(map);
        // no inspection ConstantConditions
        mSensorOrientation = getSensorOrientation(characteristics);

        // preview size
        mDisplayRotation = DisplayUtil.getDisplayRotation(mContext);

        // Find out if we need to swap dimension to get the preview size relative to sensor coordinate.
        isSwappedDimensions = getSwappedDimensions();
        mPreviewSize = calcPreviewSize(map, width, height, mLargestSize, isSwappedDimensions);

        // The area of the image sensor which corresponds to active pixels after any geometric distortion correction has been applied.
            mSensorArraySize = getSensorArraySize(characteristics);

//The maximum number of metering regions that can be used by the auto-focus (AF) routine.
            isMeteringAreaAFSupported = hasMeteringAreaAFSupported(characteristics);

       // Check if the flash is supported.
        isFlashSupported = hasFlashSupport(characteristics);

        configureTransform(width, height, mPreviewSize, mDisplayRotation);

        setUpExtend( characteristics);

} //  setUpCameraOutputs


/**
 * setUpExtend
 * for override
 */
protected void setUpExtend(CameraCharacteristics characteristics) {
    // nop
}


/**
 * hasMeteringAreaAFSupported
 * The maximum number of metering regions that can be used by the auto-focus (AF) routine.
 */
protected boolean hasMeteringAreaAFSupported(CameraCharacteristics characteristics) {
            Integer maxAFRegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
            boolean isSupported = (maxAFRegions >= 1)? true: false;
            prev_log( "maxAFRegions= "  + maxAFRegions + " isMeteringAreaAFSupported= " + isSupported);
        return  isSupported;
}


/**
 * getSensorArraySize
 */
protected Rect getSensorArraySize(CameraCharacteristics characteristics) {
        Rect rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        prev_log(  "SensorArraySize=" + rect.toString() );
        return rect;
}


/**
 * getSensorOrientation
 */
protected int getSensorOrientation(CameraCharacteristics characteristics) {
        int orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        prev_log("SensorOrientation= " + orientation);
        return orientation;
}

/**
 * getCameraFace
 */
protected String getCameraFaceString() {
    String msg = "";
    switch(mFacing) {
        case CAMERA_FACING_FRONT:
            msg = "Front Camera";
            break;
        case CAMERA_FACING_BACK:
            msg = "Back Camera";
            break;
    }
    return msg;
} // getCameraFace


/**
 * getCameraId
 */
protected String getCameraId(CameraManager manager, int cameraFacing) {
        String cameraId = null;
        try {
            String[] ids = manager.getCameraIdList();
            for (int i=0; i<ids.length; i++ ) {
                String id = ids[i];
                CameraCharacteristics c
                        = manager.getCameraCharacteristics(id);
                int facing = c.get(CameraCharacteristics.LENS_FACING);
                if (facing == cameraFacing) {
                    cameraId = id;
                    break;
                }
            } // for
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return cameraId;
} // getCameraId




/**
  * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
 * This method should be called after the camera preview size is  * determined in setUpCameraOutputs 
 */
protected void configureTransform(int viewWidth, int viewHeight, Size previewSize, int displayRotation) {
        if (null == mTextureView || null == previewSize ) {
            return;
        }
        int previewWidth = previewSize.getWidth();
        int previewHeight = previewSize.getHeight();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewHeight, previewWidth);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == displayRotation || Surface.ROTATION_270 == displayRotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.max(
                    (float) viewHeight / previewHeight,
                    (float) viewWidth / previewWidth);
                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate(90 * (displayRotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == displayRotation) {
                matrix.postRotate(180, centerX, centerY);
        }
        if( mTextureView != null ) {
            mTextureView.setTransform(matrix);
        }
} // configureTransform



/**
  * calcPreviewSize
  */
protected Size calcPreviewSize(StreamConfigurationMap map, int width, int height, Size largest, boolean isSwapped) {
                    Point displaySize = DisplayUtil.getDisplaySize(mContext);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;
                if (isSwapped) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

            // Danger
            // Attempting to use too large a preview size could  exceed the camera bus' bandwidth limitation
            Size[] choices = map.getOutputSizes(SurfaceTexture.class);
            Size previewSize = chooseOptimalSize(choices,
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);
    prev_log("PreviewSize: " + previewSize.toString());
    return previewSize;
} // calcPreviewSize


    /**
     * choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     */
protected Size chooseOptimalSize(Size[] choices, int textureViewWidth,
            int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
            prev_log("chooseOptimalSize");
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                    option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            prev_log("Couldn't find any suitable preview size");
            return choices[0];
        }
} // chooseOptimalSize



/**
 * Find out if we need to swap dimension to get the preview size relative to sensor coordinate.
 */
protected boolean getSwappedDimensions() {
         	            boolean swappedDimensions = false;
            	switch (mDisplayRotation) {
            	    case Surface.ROTATION_0:
            	    case Surface.ROTATION_180:
            	        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
            	            swappedDimensions = true;
            	        }
            	        break;
            	    case Surface.ROTATION_90:
            	    case Surface.ROTATION_270:
            	        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
            	            swappedDimensions = true;
            	        }
            	        break;
            	    default:
            	        prev_log( "Display rotation is invalid: " + mDisplayRotation);
            	}
                prev_log("SwappedDimensions= " + swappedDimensions);
                return swappedDimensions;
} // getSwappedDimensions


/**
 * hasFlashSupport
 */
protected boolean hasFlashSupport(CameraCharacteristics characteristics) {
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                boolean hasSupport = available == null ? false : available;
        prev_log("hasFlashSupport= " + hasSupport);
        return hasSupport;
} // hasFlashSupport


/**
 * getJpegLargestSize
 */
protected Size getJpegLargestSize(StreamConfigurationMap map) {
    Size[] supportedSizes = map.getOutputSizes(ImageFormat.JPEG);
    Size largest = chooseLargestSize(supportedSizes);
    prev_log("JPEG LargestSize: " + largest.toString());
    return largest;
} // getJpegLargestSize


/**
 * chooseLargestSize
 */
protected Size chooseLargestSize(Size[] supportedSizes) {
    Size largest = Collections.max(Arrays.asList(supportedSizes), new CompareSizesByArea());
    return largest;
} // chooseLargestSize


/**
  * prepareCamera
 * for override
 */
protected void prepareCamera() {
    // nop
}


/**
  * openCamera
 */
protected void openCamera() {
            prev_log("openCamera");
    if (mCameraId == null) {
                prev_log("CameraId null");
                return;
    }

    try {
            boolean ret = mCameraOpenCloseLock.tryAcquire(OPEN_CLOSE_LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
            if (!ret) {
                    prev_log("Time out waiting to lock camera opening");
            }
    } catch (InterruptedException e) {
        e.printStackTrace();
    }

    try {
            CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
                e.printStackTrace();
        } catch (NullPointerException e) {
                e.printStackTrace();
        } catch (Exception ex) {
                ex.printStackTrace();
        }
} // openCamera


/**
   * closePreviewSession
  */
    protected void closePreviewSession() {
            prev_log("closePreviewSession");
        if(mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
} // closePreviewSession


/**
   * createCameraPreviewSession
  */
    protected void createCameraPreviewSession() {
prev_log("createCameraPreviewSession");
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(mViewSurface);
                List outputs = Arrays.asList(mViewSurface);
                mCameraDevice.createCaptureSession(outputs, mPreviewSession, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
} // createCameraPreviewSession


/**
   * CameraCaptureSession.StateCallback
  */
protected CameraCaptureSession.StateCallback mPreviewSession = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured( CameraCaptureSession cameraCaptureSession) {
                    prev_log("onConfigured");
                    procCaptureSessionConfigured(cameraCaptureSession);
        }
        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                prev_log("CameraCaptureSession Configuration Failed!");
        }
}; // CameraCaptureSession.StateCallback


/**
  * procCaptureSessionConfigured
  */
protected void procCaptureSessionConfigured( CameraCaptureSession cameraCaptureSession) {
    prev_log("procCaptureSessionConfigured");
                    // The camera is already closed
                    if (null == mCameraDevice) {
                        return;
                    }

                    // When the session is ready, we start displaying the preview.
                    mCaptureSession = cameraCaptureSession;

                    try {
                        // Auto focus should be continuous for camera preview.
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, mFocusMode);
                        if(isFlashSupported) {
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, mFlashMode);
                        }

                        // Finally, we start displaying the camera preview.
                        mPreviewRequest = mPreviewRequestBuilder.build();
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
} // procCaptureSessionConfigured


/**
  * getOrientation
  */
    protected int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }


/**
 * write into logcat
 */ 
protected void prev_log( String msg ) {
	    if (D) Log.d( TAG, TAG_PREV + " " + msg );
} // prev_log


    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    protected CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened( CameraDevice cameraDevice) {
            prev_log("onOpened");
            procStateOpened(cameraDevice);
        }
        @Override
        public void onDisconnected( CameraDevice cameraDevice) {
            prev_log("onDisconnected");
            procStateDisconnected(cameraDevice);
        }
        @Override
        public void onError( CameraDevice cameraDevice, int error) {
            prev_log("StateCallback onError: " + error);
            procStateError(  cameraDevice, error);
        }
    }; // CameraDevice.StateCallback


/**
  * procStateOpened
 */
protected void procStateOpened( CameraDevice cameraDevice) {
            prev_log("procStateOpened");
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
}


/**
  * procStateDisconnected
 */
protected void procStateDisconnected( CameraDevice cameraDevice) {
            prev_log("procStateDisconnected");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
}


/**
  * procStateError
 */
protected void procStateError( CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            String msg = "State Error: " + LF + getCameraDeviceStateErrorMsg(error);
            prev_log(msg);
            notifyError(msg);
} // procStateError


/**
  * getCameraDeviceStateErrorMsg
 */
protected String getCameraDeviceStateErrorMsg(int error) {
    String msg = "";
    switch(error) {
        case ERROR_CAMERA_IN_USE:
            msg = "Camera inuse";
            break;
        case ERROR_MAX_CAMERAS_IN_USE:
            msg = "max Cameras in use";
            break;
        case ERROR_CAMERA_DISABLED:
            msg = "Camera Disabled";
            break;
        case ERROR_CAMERA_DEVICE:
            msg = "Camera Device Error";
            break;
        case ERROR_CAMERA_SERVICE:
            msg = "Camera Service Error";
            break;
    }
    return msg;
} // getCameraDeviceStateErrorMsg


/**
  * notifyError
  * callback error, or throw RuntimeException
 */
protected void notifyError(String msg) {
            prev_log("notifyError: " + msg);
            if (mErrorCallback != null) {
                        mErrorCallback.onError(msg);
            } else {
                    throw new RuntimeException(msg);
            }
} // notifyError


/**
  * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
 */
protected CameraCaptureSession.CaptureCallback mCaptureCallback =  new CameraCaptureSession.CaptureCallback() {
    @Override
    public void onCaptureProgressed( CameraCaptureSession session,
                                         CaptureRequest request,
                                         CaptureResult result) {
            //prev_log("onCaptureProgressed");
            procCaptureResult(result);
    }
    @Override
    public void onCaptureCompleted( CameraCaptureSession session,
                                        CaptureRequest request,
                                       TotalCaptureResult result) {
        //prev_log("onCaptureCompleted");
        procCaptureCompleted( session, request, result);
    }
    @Override
        public void onCaptureFailed( CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
        prev_log("onCaptureFailed");
        procCaptureFailed(session,
                                        request,
                                       failure);
    }

}; // CameraCaptureSession.CaptureCallback


/**
  * procCaptureCompleted
 * for override
 */
protected void procCaptureCompleted( CameraCaptureSession session,
                                        CaptureRequest request,
                                       TotalCaptureResult result) {
            //prev_log("procCaptureCompleted");
            procCaptureResult( result);
} // procCaptureCompleted


/**
  * procCaptureFailed
 * for override
 */
protected void procCaptureFailed( CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
    prev_log("procCaptureFailed: " + failure.getReason());
} // procCaptureFailed


/**
  * procCaptureResult
 * for override
 */
protected void procCaptureResult(CaptureResult result) {
}


} // class Camera2Base
