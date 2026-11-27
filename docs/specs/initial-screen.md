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

手順書0002で採用した `androidApp`、`iosApp`、`shared` の構成を使う。共通の `App()` で登録済みの選手を読み込み、人数に応じて選手登録画面か素点入力画面を表示する。読み込みに失敗したときは理由と再試行の操作を表示する。両画面にCompose Material 3の標準テーマを適用する。（参照：[素点入力と成績表示の設計](match-score-entry.md#設計)）

| OS | 起動処理から共通画面を呼ぶ経路 |
| --- | --- |
| Android | `Application` が `AppContainer` を持つ。`MainActivity` はそこから取得した依存をルートの `ViewModel` に渡し、その状態ホルダーを `setContent` から共通の `App()` に渡す。 |
| iOS | `iOSApp.swift` で `@StateObject` として保持するオブジェクトが `AppContainer` と状態ホルダーを持つ。`ContentView` と `ComposeView` を経由して `MainViewController()` に渡し、`ComposeUIViewController` から共通の `App()` を表示する。 |

状態ホルダーはRepositoryを通して選手一覧を読み込み、登録や改名が成功したら一覧を更新する。選手を追加する際は、選手と初期リーグへの所属を1トランザクションで保存する。改名は選手IDで対象を指定し、表示名だけを更新する。前後の空白を除いた表示名で空欄と別の選手との重複を検証する。登録や改名に失敗したら入力を保つ。（参照：[手動DIのADR](../adr/0003-use-manual-dependency-injection.md)・[保存データと制約](match-score-entry.md#保存するデータと制約)）

Androidは `androidApp` のビルド設定で `applicationId`、Manifestが参照する `app_name` でアプリ名を設定する。Androidアプリと共通モジュールのAndroidターゲットで `minSdk` を24にする。

iOSは `iosApp/Configuration/Config.xcconfig` の `BUNDLE_ID` を `blue.lions.hmc`、`APP_NAME` を `hmc` に設定する。テンプレートの `PRODUCT_BUNDLE_IDENTIFIER` から `TEAM_ID` の連結を外し、`BUNDLE_ID` の値を使う。アプリの `Info.plist` の `CFBundleDisplayName` は `$(APP_NAME)` を参照し、iOSアプリの `IPHONEOS_DEPLOYMENT_TARGET` は18.5とする。（参照：[Xcodeのビルド設定](https://developer.apple.com/documentation/xcode/build-settings-reference)・[アプリのInfo.plist](https://developer.apple.com/documentation/BundleResources/managing-your-app-s-information-property-list)）

デスクトップとWebのモジュール・ターゲット、博物館の一覧・詳細画面とデータ取得処理を取り除く。サンプルで使うネットワーク、画像読み込み、画面遷移、依存性注入などの依存関係と、Androidの通信権限や `MuseumApp` の指定、iOSのKoin初期化も取り除く。テンプレートの画像、アイコンとその参照、GitHub Actions、残すファイルの出典とライセンス表示は、手順1の合意に従う。（参照：[テンプレートから取り込む範囲](../procedures/0002-bootstrap-app.md#テンプレートの候補)）

## テスト項目

同じソースのDebugビルドを両OSで起動し、アプリ名と最初に表示される画面を確認する。起動に使う成果物でアプリ識別子と最低対応OSも確認する。選手の割り当てと素点入力画面での操作は素点入力の画面確認で確かめる。（参照：[素点入力と成績表示のテスト項目](match-score-entry.md#androidとiosでの画面確認)）

`apkanalyzer` が `PATH` にない場合は、Android SDK内の `cmdline-tools/latest/bin/apkanalyzer` を使う。

| 項目 | 確認方法 | 期待する結果 |
| --- | --- | --- |
| 起動と表示 | 両OSでアプリ名を確認し、選手が0人、1〜3人、4人以上の状態でそれぞれ新しく起動する。 | アプリ名は `hmc`。4人未満では選手登録画面に登録済みの選手が表示される。4人以上では4席とも選手未選択、素点空欄の素点入力画面を開く。 |
| 選手の追加と名前の変更 | 空欄と登録済みの名前を試し、入力を直して選手を追加する。4人登録後に素点入力へ進み、成績を確認する。登録画面を開き直して5人目を追加し、席に割り当てた選手の名前も変更してから素点入力に戻る。 | 不備の理由を表示する。有効な名前で登録・改名でき、5人全員が候補に出る。改名前と同じ選手IDで席と素点が残り、確認済みの成績には変更後の名前が表示される。 |
| Androidの設定 | `apkanalyzer manifest application-id <APK>` と `apkanalyzer manifest min-sdk <APK>` で、起動に使うAPKを確認する。（参照：[APK Analyzerのコマンド](https://developer.android.com/tools/apkanalyzer)） | アプリIDは `blue.lions.hmc`、最低APIレベルは24。 |
| iOSの設定 | 起動に使う `.app` 内の `Info.plist` から `CFBundleIdentifier` と `MinimumOSVersion` を読む。`plutil -extract <キー> raw -o - <Info.plist>` を使う。 | Bundle IDは `blue.lions.hmc`、最低対応OSは18.5。 |
| 共通画面の呼び出しとスタイル | 設計に示したAndroidとiOSの起動経路をコードでたどり、共通画面のスタイルを確認する。 | 両OSが共通の `App()` を呼び出し、選手登録画面と素点入力画面にMaterial 3の標準テーマを適用する。 |
