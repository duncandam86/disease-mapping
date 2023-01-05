#!/bin/zsh

if [[ ! -d ./resources/stage_0_outputs ]]; then
  mkdir ./resources/stage_0_outputs
fi

if [[ ! -d ./resources/stage_1_outputs ]]; then
  mkdir ./resources/stage_1_outputs
fi

if [[ ! -d ./resources/stage_2_outputs ]]; then
  mkdir ./resources/stage_2_outputs
fi

if [[ ! -d ./resources/stage_3_outputs ]]; then
  mkdir ./resources/stage_3_outputs
fi

if [[ ! -d ./resources/stage_4_outputs ]]; then
  mkdir ./resources/stage_4_outputs
fi

if [[ ! -d ./neo4j/import ]]; then
  mkdir ./neo4j/import
fi

echo "###################################"
echo "STARTING STAGE 0"
echo "###################################"

clj -M:stage-0

echo "###################################"
echo "STAGE 0 FINISHED"
echo "###################################"

echo "\n"

echo "###################################"
echo "STARTING STAGE 1"
echo "###################################"

clj -M:stage-1

echo "###################################"
echo "STAGE 1 FINISHED"
echo "###################################"

echo "\n"

echo "###################################"
echo "STARTING STAGE 2"
echo "###################################"

clj -M:stage-2

echo "###################################"
echo "STAGE 2 FINISHED"
echo "###################################"

echo "###################################"
echo "STARTING STAGE 3"
echo "###################################"

clj -X:disco

echo "###################################"
echo "STAGE 3 FINISHED"
echo "###################################"


echo "###################################"
echo "STARTING STAGE 4"
echo "###################################"

clj -X:stage-4

echo "###################################"
echo "STAGE 4 FINISHED"
echo "###################################"

echo "###################################"
echo "Copy nodes and relationships to ./neo4j/import folder"
echo "###################################"

cp ./resources/stage_4_outputs/* ./neo4j/import