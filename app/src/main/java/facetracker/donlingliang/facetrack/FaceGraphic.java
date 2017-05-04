package facetracker.donlingliang.facetrack;

/**
 * Created by Don Liang on 17/5/4.
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

/**
 * Created by Don Liang on 17/5/4.
 */

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
public class FaceGraphic extends GraphicOverlay.Graphic {

    private BitmapFactory.Options opt;
    private Resources resources;

    PointF facePosition;
    int faceId;
    float faceWidth;
    float faceHeight;
    PointF faceCenter;
    float isSmilingProbability = -1;
    float eyeRightOpenProbability = -1;
    float eyeLeftOpenProbability = -1;
    float eulerZ;
    float eulerY;
    PointF leftEyePos = null;
    PointF rightEyePos = null;
    PointF noseBasePos = null;
    PointF leftMouthCorner = null;
    PointF rightMouthCorner = null;
    PointF mouthBase = null;
    PointF leftEar = null;
    PointF rightEar = null;
    PointF leftEarTip = null;
    PointF rightEarTip = null;
    PointF leftCheek = null;
    PointF rightCheek = null;

    private static final float THRESHOLD_MOUTH_OPEN = 0.2f;
    private static final float THRESHOLD_MOUTH_HALF_OPEN = 0.05f;

    private Bitmap faceBitmap;
    private Bitmap resizeFaceBitmap;

    enum FaceEmojiType {
        CAT,
        DOG,
    }

    // Custom paint
    Paint mPaint;

    private volatile Face mFace;
    private Context mContext;
    private FaceEmojiType mFaceEmojiType = FaceEmojiType.CAT;

    public FaceGraphic(GraphicOverlay overlay, Context context) {
        super(overlay);
        opt = new BitmapFactory.Options();
        opt.inScaled = false;
        resources = context.getResources();
        mContext = context;
        initFaceGraphicPaint();
    }

    private void initFaceGraphicPaint() {
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2);
    }

    public void setId(int id) {
        faceId = id;
    }

    public void changeFaceEmojiType(FaceEmojiType faceEmojiType) {
        mFaceEmojiType = faceEmojiType;
    }

    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    public void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    public void goneFace() {
        mFace = null;
        postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            if (resizeFaceBitmap != null) {
                resizeFaceBitmap.eraseColor(Color.TRANSPARENT);
            }
            canvas.restore();
            isSmilingProbability = -1;
            eyeRightOpenProbability = -1;
            eyeLeftOpenProbability = -1;
            return;
        }

        facePosition = new PointF(translateX(face.getPosition().x), translateY(face.getPosition().y));
        faceWidth = face.getWidth() * 4;
        faceHeight = face.getHeight() * 4;
        faceCenter = new PointF(translateX(face.getPosition().x + faceWidth / 8),
                translateY(face.getPosition().y + faceHeight / 8));
        isSmilingProbability = face.getIsSmilingProbability();
        eyeRightOpenProbability = face.getIsRightEyeOpenProbability();
        eyeLeftOpenProbability = face.getIsLeftEyeOpenProbability();
        eulerY = face.getEulerY();
        eulerZ = face.getEulerZ();

        calculateLandmarkType();

        // Draws a bounding box around the face
        float left = faceCenter.x - faceWidth;
        float top = faceCenter.y - faceHeight;
        float right = faceCenter.x + faceWidth;
        float bottom = faceCenter.y + faceHeight;
        canvas.drawRect(left, top, right, bottom, mPaint);

        if(mFaceEmojiType == FaceEmojiType.CAT) {
            if (face.getIsSmilingProbability() > THRESHOLD_MOUTH_OPEN) {
                faceBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.cat_face);
            } else if (face.getIsSmilingProbability() > THRESHOLD_MOUTH_HALF_OPEN) {
                faceBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.cat_face_half);
            } else {
                faceBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.cat_face_normal);
            }
        } else {
            faceBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.dog_face);
        }

        resizeFaceBitmap = Bitmap.createScaledBitmap(faceBitmap, (int) faceWidth * 2, (int) faceHeight * 2, false);
        canvas.drawBitmap(resizeFaceBitmap, left, top, null);

        // Draw lips
//        if (mouthBase != null && leftMouthCorner != null) {
//            float width = (mouthBase.x - leftMouthCorner.x) * 2f;
//            float height = (mouthBase.y - leftMouthCorner.y) * 4f;
//
//            if (width > 0 && height > 0) {
//                Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.lips);
//                Bitmap resizeBitmap = Bitmap.createScaledBitmap(bitmap, (int) width * 2, (int) height, false);
//                canvas.drawBitmap(resizeBitmap, mouthBase.x - width, mouthBase.y - height / 2f, null);
//            }
//        }
    }

    private void calculateLandmarkType() {
        for (Landmark landmark : mFace.getLandmarks()) {
            switch (landmark.getType()) {
                case Landmark.LEFT_EYE:
                    leftEyePos = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.RIGHT_EYE:
                    rightEyePos = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.NOSE_BASE:
                    noseBasePos = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.LEFT_MOUTH:
                    leftMouthCorner = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.RIGHT_MOUTH:
                    rightMouthCorner = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.BOTTOM_MOUTH:
                    mouthBase = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.LEFT_EAR:
                    leftEar = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.RIGHT_EAR:
                    rightEar = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.LEFT_EAR_TIP:
                    leftEarTip = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.RIGHT_EAR_TIP:
                    rightEarTip = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.LEFT_CHEEK:
                    leftCheek = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.RIGHT_CHEEK:
                    rightCheek = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
            }
        }
    }
}

