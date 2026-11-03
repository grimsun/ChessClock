import ClockCore
import SwiftUI
import UIKit

/// A palette's colors, each resolving to its light or dark variant with the screen.
struct Palette {
    let primary, onPrimary, inactive, onInactive, win, onWin, loss, onLoss, background, onBackground: Color
    /// The time drawn inside a ring, on the screen background.
    var ringText: Color { onInactive }

    /// The player on move against the one waiting, as a palette's swatch shows them.
    var swatch: (active: Color, waiting: Color) { (primary, inactive) }

    private init(_ variants: PaletteVariants) {
        let (light, dark) = (variants.light, variants.dark)
        primary = .dynamic(light: light.primary, dark: dark.primary)
        onPrimary = .dynamic(light: light.onPrimary, dark: dark.onPrimary)
        inactive = .dynamic(light: light.inactive, dark: dark.inactive)
        onInactive = .dynamic(light: light.onInactive, dark: dark.onInactive)
        win = .dynamic(light: light.win, dark: dark.win)
        onWin = .dynamic(light: light.onWin, dark: dark.onWin)
        loss = .dynamic(light: light.loss, dark: dark.loss)
        onLoss = .dynamic(light: light.onLoss, dark: dark.onLoss)
        background = .dynamic(light: light.background, dark: dark.background)
        onBackground = .dynamic(light: light.onBackground, dark: dark.onBackground)
    }

    private static let walnut = Palette(ColorPalette.walnut.colors)
    private static let forest = Palette(ColorPalette.forest.colors)
    private static let ocean = Palette(ColorPalette.ocean.colors)
    private static let graphite = Palette(ColorPalette.graphite.colors)

    static func of(_ palette: ColorPalette) -> Palette {
        switch palette {
        case .walnut: walnut
        case .forest: forest
        case .ocean: ocean
        case .graphite: graphite
        }
    }
}

extension EnvironmentValues {
    /// The colors of the palette chosen in settings.
    @Entry var palette = Palette.of(.walnut)
}

extension ThemeMode {
    /// The color scheme to force, or nil to follow the system.
    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}

private extension Color {
    /// SwiftUI resolves colors on its render thread too, so the provider must not be tied to the main
    /// actor that this target's code defaults to: resolving it off the main thread would crash.
    nonisolated static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { @Sendable traits in
            traits.userInterfaceStyle == .dark ? UIColor(rgb: dark) : UIColor(rgb: light)
        })
    }
}

private extension UIColor {
    nonisolated convenience init(rgb: UInt32) {
        self.init(
            red: CGFloat(rgb >> 16 & 0xFF) / 255,
            green: CGFloat(rgb >> 8 & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1,
        )
    }
}
