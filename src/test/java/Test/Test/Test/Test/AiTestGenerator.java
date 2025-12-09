package Test.Test.Test.Test;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.testng.annotations.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import okhttp3.*;
import com.google.gson.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class AiTestGenerator {

    WebDriver driver;
    WebDriverWait wait;

    String API_KEY = "AIzaSyAF2ZjZOZo8cApoK7mJZeJcYx54YDE2O2E";

    // 💠 Global sleep for ALL element actions (your requirement)
    private void sleep5() {
        try { Thread.sleep(5000); } catch (Exception ignored) {}
    }

    @BeforeClass
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("--remote-allow-origins=*");
        opt.addArguments("--incognito");
        driver = new ChromeDriver(opt);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.manage().window().maximize();
    }

    // ⭐ Gemini 2.5 Flash Lite API Call
    private JsonArray callGemini(String prompt) throws Exception {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS)
                .build();

        JsonObject textObj = new JsonObject();
        textObj.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(textObj);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject payload = new JsonObject();
        payload.add("contents", contents);

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=" + API_KEY)
                .post(body)
                .build();

        Response resp = client.newCall(request).execute();
        String jsonText = resp.body().string();

        JsonObject json = JsonParser.parseString(jsonText).getAsJsonObject();

        String aiText =
                json.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

        return convert(aiText);
    }

    // Convert plain text to JSON steps
    private JsonArray convert(String text) {
        JsonArray steps = new JsonArray();

        for (String line : text.split("\n")) {
            if (!line.contains(":")) continue;

            String[] arr = line.split(":", 2);
            String action = arr[0].trim();
            String value = arr[1].trim();

            JsonObject o = new JsonObject();

            switch (action) {

                case "open":
                    o.addProperty("action", "open");
                    o.addProperty("value", value);
                    break;

                case "type":
                    String[] p = value.split(" ", 2);
                    o.addProperty("action", "type");
                    o.addProperty("locator", p[0]);
                    o.addProperty("value", p[1]);
                    break;

                case "click":
                    o.addProperty("action", "click");
                    o.addProperty("locator", value);
                    break;
            }
            steps.add(o);
        }
        return steps;
    }

    // ⭐ ULTRA SMART LOCATOR ENGINE (auto-corrects wrong id=shopping_cart_link bug)
    private WebElement findElementSmart(String locator) {

        sleep5(); // You requested wait before EACH step

        // ⭐ Auto-correct Gemini incorrect locator for cart
        if (locator.equals("id=shopping_cart_link")) {
            locator = "class=shopping_cart_link";
        }

        // xpath
        if (locator.startsWith("//")) {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(locator)));
        }

        // id=
        if (locator.startsWith("id=")) {
            String v = locator.substring(3);
            try { return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(v))); }
            catch (Exception ignored) {}
            // fallback to className
            return wait.until(ExpectedConditions.visibilityOfElementLocated(By.className(v)));
        }

        // class=
        if (locator.startsWith("class=")) {
            String v = locator.substring(6);
            return wait.until(ExpectedConditions.visibilityOfElementLocated(By.className(v)));
        }

        // name=
        if (locator.startsWith("name=")) {
            String v = locator.substring(5);
            return wait.until(ExpectedConditions.visibilityOfElementLocated(By.name(v)));
        }

        // css=
        if (locator.startsWith("css=")) {
            String v = locator.substring(4);
            return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(v)));
        }

        // fallback attempts
        try { return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(locator))); }
        catch (Exception ignored) {}

        try { return wait.until(ExpectedConditions.visibilityOfElementLocated(By.className(locator))); }
        catch (Exception ignored) {}

        try { return wait.until(ExpectedConditions.visibilityOfElementLocated(By.name(locator))); }
        catch (Exception ignored) {}

        throw new RuntimeException("❌ Element not found: " + locator);
    }

    // ⭐ Execute AI Steps
    private void execute(JsonArray steps) throws Exception {
        for (JsonElement e : steps) {
            JsonObject s = e.getAsJsonObject();
            String action = s.get("action").getAsString();

            switch (action) {

                case "open":
                    sleep5();
                    driver.get(s.get("value").getAsString());
                    break;

                case "type":
                    sleep5();
                    findElementSmart(s.get("locator").getAsString())
                            .sendKeys(s.get("value").getAsString());
                    break;

                case "click":
                    sleep5();
                    findElementSmart(s.get("locator").getAsString()).click();
                    break;
            }
        }
    }

    // ⭐ TEST  
    @Test
    public void aiSeleniumTest() throws Exception {

        String prompt =
                "Generate Selenium steps strictly in this exact format:\n" +
                "open: https://www.saucedemo.com/\n" +
                "type: id=user-name standard_user\n" +
                "type: id=password secret_sauce\n" +
                "click: id=login-button\n" +
                "click: id=add-to-cart-sauce-labs-backpack\n" +
                "click: id=shopping_cart_link\n" +   // Gemini will write wrong, we auto-correct
                "click: id=checkout\n" +
                "type: id=first-name John\n" +
                "type: id=last-name Doe\n" +
                "type: id=postal-code 12345\n" +
                "click: id=continue\n" +
                "click: id=finish";

        JsonArray steps = callGemini(prompt);

        System.out.println("AI Steps:");
        System.out.println(steps);

        execute(steps);
    }

    @AfterClass
    public void tearDown() throws Exception {
        Thread.sleep(3000);
        driver.quit();
    }
}
