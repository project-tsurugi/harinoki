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
| `iss` | トークン発行者 | 環境変数 `TSURUGI_JWT_CLAIM_ISS` の値 | 環境変数 `TSURUGI_JWT_CLAIM_ISS` の値 |
| `sub` | トークン用途 | 文字列 `access` | 文字列 `refresh` |
| `aud` | トークン受信者 | 環境変数 `TSURUGI_JWT_CLAIM_AUD` の値 | 環境変数 `TSURUGI_JWT_CLAIM_ISS` の値 |
| `iat` | トークン発行日時 | トークン発行時刻 | トークン発行時刻 |
| `exp` | トークン有効期限 | トークン発行時刻 + 環境変数 `TSURUGI_TOKEN_EXPIRATION` の値 | トークン発行時刻 + 環境変数 `TSURUGI_TOKEN_EXPIRATION_REFRESH` の値 |
| `tsurugi/auth/name` | 認証済みユーザ名 | ログインユーザ名 | ログインユーザ名 |

上記以外にも、認証サービス内部で利用する値を設定する場合がある。

### 署名

* 暗号化アルゴリズム
  * `RS256`
* 署名鍵
  * `TSURUGI_JWT_PRIVATE_KEY` で指定されるファイルに格納されたRSA秘密鍵（PEM形式）

## 設定情報一覧

それぞれの設定は、環境変数で指定する。

| 環境変数名 | 設定内容 | 既定値 |
|:--|:--|:-:|
| `TSURUGI_JWT_CLAIM_ISS` | トークンの発行者名 | `authentication-manager` |
| `TSURUGI_JWT_CLAIM_AUD` | トークンの受信者 | `metadata-manager` |
| `TSURUGI_JWT_PRIVATE_KEY` | 署名鍵ファイル名 | `harinoki.key' |
| `TSURUGI_TOKEN_EXPIRATION` | ATの有効期限 (*1) | `300s` |
| `TSURUGI_TOKEN_EXPIRATION_REFRESH` | RTの有効期限 (*1) | `24h` |

----
(*1): 以下の構文内の `<period>` を利用可能

```bnf
 <period> ::= <integer> <unit>?
<integer> ::= "0" | [1-9][0-9]
   <unit> ::= "h" | "min" | "s"
```

単位が未指定の場合は秒 (`s`) として扱う

### 認証管理基盤との相互運用

[認証管理基盤の環境変数](https://github.com/project-tsurugi/authentication-manager/blob/master/docs/authentication_API_specification_cpp.md#%E7%92%B0%E5%A2%83%E5%A4%89%E6%95%B0) と同様の値を [各環境変数](#設定情報一覧) に指定することで、認証管理基盤と互換性のある AT を生成できる。
より正確に言えば、本アプリケーションが生成する AT によって、 [認可情報の取得機能](https://github.com/project-tsurugi/authentication-manager/blob/master/docs/authentication_API_specification_cpp.md#get_acls%E3%83%A1%E3%82%BD%E3%83%83%E3%83%89) を利用できる。

なお、本アプリケーションが生成する RT は、認証管理基盤でリフレッシュすることを想定していない。

## 参考文献

* [JSON Web Tokens](https://jwt.io/)
* [認証管理基盤 API仕様書 C++版](https://github.com/project-tsurugi/authentication-manager/blob/master/docs/authentication_API_specification_cpp.md)
