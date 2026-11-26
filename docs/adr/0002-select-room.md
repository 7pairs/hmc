<!--
SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
SPDX-License-Identifier: Apache-2.0
Source: https://github.com/7pairs/hmc
-->

# 0002 端末内DBにRoomを採用する

- **ステータス** 採用

## 背景

最初の実戦試用から半荘の記録を端末に保存し、後のバージョンに引き継ぐ。選手、ルール、リーグの一覧・登録・更新、リーグ詳細での成績閲覧、半荘結果の登録・更新までを想定して候補を比較した。OS固有のコードと設定は、できるだけDBを開く処理にまとめたい。（参照：[プロジェクト憲章](../project-charter.md#対象と優先順位)）

## 決定と適用範囲

Room 2.8.5と `BundledSQLiteDriver`（AndroidX SQLite 2.7.1）を使う。スキーマ、DAO、トランザクション、マイグレーションは共通Kotlinに置く。AndroidとiOSはそれぞれ保存先を決めてDBのビルダーを作る。Roomのスキーマを出力して履歴を残し、実戦記録を引き継ぐスキーマ変更にはマイグレーションを用意する。（参照：[手順書0009](../procedures/0009-save-matches.md#3-保存の設計を決めてテスト項目をまとめる)）

## 採用理由

RoomとSQLDelightの検証用アプリで、両OSの読み書き、書き込み途中のロールバック、旧DBのマイグレーションを確認した。Roomでは両OSで同じSQLite 3.50.1を使えた。プレリリースから実戦記録を引き継ぐため、両OSでSQLiteの挙動をそろえやすいことを優先した。

選手、ルール、リーグの登録・更新には `@Insert` や `@Update` を使えるため、定型的なSQLを減らせる。所属選手の取得などの結合クエリはDAOにSQLで記述できる。RoomではターゲットごとのKSP設定が必要になり、SQLiteを同梱する分だけアプリの容量が増える。（参照：[RoomのKotlin Multiplatform導入例](https://developer.android.com/codelabs/kmp-migrate-room)）

## 見送った候補と見直す条件

SQLDelight 2.4.1ではスキーマとクエリを `.sq`、マイグレーションを `.sqm` にまとめられる。検証で手書きしたDBの起動コードは短かった。hmcのiOS静的フレームワークではXcode側のSQLiteリンク設定が必要で、今回のドライバー構成では実行時のSQLiteがAndroidで3.50.6、iOSで3.43.2だった。定型的な書き込みのSQLを減らせることと、両OSのSQLiteをそろえられることを優先してRoomを選んだ。（参照：[SQLDelightのKotlin Multiplatform導入例](https://sqldelight.github.io/sqldelight/latest/multiplatform_sqlite/)）

今後、対応OSやビルド構成でRoomを安定して使えなくなった場合や、SQLiteの同梱が配布上の負担になった場合は、構成を見直す。
