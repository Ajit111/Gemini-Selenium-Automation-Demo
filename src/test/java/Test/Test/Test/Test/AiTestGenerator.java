package Test.Test.Test.Test;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.testng.annotations.*;
import org.openqa.selenium.support.ui.*;

import okhttp3.*;
import com.google.gson.*;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

public class AiTestGenerator {

    WebDriver driver;
    WebDriverWait wait;

    ExtentReports extent;
    ExtentTest test;

    String API_KEY = "Enter Api Key";

    // ⭐ Updated wait from 5 sec → 2 sec
    private void sleep2() {
        try { Thread.sleep(2000); } catch (Exception ignored) {}
    }

    @BeforeClass
    public void setUp() {

        // ----------------- EXTENT REPORT SETUP -----------------
        extent = new ExtentReports();
        ExtentSparkReporter spark = new ExtentSparkReporter("AI-Selenium-Report.html");
        extent.attachReporter(spark);
        test = extent.createTest("AI Based Selenium Execution");

        // ----------------- SELENIUM SETUP ----------------------
        WebDriverManager.chromedriver().setup();
        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("--remote-allow-origins=*");
        opt.addArguments("--incognito");

        driver = new ChromeDriver(opt);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.manage().window().maximize();

        test.info("Browser launched successfully.");
    }

    // ---------------- GEMINI CALL ------------------
    private JsonArray callGemini(String prompt) throws Exception {

        test.info("Calling Gemini AI Model...");

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
        content.addProperty("role", "user");
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject payload = new JsonObject();
        payload.add("contents", contents);

        RequestBody body = RequestBody.create(
                payload.toString(),
                MediaType.parse("application/json")
        );

        Request req = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash-lite:generateContent?key=" + API_KEY)
                .post(body)
                .build();

        Response resp = client.newCall(req).execute();
        String raw = resp.body().string();

        test.info("Gemini Raw Response Received.");
        System.out.println(raw);

        JsonObject json = JsonParser.parseString(raw).getAsJsonObject();

        if (json.has("error")) {
            test.fail("Gemini Error: " + json.get("error").toString());
            throw new RuntimeException("Gemini Error: " + json.get("error").getAsJsonObject().toString());
        }

        String aiText = json.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();

        aiText = aiText.replace("```json", "").replace("```", "").trim();

        test.pass("Gemini provided steps successfully.");

        return convert(aiText);
    }

    // ---------------- CONVERT AI TEXT → JSON ------------------
    private JsonArray convert(String text) {

        test.info("Converting AI Steps...");

        JsonArray steps = new JsonArray();

        for (String line : text.split("\n")) {

            if (!line.contains(":")) continue;

            String[] arr = line.split(":", 2);
            String action = arr[0].trim();
            String data = arr[1].trim();

            JsonObject o = new JsonObject();

            switch (action) {

                case "open":
                    o.addProperty("action", "open");
                    o.addProperty("value", data);
                    test.info("AI Step: Open URL → " + data);
                    break;

                case "type":
                    String[] p = data.split(" ", 2);
                    if (p.length < 2) continue;
                    o.addProperty("action", "type");
                    o.addProperty("locator", p[0]);
                    o.addProperty("value", p[1]);
                    test.info("AI Step: Type → " + p[0] + " = " + p[1]);
                    break;

                case "click":
                    o.addProperty("action", "click");
                    o.addProperty("locator", data);
                    test.info("AI Step: Click → " + data);
                    break;
            }

            steps.add(o);
        }

        return steps;
    }

    // ---------------- SMART LOCATOR ------------------
    private WebElement findElementSmart(String locator) {

        sleep2(); // ⭐ updated here

        test.info("Finding Element → " + locator);

        if (locator.equals("id=shopping_cart_link")) {
            locator = "class=shopping_cart_link";
        }

        try {
            if (locator.startsWith("//"))
                return wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(locator)));

            if (locator.startsWith("id="))
                return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(locator.substring(3))));

            if (locator.startsWith("class="))
                return wait.until(ExpectedConditions.visibilityOfElementLocated(By.className(locator.substring(6))));

            if (locator.startsWith("name="))
                return wait.until(ExpectedConditions.visibilityOfElementLocated(By.name(locator.substring(5))));

            if (locator.startsWith("css="))
                return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(locator.substring(4))));

        } catch (Exception ex) {
            takeScreenshot("ElementNotFound");
            test.fail("❌ Element NOT found → " + locator);
            throw new RuntimeException("Element not found: " + locator);
        }

        return null;
    }

    // ---------------- EXECUTE AI STEPS ------------------
    private void execute(JsonArray steps) throws Exception {

        for (JsonElement e : steps) {

            JsonObject s = e.getAsJsonObject();
            String action = s.get("action").getAsString();

            switch (action) {

                case "open":
                    sleep2();
                    driver.get(s.get("value").getAsString());
                    test.pass("Opened URL: " + s.get("value").getAsString());
                    break;

                case "type":
                    sleep2();
                    findElementSmart(s.get("locator").getAsString())
                            .sendKeys(s.get("value").getAsString());
                    test.pass("Typed into: " + s.get("locator").getAsString());
                    break;

                case "click":
                    sleep2();
                    findElementSmart(s.get("locator").getAsString()).click();
                    test.pass("Clicked on: " + s.get("locator").getAsString());
                    break;
            }
        }
    }

    // ---------------- SCREENSHOT METHOD ------------------
    private String takeScreenshot(String name) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String path = "screenshots/" + name + "_" + System.currentTimeMillis() + ".png";

            Files.createDirectories(new File("screenshots").toPath());
            File dest = new File(path);
            Files.copy(src.toPath(), dest.toPath());

            test.addScreenCaptureFromPath(path);
            return path;

        } catch (Exception ex) {
            test.warning("Screenshot failed: " + ex.getMessage());
            return null;
        }
    }

    // ---------------- MAIN TEST ------------------
    @Test
    public void aiSeleniumTest() throws Exception {

        String prompt =
                "Generate Selenium steps in EXACT format below:\n" +
                        "open: https://www.saucedemo.com/\n" +
                        "type: id=user-name standard_user\n" +
                        "type: id=password secret_sauce\n" +
                        "click: id=login-button\n" +
                        "click: id=add-to-cart-sauce-labs-backpack\n" +
                        "click: id=shopping_cart_link\n" +
                        "click: id=checkout\n" +
                        "type: id=first-name John\n" +
                        "type: id=last-name Doe\n" +
                        "type: id=postal-code 12345\n" +
                        "click: id=continue\n" +
                        "click: id=finish\n" +
                        "NO markdown, NO explanation, ONLY raw steps.";

        JsonArray steps = callGemini(prompt);

        test.info("Final AI Steps Received.");
        System.out.println(steps);

        execute(steps);

        test.pass("AI Driven Selenium Test Executed Successfully!");
    }

    @AfterClass
    public void tearDown() {

        test.info("Closing Browser...");
        sleep2();
        driver.quit();

        extent.flush();
    }
}
