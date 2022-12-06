import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {

    public static final long TIMEOUT = 500;
    public static final long TIMEOUT_FIRST = 20000;
    public static final long TIMEOUT_FLOOD_CONTROL = 5000;
    public static final StringBuilder sb = new StringBuilder();
    public static final File dir = new File("C:\\Logs");
    public static final File logs = new File("C:\\Logs\\logBlock.log");
    public static List<Integer> friends;

    public static void main(String[] args) {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        try {
            dir.mkdir();
            logs.createNewFile();
        } catch (IOException ignored) {}
        var jFrame = new JFrame("Блокировка пользователей");
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setSize(600, 600);
        var tokenMessage = new JLabel("Введите api токен");
        tokenMessage.setBounds(50, 100, 150, 50);
        var token = new JTextField();
        token.setBounds(250, 100, 300, 50);
        var idMessage = new JLabel("Введите id пользователя");
        idMessage.setBounds(50, 300, 150, 50);
        var id = new JTextField();
        id.setBounds(250, 300, 300, 50);
        var button = new JButton("Кинуть в чс");
        button.setBounds(150, 500, 200, 80);
        button.addActionListener(x -> {
        var tokenString = token.getText();
        var inputId = Integer.parseInt(id.getText());
        try {
            jFrame.setVisible(false);
            friends = get(0, tokenString);
            Queue<Integer> queue = new ConcurrentLinkedQueue<>();
            queue.add(inputId);
            new Thread(() -> {
                try {
                    add(get(inputId, tokenString), queue, tokenString, 1);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            Thread.sleep(TIMEOUT_FIRST);
            block(tokenString, queue);
            write();
            System.out.println(queue.size());
            jFrame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            var error = new JLabel(e.getMessage());
            error.setBounds(200, 100, 200, 100);
            var jFrameError = new JFrame("Ошибка");
            jFrameError.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            jFrameError.setSize(600, 400);
            jFrameError.add(error);
            jFrameError.setLayout(null);
            jFrameError.setVisible(true);
            sb.append(LocalDateTime.now()).append(" ").append(e);
        }
        });
        jFrame.add(tokenMessage);
        jFrame.add(token);
        jFrame.add(idMessage);
        jFrame.add(id);
        jFrame.add(token);
        jFrame.add(button);
        jFrame.setLayout(null);
        jFrame.setVisible(true);
    }

    public static void block(String token, Queue<Integer> idQueue) throws IOException, InterruptedException {
        int floodControl = 0;
        boolean first = true;
        final var gson = new Gson();
        while (!idQueue.isEmpty()) {
            try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
                var id = idQueue.poll();
                HttpGet get = new HttpGet(String.format("https://api.vk.com/method/account.ban?owner_id=%d&access_token=%s&v=5.131", id, token));
                var resp = httpClient.execute(get);
                var response = new String(resp.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                sb.append(LocalDateTime.now()).append(" ").append(response).append("\n");
                if (response.contains("Flood control")) {
                    Thread.sleep(TIMEOUT_FLOOD_CONTROL);
                    floodControl++;
                    if (floodControl > 12) throw new RuntimeException("Flood Control");
                } else {
                    floodControl = 0;
                }
                Thread.sleep(TIMEOUT);
            }
        }
    }

    public static void write() {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(logs))) {
            bufferedWriter.write(sb.toString());
        } catch (IOException ignored) {}
    }

    public static int parseMonth(String s) {
        if (s.contains("янв")) return 1;
        if (s.contains("фев")) return 2;
        if (s.contains("мар")) return 3;
        if (s.contains("апр")) return 4;
        if (s.contains("мая")) return 5;
        if (s.contains("июн")) return 6;
        if (s.contains("июл")) return 7;
        if (s.contains("авг")) return 8;
        if (s.contains("сен")) return 9;
        if (s.contains("окт")) return 10;
        if (s.contains("ноя")) return 11;
        if (s.contains("дек")) return 12;
        throw new IllegalArgumentException("Месяц " + s + " не найден");
    }

    public static void add(List<Integer> friends, Queue<Integer> idQueue, String token, int lvl) {
        for (int friend : friends) {
            try {
                if (Main.friends.contains(friend)) continue;
                Document doc = Jsoup.connect(String.format("https://vk.com/id%d", friend))
                        .userAgent("Chrome/4.0.249.0 Safari/532.5")
                        .get();
                Elements elements = doc.select("span.pp_last_activity_text");
                if (elements.isEmpty()) continue;
                var date = elements.get(0).text();
                if (date.isEmpty()) continue;
                if (date.contains("Online") || date.contains("минут") || date.contains("недавно") || date.contains("час") || date.contains("вчера") || date.contains("сегодня") || date.contains("только что")) {
                    idQueue.add(friend);
                    if (lvl < 4) new Thread(() -> {
                        try {
                            add(get(friend, token), idQueue, token, lvl + 1);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).start();
                } else {
                    var now = LocalDate.now();
                    var max = now.minusDays(7);
                    var arrayString = date.split(" ");
                    var yearNow = Year.now();
                    var year = LocalDate.now().isAfter(LocalDate.of(yearNow.getValue(), 1, 6)) ? yearNow : yearNow.minusYears(1);
                    if (max.isBefore(LocalDate.of(year.getValue(), parseMonth(arrayString[2]), Integer.parseInt(arrayString[1])))) idQueue.add(friend);
                }
            } catch (Exception ignored) {}
        }
    }

    public static List<Integer> get(int id, String token) throws IOException {
        Gson gson = new Gson();
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet friendsGet = id != 0 ? new HttpGet(String.format("https://api.vk.com/method/friends.get?user_id=%d&access_token=%s&v=5.131", id, token))
                    : new HttpGet(String.format("https://api.vk.com/method/friends.get?access_token=%s&v=5.131", token));
            var friendsResponse = httpClient.execute(friendsGet);
            var friendsJson = new String(friendsResponse.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            var idArray = JsonParser.parseString(friendsJson).getAsJsonObject().get("response").getAsJsonObject().get("items");
            List<Integer> friends = gson.fromJson(idArray, new TypeToken<List<Integer>>() {}.getType());
            return friends;
        }
    }
}
