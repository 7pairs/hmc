<!--
SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
SPDX-License-Identifier: Apache-2.0
Source: https://github.com/7pairs/hmc
-->

# 0007 iOS実機用の署名チームをローカルで設定できるようにする

- **ステータス** 完了
- **作業ブランチ** `work/0007-configure-ios-device-signing`

作業は[開発プロセス](../development-process.md)と[SDD](../sdd/README.md)に従う。各手順の検証とレビューの結果を二人で確認し、次の手順に合意した上でオーナーから着手依頼を受ける。

## 手順チェックリスト

- [x] [1. iOS実機用の署名チームをローカル設定から読み込む](#1-ios実機用の署名チームをローカル設定から読み込む)
- [x] [2. Android StudioからiOS実機とシミュレータで起動を確認する](#2-android-studioからios実機とシミュレータで起動を確認する)

## 目的と対象

Android StudioからiPhone実機にDebugビルドを転送して起動するために、署名用のチームIDを開発者のローカルファイルに設定できるようにする。配布用の署名は今回の対象外とする。前回はAndroid Studioで署名チームを選んだときに `project.pbxproj` にチームIDが書き込まれ、ファイル先頭のライセンスと出典のコメントが消えた。今回はAndroid Studioで実行を試すたびに、同ファイルの差分を確認する。（参照：[前回の実機署名の記録](0005-enter-match-scores.md#ios実機用の署名設定)）

## 手順

### 1. iOS実機用の署名チームをローカル設定から読み込む

そらは `iosApp/Configuration/Config.xcconfig` の空の `TEAM_ID` のあとに `#include? "Local.xcconfig"` を追加する。`iosApp/Configuration/Local.xcconfig` を `.gitignore` に追加する。`Config.xcconfig` のコメントにはローカルファイル名、下の条件付きの記入例、詳しい手順を記した手順書0007への参照を載せる。`project.pbxproj` の自動署名と `DEVELOPMENT_TEAM` から `TEAM_ID` を参照する設定を使う。（参照：[Appleのビルド設定ファイルの説明](https://developer.apple.com/documentation/xcode/adding-a-build-configuration-file-to-your-project)）

オーナーは実機で検証するときに `Config.xcconfig` と同じディレクトリに `Local.xcconfig` を作る。次の `YOUR_TEAM_ID` を自分のチームIDに置き換えて、そのローカルファイルに記入する。

```xcconfig
TEAM_ID[config=Debug][sdk=iphoneos*] = YOUR_TEAM_ID
```

そらは `git check-ignore` で `Local.xcconfig` が除外されることを確認する。検証用にコピーしたプロジェクトで仮のチームIDを使い、`xcodebuild -showBuildSettings` が示すアプリターゲットの `DEVELOPMENT_TEAM` を次の条件で確かめる。

| ローカルファイル | ビルド設定とSDK | `DEVELOPMENT_TEAM` の期待値 |
| --- | --- | --- |
| なし | Debug、`iphoneos` | 空 |
| なし | Debug、`iphonesimulator` | 空 |
| あり | Debug、`iphoneos` | 仮のチームID |
| あり | Debug、`iphonesimulator` | 空 |
| あり | Release、`iphoneos` | 空 |

そらは共有設定からローカル設定を読み込む構成と署名への影響を見直し、追加の整理が必要かを二人で判断して理由を記録する。うみは設定の差分と検証結果を読み、ローカルファイルの除外、チームIDが効く範囲、構成を見直した判断を独立に確認する。二人で検証とレビューの結果を確認する。

### 2. Android StudioからiOS実機とシミュレータで起動を確認する

そらは各Runの前に `git diff HEAD -- iosApp/iosApp.xcodeproj/project.pbxproj` の出力が空であることを確認する。オーナーは `Local.xcconfig` がない状態でAndroid StudioからiOSシミュレータを使ってDebugビルドを実行し、hmcの素点入力画面が開くことを確かめる。次に `Local.xcconfig` にチームIDを設定し、同じIDEからiPhone実機にDebugビルドを転送して素点入力画面が開くことを確かめる。

署名チームの選択画面が出たら、オーナーはチームを選ばずにそのRunを中断する。そらは成功・失敗・中断のどの場合もRunの直後に同じコマンドで差分を確認する。選択画面が出た場合や `project.pbxproj` に差分が出た場合は、次のRunに進む前に二人で原因を調べる。端末、OS、Android Studioのバージョン、各Runの結果と差分の確認結果を記録する。

うみは検証記録を読み、ローカル設定の有無、起動結果、各Runの前後の `project.pbxproj` の確認結果が完了条件を満たすかを確認する。二人で検証とレビューの結果を確認する。オーナーはチームIDをどこに設定し、どのビルドで使われるか、各Runの直後に何を確認すれば共有設定が保たれたと判断できるかを自分の言葉で説明する。

## 完了条件

Android Studioから、ローカル設定なしでシミュレータ、設定ありでiPhone実機にDebugビルドを転送して起動できる。どちらの起動後も `project.pbxproj` に差分がなく、実際のチームIDを含むファイルはGitの管理外にある。

両手順の検証、独立レビュー、必要な再レビューが済み、設定の構成を見直した判断とオーナーの理解を二人で確認する。マージ前は開発プロセスに従い、マージ予定の差分に対するCodeRabbitのレビューと、対応する一時マージコミットでの `Android host tests` の成功を確認する。（参照：[develop向けPRの確認条件](../development-process.md#develop向けpr)）

## 作業記録

各手順の実施結果、検証・レビューの結果、予定から変わった点と理由、残した課題を記録する。

### 草稿のレビュー

うみの指摘2件と任意の提案を二人で採用した。起動に失敗したときや途中で中断したときも書き換えを検出できるよう、各Runの直後に差分を確認する手順にした。設定をDebugの実機ビルドに絞れるよう、共有設定のコメントに条件付きの記入例と手順書への参照を載せることにした。実機SDKでも設定ファイルがないときの既定値を確かめる項目を加え、配布用の署名は対象外と明記した。

うみは草稿の再レビューで、共有設定のコメントに載せる内容と、成功・失敗・中断を問わず各Runの直後に差分を確認する手順が明記され、指摘2件が解消したことを確認した。任意の提案も反映され、地の文のRunの前後から半角スペースを削除した8か所では手順の意味が保たれていた。追加の指摘はなかった。

### 手順1の作業記録

そらは `Config.xcconfig` に任意のローカルファイルを読み込む設定と記入例を追加した。`git check-ignore -v -- iosApp/Configuration/Local.xcconfig` を実行し、このパスがリポジトリの `.gitignore` で除外されることを確認した。

Xcode 27.2（27B52）でプロジェクトのコピーに対して `xcodebuild -showBuildSettings` を実行し、アプリターゲットの `DEVELOPMENT_TEAM` を比較した。ローカルファイルがないときは実機向けとシミュレータ向けのDebugで空だった。仮のチームIDを記入したときは実機向けのDebugだけでその値が入り、シミュレータ向けのDebugと実機向けのReleaseでは空だった。元の作業ツリーに `Local.xcconfig` がなく、`project.pbxproj` に差分がないことも確認した。

既存の自動署名では `DEVELOPMENT_TEAM` が `TEAM_ID` を参照する。ローカルファイルを任意で読み込み、条件付きの設定で実機向けDebugにだけ値を渡せることを確認した。この構成で必要な範囲に設定を適用できるため、二人で追加の構成変更を見送ることにした。

うみは独立レビューで設定の差分と保存済みのXcodeの出力5件を照合した。ローカルファイル用のパスがGitで除外され、仮のチームIDが実機向けDebugにだけ適用されたことを確認した。追加の構成変更を見送る判断は妥当で、指摘はなかった。二人で検証とレビューの結果を確認し、手順1を完了とした。

### 手順2の作業記録

オーナーはコミット `a9f7dfd` の作業ツリーでAndroid Studio 2026.2（ビルド262.10968.63.2622.16975389）からDebugビルドを実行した。そらはシミュレータのRun前後の差分確認に `git diff HEAD -- iosApp/iosApp.xcodeproj/project.pbxproj` を使った。

`Local.xcconfig` がない状態で、iPhone 18 Proシミュレータ（iOS 27.2）に素点入力画面が開いた。そらはスクリーンショットで画面を確認し、Runの前後で `project.pbxproj` に差分がないことを確かめた。

オーナーは `Local.xcconfig` を作成し、iPhone 16実機（iOS 27.2.1）で素点入力画面が開くことを確認した。初回はローカルファイルの作成後とRunの間に差分を確認していなかった。そらは初回のRun後に差分が空であることを確かめ、その確認を次のRunの事前確認とした。オーナーが実機で再実行し、そらはスクリーンショットで素点入力画面を確認した。再実行の直後に `git diff --exit-code --name-only HEAD -- iosApp/iosApp.xcodeproj/project.pbxproj` を実行し、出力なし、終了コード0で差分がないことを確認した。`git check-ignore -v` で `Local.xcconfig` がGitの除外対象であることも確かめた。

うみは独立レビューで、実機の再実行でRun前後の差分を確認できていると判断した。再実行後の差分確認に使ったコマンドのオプションが記録と異なるとの指摘を受け、実際に使ったコマンドと結果を照合できるよう、二人で指摘を採用した。

うみは再レビューで差分確認に使ったコマンドと、実機再実行後の出力なし・終了コード0の記録が確認時の結果と一致することを確かめた。指摘は解消し、追加の指摘はなかった。

そらは `Local.xcconfig` に記入したチームIDが、実機向けDebugで `TEAM_ID` を経由して自動署名の `DEVELOPMENT_TEAM` に渡る仕組みを説明した。Runの前後に `project.pbxproj` の差分を確認する理由も説明した。

小テストでオーナーはチームIDの記入先、Debugと実機SDKの両方の条件が揃ったときだけ設定が効くこと、Run直後に `project.pbxproj` に差分がないことを確かめる理由を自分の言葉で説明した。そらは `iphoneos*` の条件について、SDK名が `iphoneos` で始まるときに一致すると補足した。オーナーは3点とも説明できたため、そらは合格とした。

うみは回答と作業記録を照合し、オーナーが3点を理解していることを確認した。SDK名の条件はそらの補足で十分であり、作業記録は回答と補足を正確に反映していた。追加の指摘はなかった。

二人で起動検証とレビューの結果、オーナーの理解を確認し、手順2を完了とした。手順書全体の完了条件も満たしたため、ステータスを「完了」にした。
