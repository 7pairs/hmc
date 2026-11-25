<!--
SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
SPDX-License-Identifier: Apache-2.0
Source: https://github.com/7pairs/hmc
-->

# 初期画面の仕様・設計・テスト項目

## 仕様

AndroidとiOSでアプリを新しく起動すると、登録済みの選手が4人未満なら選手登録画面を表示する。4人以上なら素点入力画面を表示し、東・南・西・北の4席を選手未選択、素点空欄で開く。ホーム画面などでOSが表示するアプリ名は `hmc` とする。（参照：[素点入力と成績表示の仕様](match-score-entry.md#仕様)）

| 項目 | Android | iOS |
| --- | --- | --- |
| アプリ識別子 | アプリID `blue.lions.hmc` | Bundle ID `blue.lions.hmc` |
| 最低対応OS | Android 7.0（`minSdk 24`） | iOS 18.5 |

iOSの最低対応バージョンは、手順書0002の試験ビルドで一部のライブラリが18.5向けだったことを踏まえて決めた。（参照：[手順書0002の試験結果](../procedures/0002-bootstrap-app.md#ツールの組み合わせと試験ビルド)）

### 選手を登録する

選手登録画面では登録済みの選手を一覧で確認し、1人ずつ追加できる。登録済みの選手の表示名も同じ画面で変更できる。空欄や別の選手と同じ表示名を入力した場合は理由を表示し、入力を直して登録または変更をやり直せるようにする。登録した選手と表示名は、アプリを終了して新しく起動しても使える。

登録済みの選手が4人以上になったら、選手登録画面から素点入力画面へ進める。5人目以降も同じ画面で追加できる。素点入力画面から選手登録画面を開けるようにする。素点入力画面に戻ったときは、席の割り当てと素点、表示中の成績やエラーを保ち、変更後の表示名を使う。表示名を変更しても選手IDは変わらず、保存済みの半荘との対応を保つ。（参照：[手順書0009](../procedures/0009-save-matches.md)）

## 設計

手順書0002で採用したテンプレートの `androidApp`、`iosApp`、`shared` の構成を使う。`shared/src/commonMain/kotlin/blue/lions/hmc/App.kt` の `App()` でCompose Material 3の標準テーマを適用し、`Surface` の中で `ScoreEntryScreen()` を表示する。（参照：[素点入力と成績表示の設計](match-score-entry.md#設計)）

| OS | 起動処理から共通画面を呼ぶ経路 |
| --- | --- |
| Android | `androidApp` のManifestで起動する `MainActivity` が、`setContent` から `shared` の `App()` を呼ぶ。 |
| iOS | `iosApp` の `iOSApp.swift` が `ContentView` を開く。`ContentView.swift` の `ComposeView` が `Shared` フレームワークの `MainViewController()` を呼び出し、`shared/src/iosMain/kotlin/blue/lions/hmc/MainViewController.kt` の `ComposeUIViewController` が `App()` を表示する。 |

Androidは `androidApp` のビルド設定で `applicationId`、Manifestが参照する `app_name` でアプリ名を設定する。Androidアプリと共通モジュールのAndroidターゲットで `minSdk` を24にする。

iOSは `iosApp/Configuration/Config.xcconfig` の `BUNDLE_ID` を `blue.lions.hmc`、`APP_NAME` を `hmc` に設定する。テンプレートの `PRODUCT_BUNDLE_IDENTIFIER` から `TEAM_ID` の連結を外し、`BUNDLE_ID` の値を使う。アプリの `Info.plist` の `CFBundleDisplayName` は `$(APP_NAME)` を参照し、iOSアプリの `IPHONEOS_DEPLOYMENT_TARGET` は18.5とする。（参照：[Xcodeのビルド設定](https://developer.apple.com/documentation/xcode/build-settings-reference)・[アプリのInfo.plist](https://developer.apple.com/documentation/BundleResources/managing-your-app-s-information-property-list)）

デスクトップとWebのモジュール・ターゲット、博物館の一覧・詳細画面とデータ取得処理を取り除く。サンプルで使うネットワーク、画像読み込み、画面遷移、依存性注入などの依存関係と、Androidの通信権限や `MuseumApp` の指定、iOSのKoin初期化も取り除く。テンプレートの画像、アイコンとその参照、GitHub Actions、残すファイルの出典とライセンス表示は、手順1の合意に従う。（参照：[テンプレートから取り込む範囲](../procedures/0002-bootstrap-app.md#テンプレートの候補)）

## テスト項目

同じソースのDebugビルドを両OSで起動し、アプリ名と最初に表示される画面を確認する。起動に使う成果物でアプリ識別子と最低対応OSも確認する。選手の割り当てと素点入力画面での操作は手順書0008の手順3で確かめる。（参照：[素点入力と成績表示のテスト項目](match-score-entry.md#androidとiosでの画面確認)・[手順書0008](../procedures/0008-assign-players-to-seats.md)）

`apkanalyzer` が `PATH` にない場合は、Android SDK内の `cmdline-tools/latest/bin/apkanalyzer` を使う。

| 項目 | 確認方法 | 期待する結果 |
| --- | --- | --- |
| Androidの起動と表示 | Debug APKをインストールし、ランチャーに表示されたアプリ名を確認して起動する。 | アプリ名は `hmc`。東・南・西・北の各席が選手未選択、素点空欄で表示される。 |
| iOSの起動と表示 | Debugビルドをインストールし、ホーム画面に表示されたアプリ名を確認して起動する。 | アプリ名は `hmc`。東・南・西・北の各席が選手未選択、素点空欄で表示される。 |
| Androidの設定 | `apkanalyzer manifest application-id <APK>` と `apkanalyzer manifest min-sdk <APK>` で、起動に使うAPKを確認する。（参照：[APK Analyzerのコマンド](https://developer.android.com/tools/apkanalyzer)） | アプリIDは `blue.lions.hmc`、最低APIレベルは24。 |
| iOSの設定 | 起動に使う `.app` 内の `Info.plist` から `CFBundleIdentifier` と `MinimumOSVersion` を読む。`plutil -extract <キー> raw -o - <Info.plist>` を使う。 | Bundle IDは `blue.lions.hmc`、最低対応OSは18.5。 |
| 共通画面の呼び出しとスタイル | 設計に示したAndroidとiOSの起動経路をコードでたどり、共通画面のスタイルを確認する。 | どちらの経路も `shared/src/commonMain` の同じ `App()` を呼び出し、Material 3の標準テーマを適用した素点入力画面を表示する。 |
