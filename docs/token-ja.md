# 認証トークン仕様

## この文書について

* 認証サービスが利用する、認証トークンの形式に関する仕様

## 用語

* access token (AT)
  * 認可情報を取得するために必要な認証トークン
* refresh token (RT)
  * AT を発行するために必要な認証トークン

## トークンの形式

* それぞれの認証トークンは、 JSON Web Tokens (JWT) の形式
  * 各パートの形式は、次項以降に記載

### ヘッダ部

TBD

### ペイロード部

TBD

### 署名

TBD

## 設定情報一覧

* それぞれの設定は、 `ServletConfig` と環境変数のいずれかで指定できる
  * `ServletConfig` によって指定されたものを「設定名」とよぶ
  * 両者に設定されている場合、`ServletConfig` のものを優先する

| 設定名 | 環境変数名 | 設定内容 |
|:-:|:-:|:--|
| `TBD` | `TSURUGI_TBD` | TBD |

## 参考文献

* [JSON Web Tokens](https://jwt.io/)
