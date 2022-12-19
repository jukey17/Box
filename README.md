# Box

![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/okocraft/Box)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/okocraft/Box/gradle.yml?branch=main)
![GitHub](https://img.shields.io/github/license/okocraft/Box)

Box はあらゆるアイテムを仮想的なインベントリへ収納できるようにする Paper プラグインです。

## 特徴

Box には以下のような特徴があります。

* GUI によって直感的にアイテムの預け入れ・引き出し・クラフトができます
* コマンドからの預け入れ・引き出しもサポートします
* Box Stick (`/box stick`) を使うことで、アイテムを Box から消費することができます
* 通常アイテム、ポーション、花火、エンチャント本はデフォルトで収納できます
* `/boxadmin register` で手に持っているアイテムを Box に登録することができます
* 詳細な使い方は [Wiki](https://github.com/okocraft/Box/wiki) で確認することができます

### v3 → v4 の移行

移行前に必ず `Box` ディレクトリをバックアップしてください。

- データベースストレージ (SQLite, MySQL) は未実装で、現在は Yaml への保存になります
  - v3 まで使用していたデータベースは、BoxMigrator を利用することでデータ移行できます
  - 詳しくは [v3 からの移行方法](https://github.com/okocraft/Box/wiki/migration-from-v3) 参照してください
- 売買・自動植え替え機能は実装していません
- GUI を全面的に実装し直し、コマンドも一部構文変更しています
- その他、多くの変更点があります。v4 のすべての機能は [Wiki](https://github.com/okocraft/Box/wiki) に記載されています

## インストール

**v4.0.0 から [Paper](https://papermc.io) (Spigot の軽量フォーク) が必須** になっています。Spigot では動作しません。

[リリース](https://github.com/okocraft/Box/releases) から `Box-x.x.x.jar` をダウンロードし、
そのファイルをサーバーの `plugins/` ディレクトリに配置しサーバーを再起動します。

再起動後に

* `config.yml`
* `categories.yml`
* `languages/ja_JP.yml`

が生成されます。起動後、何も設定しなくても使い始めることができます。

## Compiling from source

```
git clone https://github.com/okocraft/Box.git
cd Box/
./gradlew build
```

You can find the bundled jar in the `bundle/build/libs` directory.


## API

### Javadocs

- [Latest Release](https://okocraft.github.io/Box/release)
- [Snapshot](https://okocraft.github.io/Box/snapshot)

### Usage

#### 1. Add a repository

```xml
<repository>
    <id>okocraft-box-repo</id>
    <url>https://okocraft.github.io/Box/maven/</url>
</repository>
```

```groovy
repositories {
    maven {
        url 'https://okocraft.github.io/Box/maven/'
    }
}
```

```kotlin
repositories {
    maven {
        url = uri("https://okocraft.github.io/Box/maven/")
    }
}
```

For snapshot version, use https://okocraft.github.io/Box/maven-snapshot/

#### 2. Add to dependencies

```xml
<dependency>
    <groupId>net.okocraft.box</groupId>
    <artifactId>box-api</artifactId>
    <version>5.0.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

```groovy
dependencies {
    compileOnly 'net.okocraft.box:box-api:5.0.0-SNAPSHOT'
}
```

```kotlin
dependencies {
    compileOnly("net.okocraft.box:box-api:5.0.0-SNAPSHOT")
}
```

#### 3. Relocate ConfigAPI and Event4J

If you are using Box events (`BoxAPI#getEventBus`) or configurations (`BoxAPI#getConfiguration` or using ConfigAPI directly), you have to relocate them.

Add `maven-shade-plugin` or [Gradle Shadow](https://github.com/johnrengelman/shadow) to your project and write the following setting:

```xml
<relocations>
  <relocation>
    <pattern>com.github.siroshun09.configapi</pattern>
    <shadedPattern>net.okocraft.box.lib.configapi</shadedPattern>
  </relocation>
  <relocation>
    <pattern>com.github.siroshun09.event4j</pattern>
    <shadedPattern>net.okocraft.box.lib.event4j</shadedPattern>
  </relocation>
</relocations>
```

```groovy
shadowJar {
  relocate 'com.github.siroshun09.configapi', 'net.okocraft.box.lib.configapi'
  relocate 'com.github.siroshun09.event4j', 'net.okocraft.box.lib.event4j'
}
```

```kotlin
shadowJar {
  relocate("com.github.siroshun09.configapi", "net.okocraft.box.lib.configapi")
  relocate("com.github.siroshun09.event4j", "net.okocraft.box.lib.event4j")
}
```
