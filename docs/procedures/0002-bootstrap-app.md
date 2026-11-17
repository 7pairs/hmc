<!--
SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
SPDX-License-Identifier: Apache-2.0
Source: https://github.com/7pairs/hmc
-->

# 0002 AndroidとiOSでhmcの初期画面を起動する

- **ステータス**　作業中
- **作業ブランチ**　`work/0002-bootstrap-app`

作業は[開発プロセス](../development-process.md)と[SDD](../sdd/README.md)に従う。各手順の検証とレビュー結果を二人で確認し、次の手順に合意した上でオーナーから着手依頼を受ける。

## 手順チェックリスト

- [x] [1. テンプレートと開発環境を確認する](#1-テンプレートと開発環境を確認する)
- [ ] [2. 初期画面の仕様・設計・テスト項目を決める](#2-初期画面の仕様設計テスト項目を決める)
- [ ] [3. テンプレートからアプリを作り共通画面を実装する](#3-テンプレートからアプリを作り共通画面を実装する)
- [ ] [4. 両OSで起動しコード構成を確認する](#4-両osで起動しコード構成を確認する)

## 目的と対象

Kotlin MultiplatformとCompose Multiplatformを使い、AndroidとiOSの両方で同じCompose画面に `hmc` と表示する。オーナーは両OSの起動処理から共通画面までコードをたどり、それぞれの技術が何を担うかを学ぶ。[プロジェクト憲章](../project-charter.md)

プロジェクトはテンプレートから作成し、IDEにはAndroid Studioを使う。AndroidのアプリIDとiOSのBundle IDには `blue.lions.hmc` を使う。動作確認には用意済みのAndroid 17エミュレータとiOS 27.2シミュレータを使う。実機での動作確認、対局成績の機能、GitHub Actionsの設定は対象外とする。

## 手順

### 1. テンプレートと開発環境を確認する

そらは候補のテンプレートが生成する構成を公式資料で調べる。手元のAndroid StudioとXcodeのバージョンを確認し、候補が指定するKotlin、Compose Multiplatform、Gradle、Android Gradle Plugin、JDKの組み合わせで両OS向けにビルドできる見込みを確かめる。

そらは候補の出典、必要なツールと設定、採用案を作業記録に残す。リポジトリに残す予定の生成ファイルは、改変して使うものも含め、ファイルごとに出典と利用条件を確認する。[手順書0001](0001-select-oss-license.md)の方針に照らし、hmcのライセンスを適用できるか、どの出典とライセンスの表示を引き継ぐかを採用案に記録する。

うみは採用案で共通画面を両OS向けに作れるか、ツールの対応関係と生成ファイルの扱いを判断する根拠があるかを確認する。二人で採用するテンプレート、ツールの組み合わせ、生成ファイルの扱いに合意する。環境の準備が必要な場合はその範囲も決める。

### 2. 初期画面の仕様・設計・テスト項目を決める

そらは `docs/specs/` に初期画面の仕様、設計、テスト項目をまとめ、手順書からリンクする。共通画面を置く場所、各OSからの呼び出し方、テンプレートから削除するサンプルは、手順1で採用するテンプレートの構成を見て決める。

今回は固定の文字列の表示が対象で、用意済みのエミュレータとシミュレータで起動と表示を確認できるため、準備に時間のかからない手動テストを選ぶ。テスト項目には起動手順、期待する表示、アプリの識別子を確認する方法を書く。テスト項目は実装前に決め、手動テストは実装後に手順4で実行する。

そらは仕様、設計、テスト項目を合意済みの作業範囲と照合する。初期画面の完了条件を一つずつ読み、対応するテスト項目やコードの確認箇所があるかを確かめる。うみは仕様と設計が作業範囲に沿っているか、合意した動作をテスト項目で確かめられるかを確認する。二人で検証とレビューの結果を読み、仕様・設計・テスト項目に合意する。

### 3. テンプレートからアプリを作り共通画面を実装する

そらは合意した環境でテンプレートからプロジェクトを生成し、両OSの識別子を設定する。設計で決めた範囲でサンプルを取り除き、共通のCompose画面に `hmc` を表示する。残す生成ファイルには手順1で合意した扱いに従って出典とライセンスを表示する。hmcのライセンスを適用するファイルには手順書0001で定めた表示を入れる。

そらは追加したファイルを仕様と設計に照らす。Android Studio Rabbit 2でGradle同期を実行し、デーモンのログの `javaHome` と `javaVendor` を合意したJDKと照合して結果を記録する。両OS向けにビルドする。うみは両OSが同じCompose画面を呼び出すこと、識別子とライセンス表示が合意どおりであることをコードと設定で確認する。二人で差分とレビュー結果を確認する。

### 4. 両OSで起動しコード構成を確認する

そらは手順2で合意したテストを実行し、使った環境と結果を記録する。二人でプロジェクト全体を見直し、共通コードとOSごとのコードの置き方や、テンプレートから残した構成に整理が必要かを判断する。構成を変えた場合は、変更に関係するテストを再実行する。うみは検証結果が完了条件を満たすかを確認し、コードを変更した場合はその差分もレビューする。

そらは実際のコードを使って、Kotlin MultiplatformとCompose Multiplatformの役割、各OSの起動処理、共通画面が表示されるまでの流れを説明する。オーナーは両技術の役割、各OSから共通画面までの経路、表示する文字を変える場合の変更箇所を自分の言葉で説明する。

## 完了条件

Android 17エミュレータとiOS 27.2シミュレータのどちらでも `hmc` が表示され、アプリの識別子が合意どおりであることを二人で確認する。オーナーが手順4の内容を自分の言葉で説明でき、構造の見直し、必要なレビューと再レビュー、仕様文書と作業記録の更新が済んだら完了とする。

## 作業記録

各手順の実施結果、検証・レビュー結果またはその参照先、予定から変わった点と理由、残した課題を記録する。うみの指摘は要点、採否と理由、再レビュー結果を残す。

### 草稿のレビュー

うみの指摘３件を二人で採用した。

- 手動テストを選ぶ理由と実行時期を明記した。テスト方法と、実装前にテスト項目を合意する手順を確認できるようにするため。
- そらが仕様・設計・テスト項目を検証する方法と、仕様文書へのリンクを追加する手順を明記した。完了条件に対応する確認方法をたどれるようにするため。
- 残す生成ファイルの扱いを採用案に含めた。改変して使う場合も含め、適用するライセンスと引き継ぐ表示を実装前に確認するため。

うみの再レビューで３件とも解消した。追加の指摘はなかった。

### 手順1の作業記録

#### テンプレートの候補

- JetBrainsのKotlin Multiplatform Wizardでは、Gradleを使い、AndroidとiOSでCompose UIを共有するプロジェクトを生成できる。サンプルテストを外した調査用のZIPには `androidApp/`、`iosApp/`、共通画面を含む `shared/` があった。改変して再配布できることを示す、生成物全体のライセンス表示はZIPとAPI資料から確認できなかった。[Wizard API](https://kmp.jetbrains.com/SKILL.md)・[JetBrainsのサイト利用規約](https://www.jetbrains.com/legal/docs/company/useterms/)
- 公開テンプレートの `Kotlin/KMP-App-Template` は同じ３つのディレクトリを持ち、ルートにApache-2.0のライセンスがある。採用するなら、調査したコミット `6b6d09e4f3f5e845116422d59b658791ed6149c6` を出典とする。デスクトップ・Web、博物館のサンプルとその依存関係、テンプレート側のGitHub Actionsを除き、Android・iOSの起動処理と共通モジュールの構成を使う案とする。[KMPの標準構成](https://blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/)・[公開テンプレート](https://github.com/Kotlin/KMP-App-Template/tree/6b6d09e4f3f5e845116422d59b658791ed6149c6)・[ライセンス](https://github.com/Kotlin/KMP-App-Template/blob/6b6d09e4f3f5e845116422d59b658791ed6149c6/LICENSE)

再配布の根拠をファイルの出典までたどれるため、公開テンプレートを採用する案とする。取り込むファイルと表示の案は次のとおり。改変したファイルには変更した旨を記し、上流の出典と既存の表示を残す。コメントを書けないファイルは、パスと出典を第三者の表示として文書に記録する。hmcが新規に書くファイルには手順書0001で定めた表示を入れる。[Apache-2.0第4条](https://www.apache.org/licenses/LICENSE-2.0)・[hmcのNOTICE](../../NOTICE)

| 取り込み候補 | 出典と扱い |
| --- | --- |
| `.gitignore`、`build.gradle.kts`、`settings.gradle.kts`、`gradle.properties`、`gradle/libs.versions.toml` | 公開テンプレートのApache-2.0。対象モジュールと依存関係を絞り、下記のツールの組み合わせに合わせる。 |
| `androidApp/build.gradle.kts`、`androidApp/src/main/AndroidManifest.xml`、`MainActivity.kt`、`androidApp/src/main/res/values/strings.xml` | 公開テンプレートのApache-2.0。起動処理を残し、アプリ識別子、パッケージ、表示名を合わせる。テンプレートのランチャーアイコンを省くため、Manifestの `android:icon` と `android:roundIcon` を削除する。 |
| `shared/build.gradle.kts`、`MainViewController.kt` | 公開テンプレートのApache-2.0。AndroidとiOSを対象とする構成と、iOSから共通画面を呼ぶ処理を残す。 |
| `iosApp/Configuration/Config.xcconfig`、`iosApp/iosApp.xcodeproj/project.pbxproj`、`iOSApp.swift`、`ContentView.swift`、`Info.plist` | 公開テンプレートのApache-2.0。起動に必要な設定とSwiftのコードを残し、アプリ識別子を `blue.lions.hmc` に合わせる。`Assets.xcassets` と `Preview Assets.xcassets` を省くため、`project.pbxproj` の両カタログの参照と、`ASSETCATALOG_COMPILER_APPICON_NAME`、`DEVELOPMENT_ASSET_PATHS` の設定を削除する。 |
| `gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.jar` | Gradle WrapperのApache-2.0。スクリプトの既存の表示とJAR内のライセンスを残し、採用する配布バージョンに対応する上流とチェックサムを記録する。 |
| `gradle/wrapper/gradle-wrapper.properties` | 公開テンプレートのApache-2.0。配布先とチェックサムをGradle 9.5.1の公開値に合わせる。 |

共通画面 `App.kt` とGradleデーモンの設定はhmcで作成する。Android SDKの場所は開発環境ごとに設定する。

#### ツールの組み合わせと試験ビルド

手元にはAndroid Studio Rabbit 2系、Kotlin Multiplatformプラグイン `262.10968.183-AS`、Xcode 27.2（27B52）、Android SDK 37.0、Android 17のエミュレータ（API 37.2）があった。Xcodeが選ぶiOSシミュレータ用SDKは27.2だった。[Android Studioのリリース情報](https://developer.android.com/studio/releases/fixed-bugs/studio/2026.2.2)・[Kotlin Multiplatformプラグイン](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform)

2026年11月17日に取得したWizardの生成例は、KotlinとCompose Compilerプラグインが2.4.21、Compose Multiplatformが1.12.2、Gradleが9.5.1、Android Gradle Pluginが9.1.1で、Androidの `compileSdk` と `targetSdk` は37だった。この組み合わせを初期構成の案とする。AGP 9.1.1が必要とするのはGradle 9.3.1以上とJDK 17以上で、AGPがサポートするAndroid APIは37.0まで。JDK 25はGradle 9.1.0以降でGradleの実行に使える。Gradle配布ZIPの設定値とWrapper JARのSHA-256はGradleの公開値に一致した。[Kotlinの互換表](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)・[Compose Multiplatform 1.12.2](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.12.2)・[AGP 9.1.1](https://developer.android.com/build/releases/agp-9-1-0-release-notes)・[Gradleの互換表](https://docs.gradle.org/current/userguide/compatibility.html)・[Gradleのチェックサム](https://gradle.org/release-checksums/)

Wizardが生成した `gradle/gradle-daemon-jvm.properties` はAzulのJDK 21を指定していた。この設定はGradleデーモンのJDK選択で `JAVA_HOME` より優先される。調査用のプロジェクトでは `toolchainVersion=25` のみを指定し、ベンダーとダウンロード先の指定を外した。開発に使うJDKは次の２案を比較した。[Androidビルドで使うJDK](https://developer.android.com/build/jdks)・[GradleデーモンのJVM選択](https://docs.gradle.org/current/userguide/gradle_daemon.html)

| Gradleに使うJDK | 手元のバージョン | 選ぶ理由 |
| --- | --- | --- |
| Android Studio同梱のJBR | 25.0.4 | IDEと一緒に入っており、ローカルのセットアップを減らせる。 |
| SDKMANで導入するTemurin | `25.0.4-tem` | IDEの更新と独立してバージョンを指定できる。調査に使ったバージョンは導入済みだった。 |

ターミナルでは選んだJDKのディレクトリを `JAVA_HOME` に指定する。Xcodeのビルドフェーズから呼ぶ `gradlew` にもこの値が渡るよう、ターミナルから `JAVA_HOME` を指定して `xcodebuild` を実行する。[Androidビルドで使うJDK](https://developer.android.com/build/jdks)

Android StudioでDaemon JVM criteriaを使うプロジェクトでは、その条件が従来のGradle JDK設定に代わる。今回の試験ではIDEでのJDKの選択方法は未確認のため、手順3でGradle同期に使われるJDKを確かめる。[Android Studioのリリースノート](https://developer.android.com/studio/releases/past-releases/as-panda-1-release-notes#gradle_daemon_jvm_criteria)

両方のJDKが `toolchainVersion=25` を満たすため、`JAVA_HOME` を変えてもGradleは切り替え前のデーモンを再利用することがある。切り替えるときはビルドが終わってから、ビルドに使ったGradleユーザーホームで `./gradlew --stop` を実行する。JDKごとにGradleユーザーホームを用意する方法でも切り替えられる。切り替え後の最初のビルドではデーモンの起動ログを読み、`javaHome` と `javaVendor` が選んだJDKと一致することを確認する。[Gradleデーモンの停止とログ](https://docs.gradle.org/current/userguide/gradle_daemon.html)・[Gradle 9.5.1の再利用条件](https://github.com/gradle/gradle/blob/v9.5.1/platforms/core-runtime/daemon-protocol/src/main/java/org/gradle/launcher/daemon/context/DaemonCompatibilitySpec.java#L61)

Kotlin 2.4.21の互換表に記載されているXcodeは26.4だった。手元の27.2での組み合わせを確認するため、リポジトリ外のWizard生成例で、Androidの `:androidApp:assembleDebug` と、XcodeのiOSシミュレータ向けDebugビルドを試した。[Kotlinの互換表](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)

試験ではJDKごとにGradleユーザーホームを分け、各ビルドで使ったJDKをデーモンの起動ログで確認した。TemurinでAndroidのビルドに成功し、58タスクが実行された。続いて同じ試験プロジェクトでJBRを使い、AndroidのビルドとiOSシミュレータ向けのKotlinフレームワークのリンクに成功した。両方を合わせた集計では7タスクが実行され、56タスクが `UP-TO-DATE` だった。JBRでのAndroidビルドではTemurinで作った成果物を再利用した。

Xcode 27.2では両方のJDKでiOSシミュレータ向けのアプリを生成できた。リンク時にはサンプルのデプロイメントターゲット18.2に対し、フレームワーク内のICUのオブジェクトが18.5向けだったため警告が出た。iOSのデプロイメントターゲットはこの警告を踏まえて手順2で検討する。

公開テンプレートをhmc向けに調整した後のビルドと、指定のエミュレータ・シミュレータでの起動は後続の手順で確認する。

#### 開発時の標準JDK

開発時にGradleを動かすJDKは、Android Studio同梱のJBR 25を標準とする。開発に使うIDEと一緒に用意でき、環境の準備が少なくて済むためである。Gradleデーモンの条件には `toolchainVersion=25` のみを指定する。

Android Studioの更新で同梱JBRがJava 25の範囲でパッチ更新された場合は、更新後のJBRを使う。同梱JBRのJavaが新しいメジャーバージョンに切り替わった場合は、hmcのJDKを更新するまでSDKMANで導入したTemurin 25を使う。hmcで使うJavaのメジャーバージョンを上げるときは、Gradleとプラグインの互換性を確認し、両OS向けのビルドに成功してから切り替える。[GradleのJava互換表](https://docs.gradle.org/current/userguide/compatibility.html#java_runtime)

GitHub ActionsでCI環境を作るときにADRが必要かを改めて検討する。

#### 手順1の独立レビュー

うみの指摘３件を二人で採用した。

- 省く画像やアセットカタログの参照を削除する箇所を記した。取り込む設定に参照切れが残らないようにするため。
- ２つのJDKを切り替える手順と、デーモンで使ったJDKの確認方法を記した。切り替え前のデーモンを再利用する場合があるため。
- JBRの試験でAndroidの成果物を再利用したことと、各試験の実行条件を記した。試験で確かめた範囲を明確にするため。

再レビューでは、Android StudioでのJDKの指定方法について指摘があった。二人で採用し、ターミナルでの試験結果とIDEで未確認の内容を明記した。手順3にGradle同期時のJDKの確認を加えた。

うみは修正後の再レビューで、その指摘が解消したことを確認した。開発時の標準JDKを決めた後の追記もレビューし、追加の指摘はなかった。

#### 手順1で合意した初期構成

二人で調査とレビューの結果を確認し、候補に挙げた `Kotlin/KMP-App-Template` のコミットを採用することに合意した。テンプレート由来のファイルは取り込み候補の表に記載した範囲と条件に沿って扱う。

Android StudioとKotlin Multiplatformプラグイン、Xcode、Android SDKは調査で確認した手元の環境を使う。Kotlin、Compose Multiplatform、Gradle、Android Gradle Pluginには試験ビルドで使ったバージョンを採用し、開発時のJDKは[標準JDKの方針](#開発時の標準jdk)に従う。初期構成を作るときにAndroid SDKの場所を設定し、ターミナルからのビルドでは `JAVA_HOME` を選んだJDKに合わせる。
