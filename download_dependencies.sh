#!/bin/bash

# Cria a pasta de destino para as dependências
mkdir -p src/main/libs

# Função para baixar e extrair uma dependência
download_and_extract() {
  local url=$1
  local zip_name=$2
  local jar_path=$3

  echo "Baixando $zip_name de $url..."
  curl -L $url -o $zip_name

  echo "Extraindo $zip_name..."
  unzip -o $zip_name -d temp

  echo "Movendo $jar_path para src/main/libs/..."
  mv temp/$jar_path src/main/libs/

  echo "Limpando arquivos temporários..."
  rm -rf temp $zip_name
}

# Baixa e extrai cada dependência individualmente
download_and_extract "https://github.com/Syphon/Processing/releases/download/latest/Syphon.zip" "Syphon.zip" "Syphon/library/Syphon.jar"
download_and_extract "https://github.com/sojamo/controlp5/releases/download/v2.2.6/controlP5-2.2.6.zip" "controlP5.zip" "controlP5/library/controlP5.jar"
download_and_extract "https://github.com/leadedge/SpoutProcessing/releases/download/latest/spout.zip" "spout.zip" "spout/library/spout.jar"

echo "Todas as dependências foram baixadas e extraídas com sucesso!"
