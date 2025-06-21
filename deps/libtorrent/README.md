# LibTorrent Build System

This repository contains a build system for LibTorrent and its dependencies, including Boost and OpenSSL.

## Usage

1. Add dependency in your app's `build.gradle`:
```gradle
android {
    buildFeatures {
        prefab true
    }
}

dependencies {
    implementation 'io.github.yearsyan:yaad-libtorrent:0.1-alpha'
}
```

2. Add dependency in your `CMakeLists.txt`:
```cmake
find_package(libtorrent REQUIRED CONFIG)

add_library(mylib SHARED mylib.cpp)
target_link_libraries(mylib libtorrent::torrent-rasterbar)
```

## Supported Architectures

The library supports the following architectures:
- `arm64-v8a`: ARM 64-bit
- `x86_64`: x86 64-bit

## Build Yourself

To build the library yourself, simply run:

```bash
./build.sh
```

The built AAR file will be output to `build/libtorrent-${version}.aar`.

## Publish to Maven Central

To publish the library to Maven Central, you need to set up the following environment variables:

1. Generate and distribute your GPG public key:
   - Follow the guide at https://central.sonatype.org/publish/requirements/gpg/#distributing-your-public-key
   - This step is required for signing your artifacts

2. Set your GPG password:
```bash
export GPG_PASSWORD="your-gpg-password"
```

3. Generate Sonatype authentication token:
   - Visit https://central.sonatype.org/publish/generate-portal-token/
   - Generate your username and password
   - Create the token using:
```bash
printf "your-username:your-password" | base64
export SONATYPE_AUTH_TOKEN="generated-base64-token"
```

