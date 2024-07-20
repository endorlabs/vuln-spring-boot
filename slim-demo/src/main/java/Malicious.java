import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Malicious {
    static {
        try {
            String cve = System.getenv("CVE") == null ? "CVE" : System.getenv("CVE");
            String filePath = cve + ".txt";
            FileWriter writer = new FileWriter(filePath);
            String asciiArt = 
                "----------------------------------------\n" +
                "  " + cve + " successfully exploited    \n" +
                "----------------------------------------\n";
            writer.write(asciiArt);
            writer.close();
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            System.out.println(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}