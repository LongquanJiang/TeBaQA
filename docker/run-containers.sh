#!/usr/bin/env bash

docker run -itd --rm --name nlp --network=demo_network nlp:latest
docker run -itd --rm --name template-classification --network=demo_network template-classification:latest
docker run -itd --rm --name entity-linking --network=demo_network entity-linking:latest
docker run -itd --rm --name query-ranking --network=demo_network query-ranking:latest
docker run -itd --rm --name tebaqa-controller --network=demo_network tebaqa-controller:latest
