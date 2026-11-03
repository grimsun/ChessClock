import Foundation
import Testing
@testable import ClockCore

struct PaletteContrastTests {
    @Test(arguments: ColorPalette.allCases)
    func everyTextColorMeetsWcagAaOnItsBackground(palette: ColorPalette) {
        for (mode, colors) in [("light", palette.colors.light), ("dark", palette.colors.dark)] {
            let pairs: KeyValuePairs = [
                "onPrimary": (colors.onPrimary, colors.primary),
                "onInactive": (colors.onInactive, colors.inactive),
                "onWin": (colors.onWin, colors.win),
                "onLoss": (colors.onLoss, colors.loss),
                "onBackground": (colors.onBackground, colors.background),
                "ring text": (colors.ringText, colors.background),
            ]
            for (name, pair) in pairs {
                let ratio = contrast(pair.0, pair.1)
                #expect(ratio >= 4.5, "\(palette) \(mode) \(name) contrast is \(ratio)")
            }
        }
    }

    private func contrast(_ a: UInt32, _ b: UInt32) -> Double {
        let (la, lb) = (luminance(a), luminance(b))
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /// WCAG relative luminance of an sRGB color.
    private func luminance(_ rgb: UInt32) -> Double {
        func linear(_ channel: UInt32) -> Double {
            let c = Double(channel & 0xFF) / 255
            return c <= 0.04045 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * linear(rgb >> 16) + 0.7152 * linear(rgb >> 8) + 0.0722 * linear(rgb)
    }
}
