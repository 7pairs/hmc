// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/Kotlin/KMP-App-Template/blob/6b6d09e4f3f5e845116422d59b658791ed6149c6/iosApp/iosApp/iOSApp.swift
// Modified for hmc (https://github.com/7pairs/hmc).

import SwiftUI

@main
struct iOSApp: App {
    #if !(DEBUG && HMC_RECREATION_CHECK)
    @StateObject private var owner = EntryOwner()
    #endif

    var body: some Scene {
        WindowGroup {
            #if DEBUG && HMC_RECREATION_CHECK
            RecreationCheckView()
            #else
            ContentView(owner: owner.kotlin)
            #endif
        }
    }
}
