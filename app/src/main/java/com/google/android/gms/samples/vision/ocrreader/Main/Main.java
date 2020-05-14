package com.google.android.gms.samples.vision.ocrreader.Main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.emoji.widget.EmojiTextView;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.samples.vision.ocrreader.AR.CustomVisualizer;
import com.google.android.gms.samples.vision.ocrreader.EmojiTranslator.EmojiService;
import com.google.android.gms.samples.vision.ocrreader.EmojiTranslator.EmojiServiceCallback;
import com.google.android.gms.samples.vision.ocrreader.OCR.OcrGraphic;
import com.google.android.gms.samples.vision.ocrreader.R;
import com.google.android.gms.samples.vision.ocrreader.TapListener;
import com.google.android.gms.samples.vision.ocrreader.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class Main extends AppCompatActivity implements TapListener {

    // AR
    private static final String TAG = Main.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ViewRenderable andyRenderable;
    private Session arSession;
    private Config arConfig;
    private AnchorNode anchorNode;

    // OCR
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    private TextRecognizer textRecognizer;
    private int frameSkip = 0;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_integrate);
        arSetup();
        ocrSetup();
    }

    private Bitmap processImageToBitmap(Image image) {

        Log.d(TAG, "getCameraFrame: image acquired");

        //The camera image received is in YUV YCbCr Format. Get buffers for each of the planes
        // and use them to create a new bytearray defined by the size of all three buffers combined
        ByteBuffer cameraPlaneY = image.getPlanes()[0].getBuffer();
        ByteBuffer cameraPlaneU = image.getPlanes()[1].getBuffer();
        ByteBuffer cameraPlaneV = image.getPlanes()[2].getBuffer();

        //Use the buffers to create a new byteArray that
        byte[] compositeByteArray = new byte[cameraPlaneY.capacity() +
                cameraPlaneU.capacity() + cameraPlaneV.capacity()];

        cameraPlaneY.get(compositeByteArray, 0, cameraPlaneY.capacity());
        cameraPlaneU.get(compositeByteArray, cameraPlaneY.capacity(), cameraPlaneU.capacity());
        cameraPlaneV.get(compositeByteArray, cameraPlaneY.capacity() + cameraPlaneU.capacity(),
                                                                            cameraPlaneV.capacity());

        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();

        YuvImage yuvImage = new YuvImage(compositeByteArray, ImageFormat.NV21,
                image.getWidth(), image.getHeight(), null);

        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(),
                yuvImage.getHeight()), 40, baOutputStream);

        byte[] byteForBitmap = baOutputStream.toByteArray();
        Bitmap bitmapImage = BitmapFactory.decodeByteArray(byteForBitmap, 0,
                byteForBitmap.length);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        // free up image resources
        image.close();
        return Bitmap.createBitmap(bitmapImage, 0, 0, bitmapImage.getWidth(),
                bitmapImage.getHeight(), matrix, true);
    }

    private void addDetectedTextGraphics() {
        Log.d(TAG, "getCameraFrame: camera frame received");
        mGraphicOverlay.clear();
        if (arFragment != null) {
            Frame frame = arFragment.getArSceneView().getArFrame();
            if (frame != null) {
                try {

                    Bitmap bitmapImage = processImageToBitmap(frame.acquireCameraImage());

                    mGraphicOverlay.setPreviewWidthAndHeight(bitmapImage.getWidth(),
                            bitmapImage.getHeight());

                    com.google.android.gms.vision.Frame visionFrame =
                            new com.google.android.gms.vision.Frame.Builder()
                                    .setBitmap(bitmapImage).build();

                    if (visionFrame != null) {
                        SparseArray<TextBlock> items = textRecognizer.detect(visionFrame);
                        Log.d(TAG, "getCameraFrame: items recognized");
                        for (int i = 0; i < items.size(); ++i) {
                            TextBlock item = items.valueAt(i);
                            Log.d(TAG, "receiveDetections: item detected = " + item.getValue());
                            OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item);
                            mGraphicOverlay.add(graphic);
                        }
                    } else {
                        Log.d(TAG, "getCameraFrame: frame not built");
                    }
                } catch (NotYetAvailableException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void ocrSetup() {

        // A text recognizer is created to find text.  An associated processor instance
        // is set to receive the text recognition results and display graphics for each text block
        // on screen.
        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        mGraphicOverlay = (GraphicOverlay<OcrGraphic>) findViewById(R.id.graphicOverlay1);
        mGraphicOverlay.setOnTapListener(this);
    }

    public void arFragmentOnUpdate(FrameTime frameTime) {
        // Let the fragment update its state first.
        arFragment.onUpdate(frameTime);

        frameSkip += frameTime.getDeltaTime(TimeUnit.MILLISECONDS);

        // If there is no frame then don't process anything.
        if (arFragment.getArSceneView().getArFrame() == null) {
            return;
        }

        if (frameSkip > 500) {
            frameSkip = 0;
            addDetectedTextGraphics();
        }

        // If ARCore is not tracking yet, then don't process anything.
        if (arFragment.getArSceneView().getArFrame().getCamera().getTrackingState() !=
                TrackingState.TRACKING) {
            return;
        }

        // Place the anchor 1m in front of the camera if anchorNode is null.
        if (anchorNode == null) {
            Session session = arFragment.getArSceneView().getSession();
            float[] pos = { 0,0,-1 };
            float[] rotation = {0,0,0,1};
            if (session != null) {
                Anchor anchor =  session.createAnchor(new Pose(pos, rotation));
                anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());

                // Create the transformable andy and add it to the anchor.
                TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());

                andy.setLocalScale(new Vector3(15f, 15f, 15f));

                andy.setParent(anchorNode);
                andyRenderable.setShadowCaster(false);
                andy.setRenderable(andyRenderable);

            } else {
                Log.d(TAG, "onUpdate: session is null");
            }
        }
    }

    public void arSetup() {
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment1);

        if (arFragment != null) {
            arFragment.getTransformationSystem().setSelectionVisualizer(new CustomVisualizer());
            arFragment.getPlaneDiscoveryController().hide();
            arFragment.getPlaneDiscoveryController().setInstructionView(null);
            arFragment.getArSceneView().getPlaneRenderer().setEnabled(false);

            arFragment.getArSceneView().getScene().addOnUpdateListener(this::arFragmentOnUpdate);

            View view = LayoutInflater.from(this.getApplicationContext()).inflate(R.layout.test_view,
                    null);

            ViewRenderable.builder()
                    .setView(this, view)
                    .setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER)
                    .setHorizontalAlignment(ViewRenderable.HorizontalAlignment.CENTER)
                    .build()
                    .thenAccept(renderable -> andyRenderable = renderable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            arSession = new Session(this);
        } catch (UnavailableArcoreNotInstalledException e) {
            e.printStackTrace();
        } catch (UnavailableApkTooOldException e) {
            e.printStackTrace();
        } catch (UnavailableSdkTooOldException e) {
            e.printStackTrace();
        } catch (UnavailableDeviceNotCompatibleException e) {
            e.printStackTrace();
        }

        arConfig = new Config(arSession);
        arConfig.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        arConfig.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
        arConfig.setFocusMode(Config.FocusMode.AUTO);
        arSession.configure(arConfig);

        if (arFragment != null) {
            arFragment.getArSceneView().setupSession(arSession);
        }
        Log.d(TAG, "onResume: session set up");
    }


    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later",
                    Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later",
                    Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    /**
     * onTap is called to capture the first TextBlock under the tap location and return it to
     * the Initializing Activity.
     *
     * @param rawX - the raw position of the tap
     * @param rawY - the raw position of the tap.
     * @return true if the activity is ending.
     */
    @Override
    public boolean onTap(float rawX, float rawY) {
        Log.d(TAG, "onTap: tap received");
        OcrGraphic graphic = mGraphicOverlay.getGraphicAtLocation(rawX, rawY);
        TextBlock text = null;
        if (graphic != null) {
            text = graphic.getTextBlock();
            if (text != null && text.getValue() != null) {
                if (andyRenderable != null) {
                    EmojiServiceCallback emojiServiceCallback = new EmojiServiceCallback() {
                        @Override
                        public void onSuccess(String translation) {
                            // display response
                            Log.d(TAG, "onResponse: received = " + translation);
                            EmojiTextView emojiTextView =
                                    andyRenderable.getView().findViewById(R.id.emojiText);
                            emojiTextView.setText(translation);
                        }

                        @Override
                        public void onFailure() {
                            Log.d(TAG, "onFailure: could not translate");
                        }
                    };

                    EmojiService emojiService = new EmojiService(emojiServiceCallback);
                    emojiService.translate(this, text.getValue());
                }
            }
            else {
                Log.d(TAG, "text data is null");
            }
        }
        else {
            Log.d(TAG,"no text detected");
        }
        return text != null;
    }


}
