#!/usr/bin/env bash

set -euo pipefail

java_home="${1?no java home given}"
libname="${2?no lib name given}"
version="${3?no version given}"
dockcross_image="${4?no arch given}"
classifier="${5?no classifier given}"
link_mode="${6?no link mode}"

if ! [ -e "$java_home/include/jni.h" ]; then
    java_home="$(dirname "$java_home")"
fi

cc_opts=('-shared' '-std=c99' '-fPIC' '-D' "MVN_VERSION=$version")

if grep -Pq -- '-SNAPSHOT$' <<<"$version"; then
    cc_opts+=('-g3' '-Og')
else
    cc_opts+=('-O2' '-flto')
fi

relative_output_dir="target"

jni="${relative_output_dir}/jni"
jni_headers="${relative_output_dir}/java-jni-headers"
jni_libs="${relative_output_dir}/java-jni-libs"
src="src/main/c"

rm -Rf "$jni_headers" "$jni_libs" 2>/dev/null
cp -r "${java_home}/include" "$jni_headers"
cp -r "${java_home}/lib" "$jni_libs"

compiler_dir="${relative_output_dir}/cross-compile"

echo "Compiling for: ${classifier} (dockcross: ${dockcross_image})"
compiler_output_dir="${compiler_dir}/${classifier}"
mkdir -p "$compiler_output_dir" 2>/dev/null
proxy="${compiler_output_dir}/proxy"


if [ -n "$(which podman)" ]
then
    podman run --rm "$dockcross_image" > "$proxy"
elif [ -n "$(which docker)" ]
then
    docker run --rm "$dockcross_image" > "$proxy"
else
    echo "Either podman or docker are required to build JavaCAN!"
fi
chmod +x "$proxy"

linker_output_dir="${relative_output_dir}/native/${classifier}/native"
mkdir -p "$linker_output_dir" 2>/dev/null
so_name="lib$libname.so"
linker_output="${linker_output_dir}/${so_name}"

includes=(
    -I"$src"
    -I"$jni"
    -I"src/include"
    -I"$jni_headers"
    -I"$jni_headers/linux"
)
out_files=()
# shellcheck disable=SC2016
CC="$("$proxy" bash -c 'echo "$CC"' | tr -d '\r')"
for c_file in "$src"/*.c "$jni"/**/*.c; do
    name="$(basename "$c_file" .c)"
    out_file="$compiler_output_dir/$(dirname "$c_file")/$name.o"
    mkdir -p "$(dirname "$out_file")"
    "$proxy" "$CC" "${includes[@]}" -Werror -o"$out_file" -c "$c_file" "${cc_opts[@]}" || exit 1
    out_files+=("$out_file")
done

case "$link_mode" in
    static)
        echo "static linking"
        link_mode_options=(-static)
        ;;
    dynamic)
        echo "dynamic linking"
        link_mode_options=()
        ;;
    *)
        echo "Unknown link mode '$link_mode'" >&2
        exit 1
        ;;
esac

"$proxy" "$CC" -I "$jni_libs" -o"$linker_output" "${out_files[@]}" "${link_mode_options[@]}" -z noexecstack "${cc_opts[@]}" -fvisibility=hidden || exit 1
