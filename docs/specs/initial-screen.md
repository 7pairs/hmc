<!--
SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
SPDX-License-Identifier: Apache-2.0
Source: https://github.com/7pairs/hmc
-->

# 初期画面の仕様・設計・テスト項目

## 仕様

AndroidとiOSでアプリを起動すると、アプリの表示領域の中央に小文字の `hmc` だけを１行で表示する。文字と背景には標準のスタイルを使う。ホーム画面などでOSが表示するアプリ名も `hmc` とする。

| 項目 | Android | iOS |
| --- | --- | --- |
| アプリ識別子 | アプリID `blue.lions.hmc` | Bundle ID `blue.lions.hmc` |
| 最低対応OS | Android 7.0（`minSdk 24`） | iOS 18.5 |

今回の対象は両OSの起動処理から共通のCompose画面を表示できるところまでとする。iOSの最低対応バージョンは、手順1の試験ビルドで一部のライブラリが18.5向けだったことを踏まえて決めた。[手順書0002の試験結果](../procedures/0002-bootstrap-app.md#ツールの組み合わせと試験ビルド)

## 設計

手順1で採用したテンプレートの `androidApp`、`iosApp`、`shared` の構成を使う。共通画面は `shared/src/commonMain/kotlin/blue/lions/hmc/App.kt` に置く。`App()` ではCompose Material 3の標準テーマを適用し、`Surface` で背景を描画する。表示領域いっぱいに広げた `Box` の中央に `Text("hmc")` を配置する。

| OS | 起動処理から共通画面を呼ぶ経路 |
| --- | --- |
| Android | `androidApp` のManifestで起動する `MainActivity` が、`setContent` から `shared` の `App()` を呼ぶ。 |
| iOS | `iosApp` の `iOSApp.swift` が `ContentView` を開く。`ContentView.swift` の `ComposeView` が `Shared` フレームワークの `MainViewController()` を呼び出し、`shared/src/iosMain/kotlin/blue/lions/hmc/MainViewController.kt` の `ComposeUIViewController` が `App()` を表示する。 |

Androidは `androidApp` のビルド設定で `applicationId`、Manifestが参照する `app_name` でアプリ名を設定する。Androidアプリと共通モジュールのAndroidターゲットで `minSdk` を24にする。

iOSは `iosApp/Configuration/Config.xcconfig` の `BUNDLE_ID` を `blue.lions.hmc`、`APP_NAME` を `hmc` に設定する。テンプレートの `PRODUCT_BUNDLE_IDENTIFIER` から `TEAM_ID` の連結を外し、`BUNDLE_ID` の値を使う。アプリの `Info.plist` の `CFBundleDisplayName` は `$(APP_NAME)` を参照し、iOSアプリの `IPHONEOS_DEPLOYMENT_TARGET` は18.5とする。[Xcodeのビルド設定](https://developer.apple.com/documentation/xcode/build-settings-reference)・[アプリのInfo.plist](https://developer.apple.com/documentation/BundleResources/managing-your-app-s-information-property-list)

デスクトップとWebのモジュール・ターゲット、博物館の一覧・詳細画面とデータ取得処理を取り除く。サンプルで使うネットワーク、画像読み込み、画面遷移、依存性注入などの依存関係と、Androidの通信権限や `MuseumApp` の指定、iOSのKoin初期化も取り除く。テンプレートの画像、アイコンとその参照、GitHub Actions、残すファイルの出典とライセンス表示は、手順1の合意に従う。[テンプレートから取り込む範囲](../procedures/0002-bootstrap-app.md#テンプレートの候補)

## テスト項目

手順4で同じソースのDebugビルドを両OSで確認する。起動に使う成果物でアプリ識別子と最低対応OSも確認する。

`apkanalyzer` が `PATH` にない場合は、Android SDK内の `cmdline-tools/latest/bin/apkanalyzer` を使う。

| 項目 | 確認方法 | 期待する結果 |
| --- | --- | --- |
| Androidの起動と表示 | Debug APKをAndroid 17エミュレータにインストールし、ランチャーに表示されたアプリ名を確認して起動する。 | アプリ名は `hmc`。起動するとアプリの表示領域の中央に小文字の `hmc` だけが１行で表示される。 |
| iOSの起動と表示 | iOSシミュレータ向けのDebug `.app` をiOS 27.2シミュレータにインストールし、ホーム画面に表示されたアプリ名を確認して起動する。 | アプリ名は `hmc`。起動するとアプリの表示領域の中央に小文字の `hmc` だけが１行で表示される。 |
| Androidの設定 | `apkanalyzer manifest application-id <APK>` と `apkanalyzer manifest min-sdk <APK>` で、起動に使うAPKを確認する。[APK Analyzerのコマンド](https://developer.android.com/tools/apkanalyzer) | アプリIDは `blue.lions.hmc`、最低APIレベルは24。 |
| iOSの設定 | 起動に使う `.app` 内の `Info.plist` から `CFBundleIdentifier` と `MinimumOSVersion` を読む。`plutil -extract <キー> raw -o - <Info.plist>` を使う。 | Bundle IDは `blue.lions.hmc`、最低対応OSは18.5。 |
| 共通画面の呼び出しとスタイル | 設計に示したAndroidとiOSの起動経路をコードでたどり、共通画面のスタイルを確認する。 | どちらの経路も `shared/src/commonMain` の同じ `App()` を呼び出し、共通画面はMaterial 3の標準テーマで文字と背景を描画する。 |
