# PriParaSearch
## 概要
[Gochiusearch](https://github.com/ksasao/Gochiusearch)のプリパラ版です。  
元のプログラムのアルゴリズムを踏襲しつつ機能削除/追加を行いました。  
詳しいアルゴリズムについては[本家をご覧ください](https://github.com/ksasao/Gochiusearch)。  
　  
開発言語は**名前が可愛かった**ので「ことりん」を使いました。(Kotlin勉強のためです。)   
JVMベースですので**マルチプラットフォーム対応です**。  
動作確認はWindows10とMacで行っています。

※※**DBデータは言語の関係でGochiusearchと互換性がありません**※※

## メイン
- PriParaSearch  
プリパラのアニメ画像をサーチするソフトです。  
画像をドラッグ・アンド・ドロップすると瞬時に判定を行います。  
アプリ上部の`検索レベル`は[Gochiusearch](https://github.com/ksasao/Gochiusearch)同様ハミング距離の閾値です。  
字幕付きのプリパラのアニメ画像を使用する場合は検索レベルを5くらいまで上げると検索にヒットすると思います。

- PriParaCreator  
データベースの作成を行うアプリです。  
MP4ファイルから一発でDBを作ったりファイルのマージ機能を備えているので他の人と分散作業することも出来ます。  
動画のエンコーダーも含まれているのでアプリ容量がかなり重いです。  
本家と違い画像データの再配置は行わないのでHDDに優しいです。  
ビッグなデータの扱う関係上、**場合によってはRAMを4GB程消費する**ので幾つかに分割して最後にマージするかRAMが8GBとか16GBのハイスペックなPCで作業することをおすすめします。  


## ダウンロード
[Releaseページからどうぞ](https://github.com/Khromium/PriParaSearch/releases)  
　  
   　
　   
## み～んなともだち！み～んなContributor！！
みんなともだちなので誰でもデータセットの追加に協力できます。  
当方でもそのうち増やしていきますが、欠けているデータについて学習済みCSVデータを提供していただければ追加します。  
現在は以下が入っています。チェックボックスの入っていないものは現状ではデータがありません。

地上波
- [x] プリパラ(無印1~140話)
- [x] アイドルタイムプリパラ(#32まで)
- [ ] アイドルタイムプリパラ(#33~)

またバグなどありましたらissue等やPull Requestお願いします。  
また、アプリアイコンも募集中です。

## ビルド方法  
JDK8が必要です。linuxはJavaFxのパッケージが別れている可能性ありなので別途導入する必要があります。  
その他の依存関係はgradleが自動で解決してくれます。  
それぞれのプロジェクトで  
`$ gradlew build`  
でビルド、  
`$ gradlew run`  
で実行できます。


Windows/Macでしか試していませんが、コードとしてはLinuxでも動作するはずです。

## 動作画像  

|検索画面|DB作成画面|
|---|---|
|![screenshot 2018-03-04 14 46 07](https://user-images.githubusercontent.com/4639391/36942751-c668acee-1fbd-11e8-865b-a51f45354a4f.jpg)|![image](https://user-images.githubusercontent.com/4639391/36906453-500b75b2-1e79-11e8-9dbd-6eb689c54836.jpg)

## 雑感
友達がアニメプリパラの字幕付きスクショで会話するんですよ。そんなのプリパラ初心者の筆者には到底ついていけないです。  
しかしそれもこのツールが出来上がるまでの間だけでした。  
検索速度はあまり変わりませんがデータベース作成に関してはいちいちディレクトリを作らないでRAM上に展開するのでHDDに優しいです。

## 謝辞
このプログラムは以下のプログラムを参考に作られています。作成に際して深く感謝申し上げます。
- [Gochiusearch](https://github.com/ksasao/Gochiusearch)  

また、動画の変換部分に関しまして、以下のライブラリを用いています
- [javacv](https://github.com/bytedeco/javacv)

JDKバンドルのネイティブアプリ作成に関して、以下のライブラリを用いています
- [javafx-gradle-plugin](https://github.com/FibreFoX/javafx-gradle-plugin)
