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

| コード | 概要 | 設定値 |
|:--|:--|:--|
| `iss` | トークン発行者 | 環境変数 `TSURUGI_JWT_CLAIM_ISS` に指定した値 |
| `sub` | トークン用途 | `access`, `refresh` のいずれか |
| `aud` | トークン受信者 | 環境変数 `TSURUGI_JWT_CLAIM_AUD` に指定した値 |
| `iat` | トークン発行日時 | トークン発行時刻 |
| `exp` | トークン有効期限 | トークン発行時刻 + 有効期限 (*1) |
| `tsurugi/auth/name` | 認証済みユーザ名 | ログインユーザ名 |

----
(*1): 有効期限はトークンの種類ごとに異なる

* AT : 環境変数 `TSURUGI_TOKEN_EXPIRATION` に指定した値
* RT : 環境変数 `TSURUGI_TOKEN_EXPIRATION_REFRESH` に指定した値

### 署名

* 暗号化アルゴリズム
  * `HS256`
* 署名鍵
  * 環境変数 `TSURUGI_JWT_SECRET_KEY` に指定された文字列

## 設定情報一覧

それぞれの設定は、環境変数で指定する。

| 環境変数名 | 設定内容 | 既定値 |
|:--|:--|:-:|
| `TSURUGI_JWT_CLAIM_ISS` | トークンの発行者名 | `authentication-manager` |
| `TSURUGI_JWT_CLAIM_AUD` | トークンの受信者 | `metadata-manager` |
| `TSURUGI_JWT_SECRET_KEY` | 署名鍵 | (N/A) |
| `TSURUGI_TOKEN_EXPIRATION` | RTの有効期限 (*1) | `300s` |
| `TSURUGI_TOKEN_EXPIRATION_REFRESH` | ATの有効期限 (*2) | `24h` |

----
(*2): 以下の構文内の `<period>` を利用可能

```bnf
 <period> ::= <integer> <unit>?
<integer> ::= "0" | [1-9][0-9]
   <unit> ::= "h" | "min" | "s"
```

単位が未指定の場合は秒 (`s`) として扱う

## 参考文献

* [JSON Web Tokens](https://jwt.io/)
* [認証管理基盤 API仕様書](https://github.com/project-tsurugi/manager/blob/master/authentication-manager/docs/authentication_API_specification.md)
