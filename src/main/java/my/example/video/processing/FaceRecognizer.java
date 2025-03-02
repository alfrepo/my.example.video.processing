package my.example.video.processing;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import my.example.main.Main;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_32F;


// https://onnxruntime.ai/
// TODO check https://onnxruntime.ai/docs/get-started/with-java.html
public class FaceRecognizer {

    private OrtSession session;
    private OrtEnvironment env;
    final String modelPath = "/src/main/resources/yolo11x.pt";

    public FaceRecognizer() throws OrtException {
        env = OrtEnvironment.getEnvironment();

        try {
            File f = getFileFromResource("yolo11s.onnx");
            final ByteBuffer modelPathBuffer = readToDirectByteBuffer(f);
            session = env.createSession(modelPathBuffer);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ByteBuffer readToDirectByteBuffer(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            long fileSize = file.length();
            if (fileSize > Integer.MAX_VALUE) {
                throw new IOException("File too large for direct ByteBuffer");
            }
            int size = (int) fileSize;
            ByteBuffer buffer = ByteBuffer.allocateDirect(size);
            buffer.order(ByteOrder.nativeOrder()); // Important for native interop.
            byte[] tempBuffer = new byte[size];
            fis.read(tempBuffer);
            buffer.put(tempBuffer);
            buffer.rewind();
            return buffer;
        }
    }


    public static File getFileFromResource(String filename) {
        ClassLoader classLoader = Main.class.getClassLoader();
        URL resource = classLoader.getResource(filename);
        if (resource == null) {
            throw new IllegalArgumentException("file is not found!");
        } else {
            return new File(resource.getFile());
        }
    }

    public List<Rectangle> recognizeFace(BufferedImage image) throws OrtException, IOException {

        // Load and preprocess the input image to inputTensor
        int inputWidth = 640;  // Replace with your model's input width
        int inputHeight = 480; // Replace with your model's input height

        // 1. Convert BufferedImage to Mat
        Mat mat = bufferedImageToMat(image);



        // 2. Resize the image to the input size
        Mat resizedMat = new Mat();
        Imgproc.resize(mat, resizedMat, new
                org.opencv.core.Size(inputWidth, inputHeight));

        // 3. Convert Mat to FloatBuffer and normalize
        FloatBuffer inputBuffer = FloatBuffer.allocate(1 * 3 * inputHeight * inputWidth); // Allocate buffer
        Mat floatMat = new Mat();
        resizedMat.convertTo(floatMat, CV_32F); // Convert to float

        // Assuming your model expects RGB input and normalization
        for (int y = 0; y < inputHeight; y++) {
            for (int x = 0; x < inputWidth; x++) {
                double[] pixel = floatMat.get(y, x);
                inputBuffer.put((float) (pixel[2] / 255.0)); // R
                inputBuffer.put((float) (pixel[1] / 255.0)); // G
                inputBuffer.put((float) (pixel[0] / 255.0)); // B
            }
        }
        inputBuffer.rewind(); // Important: Rewind the buffer

        // 4. Create input tensor
        long[] tensorShape = {1, 3, inputHeight, inputWidth}; // Batch size 1, 3 channels, height, width
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputBuffer, tensorShape);

//        // Run inference
//        try (OrtSession.Result outputs = session.run(java.util.Collections.singletonMap("input", inputTensor))) {
//            // Process the output
//            OnnxTensor outputTensor = (OnnxTensor) outputs.get(0).get();
//            FloatBuffer outputBuffer = outputTensor.getFloatBuffer();
//
//            // Example: Assuming the output is bounding boxes [x1, y1, x2, y2, confidence, class]
            List<Rectangle> rectangles = new ArrayList<>();
//            int numDetections = outputBuffer.capacity() / 6; // Assuming 6 values per detection
//
//            for (int i = 0; i < numDetections; i++) {
//                float x1 = outputBuffer.get(i * 6);
//                float y1 = outputBuffer.get(i * 6 + 1);
//                float x2 = outputBuffer.get(i * 6 + 2);
//                float y2 = outputBuffer.get(i * 6 + 3);
//                float confidence = outputBuffer.get(i * 6 + 4);
//                int classId = (int) outputBuffer.get(i * 6 + 5);
//
//                if (confidence > 0.5) { // Example threshold
//                    int rectX = (int) (x1 * image.getWidth());  // Scale back to original image size
//                    int rectY = (int) (y1 * image.getHeight());
//                    int rectWidth = (int) ((x2 - x1) * image.getWidth());
//                    int rectHeight = (int) ((y2 - y1) * image.getHeight());
//                    rectangles.add(new Rectangle(rectX, rectY, rectWidth, rectHeight));
//                }
//            }
            return rectangles;
//
//        } catch (Exception e) {
//            e.printStackTrace(); // Handle exceptions appropriately
//            return new ArrayList<>(); // Or throw an exception
//        } finally {
//            session.close();
//            env.close();
//        }
    }

    public static Mat bufferedImageToMat(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("BufferedImage cannot be null.");
        }

        int type = image.getType();
        Mat mat;

        if (type == BufferedImage.TYPE_BYTE_GRAY) {
            mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC1);
            byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        } else if (type == BufferedImage.TYPE_3BYTE_BGR) {
            mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
            byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        } else if (type == BufferedImage.TYPE_4BYTE_ABGR) { //Handle with alpha channel
            mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC4);
            byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        } else {
            // For other image types, you might need to convert them to a compatible type first.
            // Example: Convert to TYPE_3BYTE_BGR
            BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            convertedImage.getGraphics().drawImage(image, 0, 0, null);
            mat = new Mat(convertedImage.getHeight(), convertedImage.getWidth(), CvType.CV_8UC3);
            byte[] data = ((DataBufferByte) convertedImage.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);


            System.err.println("BufferedImage type " + type + " not directly supported.  Converted to TYPE_3BYTE_BGR.");
        }

        return mat;
    }
}