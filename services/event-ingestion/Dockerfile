# syntax=docker/dockerfile:1.7

FROM golang:1.27.1-bookworm@sha256:69a7b9788769bec032d238959b61854e9ae87f57be9029ec04e9885fabf99195 AS build

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
