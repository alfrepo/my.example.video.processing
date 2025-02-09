package my.example.main;

import my.example.video.processing.WebcamViewer;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WebcamViewer::new);
    }
}
