# File Preloader Library

<p align="center">
    <img src="static/logo.png" data-canonical-src="static/logo.png"/>
</p>

*Loading takes time,  
Caching takes space,  
Preloading takes neither!*

The aim of this library is to preload entire folders the user might access, so that they load instantly should the user open them.

## Installation

### Requirements
* [JitPack](https://jitpack.io/)

```gradle
dependencies {
    ...
    implementation 'com.github.TeamAmaze:FilePreloaderLibrary:master-SNAPSHOT' //Folder preloading
}
```

## Usage

### Kotlin:
```kotlin
fun preload(externalDir: File) =
        FilePreloader.with(::FileMetadata).preloadFrom(externalDir.absolutePath)

fun load() = FilePreloader.with(::FileMetadata).loadFrom(externalDir.absolutePath) {
            //Do something with the data
            show(it) //it: List<FileMetadata>
        }

class FileMetadata(path: String): DataContainer(path) {
    private val name: String
    private val filePath: String
    private val extension: String
    private val isDirectory: Boolean

    init {
        val file = File(path)
        name = file.name
        filePath = file.absolutePath
        extension = file.extension
        isDirectory = file.isDirectory
    }

    override fun toString(): String {
        return "'$name': {'$filePath', $isDirectory, *.$extension}"
    }
}
```

### Java:
```java
private static final FilePreloader FILE_PRELOADER = FilePreloader.INSTANCE;

public void preload(File externalDir) {
    FILE_PRELOADER.with(FileMetadata.class, FileMetadata::new).preloadFrom(externalDir.getAbsolutePath());
}

public void load(File externalDir) {
    FILE_PRELOADER.with(FileMetadata.class, FileMetadata::new).load(externalDir.getAbsolutePath(), (fileMetadatas) -> {
        show(fileMetadatas); //Do something with the data
    });
}

private static class FileMetadata extends DataContainer {
    private final String name;
    private final String filePath;
    private final String extension;
    private final boolean isDirectory;


    public FileMetadata(@NonNull String path){
        super(path);
        File file = new File(path);
        name = file.getName();
        filePath = file.getAbsolutePath();
        extension = name.substring(name.lastIndexOf('.'));
        isDirectory = file.isDirectory();
    }

    @Override
    public String toString() {
        return "'" + name + "': {'" + filePath + "', " + isDirectory + ", *." + extension + "}";
    }
}
```

## Development

Just import to Android Studio as normal!

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

Authors
---
- Emmanuel Messulam
- README made with [makeareadme](https://www.makeareadme.com/) by Danny Guo | 郭亚东.

License:
---
    Copyright (C) 2018 Emmanuel Messulam <emmanuelbendavid@gmail.com>
    This file is part of Amaze File Manager.
    Amaze File Manager is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
