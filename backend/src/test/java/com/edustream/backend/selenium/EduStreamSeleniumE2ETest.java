package com.edustream.backend.selenium;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EduStreamSeleniumE2ETest {

    private WebDriver driver;
    private WebDriverWait wait;
    private final String baseUrl = System.getProperty("baseUrl", "http://localhost:3000");

    private final String testEmail = "selenium.user." + UUID.randomUUID().toString().substring(0, 6) + "@example.ma";

    @BeforeAll
    void setupDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--window-size=1440,900");

        try {
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Chrome/ChromeDriver non disponible dans cet environnement : " + e.getMessage());
        }
    }

    @AfterAll
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Selenium 1: Vérification du catalogue et de la devise DH")
    void testCatalogueAndPricing() {
        driver.get(baseUrl);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("article")));

        List<WebElement> articles = driver.findElements(By.tagName("article"));
        assertTrue(articles.size() >= 3, "Le catalogue doit contenir au moins 3 formations");

        for (WebElement article : articles) {
            assertTrue(article.getText().contains("DH"), "Chaque cours doit afficher son prix en DH");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Selenium 2: Inscription étudiant et session utilisateur")
    void testStudentRegistration() {
        driver.get(baseUrl);

        WebElement loginBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(), 'Se connecter')]")));
        loginBtn.click();

        WebElement registerTab = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(), 'Créer un compte')]")));
        registerTab.click();

        driver.findElement(By.xpath("//input[@placeholder='Jean']")).sendKeys("Karim");
        driver.findElement(By.xpath("//input[@placeholder='Dupont']")).sendKeys("Bennani");
        driver.findElement(By.xpath("//input[@placeholder='nom@exemple.ma']")).sendKeys(testEmail);
        driver.findElement(By.xpath("//input[@placeholder='••••••••']")).sendKeys("Password123!");

        driver.findElement(By.xpath("//button[@type='submit' and contains(text(), \"S'inscrire\")]")).click();

        WebElement dashboardBtn = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(), 'Tableau de bord')]")));
        assertTrue(dashboardBtn.isDisplayed());
    }

    @Test
    @Order(3)
    @DisplayName("Selenium 3: Parcours d'inscription et paiement bancaire")
    void testEnrollmentAndPayment() {
        driver.get(baseUrl + "/courses/react");

        WebElement enrollBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(), \"S'inscrire maintenant\")]")));
        enrollBtn.click();

        wait.until(ExpectedConditions.urlContains("/enroll/react"));
        WebElement phoneInput = driver.findElement(By.name("phone"));
        phoneInput.clear();
        phoneInput.sendKeys("+212 6 00 00 00 00");

        WebElement terms = driver.findElement(By.name("agreeTerms"));
        if (!terms.isSelected()) terms.click();

        driver.findElement(By.xpath("//button[@type='submit' and contains(text(), 'Continuer')]")).click();

        WebElement confirmBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(), 'Procéder au paiement')]")));
        confirmBtn.click();

        wait.until(ExpectedConditions.urlContains("/payment/react"));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("cardNumber"))).sendKeys("4532 0123 4567 8901");
        driver.findElement(By.name("expiryDate")).sendKeys("12/28");
        driver.findElement(By.name("cvv")).sendKeys("123");
        driver.findElement(By.name("billingAddress")).sendKeys("Boulevard Massira");
        driver.findElement(By.name("billingCity")).sendKeys("Casablanca");
        driver.findElement(By.name("billingZip")).sendKeys("20000");

        driver.findElement(By.xpath("//button[@type='submit' and contains(text(), 'Payer')]")).click();

        wait.until(ExpectedConditions.urlContains("/success"));
        WebElement successTitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(), 'Inscription réussie !')]")));
        assertTrue(successTitle.isDisplayed());
    }

    @Test
    @Order(4)
    @DisplayName("Selenium 4: Accès au cours débloqué et validation d'une leçon")
    void testLearningSpaceAccess() {
        WebElement accessBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(text(), 'Accéder au cours maintenant')]")));
        accessBtn.click();

        wait.until(ExpectedConditions.urlContains("/courses/react"));

        WebElement unlockedBadge = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'Vous possédez cette formation') or contains(text(), 'Vous êtes inscrit')]")
        ));
        assertTrue(unlockedBadge.isDisplayed());

        List<WebElement> enrollButtons = driver.findElements(By.xpath("//button[contains(text(), \"S'inscrire maintenant\")]"));
        assertEquals(0, enrollButtons.size(), "Le bouton d'inscription ne doit plus exister pour un cours acheté");

        WebElement markDoneBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Marquer comme terminée')]")
        ));
        markDoneBtn.click();

        WebElement doneBadge = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'Leçon terminée !')]")
        ));
        assertTrue(doneBadge.isDisplayed());
    }

    @Test
    @Order(5)
    @DisplayName("Selenium 5: Tableau de bord étudiant (/dashboard)")
    void testStudentDashboard() {
        driver.get(baseUrl + "/dashboard");

        WebElement title = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'Mes Formations Disponibles')]")
        ));
        assertTrue(title.isDisplayed());

        WebElement reactCard = driver.findElement(By.xpath("//h3[contains(text(), 'React avancé & architecture')]"));
        assertTrue(reactCard.isDisplayed());

        WebElement acquisBadge = driver.findElement(By.xpath("//*[contains(text(), 'Acquis')]"));
        assertTrue(acquisBadge.isDisplayed());
    }

    @Test
    @Order(6)
    @DisplayName("Selenium 6: Protection contre les paiements en double")
    void testDuplicatePaymentProtection() {
        driver.get(baseUrl + "/enroll/react");

        WebElement alreadyEnrolled = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(), 'Vous êtes déjà inscrit à ce cours !') or contains(text(), 'déjà inscrit')]")
        ));
        assertTrue(alreadyEnrolled.isDisplayed());

        WebElement directAccess = driver.findElement(By.xpath("//*[contains(text(), 'Accéder au cours maintenant')]"));
        assertTrue(directAccess.isDisplayed());
    }
}
