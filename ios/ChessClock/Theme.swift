import SwiftUI
import UIKit

/// The Android app's Walnut palette, with its light and dark variants.
extension Color {
    static let onMove = dynamic(light: 0x924C00, dark: 0xFFB77C)
    static let onOnMove = dynamic(light: 0xFFFFFF, dark: 0x4E2600)
    static let waiting = dynamic(light: 0xE4BFA7, dark: 0x53443A)
    static let onWaiting = dynamic(light: 0x4E2600, dark: 0xFFDCC5)
    static let win = dynamic(light: 0x52643F, dark: 0xB8CE9D)
    static let loss = dynamic(light: 0xBA1A1A, dark: 0xFFB4AB)

    private static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(rgb: dark) : UIColor(rgb: light) })
    }
}

private extension UIColor {
    convenience init(rgb: UInt32) {
        self.init(
            red: CGFloat(rgb >> 16 & 0xFF) / 255,
            green: CGFloat(rgb >> 8 & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1,
        )
    }
}
