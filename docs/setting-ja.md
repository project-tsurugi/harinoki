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
  * そのファイル名が相対パスの場合は、"/"を起点とするパス（絶対パスと同意）とする
  * ${HARINOKI_PROPERTIES}が示すファイルが存在しない場合はpropertiesファイル不存在エラーとしてharinokiは起動しない
* 環境変数 HARINOKI_PROPERTIES が指定されていない場合は${TSURUGI_HOME}/var/auth/etc/harinoki/jwt.propertiesを使用する
  * 環境変数 TSURUGI_HOME が設定されていない場合はエラーとし、harinokiは起動しない
  * ${TSURUGI_HOME}/var/auth/etc/harinoki/jwt.propertiesというファイルが存在しない場合はpropertiesファイル不存在エラーとしてharinokiは起動しない

#### keyファイル
keyファイルは、propertiesファイルが置かれたディレクトリにある以下のファイルを使用する
* propertiesファイルでtsurugi.jwt.private_key_fileが設定されている場合は、その名前のファイルをkeyファイルとして使用する
  * 指定されたkeyファイルが存在しない場合はkeyファイル不存在エラーとしてharinokiは起動しない
  * tsurugi.jwt.private_key_fileで指定されるファイル名（文字列）にFile.separator文字が含まれる場合はkeyファイル名不適切エラーとしてharinokiは起動しない
* propertiesファイルでtsurugi.jwt.private_key_fileが設定されていない場合はjwt.pemをkeyファイルとして使用する 
  * jwt.pemファイルが存在しない場合はkeyファイル不存在エラーとしてharinokiは起動しない

### 各ファイルの内容
#### propertiesファイル
下記エントリの設定値をproperties形式で記述する
| エントリ名 | 概要 |
|:--|:--|
| tsurugi.jwt.claim_iss | トークンの発行者名 |
| tsurugi.jwt.claim_aud | トークンの受信者 |
| tsurugi.jwt.private_key_file | RSA鍵を格納したファイル名 |
| tsurugi.token.expiration | ATの有効期限 |
| tsurugi.token.expiration_refresh | RTの有効期限 |

AT, RTについては [認証トークン仕様](token-ja.md) 参照。

#### keyファイル
JWT署名アルゴリズムのRS256で使用するpem形式のRSA秘密鍵

例えば、下記コマンドで作成する。
```
openssl genrsa -out harinoki.pem 2048
```
なお、鍵長（2048）は、1024, 4096でも良い。user名やpasswordの最大長は鍵長の1/8文字となる。

### パーミッション
propertiesファイル、keyファイル、それらが置かれたディレクトリのotherとgroupのパーミッションはrwxともfalseとする。そうでない場合はパーミッション・エラーとしてharinokiは起動しない。