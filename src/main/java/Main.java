import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args) {
        var token = "vk1.a.JTYUtFuN9ROsTWDUzwdvyPGxY0Zo7c9aEUR-7RCFkkcSb7YZn62LLCOxA0JWiryCy9rFglVp6Gu-ZyzlJxbYj-lXmRN7KuouZuuRVpchSzz7W6OSqhLk6OIX4IGejuzuoXn9wkx9rk373gx3beOPYLXZws8DQ1g8wHeXKHhfKxBS-mtiDbSo_mUSaTQrYDGh";
        var timeWait = 500;
        var count = 35000;
            for (int i = 3526; i <= count; i++) {
                try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
                HttpGet get = new HttpGet(String.format("https://api.vk.com/method/account.ban?owner_id=%d&access_token=%s&v=5.131", i, token));
                var resp = httpClient.execute(get);
                var response = new String(resp.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println(response);
                if (response.contains("Flood control")) {
                    System.out.println("Ждем...");
                    Thread.sleep(5000);
                } else if (!response.equals("{\"response\":1}")) {
                    count++;
                }
                Thread.sleep(timeWait);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
    }
}
