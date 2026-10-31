// swift-tools-version: 6.2
import PackageDescription

let package = Package(
    name: "ClockCore",
    platforms: [.iOS(.v26), .macOS(.v26)],
    products: [
        .library(name: "ClockCore", targets: ["ClockCore"]),
    ],
    targets: [
        .target(name: "ClockCore"),
        .testTarget(name: "ClockCoreTests", dependencies: ["ClockCore"]),
    ]
)
