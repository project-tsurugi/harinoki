# 認証トークン仕様

## この文書について

* 認証サービスが利用する、認証トークンの形式に関する仕様
  * 認証管理基盤と互換性のある認証トークンを発行する

## 用語

* access token (AT)
  * 認可情報を取得するために必要な認証トークン
* refresh token (RT)
  * AT を発行するために必要な認証トークン

## トークンの形式

* それぞれの認証トークンは、 JSON Web Tokens (JWT) の形式
  * 各パートの形式は、次項以降に記載

### ペイロード

| コード | 概要 | AT 設定値 | RT 設定値 |
|:--|:--|:--|:--|
| `iss` | トークン発行者 | 設定変数 `tsurugi.jwt.claim_iss` の値 | 設定変数 `tsurugi.jwt.claim_iss` の値 |
| `sub` | トークン用途 | 文字列 `access` | 文字列 `refresh` |
| `aud` | トークン受信者 | 設定変数 `tsurugi.jwt.claim_aud` の値 | 設定変数 `tsurugi.jwt.claim_iss` の値 |
| `iat` | トークン発行日時 | トークン発行時刻 | トークン発行時刻 |
| `exp` | トークン有効期限 | トークン発行時刻 + 設定変数 `tsurugi.token.expiration` の値 | トークン発行時刻 + 設定変数 `tsurugi.token.expiration_refresh` の値 |
| `tsurugi/auth/name` | 認証済みユーザ名 | ログインユーザ名 | ログインユーザ名 |

### 署名

* 暗号化アルゴリズム
  * `RS256`
* 署名鍵
  * PEM形式のRSA秘密鍵

## 設定情報一覧

それぞれの設定は、propertiesファイルに記述する以下の設定変数で指定する。

| 設定変数名 | 設定内容 | 既定値 |
|:--|:--|:-:|
| `tsurugi.jwt.claim_iss` | トークンの発行者名 | `authentication-manager` |
| `tsurugi.jwt.claim_aud` | トークンの受信者 | `metadata-manager` |
| `tsurugi.jwt.private_key_file` | RSA鍵を格納したファイル名 | `jwt.pem` |
| `tsurugi.token.expiration` | ATの有効期限 (*1) | `300s` |
| `tsurugi.token.expiration_refresh` | RTの有効期限 (*1) | `24h` |

----
(*1): 以下の構文内の `<period>` を利用可能

```bnf
 <period> ::= <integer> <unit>?
<integer> ::= "0" | [1-9][0-9]
   <unit> ::= "h" | "min" | "s"
```

単位が未指定の場合は秒 (`s`) として扱う

## 参考文献

* [JSON Web Tokens](https://jwt.io/)
* [認証管理基盤 API仕様書 C++版](https://github.com/project-tsurugi/authentication-manager/blob/master/docs/authentication_API_specification_cpp.md)
