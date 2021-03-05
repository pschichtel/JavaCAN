#!/usr/bin/env bash

java_home="${1?no java home given}"
libname="${2?no lib name given}"
output_dir="${3?no output directory given}"
version="${4?no version given}"
arch="${5?no arch given}"

DOCKCROSS_TAG="${DOCKCROSS_TAG:-latest}"

if ! [ -e "$java_home/include/jni.h" ]
then
  java_home="$(dirname "$java_home")"
fi

cc_opts=('-shared' '-std=c99' '-fPIC' '-D' "MVN_VERSION=$version")

if grep -Pq -- '-SNAPSHOT$' <<< "$version"
then
    cc_opts+=('-g3' '-Og')
else
    cc_opts+=('-O2' '-flto')
fi

base="target"
jni="$base/jni"
jni_headers="$base/java-jni-headers"
jni_libs="$base/java-jni-libs"
src="src/main/c"
rm -R "$jni_headers" "$jni_libs" 2>/dev/null
cp -r "${java_home}/include" "$jni_headers"
cp -r "${java_home}/lib" "$jni_libs"

mkdir -p "$output_dir" 2>/dev/null

compiler_dir="$base/cross-compile"

echo "Compiling for: ${arch}"
image="dockcross/linux-${arch}:${DOCKCROSS_TAG}"
dir="$compiler_dir/$arch"
mkdir -p "$dir" 2>/dev/null
proxy="${dir}/proxy"
docker pull "$image"
docker run --rm "$image" >"$proxy"
chmod +x "$proxy"

lib_output="$base/lib$libname.so"
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
for c_file in "$src"/*.c "$jni"/**/*.c
do
    name="$(basename "$c_file" .c)"
    out_file="$dir/$(dirname "$c_file")/$name.o"
    mkdir -p "$(dirname "$out_file")"
    "$proxy" "$CC" "${includes[@]}" -Werror -o"$out_file" -c "$c_file" "${cc_opts[@]}" || exit 1
    out_files+=("$out_file")
done
"$proxy" "$CC" -I "$jni_libs" -o"$lib_output" "${out_files[@]}" -z noexecstack "${cc_opts[@]}" -fvisibility=hidden || exit 1
mv "$lib_output" "$output_dir"
