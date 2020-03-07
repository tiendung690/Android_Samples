/**
 * Cloud Vision Sample
 *  VisionClient
 * 2019-02-01 K.OHWADA
 */

package jp.ohwada.android.cloudvision1;


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;


import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.Status;


import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 *  class VisionClient
 *  original : https://github.com/GoogleCloudPlatform/cloud-vision/tree/master/android
 */
public class VisionClient  {

    // debug
	private final static boolean D = true;
    private final static String TAG = "CloudVision";
    private final static String TAG_SUB = "VisionClient";


/**
 * API_KEY forCloudVision
 */ 
    private static final String CLOUD_VISION_API_KEY = Constant.CLOUD_VISION_API_KEY;


/**
 * HTTP Header
 */ 
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";


/**
 * Request Feature Param
 */ 
    private static final String DETECTION_TYPE = "LABEL_DETECTION";
    private static final int MAX_RESULTS = 10;


/**
 * String Format for Response
 */ 
    private static final String FORMAT_LABEL_SCORE = "%s: %.3f";


/**
 * char
 */ 
    private static final String LF = "\n";


/**
  * interface VisionCallback
 */	
public interface VisionCallback {
    void onPostExecute(String result);
    void onError(String error);
}


/**
  * Activity 
 */	 
	private Activity mActivity;


/**
  * PackageName 
 */
    private String mPackageName;


/**
  * SHA1 signature of this app
 */
    private String mSignature;


/**
  * VisionCallback
 */
    private VisionCallback  mCallback;


/**
  * constractor 
 */	    
public VisionClient( Activity activity  ) {
		mActivity = activity;
        PackageManager packageManager = activity.getPackageManager();
        mPackageName = activity.getPackageName();
        mSignature = PackageManagerUtils.getSignature(packageManager, mPackageName);
} 


/** 
 *  callCloudVision
 */
public void callCloudVision(Bitmap bitmap, VisionCallback callback ) {
        mCallback = callback;

        // Do the real work in an async task, because we need to use the network anyway
                Vision.Images.Annotate annotate = prepareAnnotationRequest(bitmap);
                AsyncTask<Object, Void, AnnotateImageResponse> visionTask = new VisionTask(mActivity, annotate);
                visionTask.execute();

} // callCloudVision


/** 
 *  prepareAnnotationRequest
 */
private Vision.Images.Annotate prepareAnnotationRequest( Bitmap bitmap)  {

        Vision vision = createVision();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                new BatchAnnotateImagesRequest();

        List<AnnotateImageRequest> annotateImageRequests = createAnnotateImageRequestList(bitmap);
        batchAnnotateImagesRequest.setRequests(annotateImageRequests);

    Vision.Images.Annotate annotateRequest =
            null;
    try {
        annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
    } catch (IOException e) {
        e.printStackTrace();
    }

    // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);
        log_d( "created Cloud Vision request object, sending request");

        return annotateRequest;

} // prepareAnnotationRequest


/**
 * createVision
 */
private Vision createVision() {

        Vision.Builder builder = createVisionBuilder();
        Vision vision = builder.build();
        return vision;

} // createVision


/**
 * createVision.Builder
 */
private Vision.Builder createVisionBuilder() {

        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);

        VisionRequestInitializer requestInitializer = createVisionRequestInitializer() ;
        builder.setVisionRequestInitializer(requestInitializer);
        return  builder;

} // createVision.Builder


/**
 * createVisionRequestInitializer
 */
private VisionRequestInitializer createVisionRequestInitializer() {

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(CLOUD_VISION_API_KEY) {

                    /**
                     * We override this so we can inject important identifying fields into the HTTP
                     * headers. This enables use of a restricted cloud platform API key.
                     */
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest) {

                        try {
                            super.initializeVisionRequest(visionRequest);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // PackageName
                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, mPackageName);

                        // SHA1 signature
                        visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, mSignature);

                    }

                }; // VisionRequestInitializer

        return requestInitializer;
} // createVisionRequestInitializer


/**
 * createAnnotateImageRequestList
 */
private List<AnnotateImageRequest> createAnnotateImageRequestList(Bitmap bitmap) {

    List<AnnotateImageRequest> list = new ArrayList<AnnotateImageRequest>();
    AnnotateImageRequest annotateImageRequest = createAnnotateImageRequest(bitmap);
    list.add(annotateImageRequest);
    return list;

} // createAnnotateImageRequestList


/**
 * createAnnotateImageRequest
 */ 
private AnnotateImageRequest createAnnotateImageRequest(Bitmap bitmap) {

        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // create the image
            Image base64EncodedImage = crateEncodedImage(bitmap);
            annotateImageRequest.setImage(base64EncodedImage);

            // add the features we want
            List<Feature> features = createFeatureList();
            annotateImageRequest.setFeatures(features);

        return annotateImageRequest;

} // createAnnotateImageRequest


/**
 * crateEncodedImage
 */ 
private Image crateEncodedImage(Bitmap bitmap) {

            // create the image
            Image image = new Image();

            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands 
            // but Cloud Vision
            byte[] imageBytes = BitmapUtil.convJpegByteArray(bitmap);

            // Base64 encode the JPEG
            image.encodeContent(imageBytes);

            return image;

} // crateEncodedImage


/**
 * createFeatureList
 */ 
private List<Feature> createFeatureList() {

    List<Feature> list = new ArrayList<Feature>();
    Feature labelDetection = createLabelDetectionFeature();
    list.add(labelDetection);
    return list;

} // createFeatureList


/**
 * createLabelDetectionFeature
 */ 
private Feature createLabelDetectionFeature() {

                Feature labelDetection = new Feature();
                labelDetection.setType(DETECTION_TYPE);
                labelDetection.setMaxResults(MAX_RESULTS);
        return labelDetection;

} // reateLabelDetectionFeature


/**
 * write into logcat
 */ 
private void log_d( String msg ) {
	    if (D) Log.d( TAG, TAG_SUB + " " + msg );
} // log_d


/** 
 *  class VisionTask
 */
public  class VisionTask extends AsyncTask<Object, Void, AnnotateImageResponse> {


/**
  * WeakReference
 */	
        private final WeakReference<Activity> mActivityWeakReference;


/**
  * Vision.Images.Annotate
 */	
        private Vision.Images.Annotate mRequest;


/**
  * constractor 
 */	
public VisionTask(Activity activity, Vision.Images.Annotate annotate) {
            mActivityWeakReference = new WeakReference<>(activity);
            mRequest = annotate;
        }


/** 
 *  doInBackground
 */
        @Override
        protected AnnotateImageResponse doInBackground(Object... params) {
            log_d("doInBackground");
            AnnotateImageResponse response = sendRequest(mRequest);
            return response;
    } // doInBackground


/** 
 *  onPostExecute
 */
        protected void onPostExecute(AnnotateImageResponse response) {
                log_d("onPostExecute: ");
                callbackResponse(response);
        } // onPostExecute



} // class VisionTask


/** 
 *  sendRequest
 */
private AnnotateImageResponse sendRequest(Vision.Images.Annotate request) {

            BatchAnnotateImagesResponse batchResponse = null;
            GoogleJsonError jsonError = null;
            try {
                log_d("created Cloud Vision request object, sending request");
                 batchResponse = request.execute();
            } catch (GoogleJsonResponseException e) {
                    jsonError = e.getDetails();
			        e.printStackTrace();
            } catch (IOException e) {
			        e.printStackTrace();
            }

            if (jsonError != null) {
                procJsonError(jsonError);
            }

            if(batchResponse == null) return null;

            AnnotateImageResponse response = null;
            List<AnnotateImageResponse> list 
                = batchResponse.getResponses();
            if((list != null)&&(list.size() > 0)) {
                        response = list.get(0);
            }
            return response;
} // sendRequest


/** 
 *  procJsonError
 */
private void  procJsonError(GoogleJsonError jsonError) {

        if(jsonError == null) return;
        log_d("procJsonError: " + jsonError.toString() );

       Integer code = jsonError.getCode();
        String message = jsonError.getMessage();
        List<GoogleJsonError.ErrorInfo> list = jsonError.getErrors();
        String infos = getErrorInfoList( list );
        StringBuilder sb = new StringBuilder();
        sb.append("code: ");
        sb.append(code);
        sb.append(LF);
        sb.append(message);
        sb.append(LF);
        sb.append(infos);
        String error = sb.toString();
        log_d( "error: " + error );
        if (mCallback != null ) {
                mCallback.onError(error);
        }

} // procJsonError



/** 
 *  getErrorInfoList
 */
private String getErrorInfoList(List<GoogleJsonError.ErrorInfo> list ) {

        log_d("getErrorInfoList");
         StringBuilder sb = new StringBuilder();
        for(GoogleJsonError.ErrorInfo info: list) {
                if(list == null) continue;
                String str = getErrorInfo(info);
                if(str == null) continue;
                sb.append(str);

        } // for
        return sb.toString();

} // getErrorInfoList



/** 
 *  getErrorInfo
 */
private String getErrorInfo(GoogleJsonError.ErrorInfo info) {

        if(info == null) return null;
        log_d("getErrorInfo");

        String domain = info.getDomain();
        String message = info.getMessage();
        String reason = info.getReason();
        StringBuilder sb = new StringBuilder();
        sb.append("domain: ");
        sb.append(domain);
        sb.append(LF);
        sb.append("reason: ");
        sb.append(reason);
        sb.append(LF);
        sb.append(message);
        sb.append(LF);
        return sb.toString();

} // getErrorInfo


/** 
 *  callbackResponse
 */
private void callbackResponse(AnnotateImageResponse response) {
                if(response == null) return;
                String result = convertResponseToString(response);
                String error = getResponseError(response);
                if(mCallback != null ) {
                        // callback to Activity
                        mCallback.onPostExecute(result);
                        if(error != null ) {
                                mCallback.onError(error);
                        }
                }

} // callbackResponse


/** 
 *  convertResponseToString
 */
private  String convertResponseToString(AnnotateImageResponse response) {

        List<EntityAnnotation> labels = response.getLabelAnnotations();
        if (labels == null)  return null;
        return convertEntityAnnotationsToString( labels);

    } // convertResponseToString


/** 
 *  convertEntityAnnotationsToString
 */
private  String convertEntityAnnotationsToString(List<EntityAnnotation> labels) {

        if (labels == null) return null;

        StringBuilder message = new StringBuilder();

        for (EntityAnnotation label : labels) {
                String str_label = convertLabelToString(label);
                message.append(str_label);
                message.append(LF);
        }// for

        return message.toString();

} // convertEntityAnnotationsToString


/** 
 *  convertLabelToString
 */
private String convertLabelToString(EntityAnnotation label) {

        float score = label.getScore();
        String description = label.getDescription();
        String str_label = String.format(Locale.US, FORMAT_LABEL_SCORE, description, score);
        return str_label;

} // convertLabelToString


/** 
 *  getResponseError
 */
private String getResponseError(AnnotateImageResponse response) {
    log_d("getResponseError");
    Status  status = response.getError();
    if(status == null ) return null;
    Integer code = status.getCode();
    String message = status.getMessage();
    String details = getStatusDetails(status);
    StringBuilder sb = new StringBuilder();
        sb.append("code: ");
        sb.append(code);
         sb.append(LF);
        sb.append("message: ");
        sb.append(message);
        sb.append(LF);
    if(details != null) {
        sb.append("details: ");
        sb.append(details);
        sb.append(LF);
    }
    return sb.toString();

} // getResponseError



/** 
 *  getStatusDetails
 */
private String getStatusDetails(Status status) {
    log_d("getStatusDetails");
    List<Map<String,Object>> list = status.getDetails();
    if(list == null) return null;
    StringBuilder sb = new StringBuilder();
    for(Map<String,Object> map: list) {
            String details = getMapDetails(map);
            if(details != null ) {
                    sb.append(details);
            }
    }
    sb.append(LF);
    return sb.toString();

} // getStatusDetails



/** 
 *  getMapDetails
 */
private String getMapDetails(Map<String,Object> map) {

        if(map == null) return null;
        Set<String> keySet = map.keySet();
        StringBuilder sb = new StringBuilder();

        for (Iterator<String> iterator = keySet.iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                Object obj = map.get(key);
                sb.append(key);
                sb.append(": ");
                if(obj != null) {
                        sb.append(obj.toString());
                }
                sb.append(LF);
        } // for
        return sb.toString();

} // getMapDetails


} // class VisionClient
