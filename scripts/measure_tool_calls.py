"""Measures what the agent actually does to the database, not what it says.

An agent that answers pleasantly but calls the wrong tool - or the right tool
with wrong arguments - is worse than no agent: the reply looks like a success
either way. So every run here is judged by inspecting the accounts, never by
reading the model's prose.

    python scripts/measure_tool_calls.py --runs 10 --label llama3.1

Three scenarios, because the interesting number is not the happy path:

  valid_request     a legitimate request must produce exactly one correct account
  unknown_customer  an account for a customer who does not exist must not exist
  invalid_type      a product this bank does not sell must not be created

The last two are the ones that matter. They are what separates "the model
usually gets it right" from "a wrong answer cannot reach the database".

The model is chosen when agent-service starts, so comparing two models means
running this twice:

    LLM_MODEL=llama3.1 docker compose up -d --force-recreate agent-service
    python scripts/measure_tool_calls.py --runs 10 --label llama3.1

    LLM_MODEL=qwen2.5 docker compose up -d --force-recreate agent-service
    python scripts/measure_tool_calls.py --runs 10 --label qwen2.5
"""

import argparse
import json
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

RESULTS_DIR = Path(__file__).resolve().parent.parent / "results"

SCENARIOS = [
    {
        "name": "valid_request",
        "message": "open a current account of 5000 for customer 1",
        "expects": "one correct account",
        "expected_type": "CURRENT-ACCOUNT",
        "expected_customer": 1,
    },
    {
        "name": "unknown_customer",
        "message": "open a current account of 5000 for customer 99",
        "expects": "nothing created",
        "expected_type": None,
        "expected_customer": None,
    },
    {
        "name": "invalid_type",
        "message": "open a crypto wallet account of 5000 for customer 1",
        "expects": "nothing created",
        "expected_type": None,
        "expected_customer": None,
    },
]


def get_json(url: str, timeout: int):
    with urllib.request.urlopen(url, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def ask_agent(agent_url: str, message: str, timeout: int) -> str:
    url = f"{agent_url}/agent?" + urllib.parse.urlencode({"message": message})
    try:
        with urllib.request.urlopen(url, timeout=timeout) as response:
            return response.read().decode("utf-8")
    except urllib.error.HTTPError as error:
        return f"<HTTP {error.code}>"
    except Exception as error:            # timeout, connection reset, ...
        return f"<{type(error).__name__}>"


def classify(scenario: dict, before: list[dict], after: list[dict]) -> str:
    """Names what happened, so a failure rate can be acted on."""
    known = {account["id"] for account in before}
    created = [account for account in after if account["id"] not in known]

    if scenario["expected_type"] is None:
        # The guardrail scenarios: nothing should have been written.
        if not created:
            return "refused"
        # The service accepted it, so the arguments were valid - meaning the
        # model quietly turned an impossible request into a possible one.
        return "substituted"

    if not created:
        return "no_account_created"
    if len(created) > 1:
        return "several_accounts_created"

    account = created[0]
    if account.get("type") != scenario["expected_type"]:
        return "wrong_type"
    if account.get("customerId") != scenario["expected_customer"]:
        return "wrong_customer"
    return "correct"


GOOD_OUTCOMES = {"correct", "refused"}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--runs", type=int, default=10, help="runs per scenario")
    parser.add_argument("--agent-url", default="http://localhost:8060")
    parser.add_argument("--accounts-url", default="http://localhost:8057/accounts")
    parser.add_argument("--timeout", type=int, default=180)
    parser.add_argument("--label", default="unknown-model",
                        help="model name, used in the results table")
    args = parser.parse_args()

    started = time.time()
    report_rows = []
    detail_rows = []

    for scenario in SCENARIOS:
        outcomes: dict[str, int] = {}
        print(f"\n{scenario['name']} - expects {scenario['expects']}")

        for run in range(1, args.runs + 1):
            before = get_json(args.accounts_url, args.timeout)
            ask_agent(args.agent_url, scenario["message"], args.timeout)
            after = get_json(args.accounts_url, args.timeout)

            outcome = classify(scenario, before, after)
            outcomes[outcome] = outcomes.get(outcome, 0) + 1
            print(f"  [{run}/{args.runs}] {outcome}", flush=True)

        good = sum(count for name, count in outcomes.items() if name in GOOD_OUTCOMES)
        report_rows.append((scenario, good, outcomes))
        for name, count in sorted(outcomes.items(), key=lambda item: -item[1]):
            detail_rows.append((scenario["name"], name, count))

    elapsed = time.time() - started
    total_runs = args.runs * len(SCENARIOS)

    lines = [
        f"# Tool-call reliability - `{args.label}`",
        "",
        f"{args.runs} runs per scenario, {total_runs} in total, "
        f"{elapsed:.0f} s ({elapsed / total_runs:.0f} s per run).",
        "",
        "Every run is judged by reading the accounts afterwards. The agent's own",
        "reply is ignored: it announces success either way.",
        "",
        "| Scenario | Request | Expected | Held |",
        "|---|---|---|---:|",
    ]
    for scenario, good, _ in report_rows:
        lines.append(
            f"| `{scenario['name']}` | *\"{scenario['message']}\"* | "
            f"{scenario['expects']} | **{good}/{args.runs}** |")

    lines += ["", "### Outcomes in detail", "", "| Scenario | Outcome | Runs |", "|---|---|---:|"]
    for scenario_name, outcome, count in detail_rows:
        lines.append(f"| `{scenario_name}` | {outcome} | {count} |")

    lines += [
        "",
        "**Reading the outcomes**",
        "",
        "- `correct` - exactly one account, right type, right owner.",
        "- `refused` - nothing was written, which is the intended result for the",
        "  two guardrail scenarios.",
        "- `substituted` - an account *was* created, so the arguments passed",
        "  validation: the model silently turned an impossible request into a",
        "  possible one. The service behaved correctly; the model did not.",
        "",
    ]
    report = "\n".join(lines)

    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    destination = RESULTS_DIR / f"tool-call-reliability-{args.label}.md"
    destination.write_text(report, encoding="utf-8")

    print("\n" + report)
    print(f"-> written to {destination}")


if __name__ == "__main__":
    main()
