package br.miniguard;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Scanner;

/**
 * Mini-Guard – a minimal night-time motion alarm.
 * <p>
 * Captures frames from the default webcam, detects inter-frame differences,
 * and sends a Telegram alert whenever motion above a pixel threshold is found.
 */
public final class Main {

    private static final String PROPS_FILE          = "telegram.properties";
    private static final int    MOTION_THRESHOLD_PX = 5_000; // white pixels
    private static final int    FRAME_SLEEP_MS      = 50;    // ~20 FPS
    private static final Size   BLUR_KERNEL         = new Size(21, 21);
    private static final Properties CONFIG          = loadConfig();
    private static final HttpClient HTTP            = HttpClient.newHttpClient();

    static {
        OpenCV.loadShared();
    }

    public static void main(final String[] args) throws IOException, InterruptedException {
        final VideoCapture cam = new VideoCapture(0);
        if (!cam.isOpened()) {
            System.err.println("Could not open webcam.");
            return;
        }

        // Ensure resources are released on Ctrl-C
        Runtime.getRuntime().addShutdownHook(new Thread(cam::release));

        final Mat prev = new Mat();
        final Mat curr = new Mat();
        final Mat diff = new Mat();

        cam.read(prev);
        preprocess(prev);

        System.out.println("Mini-Guard armed. Waiting for motion … (Ctrl+C to exit)");

        while (true) {
            Thread.sleep(FRAME_SLEEP_MS);

            cam.read(curr);
            if (curr.empty()) continue;

            preprocess(curr);

            Core.absdiff(prev, curr, diff);
            Imgproc.threshold(diff, diff, 25, 255, Imgproc.THRESH_BINARY);

            final int whitePixels = Core.countNonZero(diff);
            if (whitePixels > MOTION_THRESHOLD_PX) {
                System.out.printf("⚠️  Motion detected! (%d px)%n", whitePixels);
                java.awt.Toolkit.getDefaultToolkit().beep();
                sendTelegram("⚠️ Motion detected! (" + whitePixels + " px)");
            }
            curr.copyTo(prev);
        }
    }

    private static void preprocess(final Mat frame) {
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(frame, frame, BLUR_KERNEL, 0);
    }

    private static void sendTelegram(final String text) throws IOException, InterruptedException {
        final String botToken = CONFIG.getProperty("token");
        final String chatId   = CONFIG.getProperty("chatId");

        final String url = "https://api.telegram.org/bot" + botToken
                + "/sendMessage?chat_id=" + chatId
                + "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

        HTTP.send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.discarding()
        );
    }

    /** Loads an existing telegram.properties file or creates one interactively. */
    private static Properties loadConfig() {
        final Properties props = new Properties();
        final File file = new File(PROPS_FILE);

        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
                return props;
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to read " + PROPS_FILE, ex);
            }
        }

        try (Scanner sc = new Scanner(System.in)) {
            System.out.print("Enter TELEGRAM BOT TOKEN: ");
            props.setProperty("token", sc.nextLine().trim());

            System.out.print("Enter TELEGRAM CHAT ID: ");
            props.setProperty("chatId", sc.nextLine().trim());
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "Telegram credentials for Mini-Guard");
            System.out.println(PROPS_FILE + " created.");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write " + PROPS_FILE, ex);
        }
        return props;
    }

    private Main() { /* utility class */ }
}
