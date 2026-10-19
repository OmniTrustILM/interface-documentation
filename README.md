# ILM Interface Documentation

> This repository is part of the open source project ILM. You can find more information about the project at the [ILM](https://github.com/OmniTrustILM/ilm) repository, including the contribution guide.

`Interface Documentation` is the API reference for the ILM platform: the OpenAPI documents of the Core APIs and of the interfaces between Core and its connectors, rendered as HTML pages. The current release is published at [api.otilm.com](https://api.otilm.com/).

## Container image

The image `hub.omnitrustregistry.com/ilm/interface-documentation` serves the same pages with nginx, for environments that need the API reference inside their own network:

```bash
docker run --rm -p 8080:80 hub.omnitrustregistry.com/ilm/interface-documentation:<version>
```

The documentation is then available at `http://localhost:8080/docs/`.

## Building

Maven generates the OpenAPI documents into `openapi/`:

```bash
mvn clean verify
```

`lint.sh` checks them and `bundle.sh` renders them to HTML, both with the [Redocly CLI](https://redocly.com/docs/cli/). The `Dockerfile` runs the same steps to build the image.

## License

Licensed under the Apache License 2.0. See [LICENSE.md](LICENSE.md).
