
These instructions are for Linux (specifically was tested on Ubuntu 22.04 LTS)

### Dependencies

Install the ICU, Jansson, ALSA, Readline, Edit, GCC Multilib Libraries:
```bash
sudo apt-get install libicu-dev libjansson-dev libasound2-dev libreadline-dev libedit-dev gcc-multilib
```
You will also need Java 17 or later. You can install this with:
```bash
sudo apt install openjdk-17-jdk openjdk-17-jre
```

While building, I personally ran into a couple errors which required adding:

```gradle
distTar {
    duplicatesStrategy = 'INCLUDE'
}

distZip {
    duplicatesStrategy = 'INCLUDE'
}
```
to the `/perf-test-java/gradle.build` file.

### Building

```bash
./gradlew build
```

### Running

```bash
./gradlew run
```
