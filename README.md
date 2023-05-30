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

 void copyExif(String from, String to)
```



[![](https://jitpack.io/v/hss01248/metadata.svg)](https://jitpack.io/#hss01248/metadata)

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



# 读取jpeg尾部信息



```java
byte[] jpgTail = ExifUtil.getJpgTail(from);
```

普通没有尾部信息的jpg图片:

0xFFD9之后的信息:

![image-20230529200841344](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/imagemac3/image-20230529200841344.png)



谷歌相机拍出的motion image图片:



![image-20230529201111170](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/imagemac3/image-20230529201111170.png)

其exif里xmp为:

```xml
<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">
      <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description rdf:about=""
            xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
            xmlns:Container="http://ns.google.com/photos/1.0/container/"
            xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
            xmlns:xmpNote="http://ns.adobe.com/xmp/note/"
          GCamera:MotionPhoto="1"
          GCamera:MotionPhotoVersion="1"
          GCamera:MotionPhotoPresentationTimestampUs="857949"
          xmpNote:HasExtendedXMP="9F6C5546DA50BD17DCB8DD1604C96BE6">
          <Container:Directory>
            <rdf:Seq>
              <rdf:li rdf:parseType="Resource">
                <Container:Item
                  Item:Mime="image/jpeg"
                  Item:Semantic="Primary"
                  Item:Length="0"
                  Item:Padding="0"/>
              </rdf:li>
              <rdf:li rdf:parseType="Resource">
                <Container:Item
                  Item:Mime="video/mp4"
                  Item:Semantic="MotionPhoto"
                  Item:Length="482189"
                  Item:Padding="0"/>
              </rdf:li>
            </rdf:Seq>
          </Container:Directory>
        </rdf:Description>
      </rdf:RDF>
    </x:xmpmeta>
```

> 正确的解析方式应该是: 从xmp中解析到 Item:Length="482189",然后从文件尾部读取482189的长度出来.

![image-20230530105032531](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/imagemac3/image-20230530105032531.png)
