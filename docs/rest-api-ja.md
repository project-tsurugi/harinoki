# REST API 仕様

## この文書について

* 認証サービスの REST API に関する仕様
* やり取りされるトークンについては [認証トークン仕様](token-ja.md) に記載する

## 用語

* access token (AT)
  * 認可情報を取得するために必要な認証トークン
* refresh token (RT)
  * AT を発行するために必要な認証トークン

## REST API

[IssueServlet]:../src/main/java/com/tsurugidb/harinoki/IssueServlet.java
[RefreshServlet]:../src/main/java/com/tsurugidb/harinoki/RefreshServlet.java
[VerifyServlet]:../src/main/java/com/tsurugidb/harinoki/VerifyServlet.java

### `issue`

* 概要
  * 認証済みユーザー情報を受け取り、RTを発行する
    * 認証はアプリケーションサーバーの機能を利用して行う
* リクエスト
  * パス: `/issue`
  * メソッド: `GET`
  * コンテナ認証: 必要
    * メソッド: BASIC
    * realm : `harinoki`
    * ロール名: `harinoki-user`
  * サーブレット: [IssueServlet]
* 正常系 (成功)
  * ステータスコード: 200
  * `Content-Type: application/json`
  * ボディ (JSON)
    * `token` - RT
    * `type` - `ok`
    * `message` - メッセージ (optional)
* 異常系 (認証エラー)
  * (アプリケーションサーバーの挙動に従う)

### `refresh`

* 概要
  * RTを受け取り、ATを発行する
* リクエスト
  * パス: `/refresh`
  * メソッド: `GET`
  * パラメータ
    * `Authorization: Bearer {RT}`
    * `X-Harinoki-Token-Expiration: <max expiration time in seconds>` (optional)
  * コンテナ認証: 不要
  * サーブレット: [RefreshServlet]
* 正常系 (成功)
  * ステータスコード: 200
  * `Content-Type: application/json`
  * ボディ (JSON)
    * `token` - AT
    * `message` - メッセージ (optional)
* 異常系 (トークン未指定)
  * ステータスコード: 401
  * ボディ (JSON)
    * `token` - N/A
    * `type` - `no_token`
    * `message` - メッセージ (optional)
* 異常系 (トークン期限切れ)
  * ステータスコード: 401
  * ボディ (JSON)
    * `token` - N/A
    * `type` - `token_expired`
    * `message` - メッセージ (optional)
* 異常系 (RTでないトークン)
  * ステータスコード: 401
  * ボディ (JSON)
    * `token` - N/A
    * `type` - `invalid_audience`
    * `message` - メッセージ (optional)
* 異常系 (不正なトークン)
  * ステータスコード: 401
  * ボディ (JSON)
    * `token` - N/A
    * `type` - `invalid_token`
    * `message` - メッセージ (optional)

### `verify`

* 概要
  * AT または RT を受け取り、発行者や署名等が妥当なものであるかどうかを検証する
    * ユーザー名、有効期限等の検証は行わない
* リクエスト
  * パス: `/verify`
  * メソッド: `GET`
  * パラメータ
    * `Authorization: Bearer {AT/RT}`
  * コンテナ認証: 不要
  * サーブレット: [VerifyServlet]
* 正常系 (成功)
  * ステータスコード: 200
  * `Content-Type: application/json`
  * ボディ (JSON)
    * `token` - 入力したトークン
    * `message` - メッセージ (optional)
* 異常系 (トークン未指定)
  * ステータスコード: 401
    * `token` - N/A
    * `type` - `no_token`
    * `message` - メッセージ (optional)
* 異常系 (不正なトークン)
  * ステータスコード: 401
  * ボディ (JSON)
    * `token` - N/A
    * `type` - `invalid_token`
    * `message` - メッセージ (optional)

## 参考文献

* [RFC 6750: OAuth 2.0 Bearer Token Usage](https://oauth.net/2/bearer-tokens/) - `Authorization: Bearer {token}` の仕様
