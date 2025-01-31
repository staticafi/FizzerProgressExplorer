# **Fizzer: ProgressExplorer**

The **ProgressExplorer** is a tool for exploring progress of the analyses
implemented in **Fizzer** on a benchmark program.

When **Fizzer** is run with the option --progress-recorder, then the fuzzer
records progress of its analyses to the disk. The **ProgressExplorer** allows
for exploration of the data in user friendly way. 

The purpose of **ProgressExplorer** is provide an aid to the process of
improving **Fizzer**'s effectiveness.

## License

**ProgressExplorer** is available under the **zlib** license. It is included as
file `LICENSE.txt` into the repository:
https://gitlab.fi.muni.cz/qtrtik/sbt-fizzer_progress_explorer

## Target platforms

The primary target platform is PC running either Windows 10 (or later) or Ubuntu
22.04 (or later) operating systems.

Java 8 runtime (or later) must be installed on the computer.

## Building

Type this command to the terminal:
```
mvn package
```
The result is JAR file:
```
ProgressExplorer-1.0-SNAPSHOT-jar-with-dependencies.jar
```
Located in the `target` directory created under the project's directory.

## Executing

Type this command to the terminal:
```
java -Xss100m -Xms512m -Xmx10000m -jar <path-to>/ProgressExplorer-1.0-SNAPSHOT-jar-with-dependencies.jar [<data-dir>]
```
The optional `<data-dir>` is a *directory* under which is stored recording of
FIzzer's progress.