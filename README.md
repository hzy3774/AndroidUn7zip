AndroidUn7zip(安卓7zip解压)
==================
A simple library with lzma sdk for 7z extraction.(使用LZMA SDK解压7z压缩包)

[![auc][aucSvg]][auc] [![api][apiSvg]][api]

[aucSvg]: https://img.shields.io/badge/AndroidUn7zip-v1.7.0-brightgreen.svg
[auc]: https://github.com/hzy3774/AndroidUn7zip

[apiSvg]: https://img.shields.io/badge/API-14+-brightgreen.svg
[api]: https://android-arsenal.com/api?level=14

### Introduction
* 官方网站：https://www.7-zip.org/sdk.html
* 7z is the new archive format, providing high compression ratio.


#### Begin To Use
* Add gradle dependencie:
```gradle
dependencies {
    implementation 'com.hzy:un7zip:+'
}
```
* Or just download the aar [here](https://jcenter.bintray.com/com/hzy/un7zip/)
* If you want to add some abi filters
``` gradle
android {
    ...
    defaultConfig {
        ...
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86'
         }
    }
}
```


#### Java API：
```java
boolean extractFile(filePath, outPath, callback);
boolean extractAsset(assetManager, fileName, outPath, callback);
String getLzmaVersion();
```

#### ProGuard
If you are using ProGuard you might need to add the following options:
```
-keep class com.hzy.lib7z.** { *; }
```

### Fetures
* easily extract 7z from file
* directly extract 7z from assets
* unicode file names is available
* add extract callbacks

### Screenshot
![image](https://raw.githubusercontent.com/hzy3774/AndroidUn7zip/master/misc/screen.gif)

### Thanks To
* [danhantao](https://github.com/danhantao) offered X86 compiled(修复了X86编译问题)
* [ransj](https://github.com/ransj) offered the way to extract 7z file from assets directly(添加了直接从assets解压的功能)

### About Me
 * GitHub: [https://huzongyao.github.io/](https://huzongyao.github.io/)
 * ITEye博客：[https://hzy3774.iteye.com/](https://hzy3774.iteye.com/)
 * 新浪微博: [https://weibo.com/hzy3774](https://weibo.com/hzy3774)

### Contact To Me
 * QQ: [377406997](https://wpa.qq.com/msgrd?v=3&uin=377406997&site=qq&menu=yes)
 * Gmail: [hzy3774@gmail.com](mailto:hzy3774@gmail.com)
 * Foxmail: [hzy3774@qq.com](mailto:hzy3774@qq.com)
 * WeChat: hzy3774

 ![image](https://raw.githubusercontent.com/hzy3774/AndroidP7zip/master/misc/wechat.png)

### Others
 * 想捐助我喝杯热水(¥0.01起捐)</br>
 ![donate](https://github.com/huzongyao/JChineseChess/blob/master/misc/donate.png?raw=true)



