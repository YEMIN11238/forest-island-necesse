import AppKit
import AVFoundation
import CoreVideo
import Foundation

// Encode the actual native framebuffer samples at their recorded wall-clock times.
// Readbacks are irregular; assigning an invented fixed FPS would misrepresent wind.
guard CommandLine.arguments.count == 3 else {
    fatalError("Usage: swift encode_visual_capture.swift CAPTURE_DIRECTORY OUTPUT.mp4")
}
let source = URL(fileURLWithPath: CommandLine.arguments[1], isDirectory: true)
let destination = URL(fileURLWithPath: CommandLine.arguments[2])
let rows = try String(contentsOf: source.appendingPathComponent("frames.csv"), encoding: .utf8)
    .split(separator: "\n").dropFirst().map { $0.split(separator: ",", omittingEmptySubsequences: false) }
guard let first = rows.first, let start = Double(first[2]),
      let image = NSImage(contentsOf: source.appendingPathComponent("frames/" + first[1])),
      let cg = image.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
    fatalError("Capture is missing its first valid sample")
}
let width = cg.width / 2 * 2, height = cg.height / 2 * 2
let writer = try AVAssetWriter(outputURL: destination, fileType: .mp4)
let input = AVAssetWriterInput(mediaType: .video, outputSettings: [
    AVVideoCodecKey: AVVideoCodecType.h264,
    AVVideoWidthKey: width, AVVideoHeightKey: height,
    AVVideoCompressionPropertiesKey: [AVVideoAverageBitRateKey: 10_000_000]
])
input.expectsMediaDataInRealTime = false
let attributes: [String: Any] = [
    kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB,
    kCVPixelBufferWidthKey as String: width,
    kCVPixelBufferHeightKey as String: height,
    kCVPixelBufferCGImageCompatibilityKey as String: true,
    kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
]
let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: input, sourcePixelBufferAttributes: attributes)
writer.add(input)
guard writer.startWriting() else { fatalError("Writer start: \(String(describing: writer.error))") }
writer.startSession(atSourceTime: .zero)
for row in rows {
    autoreleasepool {
        guard let stamp = Double(row[2]),
              let img = NSImage(contentsOf: source.appendingPathComponent("frames/" + row[1])),
              let cgImage = img.cgImage(forProposedRect: nil, context: nil, hints: nil),
              let pool = adaptor.pixelBufferPool else { fatalError("Invalid capture row \(row)") }
        var allocated: CVPixelBuffer?
        guard CVPixelBufferPoolCreatePixelBuffer(nil, pool, &allocated) == kCVReturnSuccess,
              let buffer = allocated else { fatalError("Pixel buffer allocation") }
        CVPixelBufferLockBaseAddress(buffer, [])
        guard let context = CGContext(data: CVPixelBufferGetBaseAddress(buffer), width: width, height: height,
            bitsPerComponent: 8, bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
            space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue) else {
            fatalError("Pixel context")
        }
        context.interpolationQuality = .none
        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
        CVPixelBufferUnlockBaseAddress(buffer, [])
        while !input.isReadyForMoreMediaData && writer.status == .writing { Thread.sleep(forTimeInterval: 0.001) }
        let time = CMTime(seconds: stamp - start, preferredTimescale: 60_000)
        guard adaptor.append(buffer, withPresentationTime: time) else {
            fatalError("Encode sample: \(String(describing: writer.error))")
        }
    }
}
input.markAsFinished()
let completion = DispatchSemaphore(value: 0)
writer.finishWriting { completion.signal() }
completion.wait()
guard writer.status == .completed else { fatalError("Video failed: \(String(describing: writer.error))") }
print("Encoded \(rows.count) native samples with recorded timestamps: \(destination.path)")
