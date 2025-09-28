import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.Socket;
import javax.imageio.ImageIO;
import java.util.concurrent.TimeUnit;

class ScreenPanel extends JPanel {
    BufferedImage frame;
    int off = 0;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // double-buffer giúp không bị nháy
        // if (frame != null) {
        //     long t1 = System.currentTimeMillis();
        //     int w = getWidth() - 2 * off;
        //     System.out.println("Width: " + w);
        //     int h = getHeight() - 2 * off;
        //     System.out.println("Height: " + h);
        //     Image img2 = frame.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        //     g.drawImage(img2, off, off, getWidth() - off, getHeight() - off,
        //             0, 0, w, h, null);
        //     long t2 = System.currentTimeMillis();
        //     System.out.println("Draw time: " + (t2 - t1) + " ms");
        // }
        if (frame != null) {
        long t1 = System.currentTimeMillis();
      int w = getWidth();
    int h = getHeight();
    g.drawImage(frame, 0, 0, w, h, null);
        long t2 = System.currentTimeMillis();
        System.out.println("Draw time: " + (t2 - t1) + " ms");
    }
    }
}

public class ScreenClient extends JFrame {
    Socket soc;
    ScreenPanel panel = new ScreenPanel();

    public static void main(String[] args) {
        new ScreenClient();
    }

    public ScreenClient() {
        setTitle("Share Screen");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        setVisible(true);

        try {
            soc = new Socket("localhost", 2345);
            System.out.println("[CLIENT] Connected to server.");
            new Thread(this::receiveFrames).start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void receiveFrames() {
        try {
            DataInputStream bis = new DataInputStream(soc.getInputStream());
            long frameCount = 0;
            long startTime = System.currentTimeMillis();

            while (true) {
                int n = bis.readInt();            // số byte ảnh
                byte tmp[] = bis.readNBytes(n);   // đọc dữ liệu ảnh

                long t0 = System.currentTimeMillis();
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(tmp)); // decode JPEG
                long decodeTime = System.currentTimeMillis() - t0;

                panel.frame = img;  // lưu frame mới
                frameCount++;

            
                if (frameCount % 100 == 0) {
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    double fps = (double) frameCount / elapsed;
                    System.out.println("[CLIENT] Frames=" + frameCount +
                            " | size=" + (tmp.length / 1024) + " KB" +
                                " | decode=" + decodeTime + " ms" +
                            " | FPS≈" + String.format("%.2f", fps));
                }

                panel.repaint(); // vẽ lại panel
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}