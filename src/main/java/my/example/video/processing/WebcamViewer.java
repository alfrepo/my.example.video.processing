package my.example.video.processing;


import ai.onnxruntime.OrtException;
import nu.pattern.OpenCV;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;



public class WebcamViewer extends JFrame {

    private static final Logger logger = LogManager.getLogger(WebcamViewer.class);

    private JLabel faceFrameLabel;
    private VideoCapture capture;
    private Mat frame;
    private boolean isRecording = false;
    private VideoWriter videoWriter;
    private int frameWidth;
    private int frameHeight;

    FaceRecognizer faceRecognizer;

    public WebcamViewer() {
        setTitle("Webcam Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            faceRecognizer = new FaceRecognizer();
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }

        // Load the OpenCV native library.  Crucial!
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // install the libraries to a temporary folder
        OpenCV.loadShared();

        faceFrameLabel = new JLabel();
        add(faceFrameLabel, BorderLayout.CENTER);

        JButton recordButton = new JButton("Start Recording");
        recordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isRecording = !isRecording;
                if (isRecording) {
                    recordButton.setText("Stop Recording");
                    // Initialize video writer when recording starts
                    String filename = "recorded_video.avi"; // Or .mp4 if you have the codec
                    videoWriter = new VideoWriter(filename, VideoWriter.fourcc('M','J','P','G'), 30, new Size(frameWidth, frameHeight)); // Adjust frame rate if needed
                    if (!videoWriter.isOpened()) {
                        System.err.println("Error opening video writer");
                        isRecording = false; // Reset if writer fails
                        recordButton.setText("Start Recording");
                    }

                } else {
                    recordButton.setText("Start Recording");
                    if (videoWriter != null) {
                        videoWriter.release(); // Release the writer
                        videoWriter = null;
                    }
                }
            }
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(recordButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setVisible(true);

        capture = new VideoCapture(0); // 0 for default webcam, or specify camera index
        frame = new Mat();

        if (!capture.isOpened()) {
            System.err.println("Error opening webcam!");
            System.exit(1);
        }

        // Get frame dimensions AFTER capture is opened
        frameWidth = (int) capture.get(org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH);
        frameHeight = (int) capture.get(org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT);
        setSize(frameWidth, frameHeight); // Set window size to match video


        new Thread(() -> {
            while (true) {
                if (capture.read(frame)) {

                    // Convert Mat to BufferedImage
                    BufferedImage image = Mat2BufferedImage(frame);
                    faceFrameLabel.setIcon(new ImageIcon(image));

                    List<Rectangle> rectangles = new ArrayList<>();

                    // recognize image
                    try {
                        rectangles = faceRecognizer.recognizeFace(image);
                        logger.debug("Face recognition: " + rectangles.size());
                    } catch (Exception e) {
                        logger.error("Face recognition failed: " + e.getMessage());
                    }

                    drawFrame(faceFrameLabel, rectangles);

                    if (isRecording && videoWriter != null) {
                        videoWriter.write(frame);
                    }

                } else {
                    System.err.println("Error reading frame!");
                    break; // or restart capture
                }
            }
            capture.release(); // Release the capture when done
            if (videoWriter != null) {
                videoWriter.release();
            }
            System.exit(0); // Exit the application when the loop finishes (e.g., webcam disconnect)

        }).start();
    }

    private void drawFrame(JLabel label, List<Rectangle> rectangles) {
        // Create a JPanel to draw on
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();

                // Set color and stroke for the frames
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(2));

                // Draw a frame around each rectangle in the list
                for (Rectangle rect : rectangles) {
                    g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
                }

                g2d.dispose();
            }
        };

        panel.setOpaque(false);
        label.add(panel);
        panel.setBounds(0, 0, label.getWidth(), label.getHeight()); // Important!
        label.repaint(); // Force repaint
    }




private BufferedImage Mat2BufferedImage(Mat matrix) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (matrix.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(matrix.cols(), matrix.rows(), type);
        WritableRaster raster = image.getRaster();
        byte[] data = new byte[matrix.cols() * matrix.rows() * matrix.channels()];
        matrix.get(0, 0, data);

        if (matrix.channels() == 1) {
            byte[] pixels = ((DataBufferByte) raster.getDataBuffer()).getData();
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = data[i];
            }
        } else {
            byte[] pixels = ((DataBufferByte) raster.getDataBuffer()).getData();
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = data[i];
            }
        }

        return image;
    }

}