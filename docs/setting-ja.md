# 認証サーバ（Harinoki）の起動設定

## この文書について
Tsurugiインストーラがインストールする認証機能の初期設定およびユーザが行う設定変更方法を説明した後、それを実現するためのTsurugiインストーラ動作を示す。
なお、本ドキュメントでは、主に [tsurugiインストーラー同梱の認証サーバについて](https://github.com/project-tsurugi/tsurugi-distribution/blob/master/docs/authentication-server.md)が説明している認証機能（第一版）との差異を説明する。

## 認証機能の初期設定
### Tsurugiインストール直後の状態
TsurugiインストーラによるTsurugidbインストール直後から `$TSURUGI_HOME/bin/authentication-server` コマンドによる認証サーバ起動が可能である。なお、認証機能（第一版）と異なり、環境変数 `$TSURUGI_JWT_SECRET_KEY` の設定は不要。

インストール直後における認証機能の設定は下記となっている。

* login可能ユーザ：Tsurugiインストーラが `$TSURUGI_HOME/var/auth/etc/harinoki-users.props` に設定したユーザ名とパスワードでtsurugidbにlogin可能
* 管理ユーザ：login可能なユーザ全員（login可能ユーザを追加した場合、全ユーザが管理ユーザになる。管理ユーザを限定する場合は「認証機能の設定変更」項参照。）

## 認証機能の設定変更
### login可能ユーザ
 [tsurugiインストーラー同梱の認証サーバについて](https://github.com/project-tsurugi/tsurugi-distribution/blob/master/docs/authentication-server.md) の`認証サーバに対する認証情報設定`項参照のこと。

### 管理ユーザ
管理ユーザの変更は、tsurugidbの構成ファイル[構成ファイルのパラメーター](https://github.com/project-tsurugi/tateyama/blob/master/docs/config_parameters.md)内のauthenticationセクションadministratorsパラメータに管理ユーザを設定した後、tsurugidbを再起動する。

### 高度な設定
#### propertiesファイルによる設定内容
propertiesファイル（詳細は次節参照）で設定可能なパラメータ（トークンの発行者名、トークンの受信者、RSA鍵を格納したファイル名、ATの有効期限、RTの有効期限）を変更する場合は、認証サーバが使用しているpropertiesファイル（デフォルトでは${TSURUGI_HOME}/var/auth/etc/harinoki.properties）を変更した後、認証サーバを再起動する。
なお、Tsurugiインストーラにより作成される${TSURUGI_HOME}/var/auth/etc/harinoki.propertiesでは、各設定項目がコメントアウトされているので、各パラメータについてデフォルト値（コメントアウトされた各行に記載）が使われるようになっている。

#### propertiesファイルの位置
propertiesファイルとして${TSURUGI_HOME}/var/auth/etc/harinoki.properties以外のファイルを使う場合は、次節に示す通り、環境変数 HARINOKI_PROPERTIES を設定したうえで認証サーバを起動する。


## Tsurugiインストーラ動作
本章は「認証機能の初期設定」で示した初期設定を実現するためのTsurugiインストーラ動作を示す。
まず、Tsurugiインストーラ動作を説明する前提として、認証機能（第一版）から追加された設定ファイルについて説明し、
その後、インストーラの動作を示す。

### 設定ファイル
認証機能（第一版）から追加された設定ファイルは以下の２つ。
* propertiesファイル: harinokiの起動設定をproperties形式で記述したファイル
* keyファイル: jwtの署名やuserとpasswordの暗号化に使用するRSA鍵を格納したファイル

#### 各ファイルの位置を指定する方法
##### propertiesファイル
propertiesファイルは以下で指定されるファイルを使用する
* 環境変数 HARINOKI_PROPERTIES が指定されている場合は、${HARINOKI_PROPERTIES}が示すファイル
  * そのファイル名が相対パスの場合は、"/"を起点とするパス（絶対パスと同意）とする
  * ${HARINOKI_PROPERTIES}が示すファイルが存在しない場合はpropertiesファイル不存在エラーとしてharinokiは起動しない
* 環境変数 HARINOKI_PROPERTIES が指定されていない場合は${TSURUGI_HOME}/var/auth/etc/harinoki.propertiesを使用する
  * 環境変数 TSURUGI_HOME が設定されていない場合はエラーとし、harinokiは起動しない
  * ${TSURUGI_HOME}/var/auth/etc/harinoki.propertiesというファイルが存在しない場合はpropertiesファイル不存在エラーとしてharinokiは起動しない

##### keyファイル
keyファイルは、propertiesファイルが置かれたディレクトリにある以下のファイルを使用する
* propertiesファイルでtsurugi.jwt.private_key_fileが設定されている場合は、その名前のファイルをkeyファイルとして使用する
  * 指定されたkeyファイルが存在しない場合はkeyファイル不存在エラーとしてharinokiは起動しない
  * tsurugi.jwt.private_key_fileで指定されるファイル名（文字列）にFile.separator文字が含まれる場合はkeyファイル名不適切エラーとしてharinokiは起動しない
* propertiesファイルでtsurugi.jwt.private_key_fileが設定されていない場合はharinoki.pemをkeyファイルとして使用する 
  * harinoki.pemファイルが存在しない場合はkeyファイル不存在エラーとしてharinokiは起動しない

#### 各ファイルの内容
##### propertiesファイル
下記エントリの設定値をproperties形式で記述する
| エントリ名 | 概要 |
|:--|:--|
| tsurugi.jwt.claim_iss | トークンの発行者名 |
| tsurugi.jwt.claim_aud | トークンの受信者 |
| tsurugi.jwt.private_key_file | RSA鍵を格納したファイル名 |
| tsurugi.token.expiration | ATの有効期限 |
| tsurugi.token.expiration_refresh | RTの有効期限 |

AT, RTについては [認証トークン仕様](token-ja.md) 参照。

Tsurugiインストーラが作成するpropertiesファイル（`${TSURUGI_HOME}/var/auth/etc/harinoki.properties`）の内容は下記とする。
```
# tsurugi.jwt.claim_iss=harinoki
# tsurugi.jwt.claim_aud=tsurugidb
# tsurugi.jwt.private_key_file=harinoki.pem
# tsurugi.token.expiration=300seconds
# tsurugi.token.expiration_refresh=24hours
```

##### keyファイル
JWT署名アルゴリズムのRS256で使用するpem形式のRSA秘密鍵

例えば、下記コマンドで作成する。
```
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out ${TSURUGI_HOME}/var/auth/etc/harinoki.pem
```
なお、鍵長（2048）は、1024, 4096でも良い。user名やpasswordの最大長は鍵長の1/8文字となる。

cf. opensslコマンドはopensslパッケージに含まれている。Tsurugiインストーラを実行する環境にopensslパッケージが入っていない場合はインストールが必要。

### パーミッション
propertiesファイル、keyファイル、それらが置かれたディレクトリのotherとgroupのパーミッションを下記により設定する。
* `${TSURUGI_HOME}/var/auth/etc` ディレクトリのパーミッションを0700にする（`chmod 700 ${TSURUGI_HOME}/var/auth/etc`）
* `${TSURUGI_HOME}/var/auth/etc/harinoki.properties` ファイルを作成し、パーミッションを0600、内容を下記にする。
* `${TSURUGI_HOME}/var/auth/etc/harinoki.key` ファイルを「keyファイル」節で示したコマンドにより作成し、パーミッションを0600にする。