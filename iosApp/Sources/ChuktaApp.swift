import SwiftUI
import UIKit
import PhotosUI
import ContactsUI
import Security
import ChuktaShared

@main struct ChuktaIOSApp: App {
    @State private var identity = UUID()
    var body: some Scene { WindowGroup { NativeDesk().id(identity).ignoresSafeArea().onOpenURL { url in
        if url.scheme == "chukta", url.host == "invite" { Vault.write("pendingInvite", url.lastPathComponent); identity = UUID() }
    } } }
}
private enum Vault {
    static func read(_ key: String) -> String? {
        if key != "session" { return UserDefaults.standard.string(forKey: "chukta." + key) }
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: "com.chukta.app", kSecAttrAccount as String: key, kSecReturnData as String: true, kSecMatchLimit as String: kSecMatchLimitOne]
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }
    static func write(_ key: String, _ value: String?) {
        if key != "session" { UserDefaults.standard.set(value, forKey: "chukta." + key); return }
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: "com.chukta.app", kSecAttrAccount as String: key]
        SecItemDelete(query as CFDictionary)
        guard let value = value else { return }
        var item = query
        item[kSecValueData as String] = value.data(using: .utf8)
        item[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        SecItemAdd(item as CFDictionary, nil)
    }
}
struct NativeDesk: UIViewControllerRepresentable {
    func makeCoordinator() -> Coordinator { Coordinator() }
    func makeUIViewController(context: Context) -> UIViewController {
        let coordinator = context.coordinator
        let url = Bundle.main.object(forInfoDictionaryKey: "ChuktaApiUrl") as? String ?? "https://api.example.com"
        let controller = MainViewControllerKt.MainViewController(
            apiUrl: url,
            read: { Vault.read($0) },
            write: { key, value in Vault.write(key, value) },
            openUrl: { text in
                guard let url = URL(string: text), UIApplication.shared.canOpenURL(url) else { return KotlinBoolean(bool: false) }
                UIApplication.shared.open(url); return KotlinBoolean(bool: true)
            },
            share: { text in
                let sheet = UIActivityViewController(activityItems: [text], applicationActivities: nil)
                if let root = coordinator.controller { sheet.popoverPresentationController?.sourceView = root.view; sheet.popoverPresentationController?.sourceRect = CGRect(x: root.view.bounds.midX, y: root.view.bounds.midY, width: 1, height: 1); root.present(sheet, animated: true) }
            },
            copy: { UIPasteboard.general.string = $0 },
            pickPhoto: { callback in
                coordinator.photoResult = callback
                var config = PHPickerConfiguration(); config.filter = .images; config.selectionLimit = 1
                let picker = PHPickerViewController(configuration: config); picker.delegate = coordinator
                coordinator.controller?.present(picker, animated: true)
            },
            pickContact: { callback in
                coordinator.contactResult = callback
                let picker = CNContactPickerViewController(); picker.delegate = coordinator
                coordinator.controller?.present(picker, animated: true)
            },
            photoData: { path in
                guard let url = URL(string: path), let data = try? Data(contentsOf: url) else { return nil }
                return "data:image/jpeg;base64," + data.base64EncodedString()
            },
            formatDate: { iso in
                let source = ISO8601DateFormatter(); source.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
                guard let date = source.date(from: iso) else { return iso }
                let output = DateFormatter(); output.dateFormat = "d MMM, h:mm a"; return output.string(from: date)
            }
        )
        coordinator.controller = controller
        return controller
    }
    func updateUIViewController(_ controller: UIViewController, context: Context) {}
    class Coordinator: NSObject, PHPickerViewControllerDelegate, CNContactPickerDelegate {
        weak var controller: UIViewController?
        var photoResult: ((String?) -> KotlinUnit)?
        var contactResult: ((String?) -> KotlinUnit)?
        func contactPicker(_ picker: CNContactPickerViewController, didSelect contact: CNContact) {
            _ = contactResult?(CNContactFormatter.string(from: contact, style: .fullName))
        }
        func contactPickerDidCancel(_ picker: CNContactPickerViewController) { _ = contactResult?(nil) }
        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            picker.dismiss(animated: true)
            guard let provider = results.first?.itemProvider else { _ = photoResult?(nil); return }
            provider.loadObject(ofClass: UIImage.self) { object, _ in
                guard let image = object as? UIImage else { return }
                let ratio = min(1, 1024 / max(image.size.width, image.size.height))
                let size = CGSize(width: image.size.width * ratio, height: image.size.height * ratio)
                let scaled = UIGraphicsImageRenderer(size: size).image { _ in image.draw(in: CGRect(origin: .zero, size: size)) }
                guard let data = scaled.jpegData(compressionQuality: 0.75) else { return }
                let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
                let path = documents.appendingPathComponent("avatar.jpg")
                do { try data.write(to: path); DispatchQueue.main.async { _ = self.photoResult?(path.absoluteString) } } catch {}
            }
        }
    }
}
