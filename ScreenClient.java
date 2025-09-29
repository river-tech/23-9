import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.Socket;
import javax.imageio.ImageIO;
import java.util.concurrent.TimeUnit;
import java.io.BufferedInputStream;

class ScreenPanel extends JPanel {
    BufferedImage frame;
    int off = 0;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); 
        if (frame != null) {
        long t1 = System.currentTimeMillis();
      int w = getWidth();
    int h = getHeight();
    g.drawImage(frame, 0, 0, w, h, null);
        long t2 = System.currentTimeMillis();
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
        System.out.println("[CLIENT] Receiving frames...");
       
        try {
            DataInputStream bis = new DataInputStream(
    new BufferedInputStream(soc.getInputStream(),1024 * 1024) 
);  
                    
            long frameCount = 0;
            long startTime = System.currentTimeMillis();
            while (true) {
                long t1 = System.currentTimeMillis();
                int n = bis.readInt();           
                long t12 = System.currentTimeMillis();
                byte tmp[] = bis.readNBytes(n);   

                long t0 = System.currentTimeMillis();
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(tmp)); 
                long decodeTime = System.currentTimeMillis() - t0;

                panel.frame = img;  
                frameCount++;

                if (frameCount % 100 == 0) {
    long elapsed = System.currentTimeMillis() - startTime; 
    double fps = (frameCount * 1000.0) / elapsed; 


    System.out.println("[CLIENT] Frames=" + frameCount +
        " | elapsed=" + elapsed/1000.0 + " s" +
        " | lastFrameSize=" + (tmp.length / 1024) + " KB" +
        " | lastFrame=" + (t12-t1) + " ms" +
        " | lastRead=" + (t0-t12) + " ms" +
        " | lastDecode=" + decodeTime + " ms" +
        " | avgPerFrame=" + (elapsed / frameCount) + " ms" +
        " | FPS≈" + String.format("%.2f", fps));
}

                panel.repaint(); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}