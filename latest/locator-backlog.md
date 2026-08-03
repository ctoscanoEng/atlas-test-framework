# Locator maintenance backlog

The following elements could not be found with their primary strategy during the
last run. The suite stayed green thanks to the declared fallbacks, but each entry
is technical debt: the primary strategy must be repaired or removed.

| Element | Times healed | Recovered with |
|---|---:|---|
| Checkout details: continue | 3 | `By.cssSelector: [data-testid='continue']` |
| Checkout review: place order | 1 | `By.cssSelector: [data-testid='place-order']` |
| Cart: proceed to checkout | 5 | `By.cssSelector: [data-testid='checkout']` |
| Login: sign in button | 19 | `By.cssSelector: [data-testid='login-submit']` |
