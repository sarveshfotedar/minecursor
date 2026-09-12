#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
JAR="$ROOT/release/cursorlink-0.1.0.jar"

if [[ ! -f "$JAR" ]]; then
  echo "Missing $JAR"
  exit 1
fi

candidates=()
if [[ "$(uname -s)" == "Darwin" ]]; then
  candidates+=(
    "$HOME/Library/Application Support/minecraft/mods"
    "$HOME/Library/Application Support/PrismLauncher/instances"
    "$HOME/Library/Application Support/com.modrinth.theseus/profiles"
    "$HOME/Library/Application Support/curseforge/minecraft/Instances"
  )
  official_mods="$HOME/Library/Application Support/minecraft/mods"
else
  candidates+=(
    "$HOME/.minecraft/mods"
    "$HOME/.local/share/PrismLauncher/instances"
    "$HOME/.local/share/modrinth-app/profiles"
    "$HOME/.curseforge/minecraft/Instances"
  )
  official_mods="$HOME/.minecraft/mods"
fi

install_into() {
  local mods_dir="$1"
  mkdir -p "$mods_dir"
  cp "$JAR" "$mods_dir/cursorlink-0.1.0.jar"
  echo "Installed: $mods_dir/cursorlink-0.1.0.jar"
  if ! ls "$mods_dir"/*abric*api* >/dev/null 2>&1 && ! ls "$mods_dir"/fabric-api*.jar >/dev/null 2>&1; then
    echo "Note: I did not see Fabric API in that folder. Download it for 26.2 and put it in the same mods folder."
  fi
}

installed=0
if [[ -d "$(dirname "$official_mods")" ]]; then
  install_into "$official_mods"
  installed=1
fi

search_roots=("${candidates[@]:1}")
existing_roots=()
for root in "${search_roots[@]}"; do
  if [[ -d "$root" ]]; then
    existing_roots+=("$root")
  fi
done

if [[ ${#existing_roots[@]} -gt 0 ]]; then
  while IFS= read -r mods_dir; do
    [[ -n "$mods_dir" ]] || continue
    install_into "$mods_dir"
    installed=1
  done < <(find "${existing_roots[@]}" -type d -name mods 2>/dev/null)
fi

if [[ "$installed" -eq 0 ]]; then
  echo "Could not find a Minecraft mods folder."
  echo "Create this folder, then run this script again:"
  echo "  $official_mods"
  exit 1
fi

echo
echo "Next: start the helper on this same computer with ./start-helper.sh"
echo "Then launch the 26.2 Fabric profile and press K."
