import XCTest

/// Plays the app as players do. Each test passes its settings in the launch arguments, so it starts
/// from known settings whatever an earlier run saved.
@MainActor
final class ClockScreenUITests: XCTestCase {
    override func setUp() {
        continueAfterFailure = false
    }

    func testTouchingAFaceStartsTheOpponentsClock() {
        let app = launch()

        app.buttons["face.second"].tap()

        XCTAssertEqual(app.buttons["face.first"].value as? String, "On move")
        XCTAssertTrue(app.buttons["Pause"].exists)
    }

    func testEndingATurnPassesTheMove() {
        let app = launch()

        app.buttons["face.second"].tap()
        app.buttons["face.first"].tap()

        XCTAssertEqual(app.buttons["face.second"].value as? String, "On move")
        XCTAssertEqual(app.buttons["face.first"].value as? String, "")
    }

    func testSettingsHideDuringPlayAndResetRestoresTheTime() {
        let app = launch()
        XCTAssertTrue(app.buttons["Settings"].exists)

        app.buttons["face.second"].tap()
        XCTAssertTrue(app.buttons["Settings"].waitForNonExistence(timeout: 2))

        app.buttons["Pause"].tap()
        XCTAssertTrue(app.buttons["Settings"].waitForExistence(timeout: 2))
        app.buttons["Reset"].tap()
        XCTAssertEqual(app.buttons["face.first"].label, "05:00")
        XCTAssertTrue(app.buttons["Start"].exists)
    }

    func testAPresetSetsTheTime() {
        let app = launch()

        app.buttons["Settings"].tap()
        app.buttons["3 minutes, plus 2 seconds per move"].tap()
        app.buttons["Done"].tap()

        XCTAssertEqual(app.buttons["face.first"].label, "03:00")
        XCTAssertEqual(app.buttons["face.second"].label, "03:00")
    }

    func testTheTimeControlButtonOffersPresets() {
        let app = launch()

        app.buttons["Time control, 5+3"].tap()
        app.buttons["3 + 2"].tap()

        XCTAssertEqual(app.buttons["face.first"].label, "03:00")
        XCTAssertTrue(app.buttons["Time control, 3+2"].exists)

        app.buttons["face.second"].tap()
        XCTAssertTrue(app.buttons["Time control, 3+2"].waitForNonExistence(timeout: 2))
    }

    func testPresetsAreSpokenInTheWordsOfTheIncrementMode() {
        let app = launch(#"{"incrementMode":"delay"}"#)

        app.buttons["Settings"].tap()

        XCTAssertTrue(app.buttons["1 minute, 0 seconds delay per move"].exists)
    }

    func testADelayIsCountedDownOnTheFaceOnMove() {
        let app = launch(#"{"incrementMode":"delay","incrementSeconds":5}"#)

        app.buttons["face.second"].tap()

        let value = app.buttons["face.first"].value as? String ?? ""
        XCTAssertTrue(value.hasPrefix("On move, Delay "), value)
    }

    private func launch(_ settings: String = "{}") -> XCUIApplication {
        let app = XCUIApplication()
        // The arguments domain wins over saved defaults; in its plist syntax, <hex> is data.
        let hex = Data(settings.utf8).map { String(format: "%02x", $0) }.joined()
        app.launchArguments = ["-settings", "<\(hex)>"]
        app.launch()
        return app
    }
}
