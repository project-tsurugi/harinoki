# 認証サーバ（Harinoki）の起動設定

## この文書について
Tsurugi認証機能（Harinoki）の初期設定およびユーザが行う設定変更方法を説明する。


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
        harinoki.pem              # keyファイル
        harinoki.properties       # Harinoki設定ファイル（harinoki.propertiesファイルと表記）
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

Tsurugi認証機能の実行はharinoki.warを実行するJettyサーバが担い、それを使う際はTsurugi認証機能に対して[RESTful API](rest-api-ja.md)経由でアクセスする。

#### 各ファイルの内容
##### harinoki.propertiesファイル
下記エントリの設定値をproperties形式で記述する
| エントリ名 | 概要 |
|:--|:--|
| `tsurugi.jwt.claim_iss` | トークンの発行者名 |
| `tsurugi.jwt.claim_aud` | トークンの受信者 |
| `tsurugi.jwt.private_key_file` | keyファイル名 |
| `tsurugi.token.expiration` | ATの有効期限 |
| `tsurugi.token.expiration_refresh` | RTの有効期限 |

AT, RTについては [認証トークン仕様](token-ja.md) 参照。

Tsurugiインストーラが作成するharinoki.propertiesファイル（`$TSURUGI_HOME/var/auth/etc/harinoki.properties`）の内容は下記とする。
```
# tsurugi.jwt.claim_iss=harinoki
# tsurugi.jwt.claim_aud=tsurugidb
# tsurugi.jwt.private_key_file=harinoki.pem
# tsurugi.token.expiration=300s
# tsurugi.token.expiration_refresh=24h
```

##### keyファイル
keyファイルはJWT署名アルゴリズムのRS256で使用するpem形式のRSA秘密鍵を格納したファイルである。

例えば、下記コマンドで作成する。
```
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out $TSURUGI_HOME/var/auth/etc/harinoki.pem
```
なお、鍵長（2048）は、1024, 4096でも良い。暗号化可能な１ブロックは鍵長の1/8文字となる。

cf. opensslコマンドはopensslパッケージに含まれている。Tsurugiインストーラを実行する環境にopensslパッケージが入っていない場合はインストールが必要。

##### harinoki-users.props
tsurugidbにログイン可能なユーザ名とパスワードをBASIC認証で使用する形式で記述。Tsurugiインストーラが設定する初期値は「login可能ユーザの変更」項参照。

##### その他
その他のファイルはJettyを適切に動作させるための設定ファイルであり、本資料が説明するファイルの対象外。

#### 各ファイルのパーミッション
harinoki.propertiesファイル、keyファイル、それらが置かれたディレクトリのotherとgroupにrwxパーミッションが付与されていると権限エラーとしてHarinokiは起動しない。
このため、パーミッションは下記となっている。
* `$TSURUGI_HOME/var/auth/etc` ディレクトリのパーミッションは0700
* `$TSURUGI_HOME/var/auth/etc/harinoki.properties` と `$TSURUGI_HOME/var/auth/etc/harinoki.pem` のパーミッションは0600。

その他のファイルについてはパーミッションに関する制約はない。


## 認証機能の起動設定
### Tsurugiインストール直後の状態
TsurugiインストーラによるTsurugidbインストール直後から `$TSURUGI_HOME/bin/authentication-server` コマンドによる認証サーバ起動が可能となっている。
このときの認証機能の起動設定は下記。

* login可能ユーザ：Tsurugiインストーラが `$TSURUGI_HOME/var/auth/etc/harinoki-users.props` に設定したユーザ名とパスワードでtsurugidbにlogin可能。
* 管理ユーザ：login可能なユーザ全員。login可能ユーザを追加した場合、全ユーザが管理ユーザになる。管理ユーザを限定する場合は「認証機能の設定変更」項参照。

#### 各ファイルの位置を指定する方法
##### harinoki.propertiesファイル
harinoki.propertiesファイルは以下で指定されるファイルを使用する
* 環境変数`HARINOKI_PROPERTIES` が指定されている場合は、`$HARINOKI_PROPERTIES`が示すファイル
  * そのファイル名が相対パスの場合は、"/"を起点とするパス（絶対パスと同意）とする
  * `$HARINOKI_PROPERTIES`が示すファイルが存在しない場合はharinoki.propertiesファイル不存在エラーとしてHarinokiは起動しない
* 環境変数HARINOKI_PROPERTIES が指定されていない場合は`$TSURUGI_HOME/var/auth/etc/harinoki.properties`を使用する
  * 環境変数TSURUGI_HOME が設定されていない場合はエラーとし、Harinokiは起動しない
  * `$TSURUGI_HOME/var/auth/etc/harinoki.properties`というファイルが存在しない場合はharinoki.propertiesファイル不存在エラーとしてHarinokiは起動しない

##### keyファイル
keyファイルは、harinoki.propertiesファイルが置かれたディレクトリにある以下のファイルを使用する
* harinoki.propertiesファイルで`tsurugi.jwt.private_key_file`が設定されている場合は、その名前のファイルをkeyファイルとして使用する
  * 指定されたkeyファイルが存在しない場合はkeyファイル不存在エラーとしてHarinokiは起動しない
  * `tsurugi.jwt.private_key_file`で指定されるファイル名（文字列）にFile.separator文字が含まれる場合はkeyファイル名不適切エラーとしてHarinokiは起動しない
* harinoki.propertiesファイルで`tsurugi.jwt.private_key_file`が設定されていない場合は`harinoki.pem`をkeyファイルとして使用する 
  * `harinoki.pem`が存在しない場合はkeyファイル不存在エラーとしてHarinokiは起動しない

##### その他
その他のファイルの位置は、環境変数`TSURUGI_HOME`を起点とする固定パスに配置する。


## 認証機能の設定変更
### login可能ユーザの変更
TsurugiインストーラによりインストールされるHarinokiでは認証方法としてBASIC認証を使用しており、BASIC認証用の初期設定として以下の認証ユーザ情報を生成する。
* ユーザ名: `tsurugi`
* パスワード: `password`

login可能ユーザを変更するには、 `$TSURUGI_HOME/var/auth/etc/harinoki-users.props` を修正した後、Harinokiを再起動する。

### 管理ユーザの変更
管理ユーザの変更は、tsurugidbの[構成ファイルのパラメーター](https://github.com/project-tsurugi/tateyama/blob/master/docs/config_parameters.md)の`authentication.administrators`に管理ユーザを設定した後、tsurugidbを再起動する。

### 高度な設定
#### harinoki.propertiesファイルによる設定内容の変更
harinoki.propertiesファイル（詳細は次節参照）で設定可能なパラメータ（トークンの発行者名、トークンの受信者、keyファイル名、ATの有効期限、RTの有効期限）を変更する場合は、認証サーバが使用しているharinoki.propertiesファイル（デフォルトでは`$TSURUGI_HOME/var/auth/etc/harinoki.properties`）を変更した後、認証サーバを再起動する。
なお、Tsurugiインストーラにより作成される`$TSURUGI_HOME/var/auth/etc/harinoki.properties`では、各設定項目がコメントアウトされているので、各パラメータについてデフォルト値（コメントアウトされた各行に記載）が使われるようになっている。

#### harinoki.propertiesファイル位置の変更
harinoki.propertiesファイルとして`$TSURUGI_HOME/var/auth/etc/harinoki.properties`以外のファイルを使う場合は、環境変数`HARINOKI_PROPERTIES` を設定したうえで認証サーバを起動する。