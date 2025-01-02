# Amarr - aMule *arr Connector

This connector allows using aMule as a download client for [Sonarr](https://sonarr.tv/)
and [Radarr](https://radarr.video/).
It works by emulating a torrent client, so Sonarr and Radarr will manage your downloads as if they were torrents.

Makes use of [jaMule](https://github.com/vexdev/jaMule) to connect to aMule, which only supports aMule versions **2.3.1** to **2.3.3**.

Amarr has been especially tested with the latest released version of [Adunanza](https://www.adunanza.net/).

## Pre-requisites

- [aMule](https://www.amule.org/) version **2.3.1** to **2.3.3** running and configured
- [Sonarr](https://sonarr.tv/) or [Radarr](https://radarr.video/) running and configured

**Amarr does not come with its own amule installation**, you need to have it running and configured.
One way to do this is by using the [Amule Docker image from ngosang](https://github.com/ngosang/docker-amule).
Or the [Adunanza Docker image from m4dfry](https://github.com/m4dfry/amule-adunanza-docker).
Or again you could run aMule in a VM or in a physical machine.

## Installation

It requires the following environment variables:

```
AMULE_HOST: aMule # The host where aMule is running, for docker containers it's usually the name of the container
AMULE_PORT: 4712 # The port where aMule is listening with the EC protocol
AMULE_PASSWORD: secret # The password to connect to aMule

Optional parameters:
AMULE_FINISHED_PATH: /finished # The directory where aMule will download the finished files
AMARR_LOG_LEVEL: INFO # The log level of amarr, defaults to INFO
```

## Radarr/Sonarr configuration (2 easy steps)

### 1. Configure amarr as a download client

You will need to add the download client. 

You can do that by adding a new download client of type **qBittorrent** with the following settings:

```
! Ensure you pressed the "Show advanced settings" button
Name: Any name you want
Host: amarr # The host where amarr is running, for docker containers it's usually the name of the container
Port: 4713 # The port where amarr is listening
Priority: 50 # This is the lowest possible priority, so Sonarr/Radarr will prefer other download clients
```

### 2. Configure amarr as a torrent indexer

Amarr provides multiple indexers with different capabilities. 
Each indexer implements the **Torznab** protocol, so it can be used as a torrent indexer for Sonarr/Radarr.

Add a new **Torznab indexer** with the following settings:

```
! Ensure you pressed the "Show advanced settings" button
Name: Any name you want
Url: http://amarr:4713/indexer/<indexer>
Download Client: The name you gave to amarr in the previous step
```

**Note:** You need to configure Sonarr/Radarr to prefer amarr as a download client for any indexers we created before.

**Note:** `<indexer-type>` is [one of the indexers supported by amarr](#indexers).

**You will have to do this for every indexer you want to use with amarr.**

## Indexers

### `amule`

This indexer will search for files in aMule through the kad/eD2k network.

It is very slow and not very reliable. Additionally, files on the kad/eD2k network are not well reviewed, so you may end
up downloading fake files.

Does not require any additional configuration.