import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

public class Main {

    public static final long TIMEOUT = 500;
    public static final long TIMEOUT_FLOOD_CONTROL = 5000;
    public static final StringBuilder sb = new StringBuilder();
    public static final File dir = new File("C:\\Logs");
    public static final File logs = new File("C:\\Logs\\logBlock.log");

    public static void main(String[] args) {
        try {
            dir.mkdir();
            logs.createNewFile();
        } catch (IOException e) {}
        var jFrame = new JFrame("Блокировка пользователей");
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setSize(600, 600);
        var tokenMessage = new JLabel("Введите api токен");
        tokenMessage.setBounds(50, 100, 150, 50);
        var token = new JTextField();
        token.setBounds(250, 100, 300, 50);
        var urlMessage = new JLabel("Введите ссылку на пост");
        urlMessage.setBounds(50, 300, 150, 50);
        var url = new JTextField();
        url.setBounds(250, 300, 300, 50);
        var button = new JButton("Кинуть в чс");
        button.setBounds(150, 500, 200, 80);
        button.addActionListener(x -> {
        var tokenString = token.getText();
        var urlString = url.getText();
        var ids = urlString.split("wall");
        var idArray = ids[1].split("_");
        try {
            jFrame.setVisible(false);
            block(tokenString, getIds(tokenString, Integer.parseInt(idArray[0]), Integer.parseInt(idArray[1])));
            write();
            jFrame.setVisible(true);
        } catch (IOException | InterruptedException e) {
            var error = new JLabel(e.getMessage());
            error.setBounds(50, 50, 100, 100);
            var jFrameError = new JFrame("Ошибка");
            jFrameError.setSize(200, 200);
            jFrameError.add(error);
            jFrame.setLayout(null);
            jFrame.setVisible(true);
            sb.append(LocalDateTime.now()).append(" ").append(e);
        }
        });
        jFrame.add(tokenMessage);
        jFrame.add(token);
        jFrame.add(urlMessage);
        jFrame.add(url);
        jFrame.add(token);
        jFrame.add(button);
        jFrame.setLayout(null);
        jFrame.setVisible(true);
    }

    public static void block(String token, List<Integer> idList) throws IOException, InterruptedException {
        for (Integer id : idList) {
            try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
                HttpGet get = new HttpGet(String.format("https://api.vk.com/method/account.ban?owner_id=%d&access_token=%s&v=5.131", id, token));
                var resp = httpClient.execute(get);
                var response = new String(resp.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                sb.append(LocalDateTime.now()).append(" ").append(response).append("\n");
                if (response.contains("Flood control")) {
                    Thread.sleep(TIMEOUT_FLOOD_CONTROL);
                }
                Thread.sleep(TIMEOUT);
            }
        }
    }

    public static List<Integer> getIds(String token, int idGroup, int idPost) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            var httpGet = new HttpGet(String.format("https://api.vk.com/method/likes.getList?type=post&owner_id=%d&item_id=%d&extended=0&count=1000&offset=0&access_token=%s&v=5.131", idGroup, idPost, token));
            var response = httpClient.execute(httpGet);
            var json = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            var idArray = JsonParser.parseString(json).getAsJsonObject().get("response").getAsJsonObject().get("items");
            var gson = new Gson();
            return gson.fromJson(idArray, new TypeToken<List<Integer>>() {}.getType());
        }
    }

    public static void write() {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(logs))) {
            bufferedWriter.write(sb.toString());
        } catch (IOException e) {}
    }
}
