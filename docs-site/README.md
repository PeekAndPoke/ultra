# docs-site

The source of [peekandpoke.io](https://peekandpoke.io), built with [Astro](https://astro.build).

## Project Structure

```text
/
├── public/                    static files, served as they are
├── src/
│   ├── components/
│   ├── data/
│   │   ├── site.ts            versions, group ids and artifacts
│   │   ├── llms/              sources of the LLM mirrors (llms.txt, llms-full.txt, /llms/<library>.md)
│   │   └── releaseNotes.ts    renders docs/release-notes/<version>.md of the repository
│   ├── layouts/
│   ├── pages/                 one route per file, the library docs under ultra/
│   └── styles/
└── package.json
```

## Commands

All commands are run from `docs-site/`:

| Command             | Action                                      |
|:--------------------|:--------------------------------------------|
| `pnpm install`      | Installs dependencies                       |
| `pnpm dev`          | Starts local dev server at `localhost:4321` |
| `pnpm build`        | Builds the production site to `./dist/`     |
| `pnpm preview`      | Previews the build locally, before deploying |
| `pnpm astro ...`    | Runs Astro CLI commands like `astro add`    |
| `pnpm astro --help` | Gets help using the Astro CLI               |
