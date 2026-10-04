# Main CI validation repair checklist

- [x] Inspect failed main and passing PR runs; identify event and CodeQL scope differences.
- [x] Start `fix/main-ci-validation` from current main with a clean checkout.
- [x] Add and observe failing regression tests.
- [x] Repair frontend push events and enforce full CodeQL scope.
- [x] Move callback validation into AuthService without weakening authentication.
- [x] Pass helper tests, actionlint, formatting, backend tests/build/coverage.
- [ ] Complete independent code review and hosted PR validation.
- [ ] Update verification evidence and archive completed plan/checklist.
