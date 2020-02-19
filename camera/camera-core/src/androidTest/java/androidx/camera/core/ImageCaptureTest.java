/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.Instrumentation;
import android.content.Context;
import android.util.Size;

import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.fakes.FakeAppConfig;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraControl;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
@MediumTest
@RunWith(AndroidJUnit4.class)
public class ImageCaptureTest {
    private FakeCamera mFakeCamera;
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    @Before
    public void setup() throws ExecutionException, InterruptedException {
        CameraXConfig cameraXConfig = CameraXConfig.Builder.fromConfig(
                FakeAppConfig.create()).build();
        Context context = ApplicationProvider.getApplicationContext();
        CameraX.initialize(context, cameraXConfig).get();
        mFakeCamera = new FakeCamera();
    }
    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        CameraX.shutdown().get();
    }
    @Test
    public void onCaptureCancelled_onErrorCAMERA_CLOSED() {
        ImageCapture imageCapture = createImageCapture();
        mInstrumentation.runOnMainSync(() -> bind(imageCapture));
        ImageCapture.OnImageCapturedCallback callback = mock(
                ImageCapture.OnImageCapturedCallback.class);
        FakeCameraControl fakeCameraControl =
                ((FakeCameraControl) mFakeCamera.getCameraControlInternal());
        fakeCameraControl.setOnNewCaptureRequestListener(captureConfigs -> {
            // Notify the cancel after the capture request has been successfully submitted
            fakeCameraControl.notifyAllRequestOnCaptureCancelled();
        });
        mInstrumentation.runOnMainSync(
                () -> imageCapture.takePicture(CameraXExecutors.mainThreadExecutor(), callback));
        final ArgumentCaptor<ImageCaptureException> exceptionCaptor = ArgumentCaptor.forClass(
                ImageCaptureException.class);
        verify(callback, timeout(1000).times(1)).onError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue().getImageCaptureError()).isEqualTo(
                ImageCapture.ERROR_CAMERA_CLOSED);
    }
    @Test
    public void onRequestFailed_OnErrorCAPTURE_FAILED() {
        ImageCapture imageCapture = createImageCapture();
        mInstrumentation.runOnMainSync(() -> bind(imageCapture));
        ImageCapture.OnImageCapturedCallback callback = mock(
                ImageCapture.OnImageCapturedCallback.class);
        FakeCameraControl fakeCameraControl =
                ((FakeCameraControl) mFakeCamera.getCameraControlInternal());
        fakeCameraControl.setOnNewCaptureRequestListener(captureConfigs -> {
            // Notify the failure after the capture request has been successfully submitted
            fakeCameraControl.notifyAllRequestsOnCaptureFailed();
        });
        mInstrumentation.runOnMainSync(
                () -> imageCapture.takePicture(CameraXExecutors.mainThreadExecutor(),
                        callback));
        final ArgumentCaptor<ImageCaptureException> exceptionCaptor = ArgumentCaptor.forClass(
                ImageCaptureException.class);
        verify(callback, timeout(1000).times(1)).onError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue().getImageCaptureError()).isEqualTo(
                ImageCapture.ERROR_CAPTURE_FAILED);
    }
    private ImageCapture createImageCapture() {
        return new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .setCaptureOptionUnpacker((config, builder) -> { })
                .setSessionOptionUnpacker((config, builder) -> { })
                .build();
    }
    // TODO(b/147698557) Should be removed when the binding of UseCase to Camera is simplified.
    private void bind(UseCase useCase) {
        // Sets bound camera to use case.
        useCase.onBind(mFakeCamera);
        useCase.addStateChangeCallback(mFakeCamera);
        useCase.attachCameraControl(mFakeCamera.getCameraInfoInternal().getCameraId(),
                mFakeCamera.getCameraControlInternal());
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mFakeCamera.getCameraInfoInternal().getCameraId(),
                new Size(640, 480));
        useCase.updateSuggestedResolution(suggestedResolutionMap);
    }
}