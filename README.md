# MetaData in Android

# Meta Data of audio/video

## you can

Get all meta data in a media file/uri

```java
 Map<String,String>  MetaDataUtil.getAllInfo(String pathOrUri)
```





# Exif : meta data of Image

get all tags form androidx.exifinterface.media.ExifInterface

## you can 

```java
//get tag value from stream by ExifInterface api:
Map<String,String> ExifUtil.readExif(java.io.InputStream)
  
 //or writeExif to a file
 void writeExif(Map<String,String> exifMap, String file)
```





#  by gradle

Add this in your root `build.gradle` file (**not** your module `build.gradle` file):

```
allprojects {
	repositories {
		...
		maven { url "https://jitpack.io" }
	}
}
```

## Dependency

Add this to your module's `build.gradle` file (make sure the version matches the JitPack badge above):

```
dependencies {
	...
	compile 'com.github.hss01248:metadata:1.0.1'

}
```

## 