// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/Kotlin/KMP-App-Template/blob/6b6d09e4f3f5e845116422d59b658791ed6149c6/iosApp/iosApp/ContentView.swift
// Modified for hmc (https://github.com/7pairs/hmc).

import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    let owner: IosEntryOwner

    func makeUIViewController(context: Context) -> UIViewController {
        owner.makeViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

final class EntryOwner: ObservableObject {
    let kotlin: IosEntryOwner

    init() {
        kotlin = IosEntryOwner(container: IosChronicleKt.createIosContainer())
    }

    deinit {
        kotlin.close()
    }
}

struct ContentView: View {
    let owner: IosEntryOwner

    var body: some View {
        ComposeView(owner: owner)
            .ignoresSafeArea()
    }
}

#if DEBUG && HMC_RECREATION_CHECK
final class RecreationCheckOwner: ObservableObject {
    let kotlin = IosSaveRecreationCheck()

    deinit {
        kotlin.close()
    }
}

private struct RecreationCheckComposeView: UIViewControllerRepresentable {
    let owner: IosSaveRecreationCheck
    let onRecreate: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        owner.makeViewController(onRecreate: onRecreate)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct RecreationCheckView: View {
    @StateObject private var owner = RecreationCheckOwner()
    @State private var generation = 0

    var body: some View {
        RecreationCheckComposeView(owner: owner.kotlin, onRecreate: { generation += 1 })
            .id(generation)
            .ignoresSafeArea()
    }
}
#endif
