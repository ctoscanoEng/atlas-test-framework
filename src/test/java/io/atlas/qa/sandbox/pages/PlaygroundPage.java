package io.atlas.qa.sandbox.pages;

import io.atlas.qa.core.element.Locator;
import io.atlas.qa.core.element.Waits;
import io.atlas.qa.core.page.BasePage;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The parts of a browser that page objects usually get wrong: dialogs, frames,
 * shadow roots, deferred rendering, second windows and file inputs.
 */
public final class PlaygroundPage extends BasePage {

    private static final Locator ALERT_BUTTON = Locator.of("Playground: alert trigger",
            By.cssSelector("[data-testid='trigger-alert']"));
    private static final Locator CONFIRM_BUTTON = Locator.of("Playground: confirm trigger",
            By.cssSelector("[data-testid='trigger-confirm']"));
    private static final Locator PROMPT_BUTTON = Locator.of("Playground: prompt trigger",
            By.cssSelector("[data-testid='trigger-prompt']"));
    private static final Locator DIALOG_RESULT = Locator.of("Playground: dialog outcome",
            By.cssSelector("[data-testid='dialog-result']"));

    private static final Locator FRAME = Locator.named("Playground: content frame")
            .by(By.id("content-frame"))
            .orBy(By.cssSelector("[data-testid='content-frame']"))
            .build();
    private static final Locator FRAME_BODY = Locator.of("Frame: body text",
            By.cssSelector("[data-testid='frame-body']"));
    private static final Locator FRAME_BUTTON = Locator.of("Frame: acknowledge",
            By.cssSelector("[data-testid='frame-button']"));

    private static final Locator SHADOW_HOST = Locator.of("Playground: web component",
            By.cssSelector("atlas-counter"));

    private static final Locator START_JOB = Locator.of("Playground: start job",
            By.cssSelector("[data-testid='start-job']"));
    private static final Locator JOB_STATUS = Locator.of("Playground: job status",
            By.cssSelector("[data-testid='job-status']"));
    private static final Locator JOB_RESULT = Locator.of("Playground: job result",
            By.cssSelector("[data-testid='job-result']"));

    private static final Locator NEW_TAB_LINK = Locator.of("Playground: open a second tab",
            By.cssSelector("[data-testid='open-new-tab']"));

    private static final Locator AMOUNT_HEADER = Locator.of("Playground: amount column header",
            By.cssSelector("[data-testid='sort-by-amount']"));
    private static final Locator AMOUNT_CELLS = Locator.of("Playground: amount cells",
            By.cssSelector("[data-testid='orders-body'] tr td:nth-child(3)"));

    private static final Locator FILE_INPUT = Locator.of("Playground: file input",
            By.cssSelector("[data-testid='file-input']"));
    private static final Locator FILE_NAME = Locator.of("Playground: selected file name",
            By.cssSelector("[data-testid='file-name']"));

    @Override
    protected Locator pageMarker() {
        return ALERT_BUTTON;
    }

    public static PlaygroundPage open() {
        PlaygroundPage page = new PlaygroundPage();
        page.open("/widgets.html");
        return page.waitUntilLoaded();
    }

    // ------------------------------------------------------------------ dialogs

    public String triggerAlertAndAccept() {
        $(ALERT_BUTTON).click();
        return acceptAlertAndReadText();
    }

    public PlaygroundPage triggerConfirmAndDismiss() {
        $(CONFIRM_BUTTON).click();
        dismissAlert();
        return this;
    }

    public PlaygroundPage triggerPromptAndAnswer(String answer) {
        $(PROMPT_BUTTON).click();
        Alert alert = Waits.untilAlertPresent(driver);
        alert.sendKeys(answer);
        alert.accept();
        return this;
    }

    public String dialogOutcome() {
        return $(DIALOG_RESULT).text();
    }

    // ------------------------------------------------------------------ frame

    /** Enters the frame, acts, and always returns to the main document. */
    public String acknowledgeInsideFrame() {
        switchToFrame(FRAME);
        try {
            $(FRAME_BUTTON).click();
            return $(FRAME_BODY).text();
        } finally {
            switchToMainDocument();
        }
    }

    // ------------------------------------------------------------------ shadow DOM

    public String incrementShadowCounter(int times) {
        for (int i = 0; i < times; i++) {
            inShadowRoot(SHADOW_HOST, By.cssSelector("#increment")).click();
        }
        return inShadowRoot(SHADOW_HOST, By.cssSelector("#value")).getText();
    }

    // ------------------------------------------------------------------ async

    public PlaygroundPage runBackgroundJob() {
        $(START_JOB).click();
        return this;
    }

    public String waitForJobToFinish() {
        waitUntil(() -> "done".equals($(JOB_STATUS).text()), "the background job to report 'done'");
        return $(JOB_RESULT).text();
    }

    // ------------------------------------------------------------------ windows

    /** Opens the second tab, reads it and leaves the driver on the original one. */
    public String readSecondTabHeading() {
        String original = driver.getWindowHandle();
        List<String> before = new ArrayList<>(driver.getWindowHandles());

        $(NEW_TAB_LINK).click();
        Waits.untilNumberOfWindowsIs(driver, before.size() + 1);

        String opened = driver.getWindowHandles().stream()
                .filter(handle -> !before.contains(handle))
                .findFirst()
                .orElseThrow();
        try {
            driver.switchTo().window(opened);
            return $(Locator.of("Second tab: heading", By.cssSelector("[data-testid='frame-heading']"))).text();
        } finally {
            driver.close();
            driver.switchTo().window(original);
        }
    }

    // ------------------------------------------------------------------ table

    public List<String> sortByAmountAndRead() {
        $(AMOUNT_HEADER).click();
        return $(AMOUNT_CELLS).allTexts();
    }

    // ------------------------------------------------------------------ upload

    public String uploadFile(String absolutePath) {
        // The input is styled but present: no OS dialog is ever involved.
        WebElement input = resolver.resolve(FILE_INPUT);
        input.sendKeys(absolutePath);
        waitUntil(() -> !$(FILE_NAME).text().equals("no file selected"), "the file name to be displayed");
        return $(FILE_NAME).text();
    }

    public boolean jobResultIsHidden() {
        return $(JOB_RESULT).isAbsent(Duration.ofMillis(500)) || !$(JOB_RESULT).isVisibleWithin(Duration.ofMillis(500));
    }
}
