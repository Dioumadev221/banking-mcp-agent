# Tool-call reliability - `qwen2.5`

10 runs per scenario, 30 in total, 683 s (23 s per run).

Every run is judged by reading the accounts afterwards. The agent's own
reply is ignored: it announces success either way.

| Scenario | Request | Expected | Held |
|---|---|---|---:|
| `valid_request` | *"open a current account of 5000 for customer 1"* | one correct account | **10/10** |
| `unknown_customer` | *"open a current account of 5000 for customer 99"* | nothing created | **10/10** |
| `invalid_type` | *"open a crypto wallet account of 5000 for customer 1"* | nothing created | **10/10** |

### Outcomes in detail

| Scenario | Outcome | Runs |
|---|---|---:|
| `valid_request` | correct | 10 |
| `unknown_customer` | refused | 10 |
| `invalid_type` | refused | 10 |

**Reading the outcomes**

- `correct` - exactly one account, right type, right owner.
- `refused` - nothing was written, which is the intended result for the
  two guardrail scenarios.
- `substituted` - an account *was* created, so the arguments passed
  validation: the model silently turned an impossible request into a
  possible one. The service behaved correctly; the model did not.
