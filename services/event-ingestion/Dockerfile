# syntax=docker/dockerfile:1.7

FROM golang:1.27.3-bookworm@sha256:d9b6a1beb9474adc2a1ce842573639b44b2a0dd49b6669f51d3821baf9ec37c9 AS build

ARG TARGETOS
ARG TARGETARCH

WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=${TARGETOS:-linux} GOARCH=${TARGETARCH:-amd64} \
    go build -trimpath -ldflags='-s -w' -o /out/event-ingestion ./cmd/event-ingestion

FROM scratch

COPY --from=build /out/event-ingestion /event-ingestion
USER 65532:65532
EXPOSE 18080
HEALTHCHECK --interval=5s --timeout=2s --start-period=2s --retries=6 CMD ["/event-ingestion", "--healthcheck"]
ENTRYPOINT ["/event-ingestion"]
