# Routing: who does what

The orchestrator reads this file whenever it hands out work. Edit it at any time, or tell the orchestrator to ("send
the advanced tasks to Sonnet", "Codex is out until six"): it changes this file and uses the new routing from the next
task. The model names are the ones in use in November 2026; change them to what you have.

## By kind of work

Only Claude workers in this project: no Codex, no Grok. The first worker that is available takes the task; the next
one is the fallback when the first is out of quota or failed twice.

| Work | First | Then |
|---|---|---|
| mechanical (follows a pattern already in the code) | claude-sonnet | claude-opus |
| bounded (fully described by the model and the test data) | claude-sonnet | claude-opus |
| advanced (several contexts, a scheduler, an external API) | claude-opus | claude-sonnet |
| review (one slice, full suite) | claude-opus | claude-sonnet |

A review is done by a worker that implemented none of the slice when there is one: Opus reviews slices Sonnet
implemented. A slice with Opus tasks is reviewed by a fresh Opus subagent (another context, same model), and the
orchestrator says so in its report.

## Workers

| Name | How it runs | Model | Notes |
|---|---|---|---|
| claude-sonnet | Agent tool, `subagent_type: implementer` or `reviewer`, `model: sonnet`, `run_in_background: true` | sonnet | parallel is fine; one per clone |
| claude-opus | the same, `model: opus` | opus | |

## Limits

- Tasks running at once: 3.
- Two tasks run together only when they touch different bounded contexts (see the `model:` files of each).

## Files the workers must never change

Checked on every task before it is committed. Edit the list to the project's layout.

```text
*.zfl *.zdl *openapi*.yml *asyncapi*.yml
src/test/resources/data.sql
src/test/resources/data/*
```

## Commands of this project

The orchestrator and the workers use the ones in `docs/zenwave-sdd-overlay.md`. Only set here what is specific to
unattended work: nothing by default.
