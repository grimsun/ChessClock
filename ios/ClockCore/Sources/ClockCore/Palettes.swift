/// The apps' color palettes as 0xRRGGBB, the same values as the Android app's `Theme.kt`, kept in
/// ClockCore so their contrast can be tested without a simulator.
public struct PaletteColors: Equatable, Sendable {
    public let primary: UInt32
    public let onPrimary: UInt32
    public let inactive: UInt32
    public let onInactive: UInt32
    public let win: UInt32
    public let onWin: UInt32
    public let loss: UInt32
    public let onLoss: UInt32
    public let background: UInt32
    public let onBackground: UInt32

    /// The time drawn inside a ring, on the screen background.
    public var ringText: UInt32 { onInactive }
}

/// A palette's colors for light and for dark.
public struct PaletteVariants: Equatable, Sendable {
    public let light: PaletteColors
    public let dark: PaletteColors
}

extension ColorPalette {
    public var colors: PaletteVariants {
        switch self {
        case .walnut: .walnut
        case .forest: .forest
        case .ocean: .ocean
        case .graphite: .graphite
        }
    }
}

extension PaletteVariants {
    static let walnut = PaletteVariants(
        light: PaletteColors(
            primary: 0x924C00, onPrimary: 0xFFFFFF,
            inactive: 0xE4BFA7, onInactive: 0x4E2600,
            win: 0x52643F, onWin: 0xFFFFFF,
            loss: 0xBA1A1A, onLoss: 0xFFFFFF,
            background: 0xFFFFFF, onBackground: 0x221A14
        ),
        dark: PaletteColors(
            primary: 0xFFB77C, onPrimary: 0x4E2600,
            inactive: 0x53443A, onInactive: 0xFFDCC5,
            win: 0xB8CE9D, onWin: 0x253514,
            loss: 0xFFB4AB, onLoss: 0x690005,
            background: 0x19120D, onBackground: 0xF0DFD6
        )
    )

    // Chessboard green; the finished winner takes teal so it doesn't read as just "on move".
    static let forest = PaletteVariants(
        light: PaletteColors(
            primary: 0x3B6939, onPrimary: 0xFFFFFF,
            inactive: 0xD5E8CF, onInactive: 0x102010,
            win: 0x00696E, onWin: 0xFFFFFF,
            loss: 0xBA1A1A, onLoss: 0xFFFFFF,
            background: 0xFBFDF7, onBackground: 0x191D17
        ),
        dark: PaletteColors(
            primary: 0xA1D39A, onPrimary: 0x0A390F,
            inactive: 0x3A4B37, onInactive: 0xD5E8CF,
            win: 0x80D4DA, onWin: 0x003739,
            loss: 0xFFB4AB, onLoss: 0x690005,
            background: 0x11140F, onBackground: 0xE1E4DC
        )
    )

    // Blue against orange, which stays distinct with red-green color blindness.
    static let ocean = PaletteVariants(
        light: PaletteColors(
            primary: 0x2A5EA8, onPrimary: 0xFFFFFF,
            inactive: 0xD6E3FF, onInactive: 0x001B3E,
            win: 0x006A60, onWin: 0xFFFFFF,
            loss: 0xA93B00, onLoss: 0xFFFFFF,
            background: 0xFAFBFF, onBackground: 0x1A1C20
        ),
        dark: PaletteColors(
            primary: 0xA8C8FF, onPrimary: 0x003062,
            inactive: 0x2E3A4E, onInactive: 0xD6E3FF,
            win: 0x7FD6C9, onWin: 0x003731,
            loss: 0xFFB599, onLoss: 0x5A1A00,
            background: 0x111318, onBackground: 0xE2E2E9
        )
    )

    // High contrast; the dark variant is true black for OLED screens.
    static let graphite = PaletteVariants(
        light: PaletteColors(
            primary: 0x1B1B1B, onPrimary: 0xFFFFFF,
            inactive: 0xE4E4E4, onInactive: 0x1B1B1B,
            win: 0x2E6B30, onWin: 0xFFFFFF,
            loss: 0xC62828, onLoss: 0xFFFFFF,
            background: 0xFFFFFF, onBackground: 0x1B1B1B
        ),
        dark: PaletteColors(
            primary: 0xF2F2F2, onPrimary: 0x1B1B1B,
            inactive: 0x2E2E2E, onInactive: 0xF2F2F2,
            win: 0xA5D6A7, onWin: 0x0B3A0E,
            loss: 0xFFB4AB, onLoss: 0x690005,
            background: 0x000000, onBackground: 0xE8E8E8
        )
    )
}
