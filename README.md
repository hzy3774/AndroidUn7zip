AndroidUn7zip(安卓7zip解压)
==================
A simple library with lzma sdk for 7z extraction.

### Introduction
Import the library:
```gradle
dependencies {
    compile 'com.hzy:un7zip:1.0.0'
}
```

Java API：
```java
public static boolean extract7z(String filePath, String outPath);
public static boolean extract7zFromAssets(Context context, String assetPath, String outPath);
```

### Fetures
* easily extract 7z from file
* directly extract 7z from assets
* unicode file names is available

### Screenshot
![image](https://raw.githubusercontent.com/hzy3774/AndroidUn7zip/master/misc/screen.gif)

### Thanks To
* [danhantao](https://github.com/danhantao) offered X86 compiled(修复了X86编译问题)
* [ransj](https://github.com/ransj) offered the way to extract 7z file from assets directly(添加了直接从assets解压的功能)

### About Me
* [GitHub: http://huzongyao.github.io/](http://huzongyao.github.io/)
* [ITEye博客：http://hzy3774.iteye.com/](http://hzy3774.iteye.com/)
* [新浪微博: http://weibo.com/hzy3774](http://weibo.com/hzy3774)

### Contact To Me
* QQ: [377406997](http://wpa.qq.com/msgrd?v=3&uin=377406997&site=qq&menu=yes)
* Gmail: [hzy3774@gmail.com](mailto:hzy3774@gmail.com)
* Foxmail: [hzy3774@qq.com](mailto:hzy3774@qq.com)



