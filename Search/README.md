# PriParaSearch
## 概要
検索を行う部分です。
実行する際には予め`index.db`と`index.txt`が必要です。  
`index.db`は[PriParaCreator](https://github.com/Khromium/PriParaSearch/tree/master/createDB)で生成できます。  
`index.txt`は手動で書く必要があります。各行に以下の形式で記入して下さい。本家と違ってニコ動の機能を削っています。  
`(作品名毎に定めたID),(話数ID),(動画のフレームレート),(話数も含むタイトル)`
それぞれ以下の値の範囲内にして下さい。

|変数|範囲|
|---|---|
作品名毎に定めたID|-128~127
話数ID|-32768~32767
動画のフレームレート|整数/小数
話数も含むタイトル|文字列


参考：
[index.txt](https://github.com/Khromium/PriParaSearch/blob/master/Search/index.txt)
