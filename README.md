Un7zip-for-Android
==================

* A simple android ndk library used to simply extract lzma 7z files.<br>

* Some times we need to compress some resources in our applications,in some cases,LZMA 
 get smaller achieves than common zip,so we need extract the resources when the app 
start to run,this library is to do the work.<br>

###1.Introduction
* This is a small free library with simple function to extract the 7z file
* It is a jni call library
* This library is based on LZMA sdk,it does the most job.

###2.Usage

1. You can download all the demo source code and compile it.<br>
2. if you don't want to compile the c code, just use the library.

####Demo screenshoot
![image](https://github.com/hzy3774/Un7zip-for-Android/blob/master/image/android_screen.png)
####JNI log
![image](https://github.com/hzy3774/Un7zip-for-Android/blob/master/image/jnilogs.png)
####File manager
![image](https://github.com/hzy3774/Un7zip-for-Android/blob/master/image/file_manager.png)

####code:
    AndUn7z.extract7z(String filePath, String outPath);

###3.Limitation
* 7z(LZMA) extract only
* based on lzma sdk465, ASCII encodding file/directory/path names only
* not support password<br>
If you need more compress / extract functions or formats,here more information.

###4.More information
More about it http://hzy3774.iteye.com/admin/blogs/2104510 <br>

[Welcome to visit my ITeye blog](http://hzy3774.iteye.com/)