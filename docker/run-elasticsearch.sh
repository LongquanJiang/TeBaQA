#!/usr/bin/env bash

export TAKE_FILE_OWNERSHIP=true
sudo docker run -itd --rm --name elasticsearch --group-add 0 --mount type=bind,source=/data/elasticsearch-data/data,target=/usr/share/elasticsearch/data --network=demo_network -p 9200:9200 docker.elastic.co/elasticsearch/elasticsearch:6.6.1
