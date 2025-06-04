# deploy方法

## この文書について

* tomcatでharinokiを動作させる設定
tomcatをインストールしたディレクトリを${CATALINA_HOME}と記す。

## 設定
### 設定ファイル
* ${CATALINA_HOME}/conf/context.xml
Contextセクションに下記を追加する。
```
<ResourceLink name="UserDatabase" global="UserDatabase"
              type="org.apache.catalina.UserDatabase" />
```

* ${CATALINA_HOME}/conf/jaas.xml
下記内容のファイルを作成する。
```
Harinoki {
    com.tsurugidb.harinoki.module.HarinokiLoginModule required debug=true;
};
```

### ユーザ名とパスワード
${CATALINA_HOME}/conf/tomcat-users.xmlに記載する。

### Harinoki.war
buildにより作成したHarinoki.warを${CATALINA_HOME}/webappsに配置する。

## 起動
### 起動コマンド
下記によりtomcatを起動する。
```
TSURUGI_JWT_SECRET_KEY="改行コードを削除したpem形式の秘密鍵" \
CATALINA_OPTS="-Djava.security.auth.login.config==${CATALINA_HOME}/conf/jaas.config" \
${CATALINA_HOME}/bin/startup.sh
```
