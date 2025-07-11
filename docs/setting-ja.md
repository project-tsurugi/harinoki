# harinokiの起動設定

## この文書について
harinokiの起動設定を指定する方法を示す。

## 設定ファイル
harinokiで使用する設定ファイルは以下の２つ
* propertiesファイル: harinokiの起動設定をproperties形式で記述したファイル
* keyファイル: jwtの署名やuserとpasswordの暗号化に使用するRSA鍵を格納したファイル

### 各ファイルの位置を指定する方法
#### propertiesファイル
propertiesファイルは以下で指定されるファイルを使用する
* 環境変数 HARINOKI_PROPERTIES が指定されている場合は、${HARINOKI_PROPERTIES}が示すファイル
  * そのファイル名が相対パスの場合は、${TSURUGI_HOME}/var/auth/etcを相対パスの起点とする
  * ${HARINOKI_PROPERTIES}が示すファイルが存在しない場合はエラーとし、harinokiは起動しない
* 環境変数 HARINOKI_PROPERTIES が指定されていない場合は${TSURUGI_HOME}/var/auth/etc/harinoki.propertiesを使用する
  * 環境変数 TSURUGI_HOME が設定されていない場合はエラーとし、harinokiは起動しない
  * ${TSURUGI_HOME}/var/auth/etc/harinoki.propertiesというファイルが存在しない場合はエラーとし、harinokiは起動しない

#### keyファイル
keyファイルは以下のファイルを使用する
* propertiesファイルでTSURUGI_JWT_PRIVATE_KEYが設定されている場合は、そのエントリが示すファイル
  * そのファイル名が相対パスの場合は、propertiesファイルが置かれたディレクトリを相対パスの起点とする
  * 指定されたkeyファイルが存在しない場合はエラーとし、harinokiは起動しない
* propertiesファイルでTSURUGI_JWT_PRIVATE_KEYが設定されていない場合はpropertiesファイルが置かれたディレクトリにあるharinoki.pemをkeyファイルとして使用する
  * 上記のkeyファイルが存在しない場合はエラーとし、harinokiは起動しない

### 各ファイルの内容
#### propertiesファイル
下記エントリの設定値をproperties形式で記述する
| エントリ名 | 概要 |
|:--|:--|
| tsurugi.jwt.claim_iss | トークンの発行者名 |
| tsurugi.jwt.claim_aud | トークンの受信者 |
| tsurugi.jwt.private_key | RSA鍵を格納したファイルのパス |
| tsurugi.token.expiration | ATの有効期限 |
| tsurugi.token.expiration_refresh | RTの有効期限 |

AT, RTについてはtoken-ja.md参照。

#### keyファイル
pem形式のRSA秘密鍵

例えば、下記コマンドで作成する。
```
openssl genrsa -out harinoki.pem 2048
```
なお、鍵長（2048）は、1024, 4096でも良い。user名やpasswordの最大長は鍵長の1/8文字となる。