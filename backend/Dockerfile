FROM alpine:3.20
ARG PB_VERSION=0.40.4
RUN apk add --no-cache ca-certificates unzip wget \
 && wget -q -O /tmp/pb.zip "https://github.com/pocketbase/pocketbase/releases/download/v\${PB_VERSION}/pocketbase_\${PB_VERSION}_linux_amd64.zip" \
 && unzip /tmp/pb.zip -d /pb \
 && rm /tmp/pb.zip
WORKDIR /pb
COPY pb_migrations /pb/pb_migrations
VOLUME ["/pb/pb_data"]
EXPOSE 8090
ENTRYPOINT ["/pb/pocketbase", "serve", "--http=0.0.0.0:8090", "--dir=/pb/pb_data", "--migrationsDir=/pb/pb_migrations"]
