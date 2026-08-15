# syntax=docker/dockerfile:1.7

FROM golang:1.27rc2-bookworm@sha256:a2f9daa5dbd9f7a68eb3c32cf91e4f9fc50a11a07f8b9cd9ffa542d2298d9f82 AS build

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
