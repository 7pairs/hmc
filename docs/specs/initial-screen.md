<!--
SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
SPDX-License-Identifier: Apache-2.0
Source: https://github.com/7pairs/hmc
-->

# 初期画面の仕様・設計・テスト項目

## 仕様

AndroidとiOSでアプリを新しく起動すると、共通の素点入力画面を表示する。「選手A」から「選手D」までを固定順に並べ、４人の素点欄は空欄で始める。ホーム画面などでOSが表示するアプリ名は `hmc` とする。[素点入力と成績表示の仕様](match-score-entry.md#仕様)

| 項目 | Android | iOS |
| --- | --- | --- |
| アプリ識別子 | アプリID `blue.lions.hmc` | Bundle ID `blue.lions.hmc` |
| 最低対応OS | Android 7.0（`minSdk 24`） | iOS 18.5 |

iOSの最低対応バージョンは、手順書0002の試験ビルドで一部のライブラリが18.5向けだったことを踏まえて決めた。[手順書0002の試験結果](../procedures/0002-bootstrap-app.md#ツールの組み合わせと試験ビルド)

## 設計

手順書0002で採用したテンプレートの `androidApp`、`iosApp`、`shared` の構成を使う。`shared/src/commonMain/kotlin/blue/lions/hmc/App.kt` の `App()` でCompose Material 3の標準テーマを適用し、`Surface` の中で `ScoreEntryScreen()` を表示する。[素点入力と成績表示の設計](match-score-entry.md#設計)

| OS | 起動処理から共通画面を呼ぶ経路 |
| --- | --- |
| Android | `androidApp` のManifestで起動する `MainActivity` が、`setContent` から `shared` の `App()` を呼ぶ。 |
| iOS | `iosApp` の `iOSApp.swift` が `ContentView` を開く。`ContentView.swift` の `ComposeView` が `Shared` フレームワークの `MainViewController()` を呼び出し、`shared/src/iosMain/kotlin/blue/lions/hmc/MainViewController.kt` の `ComposeUIViewController` が `App()` を表示する。 |

Androidは `androidApp` のビルド設定で `applicationId`、Manifestが参照する `app_name` でアプリ名を設定する。Androidアプリと共通モジュールのAndroidターゲットで `minSdk` を24にする。

iOSは `iosApp/Configuration/Config.xcconfig` の `BUNDLE_ID` を `blue.lions.hmc`、`APP_NAME` を `hmc` に設定する。テンプレートの `PRODUCT_BUNDLE_IDENTIFIER` から `TEAM_ID` の連結を外し、`BUNDLE_ID` の値を使う。アプリの `Info.plist` の `CFBundleDisplayName` は `$(APP_NAME)` を参照し、iOSアプリの `IPHONEOS_DEPLOYMENT_TARGET` は18.5とする。[Xcodeのビルド設定](https://developer.apple.com/documentation/xcode/build-settings-reference)・[アプリのInfo.plist](https://developer.apple.com/documentation/BundleResources/managing-your-app-s-information-property-list)

デスクトップとWebのモジュール・ターゲット、博物館の一覧・詳細画面とデータ取得処理を取り除く。サンプルで使うネットワーク、画像読み込み、画面遷移、依存性注入などの依存関係と、Androidの通信権限や `MuseumApp` の指定、iOSのKoin初期化も取り除く。テンプレートの画像、アイコンとその参照、GitHub Actions、残すファイルの出典とライセンス表示は、手順1の合意に従う。[テンプレートから取り込む範囲](../procedures/0002-bootstrap-app.md#テンプレートの候補)

## テスト項目

同じソースのDebugビルドを両OSで起動し、アプリ名と最初に表示される画面を確認する。起動に使う成果物でアプリ識別子と最低対応OSも確認する。素点入力画面での操作は手順書0005の手順3で確かめる。[素点入力と成績表示のテスト項目](match-score-entry.md#androidとiosでの画面確認)

`apkanalyzer` が `PATH` にない場合は、Android SDK内の `cmdline-tools/latest/bin/apkanalyzer` を使う。

| 項目 | 確認方法 | 期待する結果 |
| --- | --- | --- |
| Androidの起動と表示 | Debug APKをインストールし、ランチャーに表示されたアプリ名を確認して起動する。 | アプリ名は `hmc`。新しく起動すると４人の素点欄が空欄の素点入力画面を表示する。 |
| iOSの起動と表示 | Debugビルドをインストールし、ホーム画面に表示されたアプリ名を確認して起動する。 | アプリ名は `hmc`。新しく起動すると４人の素点欄が空欄の素点入力画面を表示する。 |
| Androidの設定 | `apkanalyzer manifest application-id <APK>` と `apkanalyzer manifest min-sdk <APK>` で、起動に使うAPKを確認する。[APK Analyzerのコマンド](https://developer.android.com/tools/apkanalyzer) | アプリIDは `blue.lions.hmc`、最低APIレベルは24。 |
| iOSの設定 | 起動に使う `.app` 内の `Info.plist` から `CFBundleIdentifier` と `MinimumOSVersion` を読む。`plutil -extract <キー> raw -o - <Info.plist>` を使う。 | Bundle IDは `blue.lions.hmc`、最低対応OSは18.5。 |
| 共通画面の呼び出しとスタイル | 設計に示したAndroidとiOSの起動経路をコードでたどり、共通画面のスタイルを確認する。 | どちらの経路も `shared/src/commonMain` の同じ `App()` を呼び出し、Material 3の標準テーマを適用した素点入力画面を表示する。 |
