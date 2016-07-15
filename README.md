
## 简介
融云的相册,提取出来的,方便使用

[phototView](https://github.com/chrisbanes/PhotoView)可以不复制,直接加依赖

root `build.gradle`
```java
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```
project `build.gradle`
```java
dependencies {
    compile 'com.github.chrisbanes:PhotoView:1.2.6'
}
```
## 效果
![预览图](https://github.com/CSnowStack/RCPics/blob/master/pics/preview.gif)



!
