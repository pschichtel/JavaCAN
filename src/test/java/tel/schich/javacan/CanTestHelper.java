package tel.schich.javacan;

import java.io.IOException;

public class CanTestHelper {
    public static void sendFrameViaUtils(String iface, CanFrame frame) throws IOException {
        StringBuilder data = new StringBuilder();
        for (byte b : frame.getPayload()) {
            data.append(String.format("%X", b));
        }
        String textframe = String.format("%X#%s", frame.getId(), data);
        Runtime.getRuntime().exec(new String[] {"cansend", iface, textframe});
    }
}
