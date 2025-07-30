# 認証サーバ（Harinoki）の起動設定

## この文書について
Tsurugiインストーラがインストールする認証機能の初期設定およびユーザが行う設定変更方法を説明した後、それを実現するためのTsurugiインストーラ動作を示す。

## 認証機能の初期設定
### Tsurugiインストール直後の状態
TsurugiインストーラによるTsurugidbインストール直後から `$TSURUGI_HOME/bin/authentication-server` コマンドによる認証サーバ起動が可能となるようにHarinokiをインストールする。

インストール直後における認証機能の設定は下記とする。

* login可能ユーザ：Tsurugiインストーラが `$TSURUGI_HOME/var/auth/etc/harinoki-users.props` に設定したユーザ名とパスワードでtsurugidbにlogin可能。
* 管理ユーザ：login可能なユーザ全員。login可能ユーザを追加した場合、全ユーザが管理ユーザになる。管理ユーザを限定する場合は「認証機能の設定変更」項参照。

## 認証機能の設定変更
### login可能ユーザの変更
TsurugiインストーラによりインストールされるHarinokiでは認証方法としてBASIC認証を使用しており、BASIC認証用の初期設定として以下の認証ユーザ情報を生成する。
* ユーザ名: `tsurugi`
* パスワード: `password`

login可能ユーザを変更するには、 `$TSURUGI_HOME/var/auth/etc/harinoki-users.props` を修正した後、Harinokiを再起動する。

### 管理ユーザの変更
管理ユーザの変更は、tsurugidbの構成ファイル[構成ファイルのパラメーター](https://github.com/project-tsurugi/tateyama/blob/master/docs/config_parameters.md)内のauthenticationセクションadministratorsパラメータに管理ユーザを設定した後、tsurugidbを再起動する。

### 高度な設定
#### propertiesファイルによる設定内容の変更
propertiesファイル（詳細は次節参照）で設定可能なパラメータ（トークンの発行者名、トークンの受信者、RSA鍵を格納したファイル名、ATの有効期限、RTの有効期限）を変更する場合は、認証サーバが使用しているpropertiesファイル（デフォルトでは$TSURUGI_HOME/var/auth/etc/harinoki.properties）を変更した後、認証サーバを再起動する。
なお、Tsurugiインストーラにより作成される$TSURUGI_HOME/var/auth/etc/harinoki.propertiesでは、各設定項目がコメントアウトされているので、各パラメータについてデフォルト値（コメントアウトされた各行に記載）が使われるようになっている。

#### propertiesファイル位置の変更
propertiesファイルとして$TSURUGI_HOME/var/auth/etc/harinoki.properties以外のファイルを使う場合は、次節に示す通り、環境変数 HARINOKI_PROPERTIES を設定したうえで認証サーバを起動する。


## Tsurugiインストーラ動作
本章は「認証機能の初期設定」で示した初期設定を実現するためにTsurugiインストーラが実施するインストール操作を示す。

## 認証機能関連のインストール構成
Tsurugiインストーラーによって生成される、認証機能関連のインストール構成を以下に示す。
ここで、`$TSURUGI_HOME` はTsurugiのインストールパスを表記したもの。

```
$TSURUGI_HOME/
  bin/
    authentication-server         # 認証サーバの起動・停止コマンド
  lib/
    jetty/                        # 認証サーバの実行環境であるJetty(JETTY_HOME)
  var/
    auth/
      etc/
        harinoki-users.props      # テスト用の認証ユーザ管理ファイル
        harinoki.pem              # jwtの署名やuserとpasswordの暗号化に使用するRSA鍵を格納したファイル
        harinoki.properties       # Harinokiの起動設定をproperties形式で記述するファイルのテンプレート
        jaas-login-service.xml    # Jetty JAASログインサービスの構成定義ファイル
        login.conf                # JAASログインモジュールの構成定義ファイル
      logs/
        YYYY_MM_DD.jetty.log      # Jettyのログファイル（Jettyが作成、tsurugiインストーラは作成しない）
      resoruces/
        jetty-logging.properties  # Jettyのログ定義ファイル
      start.d/                    # Jettyモジュールの設定ファイル
        console-capture.ini
        deploy.ini
        http.ini
        jaas.ini
      webapps/
        harinoki.war              # HarinokiのWebアプリケーションアーカイブ
```

#### 各ファイルの位置を指定する方法
##### propertiesファイル
propertiesファイルは以下で指定されるファイルを使用する
* 環境変数 HARINOKI_PROPERTIES が指定されている場合は、$HARINOKI_PROPERTIESが示すファイル
  * そのファイル名が相対パスの場合は、"/"を起点とするパス（絶対パスと同意）とする
  * $HARINOKI_PROPERTIESが示すファイルが存在しない場合はpropertiesファイル不存在エラーとしてHarinokiは起動しない
* 環境変数 HARINOKI_PROPERTIES が指定されていない場合は$TSURUGI_HOME/var/auth/etc/harinoki.propertiesを使用する
  * 環境変数 TSURUGI_HOME が設定されていない場合はエラーとし、Harinokiは起動しない
  * $TSURUGI_HOME/var/auth/etc/harinoki.propertiesというファイルが存在しない場合はpropertiesファイル不存在エラーとしてHarinokiは起動しない

##### keyファイル
keyファイルは、propertiesファイルが置かれたディレクトリにある以下のファイルを使用する
* propertiesファイルでtsurugi.jwt.private_key_fileが設定されている場合は、その名前のファイルをkeyファイルとして使用する
  * 指定されたkeyファイルが存在しない場合はkeyファイル不存在エラーとしてHarinokiは起動しない
  * tsurugi.jwt.private_key_fileで指定されるファイル名（文字列）にFile.separator文字が含まれる場合はkeyファイル名不適切エラーとしてHarinokiは起動しない
* propertiesファイルでtsurugi.jwt.private_key_fileが設定されていない場合はharinoki.pemをkeyファイルとして使用する 
  * harinoki.pemファイルが存在しない場合はkeyファイル不存在エラーとしてHarinokiは起動しない

##### その他
その他のファイルの位置は、$TSURUGI_HOMEを起点とする固定パスに配置する。

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

Tsurugiインストーラが作成するpropertiesファイル（`$TSURUGI_HOME/var/auth/etc/harinoki.properties`）の内容は下記とする。
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
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out $TSURUGI_HOME/var/auth/etc/harinoki.pem
```
なお、鍵長（2048）は、1024, 4096でも良い。暗号化可能な１ブロックは鍵長の1/8文字となる。

cf. opensslコマンドはopensslパッケージに含まれている。Tsurugiインストーラを実行する環境にopensslパッケージが入っていない場合はインストールが必要。

##### harinoki-users.props
tsurugidbにログイン可能なユーザ名とパスワードをBASIC認証で使用する形式で記述。Tsurugiインストーラが設定する初期値は「login可能ユーザの変更」項参照。

##### その他
その他のファイルはJettyを適切に動作させるための設定ファイルであり、本資料の説明対象外。

### ファイル作成とパーミッション
propertiesファイル、keyファイル、それらが置かれたディレクトリのotherとgroupのパーミッションを下記により設定する。
* `$TSURUGI_HOME/var/auth/etc` ディレクトリのパーミッションを0700にする（`chmod 700 $TSURUGI_HOME/var/auth/etc`）
* `$TSURUGI_HOME/var/auth/etc/harinoki.properties` 「各ファイルの内容」項の内容で作成し、パーミッションを0600にする。
* `$TSURUGI_HOME/var/auth/etc/harinoki.key` ファイルを「keyファイル」節で示したコマンドにより作成し、パーミッションを0600にする。