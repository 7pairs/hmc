<!--
SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
SPDX-License-Identifier: Apache-2.0
Source: https://github.com/7pairs/hmc
-->

# 0004 GitHub Actionsで共通テストを実行する

- **ステータス** 完了
- **作業ブランチ** `work/0004-run-common-tests-in-ci`

作業は[開発プロセス](../development-process.md)と[SDD](../sdd/README.md)に従う。各手順の検証とレビューの結果を二人で確認し、次の手順に合意した上でオーナーから着手依頼を受ける。

## 手順チェックリスト

- [x] [1. CIの実行環境とテスト結果の確認方法を決める](#1-ciの実行環境とテスト結果の確認方法を決める)
- [x] [2. 共通テストを実行するワークフローを追加する](#2-共通テストを実行するワークフローを追加する)
- [x] [3. ドラフトPRで実行を確認し開発プロセスを更新する](#3-ドラフトprで実行を確認し開発プロセスを更新する)

## 目的と対象

`develop` 向けのPRで、半荘成績の共通テストをGitHub Actionsで実行する。PRの作成時と更新時に、文書だけを変更した場合も、1つのジョブで `./gradlew :shared:testAndroidHostTest` を実行する。この手順書に対応するドラフトPRで実行を確かめ、成功後にマージ前の確認事項を開発プロセスに明記する。（参照：[半荘成績のテスト項目](../specs/match-results.md#テスト項目)）

GitHubの設定でテストの成功を必須にする作業は、次のPRを進めるときに扱う。`main` 向けの昇格PRと `main` のマージコミットに対するCI、両OSのビルドと自動テストを含むリリース前のCIは、初回リリースに向けた作業で扱う。（参照：[開発プロセスの検証とCI](../development-process.md#検証とci)・[Sora Flowのマージ後の確認](../sora-flow/README.md#6-マージ後の確認とリリースの完了)）

## 手順

### 1. CIの実行環境とテスト結果の確認方法を決める

そらはGitHubホストの `ubuntu-24.04` とTemurin 25を候補に、現行のGradle WrapperとAndroid Gradle Pluginでホストテストを実行できる構成を調べる。現行の `compileSdk` は37のため、必要なAndroid SDKパッケージと入手方法を確認する。ランナーでの検証が必要な事項は、確認方法とともに手順3の検証項目に加える。（参照：[ランナーの仕様](https://docs.github.com/en/actions/reference/runners/github-hosted-runners)・[Android SDK Manager](https://developer.android.com/tools/sdkmanager)）

そらはランナー、JDK、SDKの準備方法、使用するアクション、Gradleキャッシュの扱いを案にまとめる。PRに表示するジョブ名、Gradleデーモンが実際に使ったJDKの確認方法、Actionsからテスト件数と結果を確認する方法も決める。ローカルでAndroid Studio同梱のJBR 25を標準とした経緯を参照し、CIで使うJDKの選択理由を作業記録に残す。開発環境とCIに共通する方針を決める場合は、ADRに残すかを二人で判断する。（参照：[手順書0002のJDK方針](0002-bootstrap-app.md#開発時の標準jdk)）

そらは案を現行のGradle設定と公式資料に照らす。うみはタスクと実行環境の組み合わせ、SDKの準備方法、実行結果を確認する方法をレビューする。二人で採用する構成と検証方法に合意する。

### 2. 共通テストを実行するワークフローを追加する

そらは合意した構成を `.github/workflows/` に追加する。`develop` 向けのPRで、ドラフトの段階から起動するように設定する。1つのジョブでJDKとSDKを用意し、Gradle Wrapperで共通テストを実行する。テスト失敗時はジョブも失敗とし、テストが実行された場合は手順1で決めた方法で結果を確認できるようにする。（参照：[GitHub ActionsのPR起動条件](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#pull_request)）

そらは差分を設計に照らし、起動対象、実行タスク、結果の確認方法、失敗時の扱いを確かめる。うみはワークフローが合意した条件で起動し、テストの結果を確認できる構成かをレビューする。二人で検証とレビューの結果を確認する。

### 3. ドラフトPRで実行を確認し開発プロセスを更新する

オーナーはワークフローを含むコミットをプッシュし、`develop` 向けのドラフトPRを作成する。そらはActionsのログとテスト結果を読み、Gradleデーモンが使ったJDK、準備したSDK、テストタスクが実際に実行されたことを確認する。現行の12件がすべて成功したことを二人で確かめ、対象コミット、実行リンク、結果を作業記録に残す。12件は今回の検証で照合する数とする。

初回の成功後、そらは開発プロセスの「develop向けPR」に、共通テストのジョブ名と[マージ前のCIの確認手順](#マージ前の確認)を記す。「検証とCI」にある通常PRのCIジョブを後で決める旨の一文は、「develop向けPR」の確認条件への参照に置き換える。手順1でADRを作ることに合意した場合は、実行結果を踏まえて判断理由を記す。（参照：[develop向けPR](../development-process.md#develop向けpr)・[検証とCI](../development-process.md#検証とci)）

二人でワークフローと既存のGradle設定の構成を見直し、整理の要否と理由を記録する。設定を変更した場合は関係する検証を再実行する。そらはPRの起動条件からテスト結果を確認するまでを説明し、オーナーはマージ前にどのジョブのどの実行結果を見るかを自分の言葉で説明する。うみは検証結果、開発プロセスの変更、構成を見直した判断を確認し、ワークフローを修正した場合はその差分もレビューする。二人で検証とレビューの結果を確認する。

## 完了条件

`develop` 向けのドラフトPRで共通テストのジョブが実行され、現行の12件がすべて成功したことを確認する。開発プロセスにジョブ名と確認条件を記し、構成の見直し、理解の確認、必要なレビューと再レビュー、作業記録の更新が済んだことを二人で確認する。

## マージ前の確認

手順書を完了にして記録を反映したあと、オーナーはドラフトPRをレビュー可能にする。そらはCIがチェックアウトした一時マージコミットのSHAを、マージ直前のPRの一時マージコミットのSHAと照合し、一致する実行で共通テストが成功したことを確認する。`develop` の更新などでSHAが変わり、対応する実行がなければ、オーナーがPRブランチを更新し、そらが新しい実行のSHAと結果を確認する。（参照：[PRイベントの仕様](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#pull_request)・[再実行時のコミット](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/re-run-workflows-and-jobs)）

そらはマージ予定の最新差分に対するCodeRabbitのレビューが完了し、未解決のレビュー会話がないことも確かめる。CIとレビューの確認結果をオーナーに伝え、オーナーはその結果を受けてマージする。（参照：[develop向けPRのマージ条件](../development-process.md#develop向けpr)・[CodeRabbitの自動レビュー設定](https://docs.coderabbit.ai/pr-reviews/when-reviews-run#how-coderabbit-decides)）

## 作業記録

各手順の実施結果、検証とレビューの結果またはその参照先、予定から変わった点と理由、残した課題を記録する。うみの指摘は要点、採否と理由、再レビューの結果を残す。

### 草稿のレビュー

うみの指摘2件を二人で採用した。

- 開発プロセスの「検証とCI」から「develop向けPR」の確認条件を参照するよう、手順3に記した。ジョブを決めたあとも「後で決める」という記述が残るのを防ぐため。
- マージ前に最新差分のCIとCodeRabbitの結果を確認してオーナーに伝え、オーナーがマージする流れを明記した。マージする差分と検証・レビューを受けた差分をそろえるため。

うみは再レビューで2件とも解消したことを確認した。追加の指摘はなかった。

### 手順1の作業記録

#### 実行環境の調査

現行の構成はGradle Wrapper 9.5.1、Android Gradle Plugin 9.1.1、`compileSdk = 37` で、Gradleデーモンの条件はJava 25である。Android Gradle Plugin 9.1.1の対応上限はAndroid API 37.0で、標準のBuild Toolsは36.0.0。調査時の `ubuntu-24.04` の公開一覧には、必要な `android-37.0` とBuild Tools 36.0.0が載っていた。（参照：[AGP 9.1.1の互換性](https://developer.android.com/build/releases/agp-9-1-0-release-notes#compatibility)・[Ubuntu 24.04のインストール済みソフトウェア](https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2404-Readme.md#android)）

コミット `b64fdd2` の一時コピーをmacOSで用意し、`local.properties` のない状態で `ANDROID_HOME` に手元のSDK、`JAVA_HOME` にTemurin 25.0.4を指定した。`./gradlew :shared:testAndroidHostTest --no-daemon --console=plain --info --no-build-cache --no-configuration-cache` を実行した。Gradleのログに `compileSdkVersion=android-37.0` が出て、デーモンのログでJava 25と `javaVendor=Eclipse Adoptium` を確認した。XMLのテスト結果は12件実行、失敗・エラー・スキップは0件だった。依存関係には既存のGradleユーザーキャッシュを使った。

#### ワークフローの案

- `.github/workflows/common-tests.yml` にワークフロー `Common tests` とジョブ `Android host tests` を置く。起動条件は `pull_request` の宛先が `develop` であることとする。ドラフトPRと文書のみの変更も対象にする。（参照：[GitHub ActionsのPR起動条件](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#pull_request)）
- `ubuntu-24.04` 上で `actions/setup-java` に `distribution: temurin` と `java-version: '25'` を指定する。Temurinは手順書0002でローカルの代替として確認済みで、アクションで配布元を指定できる。ローカルでは手順書0002のJBR 25の方針を適用する。ローカルとCIでJavaのメジャーバージョンをそろえ、CIで使う配布元とその理由はこの手順書に残す。今回はADRの作成を見送る案とする。（参照：[Setup Java](https://github.com/actions/setup-java#install-eclipse-temurin)・[手順書0002のJDK方針](0002-bootstrap-app.md#開発時の標準jdk)）
- ランナーの `ANDROID_HOME` に `platforms;android-37.0` と `build-tools;36.0.0` があることを確かめ、不足分は同じSDKの `cmdline-tools/latest/bin/sdkmanager` で用意する。使用したパッケージがログで分かるようにする。（参照：[ランナーのSDK準備スクリプト](https://github.com/actions/runner-images/blob/main/images/ubuntu/scripts/build/install-android-sdk.sh)・[sdkmanager](https://developer.android.com/tools/sdkmanager)）
- PRの一時マージコミットをチェックアウトし、そのSHAをログに出してから `./gradlew :shared:testAndroidHostTest --no-daemon --console=plain --info` を実行する。Gradleデーモンの起動行にある `java` のパスを、JDKセットアップ後の `JAVA_HOME` と照合する。Gradleの終了ステータスをジョブに反映する。初回は実行間で再利用するGradleキャッシュを設定せず、Actionsで実行時間を測る。（参照：[PRで使うコミット](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#pull_request)・[Gradleデーモンが使うJVM](https://docs.gradle.org/current/userguide/gradle_daemon.html#sec:daemon_jvm_criteria)）
- `shared/build/test-results/testAndroidHostTest/` のXMLと `shared/build/reports/tests/testAndroidHostTest/` のHTMLを `android-host-test-results` というアーティファクトに保存する。Gradleが成功した場合はXMLとHTMLの両方が生成されたことを確認してから保存し、どちらかが欠けていればジョブを失敗とする。テストが失敗した場合も、ジョブがキャンセルされていなければ生成済みのレポートを保存する。実行ページから取得したレポートで、実行件数、失敗、エラー、スキップを確認する。（参照：[Upload Artifact](https://github.com/actions/upload-artifact#usage)・[アーティファクトの取得](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts)）

アクションの参照は公式リリースのコミットに固定し、バージョンをコメントに記す。ワークフローの権限は `contents: read` とし、チェックアウト時に資格情報を保存しない設定を使う。（参照：[GitHub Actionsの安全な利用](https://docs.github.com/en/actions/reference/security/secure-use)）

| アクション | リリース | コミット |
| --- | --- | --- |
| `actions/checkout` | [v7.0.1](https://github.com/actions/checkout/releases/tag/v7.0.1) | `3d3c42e5aac5ba805825da76410c181273ba90b1` |
| `actions/setup-java` | [v6.3.0](https://github.com/actions/setup-java/releases/tag/v6.3.0) | `d1374f8ac74474200d16ef8ecee22c7ae7a51880` |
| `actions/upload-artifact` | [v7.0.2](https://github.com/actions/upload-artifact/releases/tag/v7.0.2) | `cf430e030ddbb5b0abf93d22962f4752f3646cd9` |

#### ドラフトPRで確認すること

手順3ではドラフトPRの作成をきっかけにジョブが起動すること、実際のランナーイメージ、JDKとSDK、テストの実行とレポートの取得を確認する。12件すべてが成功したことをレポートで確かめ、実行リンク、PRのヘッドコミット、テストに使った一時マージコミット、ジョブ名と実行時間を記録する。完了記録を反映したあとは[マージ前の確認](#マージ前の確認)に従う。

#### 独立レビュー

うみの指摘2件を二人で採用した。

- マージ直前のPRの一時マージコミットを、CIで検証したコミットと照合する手順を記した。`develop` だけが進んだ場合も、マージする組み合わせでテストが成功したことを確かめるため。
- Gradleが成功した場合はXMLとHTMLの両方が揃っていることを確かめる案とした。ジョブが成功したのにテスト結果を確認できない事態を防ぐため。

うみは再レビューで、CIが検証したコミットの照合とレポートの確認条件が案に反映され、2件とも解消したことを確認した。追加の指摘はなかった。

二人で調査と再レビューの結果を確認し、ワークフローの構成とドラフトPRでの検証方法に合意して手順1を完了とした。CIでTemurin 25を使う理由はこの手順書に記録し、ADRの作成は見送る。

### 手順2の作業記録

[共通テストのワークフロー](../../.github/workflows/common-tests.yml)を追加した。`actionlint` の検査は通過し、各アクションの固定SHAは公式リリースのタグと一致した。

そらはmacOS上でTemurin 25.0.4を使ってワークフローと同じGradleコマンドを実行し、12件すべてが成功してXMLとHTMLのレポートが生成されたことを確認した。仮のSDKと `sdkmanager` を使い、不足パッケージだけがインストール対象に選ばれることを確かめた。テスト用のレポートでは、XMLとHTMLがそろえば確認が成功し、片方でも欠ければ失敗することを確かめた。

うみは起動条件、実行環境、レポートの保存条件などを手順1の合意と照合した。修正が必要な指摘はなかった。二人で検証とレビューの結果を確認し、手順2を完了とした。

### 手順3の作業記録

#### ドラフトPRでのCIの検証

`develop` 向けのドラフトPR #7を作成すると、`Common tests` の `Android host tests` が起動した。初回はGradleがMaven Centralから依存関係を取得できず、テスト実行前に失敗した。失敗時のログで `Resource missing` と表示されたPOMのうち、GsonとSLF4JのURLは調査時にHTTP 200を返した。取得に失敗した原因は特定できていない。（参照：[ドラフトPR #7](https://github.com/7pairs/hmc/pull/7)・[初回のジョブ](https://github.com/7pairs/hmc/actions/runs/44709329166/job/136196712317)）

設定を変えずに同じジョブを再実行すると、約1分33秒で成功した。PRのヘッドは `498fe8ea9161cd3250edb88079b1131bbf2e0934`。ログに出た一時マージコミット `33c7dc6ea3a8628f8b994f805e3adb767db1a6dd` は、確認時点のPRの一時マージコミットと一致した。（参照：[再実行のジョブ](https://github.com/7pairs/hmc/actions/runs/44709329166/job/136201725706)）

使用したランナーイメージは `ubuntu-24.04` の `20261111.370` だった。ログでSDKの `android-37.0` と Build Tools `36.0.0` を確認した。Gradleデーモンが使ったのは、ランナーにインストール済みの Temurin 25.0.5（`/usr/lib/jvm/temurin-25-jdk-amd64/bin/java`）だった。`setup-java` が設定した `JAVA_HOME`（`/opt/hostedtoolcache/Java_Temurin-Hotspot_jdk/25.0.5-8/x64`）とはパスが異なるが、デーモンが使ったJDKの配布元とメジャーバージョンは手順1の合意どおりだった。（参照：[ランナーイメージの情報](https://github.com/actions/runner-images/blob/ubuntu24/20261111.370/images/ubuntu/Ubuntu2404-Readme.md)・[再実行のログ](https://github.com/7pairs/hmc/actions/runs/44709329166/job/136201725706)）

ログで `:shared:testAndroidHostTest` の実行を確認した。`android-host-test-results` をダウンロードし、XMLとHTMLの両方があり、12件すべてが成功して失敗・エラー・スキップが各0件だったことを確かめた。（参照：[テスト結果のアーティファクト](https://github.com/7pairs/hmc/actions/runs/44709329166/artifacts/14617068113)）

#### 構成の見直し

二人はワークフローとGradle設定の整理を見送った。1つのジョブで環境の準備、テストの実行、レポートの保存を順に追えるため、処理を別のスクリプトに切り出す必要はないと判断した。Gradleデーモンは手順1で合意したTemurin 25を使っていた。今後 `compileSdk` を上げるときは、ワークフローで準備するSDKパッケージも見直す。

#### 理解の確認

そらは、ドラフトを含む `develop` 向けPRの作成や更新でジョブが起動し、PRの一時マージコミットで共通テストを実行した結果をActionsの実行ページにある `android-host-test-results` のXMLとHTMLで確認できることを説明した。

CIが成功した後に `develop` に別のPRがマージされた場合を題材に、小テストを行った。オーナーは `Common tests` の `Android host tests` を対象に、ログのSHAを照合して、以前に成功した実行がマージ前の確認に使えるか判断すると説明した。使える実行がなければ `develop` の変更をPRブランチに取り込み、新しい実行が成功したこととSHAが一致することを確認すると説明した。

そらは、照合に使うSHAが `Run Android host tests` ステップのログの `Checked-out commit` にあり、照合相手はマージ直前にGitHubがPR用に作る一時マージコミットのSHAだと補足した。小テストでは、オーナーが確認するジョブと、対応する実行がない場合の手順を自分の言葉で説明できたことを確認した。

#### 独立レビューと完了

うみはドラフトPRのCIの検証結果、開発プロセスの変更、構成の整理を見送った判断、小テストの記録を確認した。修正が必要な指摘はなかった。起動条件からレポートの確認先までそらが説明したことも記録するよう提案があり、二人で採用した。

二人で検証とレビューの結果を確認し、手順3と手順書全体を完了とした。
