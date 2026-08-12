# syntax=docker/dockerfile:1.7

FROM golang:1.26.5-bookworm@sha256:53eeac89074db483fdf0ab3be1df32bf6e47562263d2d0d6baa7f26acb4957dd AS build

WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
    go build -trimpath -ldflags='-s -w' -o /out/event-ingestion ./cmd/event-ingestion

FROM scratch

COPY --from=build /out/event-ingestion /event-ingestion
USER 65532:65532
EXPOSE 18080
HEALTHCHECK --interval=5s --timeout=2s --start-period=2s --retries=6 CMD ["/event-ingestion", "--healthcheck"]
ENTRYPOINT ["/event-ingestion"]
