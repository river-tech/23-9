import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Graphics2D;
import java.util.concurrent.CopyOnWriteArrayList;
import java.awt.Dimension;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.IIOImage;
import javax.imageio.stream.ImageOutputStream; 
import java.nio.ByteBuffer;
// import java.awt.image.ImageOutputStream;



public class ScreenServer {
    public static void main(String[] args) {
        new ScreenServer();
    }

    public ScreenServer() {
        System.out.println("ScreenServer");
        Screen s = new Screen();
        s.start();
        try (ServerSocket server = new ServerSocket(2345)) {
            System.out.println("[SERVER] Listening on port 2345...");
            while (true) {
                Socket soc = server.accept();
                System.out.println("[SERVER] Client connected: " + soc.getInetAddress());
                ScreenProcessing sp = new ScreenProcessing(soc);
                sp.start();
                // ClientManager.addClient(soc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Screen extends Thread {
    static byte[] tmp;
    static int count = 0;
    private long startTime = System.currentTimeMillis();
    public void run() {
        System.out.println("Screen");
        try {
            Robot r = new Robot();
            
            Rectangle capture = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

            while (true) {
                long t0 = System.currentTimeMillis();
               	BufferedImage img = r.createScreenCapture(capture);
                
                long t1 = System.currentTimeMillis();

				// int targetW = 1080, targetH = 600;
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int targetW = (int) (screenSize.getWidth() /1.5);
                int targetH = (int) (screenSize.getHeight() /1.5);
				Image scaled = img.getScaledInstance(targetW, targetH, Image.SCALE_DEFAULT);

				BufferedImage down = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
				Graphics2D g2d = down.createGraphics();
				g2d.drawImage(scaled, 0, 0, targetW, targetH, null);
				g2d.dispose();

                long t2 = System.currentTimeMillis();

                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
                ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();

                jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                jpgWriteParam.setCompressionQuality(0.8f); 

                ImageOutputStream ios = ImageIO.createImageOutputStream(bos);
                jpgWriter.setOutput(ios);
                jpgWriter.write(null, new IIOImage(down, null, null), jpgWriteParam);
                
                ios.close();
                jpgWriter.dispose();
                tmp = bos.toByteArray();

                long t3 = System.currentTimeMillis();
				count++;
                // ClientManager.broadcast(tmp);
                
                long encodeTime = t3 - t0;

               if (count % 100 == 0) {
                 double elapsed = (t3 - startTime) / 1000.0;
        double fps = count / elapsed;

        System.out.println("[SERVER] Frames=" + count +
            " | size=" + (tmp.length / 1024) + " KB" +
            " | capture=" + (t1 - t0) + " ms" +
            " | scale=" + (t2 - t1) + " ms" +
            " | encodeJPEG=" + (t3 - t2) + " ms" +
            " | total=" + encodeTime + " ms" +
            " | FPS≈" + String.format("%.2f", fps));
    }
				Thread.sleep(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class ScreenProcessing extends Thread {
    Socket soc;
    int countNow;
  
    public ScreenProcessing(Socket soc) {
        this.soc = soc;
    }

    public void run() {
          System.out.println("ScreenProcessing");
        try (DataOutputStream output = new DataOutputStream(soc.getOutputStream())) {
            while (true) {
                if (countNow == Screen.count || Screen.tmp == null) {
                    Thread.sleep(5);
                    continue;
                }
                byte[] tmp = Screen.tmp.clone();
                countNow = Screen.count;
                output.writeInt(tmp.length);
                output.write(tmp);
                output.flush();
            }
        } catch (Exception e) {
            System.out.println("[SERVER] Client disconnected.");
        }
    }
}

// class ClientManager {
//     static CopyOnWriteArrayList<DataOutputStream> clients = new CopyOnWriteArrayList<>();
//     public static void addClient(Socket soc) {
//         System.out.println("ClientManager");
//         try {
//             clients.add(new DataOutputStream(soc.getOutputStream()));
//             System.out.println("[SERVER] Added client: " + soc.getInetAddress());
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }

//     public static void broadcast(byte[] frame) {
//         for (DataOutputStream out : clients) {
//             try {
//                 // long t0 = System.currentTimeMillis();

//                ByteBuffer packet = ByteBuffer.allocate(4 + frame.length);
// packet.putInt(frame.length);
// packet.put(frame);
// out.write(packet.array());
// out.flush();
//                 // long t1 = System.currentTimeMillis();
//                 // System.out.println("Send time: " + (t1 - t0) + " ms");
//             } catch (Exception e) {
//                 clients.remove(out);
//                 System.out.println("[SERVER] Removed disconnected client.");
//             }
//         }
//     }
// }