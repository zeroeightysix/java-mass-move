# jmm
A small tool to rename files using regular expressions.
Not really meant for production. Just a small program I made for fun.

## Installing on linux
```
git clone https://github.com/zeroeightysix/java-mass-move/
cd java-mass-move
./gradlew build
cd build/libs
echo -e "#!/bin/bash\njava -jar $PWD/jmm.jar -d \"\$PWD\" \"\$@\"" > jmm
chmod +x jmm
sudo mv jmm /usr/bin/
```
Will create a script `jmm` that references the current location of `jmm.jar` and executes it. Puts it in `/usr/bin` so you can use it anywhere as if a normal command.

## Usage
```
Usage: jmm [-chnorsvV] [-fv] [-d=<directory>] SELECTOR TARGET PROCESSORS...
      SELECTOR             Regex to find targeted files
      TARGET               Regex to rename files to
      PROCESSORS...        Reference modifiers
  -c, --confirm            Ask for confirmation upon acting on a file
  -d, --directory=<directory>
                           Defines the working directory of JMM
      -fv, --fineverbose   Fine verbose mode.
  -h, --help               Show this help message and exit.
  -n, --noact              Do everything but act on files
  -o, --no-overwrite       Do not overwrite files
  -r, --recursive          Recursively check directories
  -s, --stacks             Print stack traces
  -v, --verbose            Verbose mode.
  -V, --version            Print version information and exit.
```

`SELECTOR`:     Any file matching this regex will be targeted by JMM.

`TARGET`:       What to rename all targeted files to. To reference groups from their original name (originating from the `SELECTOR` regex), use `$<group number>`.

`PROCESSORS`:   See processors below. In short: these modify all references to groups in `TARGET`

`--confirm`:    Before renaming a file, jmm will ask for confirmation, displaying the original and new name.

`--directory`:  Sets the directory where JMM will search for files.

`-fv`:          Fine verbose mode. Displays all logging.

`--help`:       Shows all arguments

`--noact`:      Do everything but don't actually move files: helpful if you'd like to check your results first.

`-o`:           Don't overwrite already existing files.

`--recursive`:  Searches subdirectories of the specified working directory (`-d`) as well.

`--stacks`:     Show java exception stack traces instead of simple error messages.

`--verbose`:    Show logging.

`--version`:    Shows your current JMM version

### Processors
Processors modify the value of references to groups in your `TARGET` regex.
Say, you have a file `birthday_cats.jpg`, which you would like to be renamed to `Birthday cats.jpg`, use `REMOVE_SNAKE` and then `NORMAL_CASE`:
```
jmm "(.*)\.jpg" "$1.jpg" REMOVE_SNAKE_CASE NORMAL_CASE
```
Will:
* Match `birthday_cats.jpg` where `$1` is `birthday_cats`
* Apply `REMOVE_SNAKE_CASE` and `NORMAL_CASE` to `$1`:
  * `birthday_cats` -> `birthday cats`
  * `birthday cats` -> `Birthday cats`
* Move `birthday_cats.jpg` to `Birthday cats.jpg` (`$1.jpg`)

**note:** Processors are applied in the order they are supplied, and each takes in the output of the previous processor.

#### Existing processors
| Name | Operator | Example |
| - | - | - |
| REMOVE_SNAKE_CASE   | Replaces `_` with a space | `birthday_cat` → `birthday cat` |
| TITLE_CASE | Capitalises every character after a space | `birthday cat` → `Birthday Cat` |
| NORMAL_CASE | Capitalises the very first character | `birthday cat` → `Birthday cat` |
| LOWER_CASE_FIRST | Lowercase every character after a space | `BIRTHDAY CAT` → `bIRTHDAY cAT` |
| UPPERCASE | Convert all to uppercase | `bIrtHdAy CaT` → `BIRTHDAY CAT` |
| LOWERCASE | Convert all to lowercase | `bIrtHdAy CaT` → `birthday cat` |
| REMOVE_SPACES | Removes all spaces | `Birthday cat` → `Birthdaycat` |

### Examples

#### Removing branding from files

| From | To |
| - | - |
| `[EPIC_BRAND] 01 epicSong.mp4` | `01 epicSong.mp4` |
| `[EPIC_BRAND] 02 dopeSong.mp4` | `02 dopeSong.mp4` |
| `[EPIC_BRAND] 03 litSong.mp4` | `01 litSong.mp4` |
| ... | ... |

```
jmm "\[.*\](.*)" "$1"
```

#### Re-casing poorly named files

| From | To |
| - | - |
| `THE RISING.mp3` | `The rising.mp3` |
| `THE DOWNFALL.mp3` | `The downfall.mp3` |
| ... | ... |

```
jmm "(.*)" "$1" LOWERCASE NORMAL_CASE
```
