package io.atlas.qa.sandbox.tests;

import io.atlas.qa.base.BaseWebTest;
import io.atlas.qa.sandbox.pages.PlaygroundPage;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The browser surfaces that break naive automation.
 *
 * <p>Native dialogs, nested browsing contexts, shadow roots, deferred rendering,
 * a second window and a file input: each of these is where a "click and hope"
 * suite starts producing intermittent failures, and each is handled here by an
 * explicit, deterministic mechanism.
 */
public class BrowserCapabilityTests extends BaseWebTest {

    @Test(groups = {"regression", "widgets"},
            description = "Native alert, confirm and prompt dialogs are driven and their outcome asserted")
    public void nativeDialogsAreHandled() {
        PlaygroundPage playground = PlaygroundPage.open();

        assertThat(playground.triggerAlertAndAccept()).isEqualTo("Inventory synchronisation finished");
        assertThat(playground.dialogOutcome()).isEqualTo("alert acknowledged");

        assertThat(playground.triggerConfirmAndDismiss().dialogOutcome()).isEqualTo("dismissed");

        assertThat(playground.triggerPromptAndAnswer("ATL-2024-000119").dialogOutcome())
                .isEqualTo("reopening ATL-2024-000119");
    }

    @Test(groups = {"regression", "widgets"},
            description = "The driver enters an iframe, acts inside it and returns to the main document")
    public void iframeContentIsReachable() {
        PlaygroundPage playground = PlaygroundPage.open();

        assertThat(playground.acknowledgeInsideFrame()).isEqualTo("Acknowledged by the operator");

        // Proving we came back: an element of the top document must be usable again.
        assertThat(playground.dialogOutcome()).isEqualTo("No dialog handled yet");
    }

    @Test(groups = {"regression", "widgets"},
            description = "A web component is driven through its shadow root, where XPath cannot reach")
    public void shadowDomComponentIsDriven() {
        assertThat(PlaygroundPage.open().incrementShadowCounter(3)).isEqualTo("3");
    }

    @Test(groups = {"regression", "widgets"},
            description = "A two second background job is awaited by condition, never by sleeping")
    public void deferredRenderingIsAwaited() {
        String outcome = PlaygroundPage.open().runBackgroundJob().waitForJobToFinish();

        assertThat(outcome).isEqualTo("Job completed successfully");
    }

    @Test(groups = {"regression", "widgets"},
            description = "A second tab is read and closed, leaving the driver on the original window")
    public void secondWindowIsReadAndClosed() {
        PlaygroundPage playground = PlaygroundPage.open();

        assertThat(playground.readSecondTabHeading()).isEqualTo("Release 3.4 — inventory service");
        assertThat(driver().getWindowHandles()).hasSize(1);
        assertThat(playground.isLoaded()).as("the original tab must still be usable").isTrue();
    }

    @Test(groups = {"regression", "widgets"},
            description = "Sorting a table by a numeric column orders it numerically, not alphabetically")
    public void tableSortsNumerically() {
        List<String> amounts = PlaygroundPage.open().sortByAmountAndRead();

        assertThat(amounts).containsExactly("64.90", "98.00", "412.50", "1204.90");
    }

    @Test(groups = {"regression", "widgets"},
            description = "A file is uploaded by writing its path into the input, with no OS dialog")
    public void fileIsUploaded() throws IOException {
        Path artefact = Files.createDirectories(Path.of("target/atlas-report/fixtures"))
                .resolve("delivery-note.csv");
        Files.writeString(artefact, "reference,quantity%nATL-001,2%n".formatted());

        String selected = PlaygroundPage.open().uploadFile(artefact.toAbsolutePath().toString());

        assertThat(selected).isEqualTo("delivery-note.csv");
    }
}
