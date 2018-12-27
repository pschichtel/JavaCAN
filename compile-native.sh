#!/usr/bin/env bash

java_home="${1?no java home given}"
libname="${2?no lib name given}"
output_dir="${3?no output directory given}"

base="target"
jni_headers="$base/java-jni-headers"
jni_libs="$base/java-jni-libs"
src="src/main/c"
rm -R "$jni_headers" "$jni_libs" 2>/dev/null
cp -r "${java_home}/include" "$jni_headers"
cp -r "${java_home}/lib" "$jni_libs"

mkdir -p "$output_dir" 2>/dev/null

translate_arch() {
    case "$1" in
        x86)
            echo "x86_32"
            ;;
        x64)
            echo "x86_64"
            ;;
        arm64)
            echo "aarch64"
            ;;
       *)
        echo "$1"
        ;;
    esac
}

if [[ -z "${BUILD_ARCH}" ]]
then
    archs=(x86 x64 armv7 arm64)
else
    archs=(${BUILD_ARCH})
fi

compiler_dir="$base/cross-compile"

for arch in "${archs[@]}"
do
    translated_arch=$(translate_arch "$arch")
    echo "Compiling for: $translated_arch"
    image="dockcross/linux-$arch"
    dir="$compiler_dir/$arch"
    mkdir -p "$dir" 2>/dev/null
    proxy="${dir}/proxy"
    docker run --rm "$image" > "$proxy"
    chmod +x "$proxy"

    lib_output="$base/lib$libname-${translated_arch}.so"
    c_files=(helpers javacan_socketcan javacan_epoll)
    includes=(
        -I"$src"
        -I"$base/jni"
        -I"src/include"
        -I"$jni_headers"
        -I"$jni_headers/linux"
    )
    out_files=()
    CC="$("$proxy" bash -c 'echo "$CC"')"
    for c_file in "${c_files[@]}"
    do
        out_file="$dir/$c_file.o"
        "$proxy" "$CC" "${includes[@]}" -o"$out_file" -c "$src/$c_file.c" -shared -fPIC -std=c99 || exit 1
        out_files+=("$out_file")
    done
    "$proxy" "$CC" -I "$jni_libs" -o"$lib_output" "${out_files[@]}" -z noexecstack -fPIC -fvisibility=hidden -std=c99 -shared || exit 1
    mv "$lib_output" "$output_dir"
done
