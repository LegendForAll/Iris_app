package com.example.hothaingoc.flower_cv_a1;


import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Trace;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

// a classifier specialized to label images using TensorFlow.
public class TensorFlowImageClassifier implements Classifier {

    private static final String TAG = "TensorFlowImageClassifier";

    // many results with at least this confidence
    private static final int MAX_RESULTS = 3;
    private static final float THRESHOLD = 0.1f;

    // config
    private String inputName;
    private String outputName;
    private int inputSize;
    private int imageMean;
    private float imageStd;

    // pre-allocated buffers
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    private float[] floatValues;
    private float[] outputs;
    private String[] outputNames;

    private boolean logStats = false;

    private TensorFlowInferenceInterface inferenceInterface;

    // contructor
    public TensorFlowImageClassifier() {
    }

    // initializes a native TensorFlow session for classifying images
    /*
     * @param assetManager The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labelFilename The filepath of label file for classes.
     * @param inputSize The input size. A square image of inputSize x inputSize is assumed.
     * @param imageMean The assumed mean of the image values.
     * @param imageStd The assumed std of the image values.
     * @param inputName The label of the image input node.
     * @param outputName The label of the output node.
     * @throws IOException
     * */

    // method create TensorFlow session
    public static Classifier create (
            AssetManager assetManager,
            String modelFilename,
            String labelFilename,
            int inputSize,
            int imageMean,
            float imageStd,
            String inputName,
            String outputName){
        TensorFlowImageClassifier c = new TensorFlowImageClassifier();
        c.inputName = inputName;
        c.outputName = outputName;
        String actualFilename = labelFilename;

        // Read the label names into memory - doc du lieu tu file label luu vao vector label
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(assetManager.open(actualFilename)));
            String line;
            while ((line = br.readLine()) != null) {
                c.labels.add(line);
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException("Problem reading label file!" , e);
        }

        c.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);

        // The shape of the output is [N, NUM_CLASSES], where N is the batch size.
        final Operation operation = c.inferenceInterface.graphOperation(outputName);
        final int numClasses = (int) operation.output(0).shape().size(1);
        c.inputSize = inputSize;
        c.imageMean = imageMean;
        c.imageStd = imageStd;

        // Pre-allocate buffers.
        c.outputNames = new String[] {outputName};
        //ma tran anh trang den
        c.intValues = new int[inputSize * inputSize];
        //ma tran anh mau
        c.floatValues = new float[inputSize * inputSize * 3];
        //class ket qua duoc tra ve tu N classes
        c.outputs = new float[numClasses];

        return  c;
    }

    @Override
    public List<Recognition> recognizeImage(final Bitmap bitmap){

        // GIAI DOAN TIEN XU LY ANH
        //1. read image. getPixels la method doc toan bo cac pixels cua 1 image vao array(int)
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        //2. duyet mang so nguyen - duyet tung pixels trong 1 image
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            //chuyen mau RGB
            // moi 1 pixel duoc chia ra 3 kenh mau Red, Green, Bule tuong ung 3 dong ben duoi
            floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - imageMean) / imageStd;
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - imageMean) / imageStd;
            floatValues[i * 3 + 2] = ((val & 0xFF) - imageMean) / imageStd;
        }


        // PREDICT MODEL TensorFlow
        // 1.Copy the input data into TensorFlow
        inferenceInterface.feed(inputName, floatValues, 1, inputSize, inputSize, 3);
        // 2.Run the inference call
        inferenceInterface.run(outputNames, logStats);
        // 3.Copy the output Tensor back into the output array
        inferenceInterface.fetch(outputName, outputs);

        // GAN NHAN OUTPUT
        // 1.Find the best classifications
        PriorityQueue<Recognition> pq =
                new PriorityQueue<Recognition>(
                        3,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition lhs, Recognition rhs) {
                                // Intentionally reversed to put high confidence at the head of the queue.
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });

        for (int i = 0; i < outputs.length; ++i) {
            if (outputs[i] > THRESHOLD) {
                pq.add(
                        new Recognition(
                                "" + i, labels.size() > i ? labels.get(i) : "unknown", outputs[i], null));
            }
        }

        final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }

        return recognitions;
    }

    @Override
    public void enableStatLogging(boolean logStats){
        this.logStats = logStats;
    }
    @Override
    public String getStatString() {
        return inferenceInterface.getStatString();
    }
    @Override
    public void close() {
        inferenceInterface.close();
    }
}
