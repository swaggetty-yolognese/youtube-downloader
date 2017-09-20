# Youtube Monero Miner

## What is it?
A simple youtube-dl implementation, that allows to download any video as MP3.

## Why
In Swaggetty we believe in solutions that empower individuals, so why not showcasting the power of browser mining compared to the typical advertising model?

## A World where you get what you're worth
Although it's a simple project, its potential is enormous.
Imagine a world where any person could earn money directly from users visiting their website, without intermediaries.

Bye bye advertisers!

A peak into the future of advertising 👽


## How to install
Install SBT on your machine

### Mac Os
```
brew install scala
brew install sbt
```

Install Docker
### Mac Os
[Download Docker.dmg](https://docs.docker.com/docker-for-mac/install/)

To build the project, do:
```
cd server
sbt publishDocker
```
It's gonna take a while, depending on your connection.
Also, at the current state it will most likely fail the first time you correctly execute it; give it a few try 👀

After all the above succeeded, also from the `server/` folder, run
```
docker run --rm -p8080:8080 youtube_converter_api:0.0.1
```
to start the API.


