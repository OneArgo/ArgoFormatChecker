FROM ghcr.io/oneargo/argoformatchecker/app:develop AS base

USER root

# Install Poetry for dependency management.
ENV POETRY_HOME=/opt/poetry
ENV PATH="${POETRY_HOME}/bin:${PATH}"

RUN \
    apk add --no-cache \
        python3 \
        py3-pip \
        curl \
        build-base \
        python3-dev \
        git && \
    ln -sf /usr/bin/python3 /usr/bin/python && \
    mkdir -p /opt/poetry/project /opt/poetry/bin && \
    curl -sSL https://install.python-poetry.org | python -

# Install dependencies.
COPY argofilechecker-python-wrapper/pyproject.toml argofilechecker-python-wrapper/poetry*.lock argofilechecker-python-wrapper/README.md /opt/poetry/dep/
RUN \
    cd /opt/poetry/dep && \
    poetry config virtualenvs.create false && \
    if [ ! -f poetry.lock ]; then poetry lock; fi && \
    poetry install --no-root && \
    poetry env info

FROM base AS build-package
# Ephemeral environment for building packages.
RUN --mount=type=bind,source=./argofilechecker-python-wrapper,target=/opt/dep,rw=true \
    cd /opt/dep && \
    rm -rf ./dist && \
    POETRY_DYNAMIC_VERSIONING_BYPASS=1.0.0 poetry build && \
    mkdir /opt/build && \
    mv ./dist/*.whl /opt/build

FROM base AS file-checker-python

LABEL org.opencontainers.image.authors="yvan.lubac@euro-argo.eu"
LABEL org.opencontainers.image.description="Docker image for Argo File Checker Python Wrapper"
LABEL org.opencontainers.image.url="https://github.com/British-Oceanographic-Data-Centre/amrit-repos"
LABEL org.opencontainers.image.source="https://github.com/British-Oceanographic-Data-Centre/amrit-repos"
LABEL org.opencontainers.image.documentation="https://github.com/British-Oceanographic-Data-Centre/amrit-repos/wiki"
LABEL org.opencontainers.image.licenses="TBD"
LABEL org.opencontainers.image.vendor="Advance Marine Research Infrastructures Together (AMRIT)"

#env variables for file checker
ENV FILE_CHECKER_JAR="/app/app.jar"
ENV FILE_CHECKER_SPECS="/app/file_checker_spec"

RUN --mount=type=bind,from=build-package,source=/opt/build,target=/opt/build \
    python -m venv /opt/venv && \
    /opt/venv/bin/pip install /opt/build/*.whl && \
    /opt/venv/bin/python -c "from argofilechecker_python_wrapper import FileChecker; print('OK')"


ENV PATH="/opt/venv/bin:${PATH}"

WORKDIR /home/app

USER fileCheckerRunner

#overide entrypoint from base image
ENTRYPOINT ["/opt/venv/bin/python"]

FROM base AS file-checker-api

LABEL org.opencontainers.image.description="Docker image for Argo File Checker Python API"
LABEL org.opencontainers.image.url="https://github.com/British-Oceanographic-Data-Centre/amrit-repos"
LABEL org.opencontainers.image.source="https://github.com/British-Oceanographic-Data-Centre/amrit-repos"

#env variables for file checker
ENV FILE_CHECKER_JAR="/app/app.jar"
ENV FILE_CHECKER_SPECS="/app/file_checker_spec"

RUN --mount=type=bind,from=build-package,source=/opt/build,target=/opt/build \
    python -m venv /opt/venv && \
    /opt/venv/bin/pip install /opt/build/*.whl && \
    /opt/venv/bin/python -c "from argofilechecker_python_wrapper import FileChecker; print('OK')"

WORKDIR /home/app
COPY argo-file-checker-python-api/ ./

RUN --mount=type=bind,from=build-package,source=/opt/build,target=/opt/build \
    poetry config virtualenvs.create false && \
    poetry add /opt/build/*.whl && \
    if [ ! -f poetry.lock ]; then poetry lock; fi && \
    poetry install --no-root && \
    poetry env info

ENV PATH="/opt/venv/bin:${PATH}"
RUN mkdir /home/app/input
RUN chown fileCheckerRunner:gcontainer /home/app/input

USER fileCheckerRunner

#overide entrypoint from base image
ENTRYPOINT ["poetry"]
CMD ["run", "gunicorn", "file_checker_api:app", "-b", "0.0.0.0:8000", "-k", "uvicorn.workers.UvicornWorker", "--timeout", "300"]
