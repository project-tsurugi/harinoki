# deploy方法

## この文書について

* tomcatでharinokiを動作させる設定
tomcatをインストールしたディレクトリを${CATALINA_HOME}と記す。なお、本メモはtomcatの起動・停止方法およびRSA秘密鍵ファイルの作成方法の知識を有する読者を想定しており、その詳細説明は省略する。

## 設定
### 初期設定のためのtomcat起動
git@github.com:project-tsurugi/harinoki.gitをcloneし、それをbuildして作成したharinoki.warを${CATALINA_HOME}/webappsディレクトリにコピーしてtomcatを起動した後、停止する。
これにより、${CATALINA_HOME}/webapps/harinoki ディレクトリとその配下のファイルが作成される。

### 秘密鍵
${CATALINA_HOME}/webapps/harinoki/WEB-INF/classes/tsurugi.pemファイルを使用する秘密鍵ファイル（pem形式）に置き換える。

### ユーザ名とパスワード
デフォルトでは${CATALINA_HOME}/webapps/harinoki/WEB-INF/web.xmlのlogin-configエントリによりBASIC認証が設定されている。
そのままBASIC認証を使用する場合は、ユーザ名とパスワードを${CATALINA_HOME}/conf/tomcat-users.xmlに記載する。
web.xmlにBASIC認証以外の認証方法を指定し、それを使用する場合は認証方法に応じた設定を行う。

## 運用のためのtomcat起動
上記の設定を行った後に、tomcatを起動する。
