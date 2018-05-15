package com.facedetection.wcy.facedetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.AttributeSet;
import android.util.Log;

import com.facedetection.wcy.facedetection.faceclassifier.ImageClassifier;
import com.facedetection.wcy.facedetection.faceclassifier.Recognition;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ObjectDetectingView extends BaseCameraView {

    private static final String TAG = "ObjectDetectingView";
    private ArrayList<ObjectDetector> mObjectDetects;

    private MatOfRect mObject;

    private ImageClassifier imageClassifier;
    private List<Recognition> classifierResults;

    @Override
    public void onOpenCVLoadSuccess() {
        Log.i(TAG, "onOpenCVLoadSuccess: ");

        mObject = new MatOfRect();

        mObjectDetects = new ArrayList<>();
    }

    @Override
    public void onOpenCVLoadFail() {
        Log.i(TAG, "onOpenCVLoadFail: ");
    }

    public ObjectDetectingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // 子线程（非UI线程）
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        /**
         * Todo: fix buggy portrait view
        Mat dst = mRgba.t();
        Core.flip(mRgba.t(),dst,1);
        Imgproc.resize(dst,dst,mRgba.size());
        mRgba = dst;
        */
        for (ObjectDetector detector : mObjectDetects) {
            // 检测目标
            Rect[] object = detector.detectObject(mGray, mObject);
            for (Rect rect : object) {
                Imgproc.rectangle(mRgba, rect.tl(), rect.br(), detector.getRectColor(), 3);

                Mat crop = new Mat(mRgba,rect);
                Mat resize = new Mat();
                org.opencv.core.Size sz = new org.opencv.core.Size(ImageClassifier.INPUT_SIZE,
                        ImageClassifier.INPUT_SIZE);
                Imgproc.resize(crop,resize,sz);

                Bitmap bitmap = Bitmap.createBitmap(ImageClassifier.INPUT_SIZE,
                        ImageClassifier.INPUT_SIZE,Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(resize,bitmap);
                classifierResults = imageClassifier.recognizeImage(bitmap);
                Point point = rect.tl();
                double offset = 80;
                for (Recognition result : classifierResults) {
                    Imgproc.putText(mRgba,result.toString(), point, Core.FONT_HERSHEY_COMPLEX,
                            2.5,new Scalar(0,255,255));
                    point = new Point(point.x, point.y+offset);
                }
            }
        }
        return mRgba;
    }

    /**
     * 添加检测器
     *
     * @param detector 检测器
     */
    public synchronized void addDetector(ObjectDetector detector) {
        if (!mObjectDetects.contains(detector)) {
            mObjectDetects.add(detector);
        }
    }

    /**
     * 移除检测器
     *
     * @param detector 检测器
     */
    public synchronized void removeDetector(ObjectDetector detector) {
        if (mObjectDetects.contains(detector)) {
            mObjectDetects.remove(detector);
        }
    }

    public void setImageClassifier(ImageClassifier mImageClassifier) {
        imageClassifier = mImageClassifier;
    }

}
