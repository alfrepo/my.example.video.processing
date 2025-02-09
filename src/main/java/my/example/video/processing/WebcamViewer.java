package my.example.video.processing;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class WebcamViewer extends JFrame {

    private JLabel imageLabel;
    private VideoCapture capture;
    private Mat frame;
    private boolean isRecording = false;
    private VideoWriter videoWriter;
    private int frameWidth;
    private int frameHeight;

    public WebcamViewer() {
        setTitle("Webcam Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Load the OpenCV native library.  Crucial!
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // install the libraries to a temporary folder
        OpenCV.loadShared();

        imageLabel = new JLabel();
        add(imageLabel, BorderLayout.CENTER);

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
                    imageLabel.setIcon(new ImageIcon(image));

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