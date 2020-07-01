# Build

```
docker build \
  --build-arg PROXY_HOST=$PROXY_HOST \
  --build-arg PROXY_PORT=$PROXY_PORT \
  --build-arg PROXY_USER=$PROXY_USER \
  --build-arg PROXY_PASS=$PROXY_PASS \
  --build-arg http_proxy=$http_proxy \
  --build-arg https_proxy=$https_proxy \
  -t eval-spec-maker \
  .
```

# Run

```
docker run --rm -t \
  -v $PWD/testcase.md:/testcase.md \
  -v $PWD/out:/out \
  eval-spec-maker \
  java -jar /src/export/evalSpecMaker.jar /testcase.md /out/testcase.xlsx
```
