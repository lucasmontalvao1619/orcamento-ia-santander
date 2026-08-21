#!/usr/bin/env bash
# Monta os atalhos clicaveis do macOS a partir do icone e dos scripts.
#
# Eles nao ficam versionados de proposito: um .app e um diretorio com binario
# e plist dentro, e versionar isso significa guardar no historico do Git um
# arquivo que qualquer maquina reconstroi em um segundo. O que o repositorio
# guarda e a receita — este script — nao o produto.
set -e
cd "$(dirname "$0")/.."

criar_atalho() {
    local nome="$1" executavel="$2" identificador="$3" script="$4"
    local app="$nome.app"

    rm -rf "$app"
    mkdir -p "$app/Contents/MacOS" "$app/Contents/Resources"
    cp scripts/icone.icns "$app/Contents/Resources/icone.icns"

    cat > "$app/Contents/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>$nome</string>
    <key>CFBundleDisplayName</key>
    <string>$nome</string>
    <key>CFBundleIdentifier</key>
    <string>$identificador</string>
    <key>CFBundleExecutable</key>
    <string>$executavel</string>
    <key>CFBundleIconFile</key>
    <string>icone</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSMinimumSystemVersion</key>
    <string>11.0</string>
    <key>LSUIElement</key>
    <true/>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
PLIST

    cp "scripts/atalhos/$script" "$app/Contents/MacOS/$executavel"
    chmod +x "$app/Contents/MacOS/$executavel"
    echo "  $app"
}

echo "Gerando atalhos:"
criar_atalho "Iniciar Fast Finance Helper" "iniciar" "com.lucdev.fastfinance.iniciar" "iniciar"
criar_atalho "Parar Fast Finance Helper" "parar" "com.lucdev.fastfinance.parar" "parar"
echo "Pronto."
