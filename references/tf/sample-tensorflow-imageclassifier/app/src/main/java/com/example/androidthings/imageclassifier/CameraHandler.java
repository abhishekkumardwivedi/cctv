/*
 * Copyright 2017 The Android Things Samples Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androidthings.imageclassifier;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;

import java.util.Arrays;
import java.util.Collections;
import static android.content.Context.CAMERA_SERVICE;

public class CameraHandler {
    private static final String TAG = CameraHandler.class.getSimpleName();

    public static final int IMAGE_WIDTH = 640;
    public static final int IMAGE_HEIGHT = 480;
    private ImageView mImage;

    private static final int MAX_IMAGES = 1;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;
    private TextureView mTextureView;

    // Lazy-loaded singleton, so only one instance of the camera is created.
    private CameraHandler() {
    }
    private static class InstanceHolder {
        private static CameraHandler mCamera = new CameraHandler();
    }
    public static CameraHandler getInstance() {
        return InstanceHolder.mCamera;
    }
    /**
     * Initialize the camera device
     */
    public void initializeCamera(Context context,
                                 Handler backgroundHandler,
                                 ImageReader.OnImageAvailableListener imageAvailableListener,
                                 TextureView mTextureView) {

        this.mTextureView = mTextureView;
        // Discover the camera instance
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        String[] camIds = {};
        try {
            camIds = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.d(TAG, "Cam access exception getting IDs", e);
        }
        if (camIds.length < 1) {
            Log.d(TAG, "No cameras found");
            return;
        }
        String id = camIds[0];
        Log.d(TAG, "Using camera id " + id);
        // Initialize the image processor
        mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT,
                ImageFormat.JPEG, MAX_IMAGES);
        mImageReader.setOnImageAvailableListener(
                imageAvailableListener, backgroundHandler);
        // Open the camera resource
        try {
            manager.openCamera(id, mStateCallback, backgroundHandler);
        } catch (CameraAccessException cae) {
            Log.d(TAG, "Camera access exception", cae);
        }
    }
    /**
     * Callback handling device state changes
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "Opened camera.");
            mCameraDevice = cameraDevice;
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "Camera disconnected, closing.");
            closeCaptureSession();
            cameraDevice.close();
        }
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Log.d(TAG, "Camera device error, closing.");
            closeCaptureSession();
            cameraDevice.close();
        }
        @Override
        public void onClosed(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "Closed camera, releasing");
            mCameraDevice = null;
        }
    };
    /**
     * Begin a still image capture
     */
    public void takePicture() {
        if (mCameraDevice == null) {
            Log.w(TAG, "Cannot capture image. Camera not initialized.");
            return;
        }
        // Here, we create a CameraCaptureSession for capturing still images.
        try {
            mCameraDevice.createCaptureSession(
                    Collections.singletonList(mImageReader.getSurface()),
                    mSessionCallback,
                    null);
        } catch (CameraAccessException cae) {
            Log.d(TAG, "access exception while preparing pic", cae);
        }
    }
    /**
     * Callback handling session state changes
     */
    private CameraCaptureSession.StateCallback mSessionCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Handler mUiHandler = new Handler(Looper.getMainLooper());

                    // When the session is ready, we start previewing.
                    mCaptureSession = cameraCaptureSession;
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // The camera is already closed
                            if (mCameraDevice == null) {
                                return;
                            }
                            // We want preview for now and not capture.
                            // triggerImageCapture();
                            triggerPreview();
                        }
                    });
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.w(TAG, "Failed to configure camera");
                }
            };
    /**
     * Execute a new capture request within the active session
     */
    private void triggerImageCapture() {
        try {
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            
            Log.d(TAG, "Capture request created.");
            mCaptureSession.capture(captureBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException cae) {
            Log.d(TAG, "camera capture exception");
        }
    }
    
    private void triggerPreview() {

        final CaptureRequest.Builder previewBuilder;
        try {
            previewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

        previewBuilder.addTarget(mImageReader.getSurface());
        previewBuilder.setTag(new Object());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

//    private CaptureRequest.Builder mPreviewRequestBuilder;
//    private CaptureRequest mPreviewRequest;
//
//    private void createCameraPreviewSession() {
//        try {
//            SurfaceTexture texture = mTextureView.getSurfaceTexture();
//            assert texture != null;
//
//            // We configure the size of default buffer to be the size of camera preview we want.
//            texture.setDefaultBufferSize(IMAGE_WIDTH, IMAGE_HEIGHT);
//
//            // This is the output Surface we need to start preview.
//            Surface surface = new Surface(texture);
//
//            // We set up a CaptureRequest.Builder with the output Surface.
//            mPreviewRequestBuilder
//                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//            mPreviewRequestBuilder.addTarget(surface);
//
//            // Here, we create a CameraCaptureSession for camera preview.
//            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
//                    new CameraCaptureSession.StateCallback() {
//
//                        @Override
//                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
//                            // The camera is already closed
//                            if (null == mCameraDevice) {
//                                return;
//                            }
//
//                            // When the session is ready, we start displaying the preview.
//                            mCaptureSession = cameraCaptureSession;
//                            try {
//                                // Auto focus should be continuous for camera preview.
//                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//
//                                // Finally, we start displaying the camera preview.
//                                mPreviewRequest = mPreviewRequestBuilder.build();
//                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
//                                        mCaptureCallback, mBackgroundHandler);
//                            } catch (CameraAccessException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        @Override
//                        public void onConfigureFailed(
//                                @NonNull CameraCaptureSession cameraCaptureSession) {
//                            Log.d(TAG,"onConfigureFailed");
//                        }
//                    }, null
//            );
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * Callback handling capture session events
     */
    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {


                @Override
                public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                                @NonNull CaptureRequest request,
                                                @NonNull CaptureResult partialResult) {
                    Log.d(TAG, "Partial result");
                }
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    session.close();
                    mCaptureSession = null;
                    Log.d(TAG, "CaptureSession closed");
                }
            };

    private void closeCaptureSession() {
        if (mCaptureSession != null) {
            try {
                mCaptureSession.close();
            } catch (Exception ex) {
                Log.e(TAG, "Could not close capture session", ex);
            }
            mCaptureSession = null;
        }
    }

    /**
     * Close the camera resources
     */
    public void shutDown() {
        closeCaptureSession();
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
    }

    /**
     * Helpful debugging method:  Dump all supported camera formats to log.  You don't need to run
     * this for normal operation, but it's very helpful when porting this code to different
     * hardware.
     */
    public static void dumpFormatInfo(Context context) {
        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        String[] camIds = {};
        try {
            camIds = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.d(TAG, "Cam access exception getting IDs");
        }
        if (camIds.length < 1) {
            Log.d(TAG, "No cameras found");
        }
        String id = camIds[0];
        Log.d(TAG, "Using camera id " + id);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
            StreamConfigurationMap configs = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            for (int format : configs.getOutputFormats()) {
                Log.d(TAG, "Getting sizes for format: " + format);
                for (Size s : configs.getOutputSizes(format)) {
                    Log.d(TAG, "\t" + s.toString());
                }
            }
            int[] effects = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
            for (int effect : effects) {
                Log.d(TAG, "Effect available: " + effect);
            }
        } catch (CameraAccessException e) {
            Log.d(TAG, "Cam access exception getting characteristics.");
        }
    }
}